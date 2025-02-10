package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.entity.Sale;
import com.halilsahin.stockautomation.entity.SaleItem;
import com.halilsahin.stockautomation.exception.InsufficientStockException;
import com.halilsahin.stockautomation.exception.ProductNotFoundException;
import com.halilsahin.stockautomation.repository.SaleItemRepository;
import com.halilsahin.stockautomation.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SaleService {
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductService productService;
    private final TransactionService transactionService;

    public SaleService(SaleRepository saleRepository, 
                      SaleItemRepository saleItemRepository,
                      ProductService productService,
                      TransactionService transactionService) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productService = productService;
        this.transactionService = transactionService;
    }

    public Sale completeSale(List<SaleItem> saleItems, double total, double discountRate, double finalTotal) {
        Sale sale = new Sale();
        sale.setDate(LocalDateTime.now());
        sale.setTotal(total);
        sale.setDiscountRate(discountRate);
        sale.setFinalTotal(finalTotal);
        sale = saleRepository.save(sale);

        // Önce tüm ürünleri kaydet ve stokları güncelle
        for (SaleItem saleItem : saleItems) {
            saleItem.setSale(sale);
            saleItemRepository.save(saleItem);
            productService.decreaseStock(saleItem.getProduct(), saleItem.getQuantity());
        }
            
        // Tek bir transaction kaydı oluştur
        transactionService.createSaleTransaction(sale, saleItems);

        return sale;
    }

    public SaleItem addItemToSale(String barcode, int quantity, List<SaleItem> currentItems) {
        Product product = productService.findByBarcode(barcode);
        if (product == null) {
            throw new ProductNotFoundException("Ürün bulunamadı: " + barcode);
        }

        // Sepette aynı ürün var mı kontrol et
        SaleItem existingItem = currentItems.stream()
            .filter(item -> item.getProduct().getBarcode().equals(barcode))
            .findFirst()
            .orElse(null);

        if (existingItem != null) {
            // Toplam miktarı hesapla
            int totalQuantity = existingItem.getQuantity() + quantity;
            
            // Stok kontrolü
            if (product.getStock() < totalQuantity) {
                throw new InsufficientStockException("Yetersiz stok! En fazla " + 
                    product.getStock() + " adet ekleyebilirsiniz.");
            }
            
            // Mevcut ürünün miktarını güncelle
            existingItem.setQuantity(totalQuantity);
            existingItem.setSubtotal(product.getPrice() * totalQuantity);
            return existingItem;
        }

        // Yeni ürün için stok kontrolü
        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Yetersiz stok!");
        }

        SaleItem saleItem = new SaleItem();
        saleItem.setProduct(product);
        saleItem.setQuantity(quantity);
        saleItem.setUnitPrice(product.getPrice());
        saleItem.setSubtotal(product.getPrice() * quantity);

        return saleItem;
    }

    public void updateItemQuantity(String barcode, int quantity, List<SaleItem> saleItems) {
        SaleItem item = saleItems.stream()
            .filter(saleItem -> saleItem.getProduct().getBarcode().equals(barcode))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı: " + barcode));

        Product product = item.getProduct();
        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Yetersiz stok! En fazla " + 
                product.getStock() + " adet ekleyebilirsiniz.");
        }

        item.setQuantity(quantity);
        item.setSubtotal(item.getUnitPrice() * quantity);
    }
} 
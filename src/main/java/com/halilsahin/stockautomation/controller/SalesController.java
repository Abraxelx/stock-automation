package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.constants.Constants;
import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.entity.Sale;
import com.halilsahin.stockautomation.entity.SaleItem;
import com.halilsahin.stockautomation.repository.ProductRepository;
import com.halilsahin.stockautomation.repository.SaleItemRepository;
import com.halilsahin.stockautomation.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/sales")
public class SalesController {
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;

    private List<SaleItem> saleItems = new ArrayList<>(); // Geçici satış listesi
    private double total = 0; // Toplam tutar

    @Autowired
    public SalesController(ProductRepository productRepository, SaleRepository saleRepository, SaleItemRepository saleItemRepository) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
    }

    // Satış Sayfasını Görüntüleme
    @GetMapping
    public String showSalesPage(Model model) {
        model.addAttribute("saleItems", saleItems);
        model.addAttribute("total", total);
        return Constants.SALES;
    }

    // Sepete Ürün Ekleme
    @PostMapping
    public String addItemToSale(@RequestParam String barcode, @RequestParam int quantity, Model model) {
        // Barkoda göre ürünü bul
        Product product = productRepository.findByBarcode(barcode);
        if (product == null) {
            model.addAttribute("error", "Ürün bulunamadı!");
            return Constants.SALES;
        }

        // Stok kontrolü
        if (product.getStock() < quantity) {
            model.addAttribute("error", "Yetersiz stok!");
            return Constants.SALES;
        }

        // SaleItem oluştur ve sepete ekle
        SaleItem saleItem = new SaleItem();
        saleItem.setProduct(product);
        saleItem.setQuantity(quantity);
        saleItem.setUnitPrice(product.getPrice());
        saleItem.setSubtotal(product.getPrice() * quantity);

        saleItems.add(saleItem);

        // Toplam tutarı güncelle
        total += saleItem.getSubtotal();

        // Görünümü güncelle
        model.addAttribute("saleItems", saleItems);
        model.addAttribute("total", total);
        return Constants.SALES;
    }

    // Sepetten Ürün Silme
    @GetMapping("/item/{id}")
    public String removeItemFromSale(@PathVariable int id, Model model) {
        // Listeden ilgili ürünü çıkar
        if (id > 0 && id <= saleItems.size()) {
            SaleItem saleItem = saleItems.get(id - 1);
            total -= saleItem.getSubtotal();
            saleItems.remove(id - 1);
        }

        // Görünümü güncelle
        model.addAttribute("saleItems", saleItems);
        model.addAttribute("total", total);
        return "redirect:/sales";
    }

    // Satışı Tamamlama
    @PostMapping("/complete")
    public String completeSale() {
        // Yeni bir satış kaydı oluştur
        Sale sale = new Sale();
        sale.setDate(LocalDateTime.now());
        sale.setTotal(total);
        sale = saleRepository.save(sale); // Veritabanına kaydet

        // Her bir SaleItem için ilişkiyi kaydet
        for (SaleItem saleItem : saleItems) {
            saleItem.setSale(sale); // Satış ilişkisini bağla
            saleItemRepository.save(saleItem);

            // Stok güncelle
            Product product = saleItem.getProduct();
            product.setStock(product.getStock() - saleItem.getQuantity());
            productRepository.save(product);
        }

        // Geçici listeyi ve toplamı sıfırla
        saleItems.clear();
        total = 0;

        return "redirect:/sales";
    }

    // Sepeti Temizleme
    @GetMapping("/reset")
    public String resetSale() {
        saleItems.clear();
        total = 0;
        return "redirect:/sales";
    }
}


package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.entity.SaleItem;
import java.math.BigDecimal;
import java.util.*;

public class SaleServiceTest {
    public static void main(String[] args) {
        // NOT: Gerçek projede Spring context ile bean alınmalı, burada örnek amaçlıdır.
        ProductService productService = null; // Bean olarak al
        SaleService saleService = null;       // Bean olarak al

        List<SaleItem> items = new ArrayList<>();
        Random random = new Random();

        // 40 farklı ürün için test
        for (int i = 0; i < 40; i++) {
            String barcode = "TESTBARCODE" + i;
            int quantity = random.nextInt(10) + 1; // 1-10 arası rastgele miktar

            // Ürünü oluştur ve stok bırak
            Product product = null;
            try {
                product = productService.findByBarcode(barcode);
            } catch (Exception ignored) {}
            if (product == null) {
                product = new Product();
                product.setBarcode(barcode);
                product.setName("Test Ürün " + i);
                product.setStock(quantity + 10); // Yeterli stok
                product.setPrice(BigDecimal.valueOf(10 + i)); // Farklı fiyatlar
                productService.save(product);
            } else {
                product.setStock(quantity + 10);
                productService.save(product);
            }

            SaleItem item = new SaleItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            items.add(item);
        }

        // Toplam ve finalTotal hesapla
        double total = items.stream().mapToDouble(i -> i.getSubtotal().doubleValue()).sum();
        double discountRate = 0;
        double finalTotal = total;

        // Satış işlemini test et
        try {
            saleService.completeSale(items, total, discountRate, finalTotal);
            System.out.println("Satış başarıyla tamamlandı!");
        } catch (Exception e) {
            System.err.println("Satış sırasında hata: " + e.getMessage());
        }

        // Son kontrol: stoklar doğru azaldı mı?
        for (SaleItem item : items) {
            Product p = productService.findByBarcode(item.getProduct().getBarcode());
            int beklenenStok = (item.getQuantity() + 10) - item.getQuantity();
            if (p.getStock() != beklenenStok) {
                System.err.println("Stok hatası! Barkod: " + p.getBarcode() + " Beklenen: " + beklenenStok + " Gerçek: " + p.getStock());
            }
        }
    }
} 
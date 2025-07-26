package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.entity.SaleItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.*;

@SpringBootTest
public class SaleServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private SaleService saleService;

    @Test
    public void testBulkSaleWith40Products() {
        List<SaleItem> items = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 40; i++) {
            String barcode = "TESTBARCODE" + i;
            int quantity = random.nextInt(10) + 1;

            Product product = productService.findByBarcode(barcode);
            if (product == null) {
                product = new Product();
                product.setBarcode(barcode);
                product.setName("Test Ürün " + i);
                product.setStock(quantity + 10);
                product.setPrice(BigDecimal.valueOf(10 + i));
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

        double total = items.stream().mapToDouble(i -> i.getSubtotal().doubleValue()).sum();
        double discountRate = 0;
        double finalTotal = total;

        saleService.completeSale(items, total, discountRate, finalTotal);

        // Stok kontrolü
        for (SaleItem item : items) {
            Product p = productService.findByBarcode(item.getProduct().getBarcode());
            int beklenenStok = (item.getQuantity() + 10) - item.getQuantity();
            assert p.getStock() == beklenenStok : "Stok hatası! Barkod: " + p.getBarcode();
        }
    }
} 
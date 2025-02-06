package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/stock-report")
public class StockReportController {

    private final ProductService productService;

    @Autowired
    public StockReportController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String showStockReport(Model model) {
        // Toplam stok değerleri
        double totalPurchaseValue = productService.calculateTotalPurchaseValue();
        double totalSaleValue = productService.calculateTotalSaleValue();
        double potentialProfit = totalSaleValue - totalPurchaseValue;
        
        // Ürün listesi
        List<Product> products = productService.findAllByOrderByStockDesc();
        List<Product> criticalProducts = productService.findCriticalStockProducts();
        List<Product> mostValuableProducts = productService.findAllByStockValue()
                .stream().limit(5).collect(Collectors.toList());
        
        model.addAttribute("totalPurchaseValue", totalPurchaseValue);
        model.addAttribute("totalSaleValue", totalSaleValue);
        model.addAttribute("potentialProfit", potentialProfit);
        model.addAttribute("products", products);
        model.addAttribute("criticalProducts", criticalProducts);
        model.addAttribute("mostValuableProducts", mostValuableProducts);
        
        return "stock-report";
    }
} 
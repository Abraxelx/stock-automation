package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class StockReportController {
    private static final int PAGE_SIZE = 20;

    private final ProductService productService;

    @Autowired
    public StockReportController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/stock-report")
    public String showStockReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "stock") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Model model) {

        Sort sort = Sort.by(direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, sort);
        
        Page<Product> productPage = productService.findAllPaged(pageable);
        
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
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("criticalProducts", criticalProducts);
        model.addAttribute("mostValuableProducts", mostValuableProducts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        
        return "stock-report";
    }
} 
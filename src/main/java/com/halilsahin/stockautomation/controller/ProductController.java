package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    // Ürün Ekleme ve Listeleme
    @GetMapping
    public String showProductPage(Model model) {
        model.addAttribute("products", productRepository.findAllByOrderByIdDesc());
        return "products";
    }

    @PostMapping
    public String addProduct(@RequestParam String name,
                             @RequestParam String barcode,
                             @RequestParam int stock,
                             @RequestParam double price,
                             Model model) {
        // Barkod kontrolü
        if (productRepository.findByBarcode(barcode) != null) {
            model.addAttribute("error", "Bu barkod zaten mevcut!");
            model.addAttribute("products", productRepository.findAllByOrderByIdDesc());
            return "products";
        }

        // Yeni ürün ekleme
        Product product = new Product();
        product.setName(name);
        product.setBarcode(barcode);
        product.setStock(stock);
        product.setPrice(price);

        productRepository.save(product);

        model.addAttribute("success", "Ürün başarıyla eklendi!");
        model.addAttribute("products", productRepository.findAllByOrderByIdDesc());
        return "products";
    }
}


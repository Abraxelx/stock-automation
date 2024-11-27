package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.constants.Constants;
import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.ProductRepository;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import com.halilsahin.stockautomation.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final TransactionRepository transactionRepository;

    @Autowired
    public ProductController(ProductRepository productRepository, ProductService productService, TransactionRepository transactionRepository) {
        this.productRepository = productRepository;
        this.productService = productService;
        this.transactionRepository = transactionRepository;
    }
    @GetMapping
    public String showProductPage(Model model) {
        model.addAttribute(Constants.PRODUCTS, productService.findAllByOrderByIdDesc());
        return Constants.PRODUCTS;
    }

    @PostMapping
    public String addProduct(@RequestParam String name,
                             @RequestParam String barcode,
                             @RequestParam String description,
                             @RequestParam int stock,
                             @RequestParam double price,
                             Model model) {
        if (productService.findByBarcode(barcode) != null) {
            model.addAttribute("error", "Bu barkod zaten mevcut!");
            model.addAttribute(Constants.PRODUCTS, productService.findAllByOrderByIdDesc());
            return Constants.PRODUCTS;
        }

        Product product = new Product();
        product.setName(name);
        product.setBarcode(barcode);
        product.setStock(stock);
        product.setPrice(price);

        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.STOCK_IN);
        transaction.setAmount(product.getPrice() * stock);
        transaction.setDate(LocalDateTime.now());
        transaction.setRelatedEntity("Product");
        if(description != null && !description.isEmpty()) {
            transaction.setDescription("ÜRÜN ADI: ".concat(name).concat(description));
        } else {
            transaction.setDescription("ÜRÜN ADI: ".concat(name).concat("GİRİLEN ÜRÜN ADETİ: " + stock ));
        }
        transactionRepository.save(transaction);
        productRepository.save(product);

        model.addAttribute("success", "Ürün başarıyla eklendi!");
        model.addAttribute(Constants.PRODUCTS, productRepository.findAllByOrderByIdDesc());
        return Constants.PRODUCTS;
    }

    @GetMapping("/search")
    public String searchProducts(@RequestParam(value = "keyword", required = false, defaultValue = "") String keyword, Model model) {
        List<Product> products;

        if (keyword.isEmpty()) {
            products = productService.findAllByOrderByIdDesc();
        } else {
            products = productService.searchByNameOrBarcode(keyword);
        }

        model.addAttribute(Constants.PRODUCTS, products);
        model.addAttribute("keyword", keyword);
        return Constants.PRODUCTS;
    }


}


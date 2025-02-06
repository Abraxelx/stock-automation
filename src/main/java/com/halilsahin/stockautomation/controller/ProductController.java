package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.constants.Constants;
import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.enums.UnitType;
import com.halilsahin.stockautomation.repository.ProductRepository;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import com.halilsahin.stockautomation.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @PostMapping("/check-barcode")
    @ResponseBody
    public Map<String, Object> checkBarcode(@RequestParam String barcode) {
        Map<String, Object> response = new HashMap<>();
        Product existingProduct = productService.findByBarcode(barcode);
        
        if (existingProduct != null) {
            response.put("exists", true);
            response.put("productId", existingProduct.getId());
        } else {
            response.put("exists", false);
        }
        
        return response;
    }

    @PostMapping
    public String addProduct(@RequestParam String name,
                             @RequestParam String barcode,
                             @RequestParam String description,
                             @RequestParam int stock,
                             @RequestParam UnitType unitType,
                             @RequestParam double price,
                             @RequestParam double purchasePrice,
                             RedirectAttributes redirectAttributes) {
        Product existingProduct = productService.findByBarcode(barcode);
        if (existingProduct != null) {
            redirectAttributes.addFlashAttribute("error", 
                "Bu barkod zaten mevcut! Ürün düzenleme sayfasına yönlendiriliyorsunuz.");
            return "redirect:/products/edit/" + existingProduct.getId();
        }

        Product product = new Product();
        product.setName(name);
        product.setBarcode(barcode);
        product.setStock(stock);
        product.setUnitType(unitType);
        product.setPrice(price);
        product.setPurchasePrice(purchasePrice);

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

        redirectAttributes.addFlashAttribute("success", "Ürün başarıyla eklendi!");
        redirectAttributes.addFlashAttribute(Constants.PRODUCTS, productRepository.findAllByOrderByIdDesc());
        return "redirect:/products";
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

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.findById(id);
            model.addAttribute("product", product);
            return "edit-product"; // HTML dosyasının adı
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ürün bulunamadı.");
            return "redirect:/products";
        }
    }


    @PostMapping("/update")
    public String updateProduct(@ModelAttribute("product") Product product, RedirectAttributes redirectAttributes) {
        try {
            productService.updateProduct(product);
            redirectAttributes.addFlashAttribute("success", "Ürün başarıyla güncellendi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ürün güncellenirken bir hata oluştu.");
        }
        return "redirect:/products";
    }


    @PostMapping("/update/{id}")
    public String updateProduct(@PathVariable Long id, @ModelAttribute("product") Product updatedProduct,
                                RedirectAttributes redirectAttributes) {
        productService.updateProduct(updatedProduct);
        redirectAttributes.addFlashAttribute("success", "Ürün başarıyla güncellendi.");
        return "redirect:/products";
    }


    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Ürün başarıyla silindi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ürün silinirken bir hata oluştu.");
        }
        return "redirect:/products";
    }



}


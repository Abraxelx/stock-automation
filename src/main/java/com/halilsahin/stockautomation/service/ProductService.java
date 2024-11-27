package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    public List<Product> findAllByOrderByIdDesc() {
        return productRepository.findAllByOrderByIdDesc();
    }

    public Product findByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode);
    }

    public List<Product> searchByNameOrBarcode(String keyword) {
        List<Product> products = new ArrayList<>();
        if (keyword != null && !keyword.isEmpty()) {
            if (keyword.length() == 13 && keyword.matches("\\d+")) {
                products.add(productRepository.findByBarcode(keyword));
                return products;
            } else {
                return productRepository.findByNameContainingIgnoreCase(keyword);
            }
        }
        return products;
    }
}

package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product findByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode);
    }
}

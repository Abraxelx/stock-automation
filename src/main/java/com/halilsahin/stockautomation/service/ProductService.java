package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.exception.InsufficientStockException;
import com.halilsahin.stockautomation.exception.ProductNotFoundException;
import com.halilsahin.stockautomation.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
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
        return productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException("Ürün bulunamadı: " + id));
    }

    public void updateProduct(Product updatedProduct) {
        Product existingProduct = findById(updatedProduct.getId());
        existingProduct.setBarcode(updatedProduct.getBarcode());
        existingProduct.setName(updatedProduct.getName());
        existingProduct.setStock(updatedProduct.getStock());
        existingProduct.setPurchasePrice(updatedProduct.getPurchasePrice());
        existingProduct.setPrice(updatedProduct.getPrice());
        productRepository.save(existingProduct);
    }

    public void deleteById(Long id) {
        productRepository.deleteById(id);
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

    public double calculateTotalPurchaseValue() {
        return productRepository.findAll().stream()
                .mapToDouble(p -> p.getPurchasePrice() * p.getStock())
                .sum();
    }

    public double calculateTotalSaleValue() {
        return productRepository.findAll().stream()
                .mapToDouble(p -> p.getPrice() * p.getStock())
                .sum();
    }

    public List<Product> findAllByOrderByStockDesc() {
        return productRepository.findAllByOrderByStockDesc();
    }

    public List<Product> findCriticalStockProducts() {
        return productRepository.findAll().stream()
                .filter(p -> p.getStock() <= 5)
                .collect(Collectors.toList());
    }

    public List<Product> findAllByStockValue() {
        return productRepository.findAll().stream()
                .sorted((p1, p2) -> {
                    double value1 = p1.getStock() * p1.getPrice();
                    double value2 = p2.getStock() * p2.getPrice();
                    return Double.compare(value2, value1);
                })
                .collect(Collectors.toList());
    }

    public void decreaseStock(Product product, int quantity) {
        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Yetersiz stok! Mevcut stok: " + product.getStock());
        }
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }

    public void save(Product product) {
        productRepository.save(product);
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}

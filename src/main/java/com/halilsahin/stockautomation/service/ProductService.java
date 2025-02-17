package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.exception.InsufficientStockException;
import com.halilsahin.stockautomation.exception.ProductNotFoundException;
import com.halilsahin.stockautomation.repository.ProductRepository;
import com.halilsahin.stockautomation.transaction.TransactionContext;
import com.halilsahin.stockautomation.transaction.TransactionHandler;
import com.halilsahin.stockautomation.transaction.TransactionHandlerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final TransactionHandlerFactory transactionHandlerFactory;

    @Autowired
    public ProductService(ProductRepository productRepository,
                        TransactionHandlerFactory transactionHandlerFactory) {
        this.productRepository = productRepository;
        this.transactionHandlerFactory = transactionHandlerFactory;
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
            .map(p -> p.getPurchasePrice().multiply(BigDecimal.valueOf(p.getStock())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .doubleValue();
    }

    public double calculateTotalSaleValue() {
        return productRepository.findAll().stream()
            .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getStock())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .doubleValue();
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
                BigDecimal value1 = p1.getPrice() != null ? 
                    p1.getPrice().multiply(BigDecimal.valueOf(p1.getStock())) : 
                    BigDecimal.ZERO;
                BigDecimal value2 = p2.getPrice() != null ? 
                    p2.getPrice().multiply(BigDecimal.valueOf(p2.getStock())) : 
                    BigDecimal.ZERO;
                return value2.compareTo(value1);
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

    public void updateStock(Product product, int quantity) {
        product.setStock(product.getStock() + quantity);
        productRepository.save(product);

        BigDecimal amount = product.getPrice() != null ? 
            product.getPrice().multiply(BigDecimal.valueOf(Math.abs(quantity))) : 
            BigDecimal.ZERO;

        TransactionContext context = TransactionContext.builder()
            .relatedEntity("Product")
            .amount(amount.doubleValue())
            .date(LocalDateTime.now())
            .additionalData(Map.of(
                "product", product,
                "quantity", quantity,
                "transactionType", quantity > 0 ? TransactionType.STOCK_IN : TransactionType.STOCK_OUT
            ))
            .build();

        TransactionHandler handler = transactionHandlerFactory.getHandler(
            quantity > 0 ? TransactionType.STOCK_IN : TransactionType.STOCK_OUT
        );
        handler.handleTransaction(context);
    }

    public Page<Product> findAllPaged(Pageable pageable) {
        return productRepository.findAll(pageable);
    }
}

package com.halilsahin.stockautomation.repository;

import com.halilsahin.stockautomation.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findByBarcode(String barcode);
    List<Product> findAllByOrderByIdDesc();
    List<Product> findByNameContainingIgnoreCase(String name);
}

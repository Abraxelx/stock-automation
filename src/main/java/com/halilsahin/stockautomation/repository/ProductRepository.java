package com.halilsahin.stockautomation.repository;

import com.halilsahin.stockautomation.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findByBarcode(String barcode);
    List<Product> findAllByOrderByIdDesc();
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findAllByOrderByStockDesc();

    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "p.barcode LIKE CONCAT('%', :keyword, '%')")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);
}

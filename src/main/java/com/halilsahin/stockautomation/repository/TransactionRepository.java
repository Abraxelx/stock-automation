package com.halilsahin.stockautomation.repository;

import com.halilsahin.stockautomation.entity.Customer;
import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByOrderByDateDesc();
    Page<Transaction> findAll(Pageable pageable);
    Page<Transaction> findByTypeAndDateBetween(
            TransactionType type, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<Transaction> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    List<Transaction> findByProductOrderByDateDesc(Product product);
    List<Transaction> findByCustomerOrderByDateDesc(Customer customer);
}

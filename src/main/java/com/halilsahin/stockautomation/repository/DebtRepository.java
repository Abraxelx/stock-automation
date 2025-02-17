package com.halilsahin.stockautomation.repository;

import com.halilsahin.stockautomation.entity.Debt;
import com.halilsahin.stockautomation.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {
    List<Debt> findByIsPaid(boolean isPaid);
    List<Debt> findByCustomerFirstNameContainingIgnoreCase(String customerName);
    List<Debt> findByCustomerOrderByDueDateDesc(Customer customer);
    List<Debt> findByIsPaidFalseAndDueDateBefore(LocalDateTime date);
    List<Debt> findByIsPaidFalseAndDueDateBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT d FROM Debt d ORDER BY d.isPaid ASC, d.dueDate DESC")
    List<Debt> findAllOrderByIsPaidAndDueDateDesc();
}

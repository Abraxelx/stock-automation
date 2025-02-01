package com.halilsahin.stockautomation.repository;

import com.halilsahin.stockautomation.entity.Debt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {
    List<Debt> findDebtsByDebtorFirstNameContainsIgnoreCase(String customerName); // Müşteriye göre arama
    List<Debt> findByIsPaid(boolean isPaid); // Ödeme durumuna göre filtreleme
}

package com.halilsahin.stockautomation.repository;

import com.halilsahin.stockautomation.entity.DebtProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DebtProductRepository extends JpaRepository<DebtProduct, Long> {
    List<DebtProduct> findByDebtId(Long debtId);
} 
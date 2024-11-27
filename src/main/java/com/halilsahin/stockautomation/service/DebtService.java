package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Debt;
import com.halilsahin.stockautomation.repository.DebtRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DebtService {

    private final DebtRepository debtRepository;

    public DebtService(DebtRepository debtRepository) {
        this.debtRepository = debtRepository;
    }

    // Yeni borç kaydı ekleme
    public Debt addDebt(Debt debt) {
        return debtRepository.save(debt);
    }

    // Ödemesi yapılan borçları güncelleme
    public Debt markAsPaid(Long debtId) {
        Debt debt = debtRepository.findById(debtId).orElseThrow();
        debt.setPaid(true);
        return debtRepository.save(debt);
    }

    // Tüm borçları listeleme
    public List<Debt> getAllDebts() {
        return debtRepository.findAll();
    }

    // Müşteriye göre borç arama
    public List<Debt> findDebtsByCustomerName(String customerName) {
        return debtRepository.findByCustomerFirstNameContaining(customerName);
    }

    // Ödeme durumu ile filtreleme
    public List<Debt> findDebtsByPaymentStatus(boolean isPaid) {
        return debtRepository.findByIsPaid(isPaid);
    }
}

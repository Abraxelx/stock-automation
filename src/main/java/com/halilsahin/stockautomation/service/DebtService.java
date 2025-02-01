package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Debt;
import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.DebtRepository;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DebtService {

    private final DebtRepository debtRepository;
    private final TransactionRepository transactionRepository;


    public DebtService(DebtRepository debtRepository, TransactionRepository transactionRepository) {
        this.debtRepository = debtRepository;
        this.transactionRepository = transactionRepository;
    }

    // Yeni borç kaydı ekleme
    public Debt addDebt(Debt debt) {
        return debtRepository.save(debt);
    }

    // Ödemesi yapılan borçları güncelleme
    public Debt markAsPaid(Long debtId) {
        Debt debt = debtRepository.findById(debtId).orElseThrow();
        debt.setPaid(true);
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.DEBT_RE_PAYMENT);
        return debtRepository.save(debt);
    }

    // Tüm borçları listeleme
    public List<Debt> getAllDebts() {
        return debtRepository.findAll();
    }

    // Müşteriye göre borç arama
    public List<Debt> findDebtsByCustomerName(String customerName) {
        return debtRepository.findDebtsByDebtorFirstNameContainsIgnoreCase(customerName);
    }

    // Ödeme durumu ile filtreleme
    public List<Debt> findDebtsByPaymentStatus(boolean isPaid) {
        return debtRepository.findByIsPaid(isPaid);
    }

    public Debt findDebtById(Long debtId) {
        return debtRepository.findById(debtId).orElseThrow();
    }
}

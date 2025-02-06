package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Debt;
import com.halilsahin.stockautomation.entity.Installment;
import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.DebtRepository;
import com.halilsahin.stockautomation.repository.InstallmentRepository;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DebtService {

    private final DebtRepository debtRepository;
    private final TransactionRepository transactionRepository;
    private final InstallmentRepository installmentRepository;

    public DebtService(DebtRepository debtRepository, TransactionRepository transactionRepository, InstallmentRepository installmentRepository) {
        this.debtRepository = debtRepository;
        this.transactionRepository = transactionRepository;
        this.installmentRepository = installmentRepository;
    }

    // Yeni borç kaydı ekleme (taksitler cascade sayesinde birlikte kaydedilir)
    public Debt addDebt(Debt debt) {
        return debtRepository.save(debt);
    }

    // Ödemesi yapılan borcu güncelle ve varsa tüm taksitleri de ödenmiş olarak işaretle
    public Debt markAsPaid(Long debtId) {
        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new RuntimeException("Borç bulunamadı: " + debtId));
        debt.setPaid(true);
        // Eğer borcun taksitleri varsa, tüm taksitleri de ödenmiş olarak işaretle
        if (debt.getInstallments() != null) {
            for (Installment inst : debt.getInstallments()) {
                inst.setPaid(true);
                inst.setPaymentDate(debt.getPaymentDate()); // Borcun ödeme tarihini taksitlere de uygula (isteğe bağlı)
            }
        }
        // Ödeme işlemi için ayrıca bir Transaction oluşturulabilir (işlem burada yapılmamışsa controller üzerinden de yapılabilir)
        Transaction transaction = new Transaction();
        transaction.setDate(debt.getPaymentDate());
        transaction.setDescription("Borç ödemesi yapıldı: " + debt.getAmount() + " TL");
        transaction.setAmount(debt.getAmount());
        transaction.setTransactionType(TransactionType.DEBT_OUT);
        transactionRepository.save(transaction);
        return debtRepository.save(debt);
    }

    // Tüm borçları listeleme
    public List<Debt> getAllDebts() {
        return debtRepository.findAll();
    }

    // Ödenmemiş (pending) borçları döndürme
    public List<Debt> getPendingDebts() {
        return debtRepository.findByIsPaid(false);
    }

    // Müşteriye göre borç arama
    public List<Debt> findDebtsByCustomerName(String customerName) {
        return debtRepository.findDebtsByDebtorFirstNameContainsIgnoreCase(customerName);
    }

    // Ödeme durumu ile filtreleme
    public List<Debt> findDebtsByPaymentStatus(boolean isPaid) {
        return debtRepository.findByIsPaid(isPaid);
    }

    // Belirtilen ID'ye sahip borcu döndürme
    public Debt findDebtById(Long debtId) {
        return debtRepository.findById(debtId)
                .orElseThrow(() -> new RuntimeException("Borç bulunamadı: " + debtId));
    }


    public Installment findInstallmentById(Long id) {
        return installmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Taksit Seçeneği Bulunamadı" + id));
    }

    public void saveInstallment(Installment installment) {
        installmentRepository.save(installment);
    }
}

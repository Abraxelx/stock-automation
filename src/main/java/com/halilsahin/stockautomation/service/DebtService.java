package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Debt;
import com.halilsahin.stockautomation.entity.Installment;
import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.enums.PaymentMethod;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.DebtRepository;
import com.halilsahin.stockautomation.repository.InstallmentRepository;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

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
        // Ödeme işlemi için ayrıca bir Transaction oluşturulabilir
        Transaction transaction = Transaction.createDebtTransaction(
            debt.getDebtor(),
            BigDecimal.valueOf(-debt.getAmount()),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
        transaction.setDescription("Borç ödemesi yapıldı: " + debt.getAmount() + " TL");
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

    public void payDebt(Long debtId, PaymentMethod paymentMethod) {
        Debt debt = findDebtById(debtId);
        debt.setPaid(true);
        debt.setPaymentDate(LocalDateTime.now());
        debt.setPaymentMethod(paymentMethod);
       
        // Taksitleri de ödenmiş olarak işaretle
        if (debt.getInstallments() != null) {
            for (Installment inst : debt.getInstallments()) {
                inst.setPaid(true);
                inst.setPaymentDate(debt.getPaymentDate());
            }
        }
       
        // Ödeme işlemi için Transaction kaydı
        Transaction transaction = Transaction.createDebtTransaction(
            debt.getDebtor(),
            BigDecimal.valueOf(-debt.getAmount()), // negative for payment
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
        transaction.setDescription(String.format("Borç Ödemesi - %s: %s TL (%s)", 
            debt.getDebtor().getFirstName() + " " + debt.getDebtor().getLastName(),
            debt.getAmount(),
            paymentMethod.getDisplayName()));
        transactionRepository.save(transaction);
       
        debtRepository.save(debt);
    }

    // Vadesi geçen borçları getir
    public List<Debt> getOverdueDebts() {
        return debtRepository.findByIsPaidFalseAndDueDateBefore(LocalDateTime.now());
    }

    // Vadesi yaklaşan borçları getir (3 gün)
    public List<Debt> getUpcomingDebts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysLater = now.plusDays(3);
        return debtRepository.findByIsPaidFalseAndDueDateBetween(now, threeDaysLater);
    }

    // Borç istatistiklerini getir
    public Map<String, Object> getDebtStatistics() {
        Map<String, Object> stats = new HashMap<>();
        List<Debt> allDebts = getAllDebts();
        
        double totalDebtAmount = allDebts.stream()
            .mapToDouble(Debt::getAmount)
            .sum();
            
        double paidDebtAmount = allDebts.stream()
            .filter(Debt::isPaid)
            .mapToDouble(Debt::getAmount)
            .sum();
            
        double unpaidDebtAmount = totalDebtAmount - paidDebtAmount;
        
        stats.put("totalDebtAmount", totalDebtAmount);
        stats.put("paidDebtAmount", paidDebtAmount);
        stats.put("unpaidDebtAmount", unpaidDebtAmount);
        stats.put("overdueDebts", getOverdueDebts());
        stats.put("upcomingDebts", getUpcomingDebts());
        
        return stats;
    }
}

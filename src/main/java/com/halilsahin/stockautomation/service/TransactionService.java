package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.entity.Sale;
import com.halilsahin.stockautomation.entity.SaleItem;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import com.halilsahin.stockautomation.transaction.TransactionHandler;
import com.halilsahin.stockautomation.transaction.TransactionHandlerFactory;
import com.halilsahin.stockautomation.transaction.TransactionContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionHandlerFactory handlerFactory;

    public TransactionService(TransactionRepository transactionRepository, TransactionHandlerFactory handlerFactory) {
        this.transactionRepository = transactionRepository;
        this.handlerFactory = handlerFactory;
    }

    public Page<Transaction> findTransactions(String type, String startDate, String endDate, Pageable pageable) {
        if (type != null && !type.isEmpty() || startDate != null && !startDate.isEmpty() || endDate != null && !endDate.isEmpty()) {
            LocalDateTime start = startDate != null && !startDate.isEmpty() ? 
                LocalDate.parse(startDate).atStartOfDay() : LocalDate.of(2000, 1, 1).atStartOfDay();
            LocalDateTime end = endDate != null && !endDate.isEmpty() ? 
                LocalDate.parse(endDate).atTime(23, 59, 59) : LocalDateTime.now().plusYears(10);
            
            if (type != null && !type.isEmpty()) {
                return transactionRepository.findByTypeAndDateBetween(
                    TransactionType.valueOf(type), start, end, pageable);
            }
            return transactionRepository.findByDateBetween(start, end, pageable);
        }
        return transactionRepository.findAll(pageable);
    }

    public void createSaleTransaction(Sale sale, List<SaleItem> saleItems) {
        Transaction transaction = Transaction.createSaleTransaction(
            sale, 
            BigDecimal.valueOf(sale.getFinalTotal())
        );
        
        StringBuilder description = new StringBuilder();
        description.append("Satılan Ürünler: ");
        
        for (int i = 0; i < saleItems.size(); i++) {
            SaleItem item = saleItems.get(i);
            description.append(item.getQuantity())
                .append(" Adet ")
                .append(item.getProduct().getName());
            
            if (i < saleItems.size() - 1) {
                description.append(", ");
            }
        }
        
        String discountInfo = sale.getDiscountRate() > 0 ? 
            String.format(" (Toplam %%%.1f iskonto uygulandı)", sale.getDiscountRate()) : "";
        description.append(discountInfo);
        
        transaction.setDescription(description.toString());
        
        transactionRepository.save(transaction);
    }

    public void processTransaction(TransactionType type, TransactionContext context) {
        TransactionHandler handler = handlerFactory.getHandler(type);
        handler.handleTransaction(context);
    }

    public void save(Transaction transaction) {
        transactionRepository.save(transaction);
    }

    public Transaction findById(Long id) {
        return transactionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("İşlem bulunamadı: " + id));
    }

    /**
     * Toplam işlem sayısını döndürür
     */
    public long countAllTransactions() {
        return transactionRepository.count();
    }
    
    /**
     * Belirli tarih aralığındaki işlem sayısını döndürür
     */
    public long countTransactionsByDateRange(LocalDateTime start, LocalDateTime end) {
        return transactionRepository.findAll().stream()
            .filter(t -> t.getDate().isAfter(start) && t.getDate().isBefore(end))
            .count();
    }
    
    /**
     * En son yapılan işlemleri döndürür
     */
    public List<Transaction> findRecentTransactions(int limit) {
        return transactionRepository.findAllByOrderByDateDesc().stream()
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * En yüksek tutarlı işlemleri döndürür
     */
    public List<Transaction> findHighestAmountTransactions(int limit) {
        return transactionRepository.findAll().stream()
            .sorted((t1, t2) -> t2.getAmount().compareTo(t1.getAmount()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Günlük işlem toplamını hesaplar
     */
    public BigDecimal calculateDailyTotal() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        
        return transactionRepository.findAll().stream()
            .filter(t -> t.getDate().isAfter(startOfDay) && t.getDate().isBefore(endOfDay))
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // İki tarih arasındaki tüm işlemleri getiren metod
    public List<Transaction> findAllTransactionsBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByDateBetween(startDate, endDate, Pageable.unpaged()).getContent();
    }
    
    // Filtreleme parametrelerine göre işlemleri getiren metod
    public List<Transaction> findFilteredTransactions(String type, String startDate, String endDate, 
                                                      Double minAmount, Double maxAmount) {
        // Tarihleri dönüştür
        LocalDateTime start = startDate != null && !startDate.isEmpty() ? 
            LocalDate.parse(startDate).atStartOfDay() : LocalDate.of(2000, 1, 1).atStartOfDay();
        LocalDateTime end = endDate != null && !endDate.isEmpty() ? 
            LocalDate.parse(endDate).atTime(23, 59, 59) : LocalDateTime.now().plusYears(10);
        
        // Tüm işlemleri getir ve manuel filtrele
        List<Transaction> transactions;
        
        if (type != null && !type.isEmpty()) {
            transactions = transactionRepository.findByTypeAndDateBetween(
                TransactionType.valueOf(type), start, end, Pageable.unpaged()).getContent();
        } else {
            transactions = transactionRepository.findByDateBetween(start, end, Pageable.unpaged()).getContent();
        }
        
        // Tutar filtresi varsa uygula
        if (minAmount != null || maxAmount != null) {
            BigDecimal min = minAmount != null ? new BigDecimal(minAmount) : BigDecimal.ZERO;
            BigDecimal max = maxAmount != null ? new BigDecimal(maxAmount) : new BigDecimal("999999999");
            
            transactions = transactions.stream()
                .filter(t -> t.getAmount().compareTo(min) >= 0 && t.getAmount().compareTo(max) <= 0)
                .collect(Collectors.toList());
        }
        
        return transactions;
    }
}

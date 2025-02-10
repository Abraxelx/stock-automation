package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.entity.Sale;
import com.halilsahin.stockautomation.entity.SaleItem;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Page<Transaction> findTransactions(String type, String startDate, String endDate, Pageable pageable) {
        if (type != null && !type.isEmpty() || startDate != null && !startDate.isEmpty() || endDate != null && !endDate.isEmpty()) {
            LocalDateTime start = startDate != null && !startDate.isEmpty() ? 
                LocalDate.parse(startDate).atStartOfDay() : LocalDate.of(2000, 1, 1).atStartOfDay();
            LocalDateTime end = endDate != null && !endDate.isEmpty() ? 
                LocalDate.parse(endDate).atTime(23, 59, 59) : LocalDateTime.now().plusYears(10);
            
            if (type != null && !type.isEmpty()) {
                return transactionRepository.findByTransactionTypeAndDateBetween(
                    TransactionType.valueOf(type), start, end, pageable);
            }
            return transactionRepository.findByDateBetween(start, end, pageable);
        }
        return transactionRepository.findAll(pageable);
    }

    public void createSaleTransaction(Sale sale, List<SaleItem> saleItems) {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.SALE);
        transaction.setAmount(sale.getFinalTotal());
        transaction.setRelatedEntity("SALE");
        transaction.setDate(LocalDateTime.now());
        
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
}

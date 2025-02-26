package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Customer;
import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.entity.Sale;
import com.halilsahin.stockautomation.entity.SaleItem;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import com.halilsahin.stockautomation.transaction.TransactionHandler;
import com.halilsahin.stockautomation.transaction.TransactionHandlerFactory;
import com.halilsahin.stockautomation.transaction.TransactionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionHandlerFactory handlerFactory;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, TransactionHandlerFactory handlerFactory) {
        this.transactionRepository = transactionRepository;
        this.handlerFactory = handlerFactory;
    }

    public Page<Transaction> findTransactions(String type, String startDate, String endDate, Pageable pageable) {
        Specification<Transaction> spec = createSpecification(type, startDate, endDate, null, null, null, null);
        return transactionRepository.findAll(spec, pageable);
    }

    public Page<Transaction> findTransactionsAdvanced(
            String type, 
            String startDate, 
            String endDate, 
            Long customerId, 
            Long productId, 
            BigDecimal minAmount, 
            BigDecimal maxAmount, 
            Pageable pageable) {
        
        Specification<Transaction> spec = createSpecification(
            type, startDate, endDate, customerId, productId, minAmount, maxAmount);
        
        return transactionRepository.findAll(spec, pageable);
    }

    public List<Transaction> findTransactionsForExport(
            String type, 
            String startDate, 
            String endDate, 
            Long customerId, 
            Long productId) {
        
        Specification<Transaction> spec = createSpecification(
            type, startDate, endDate, customerId, productId, null, null);
        
        return transactionRepository.findAll(spec);
    }

    private Specification<Transaction> createSpecification(
            String type, 
            String startDate, 
            String endDate, 
            Long customerId, 
            Long productId, 
            BigDecimal minAmount, 
            BigDecimal maxAmount) {
        
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // İşlem türü filtresi
            if (type != null && !type.isEmpty()) {
                try {
                    TransactionType transactionType = TransactionType.valueOf(type);
                    predicates.add(criteriaBuilder.equal(root.get("type"), transactionType));
                } catch (IllegalArgumentException e) {
                    // Geçersiz tür, filtre uygulanmaz
                }
            }

            // Tarih aralığı filtresi
            if (startDate != null && !startDate.isEmpty()) {
                LocalDateTime startDateTime = LocalDate.parse(startDate, formatter).atStartOfDay();
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("date"), startDateTime));
            }

            if (endDate != null && !endDate.isEmpty()) {
                LocalDateTime endDateTime = LocalDate.parse(endDate, formatter).plusDays(1).atStartOfDay();
                predicates.add(criteriaBuilder.lessThan(root.get("date"), endDateTime));
            }

            // Müşteri filtresi
            if (customerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("customer").get("id"), customerId));
            }

            // Ürün filtresi
            if (productId != null) {
                predicates.add(criteriaBuilder.equal(root.get("product").get("id"), productId));
            }

            // Tutar aralığı filtresi
            if (minAmount != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }

            if (maxAmount != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
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
        return transactionRepository.findById(id).orElse(null);
    }

    // Dashboard için gerekli metotlar
    
    public long countAllTransactions() {
        return transactionRepository.count();
    }
    
    public long countTransactionsByDateRange(LocalDateTime start, LocalDateTime end) {
        Specification<Transaction> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("date"), start));
            predicates.add(criteriaBuilder.lessThan(root.get("date"), end));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return transactionRepository.count(spec);
    }
    
    public List<Transaction> findRecentTransactions(int limit) {
        return transactionRepository.findTop5ByOrderByDateDesc();
    }
    
    public List<Transaction> findHighestAmountTransactions(int limit) {
        return transactionRepository.findTop5ByOrderByAmountDesc();
    }
    
    // Grafikler için gerekli metotlar
    
    public Map<TransactionType, Long> getTransactionCountByType() {
        List<Transaction> allTransactions = transactionRepository.findAll();
        return allTransactions.stream()
            .collect(Collectors.groupingBy(
                Transaction::getType,
                Collectors.counting()
            ));
    }
    
    public Map<LocalDate, BigDecimal> getTransactionVolumeByDateRange(LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();
        
        List<Transaction> transactions = transactionRepository.findByDateBetween(startDateTime, endDateTime);
        
        // Tüm günlerin listesini oluştur
        Map<LocalDate, BigDecimal> volumeByDate = new LinkedHashMap<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            volumeByDate.put(current, BigDecimal.ZERO);
            current = current.plusDays(1);
        }
        
        // İşlem tutarlarını ilgili tarihlere ekle
        for (Transaction transaction : transactions) {
            LocalDate transactionDate = transaction.getDate().toLocalDate();
            volumeByDate.put(
                transactionDate, 
                volumeByDate.getOrDefault(transactionDate, BigDecimal.ZERO).add(transaction.getAmount())
            );
        }
        
        return volumeByDate;
    }
    
    public Map<Customer, Long> getTransactionCountByCustomer(int limit) {
        List<Transaction> transactions = transactionRepository.findByCustomerIsNotNullOrderByDateDesc();
        
        Map<Customer, Long> countByCustomer = transactions.stream()
            .filter(t -> t.getCustomer() != null)
            .collect(Collectors.groupingBy(
                Transaction::getCustomer,
                Collectors.counting()
            ));
        
        // En çok işlem yapılan müşterileri bul
        return countByCustomer.entrySet().stream()
            .sorted(Map.Entry.<Customer, Long>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }
    
    public List<Transaction> findTransactionsByDateRange(LocalDateTime start, LocalDateTime end) {
        return transactionRepository.findByDateBetween(start, end);
    }
}

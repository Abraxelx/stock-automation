package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Customer;
import com.halilsahin.stockautomation.entity.Debt;
import com.halilsahin.stockautomation.entity.Installment;
import com.halilsahin.stockautomation.enums.PaymentMethod;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.enums.DebtDirection;
import com.halilsahin.stockautomation.enums.DebtType;
import com.halilsahin.stockautomation.transaction.TransactionContext;
import com.halilsahin.stockautomation.transaction.TransactionHandler;
import com.halilsahin.stockautomation.transaction.TransactionHandlerFactory;
import com.halilsahin.stockautomation.repository.DebtRepository;
import com.halilsahin.stockautomation.repository.InstallmentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class DebtService {

    private final DebtRepository debtRepository;
    private final InstallmentRepository installmentRepository;
    private final TransactionHandlerFactory transactionHandlerFactory;

    public DebtService(DebtRepository debtRepository, InstallmentRepository installmentRepository, TransactionHandlerFactory transactionHandlerFactory) {
        this.debtRepository = debtRepository;
        this.installmentRepository = installmentRepository;
        this.transactionHandlerFactory = transactionHandlerFactory;
    }

    // Yeni borç kaydı ekleme (taksitler cascade sayesinde birlikte kaydedilir)
    public Debt addDebt(Debt debt) {
        if (debt.getCustomer() == null) {
            throw new IllegalArgumentException("Müşteri seçilmedi");
        }
        Debt savedDebt = debtRepository.save(debt);
        
        TransactionContext context = TransactionContext.builder()
            .relatedEntity("Debt")
            .amount(debt.getAmount())
            .date(LocalDateTime.now())
            .additionalData(Map.of(
                "debt", savedDebt,
                "transactionType", debt.getDirection() == DebtDirection.PAYABLE ? 
                    TransactionType.DEBT_IN : TransactionType.DEBT_OUT
            ))
            .build();

        TransactionHandler handler = transactionHandlerFactory.getHandler(
            debt.getDirection() == DebtDirection.PAYABLE ? 
                TransactionType.DEBT_IN : TransactionType.DEBT_OUT
        );
        handler.handleTransaction(context);

        return savedDebt;
    }

    // Ödemesi yapılan borcu güncelle ve varsa tüm taksitleri de ödenmiş olarak işaretle
    public Debt markAsPaid(Long debtId) {
        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new RuntimeException("Borç bulunamadı: " + debtId));
        debt.setPaid(true);
        debt.setPaymentDate(LocalDateTime.now());

        // Taksitleri de ödenmiş olarak işaretle
        if (debt.getInstallments() != null) {
            for (Installment inst : debt.getInstallments()) {
                inst.setPaid(true);
                inst.setPaymentDate(debt.getPaymentDate()); // Borcun ödeme tarihini taksitlere de uygula (isteğe bağlı)
            }
        }

        TransactionContext context = TransactionContext.builder()
            .relatedEntity("Debt")
            .amount(debt.getAmount())
            .date(LocalDateTime.now())
            .additionalData(Map.of(
                "debt", debt,
                "transactionType", debt.getDirection() == DebtDirection.PAYABLE ? 
                    TransactionType.DEBT_PAYMENT : TransactionType.DEBT_COLLECTION
            ))
            .build();

        TransactionHandler handler = transactionHandlerFactory.getHandler(
            debt.getDirection() == DebtDirection.PAYABLE ? 
                TransactionType.DEBT_PAYMENT : TransactionType.DEBT_COLLECTION
        );
        handler.handleTransaction(context);

        return debtRepository.save(debt);
    }

    // Tüm borçları listeleme
    public List<Debt> getAllDebts() {
        return debtRepository.findAllOrderByIsPaidAndDueDateDesc();
    }

    // Tüm borçları sıralı getir
    public List<Debt> getAllDebtsSorted(String sortBy, String direction) {
        Sort sort = Sort.by(direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        return debtRepository.findAll(sort);
    }

    // Borçları filtreleme
    public List<Debt> filterDebts(
            Long customerId, 
            String direction, 
            String debtType, 
            Boolean paid, 
            String dateRange, 
            String startDateStr, 
            String endDateStr, 
            String sortBy, 
            String sortDirection) {
        
        // Tüm borçları al
        List<Debt> allDebts = debtRepository.findAll();
        
        // Müşteri filtresi
        if (customerId != null) {
            allDebts = allDebts.stream()
                .filter(debt -> debt.getCustomer() != null && 
                               debt.getCustomer().getId().equals(customerId))
                .collect(Collectors.toList());
        }
        
        // Borç yönü filtresi
        if (direction != null && !direction.isEmpty()) {
            try {
                DebtDirection directionEnum = DebtDirection.valueOf(direction);
                allDebts = allDebts.stream()
                    .filter(debt -> debt.getDirection() == directionEnum)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Geçersiz enum değeri, filtreleme yapma
            }
        }
        
        // Borç tipi filtresi
        if (debtType != null && !debtType.isEmpty()) {
            try {
                DebtType typeEnum = DebtType.valueOf(debtType);
                allDebts = allDebts.stream()
                    .filter(debt -> debt.getDebtType() == typeEnum)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Geçersiz enum değeri, filtreleme yapma
            }
        }
        
        // Ödeme durumu filtresi
        if (paid != null) {
            allDebts = allDebts.stream()
                .filter(debt -> debt.isPaid() == paid)
                .collect(Collectors.toList());
        }
        
        // Tarih aralığı filtresi
        if (dateRange != null && !dateRange.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            
            switch (dateRange) {
                case "TODAY":
                    LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
                    LocalDateTime endOfDay = startOfDay.plusDays(1);
                    allDebts = allDebts.stream()
                        .filter(debt -> debt.getDueDate().isAfter(startOfDay) && 
                                       debt.getDueDate().isBefore(endOfDay))
                        .collect(Collectors.toList());
                    break;
                case "THIS_WEEK":
                    LocalDateTime startOfWeek = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .atStartOfDay();
                    LocalDateTime endOfWeek = startOfWeek.plusDays(7);
                    allDebts = allDebts.stream()
                        .filter(debt -> debt.getDueDate().isAfter(startOfWeek) && 
                                       debt.getDueDate().isBefore(endOfWeek))
                        .collect(Collectors.toList());
                    break;
                case "THIS_MONTH":
                    LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                    LocalDateTime endOfMonth = startOfMonth.plusMonths(1);
                    allDebts = allDebts.stream()
                        .filter(debt -> debt.getDueDate().isAfter(startOfMonth) && 
                                       debt.getDueDate().isBefore(endOfMonth))
                        .collect(Collectors.toList());
                    break;
                case "OVERDUE":
                    allDebts = allDebts.stream()
                        .filter(debt -> !debt.isPaid() && debt.getDueDate().isBefore(now))
                        .collect(Collectors.toList());
                    break;
                case "UPCOMING":
                    LocalDateTime threeDaysLater = now.plusDays(3);
                    allDebts = allDebts.stream()
                        .filter(debt -> !debt.isPaid() && 
                                       debt.getDueDate().isAfter(now) && 
                                       debt.getDueDate().isBefore(threeDaysLater))
                        .collect(Collectors.toList());
                    break;
            }
        } else if (startDateStr != null && !startDateStr.isEmpty() && 
                  endDateStr != null && !endDateStr.isEmpty()) {
            try {
                // Manuel tarih aralığı
                LocalDateTime startDate = LocalDate.parse(startDateStr).atStartOfDay();
                LocalDateTime endDate = LocalDate.parse(endDateStr).plusDays(1).atStartOfDay();
                
                allDebts = allDebts.stream()
                    .filter(debt -> debt.getDueDate().isAfter(startDate) && 
                                   debt.getDueDate().isBefore(endDate))
                    .collect(Collectors.toList());
            } catch (Exception e) {
                // Tarih ayrıştırma hatası, filtreleme yapma
            }
        }
        
        // Sıralama
        if (sortBy != null && !sortBy.isEmpty()) {
            final String finalSortBy = sortBy;
            final boolean isAscending = "asc".equalsIgnoreCase(sortDirection);
            
            Comparator<Debt> comparator = null;
            
            switch (finalSortBy) {
                case "id":
                    comparator = Comparator.comparing(Debt::getId);
                    break;
                case "amount":
                    comparator = Comparator.comparing(Debt::getAmount);
                    break;
                case "dueDate":
                    comparator = Comparator.comparing(Debt::getDueDate);
                    break;
                case "customer":
                    comparator = Comparator.comparing(
                        debt -> debt.getCustomer() != null ? 
                            debt.getCustomer().getFirstName() : "", 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                    );
                    break;
                case "isPaid":
                    comparator = Comparator.comparing(Debt::isPaid);
                    break;
                case "direction":
                    comparator = Comparator.comparing(
                        debt -> debt.getDirection() != null ? 
                            debt.getDirection().name() : "", 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                    );
                    break;
                case "debtType":
                    comparator = Comparator.comparing(
                        debt -> debt.getDebtType() != null ? 
                            debt.getDebtType().name() : "", 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                    );
                    break;
                default:
                    comparator = Comparator.comparing(Debt::getDueDate);
            }
            
            if (!isAscending) {
                comparator = comparator.reversed();
            }
            
            allDebts.sort(comparator);
        }
        
        return allDebts;
    }

    // Ödenmemiş (pending) borçları döndürme
    public List<Debt> getPendingDebts() {
        return debtRepository.findByIsPaid(false);
    }

    // Müşteriye göre borç arama
    public List<Debt> findDebtsByCustomerName(String customerName) {
        return debtRepository.findByCustomerFirstNameContainingIgnoreCase(customerName);
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

    public void payDebt(Long debtId, PaymentMethod paymentMethod, BigDecimal amount) {
        Debt debt = findDebtById(debtId);
        if (debt == null || debt.isPaid()) {
            throw new RuntimeException("Geçersiz borç veya borç zaten ödenmiş");
        }

        if (amount.compareTo(BigDecimal.valueOf(debt.getAmount())) > 0) {
            throw new RuntimeException("Ödeme tutarı borç tutarından büyük olamaz");
        }

        // Tam ödeme yapılıyorsa
        if (amount.compareTo(BigDecimal.valueOf(debt.getAmount())) == 0) {
            debt.setPaid(true);
        }

        debt.setAmount(debt.getAmount() - amount.doubleValue());
        debt.setPaymentMethod(paymentMethod);
        debt.setPaymentDate(LocalDateTime.now());

        debtRepository.save(debt);

        // Transaction kaydı ekle
        TransactionContext context = TransactionContext.builder()
            .relatedEntity("Debt")
            .amount(amount.doubleValue())
            .date(LocalDateTime.now())
            .additionalData(Map.of(
                "debt", debt,
                "transactionType", debt.getDirection() == DebtDirection.PAYABLE ? 
                    TransactionType.DEBT_PAYMENT : TransactionType.DEBT_COLLECTION
            ))
            .build();

        TransactionHandler handler = transactionHandlerFactory.getHandler(
            debt.getDirection() == DebtDirection.PAYABLE ? 
                TransactionType.DEBT_PAYMENT : TransactionType.DEBT_COLLECTION
        );
        handler.handleTransaction(context);
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
        
        // Alınan borçlar (PAYABLE)
        double totalPayableAmount = allDebts.stream()
            .filter(d -> d.getDirection() == DebtDirection.PAYABLE && !d.isPaid())
            .mapToDouble(Debt::getAmount)
            .sum();
            
        // Verilen borçlar (RECEIVABLE)
        double totalReceivableAmount = allDebts.stream()
            .filter(d -> d.getDirection() == DebtDirection.RECEIVABLE && !d.isPaid())
            .mapToDouble(Debt::getAmount)
            .sum();
            
        // Ödenmiş borçlar
        double paidDebtAmount = allDebts.stream()
            .filter(Debt::isPaid)
            .mapToDouble(Debt::getAmount)
            .sum();
        
        stats.put("totalPayableAmount", totalPayableAmount);
        stats.put("totalReceivableAmount", totalReceivableAmount);
        stats.put("paidDebtAmount", paidDebtAmount);
        stats.put("overdueDebts", getOverdueDebts());
        stats.put("upcomingDebts", getUpcomingDebts());
        
        return stats;
    }

    public List<Debt> findDebtsByCustomer(Customer customer) {
        return debtRepository.findByCustomerOrderByDueDateDesc(customer);
    }
}

package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.entity.Customer;
import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.service.CustomerService;
import com.halilsahin.stockautomation.service.ProductService;
import com.halilsahin.stockautomation.service.TransactionService;
import com.halilsahin.stockautomation.service.DebtService;
import com.halilsahin.stockautomation.util.CSVExporter;
import com.halilsahin.stockautomation.util.ExcelExporter;
import com.halilsahin.stockautomation.util.PDFExporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class TransactionController {

    private final TransactionService transactionService;
    private final DebtService debtService;
    private final CustomerService customerService;
    private final ProductService productService;
    private static final int PAGE_SIZE = 20;

    @Autowired
    public TransactionController(TransactionService transactionService, 
                                DebtService debtService,
                                CustomerService customerService,
                                ProductService productService) {
        this.transactionService = transactionService;
        this.debtService = debtService;
        this.customerService = customerService;
        this.productService = productService;
    }

    @GetMapping("/transactions")
    public String showTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) String view,
            Model model) {
        
        // Ön tanımlı tarih aralıkları
        if (dateRange != null) {
            LocalDate today = LocalDate.now();
            
            switch (dateRange) {
                case "TODAY":
                    startDate = today.toString();
                    endDate = today.toString();
                    break;
                case "THIS_WEEK":
                    startDate = today.minusDays(today.getDayOfWeek().getValue() - 1).toString();
                    endDate = today.toString();
                    break;
                case "THIS_MONTH":
                    startDate = today.withDayOfMonth(1).toString();
                    endDate = today.toString();
                    break;
                case "LAST_30_DAYS":
                    startDate = today.minusDays(30).toString();
                    endDate = today.toString();
                    break;
                case "LAST_90_DAYS":
                    startDate = today.minusDays(90).toString();
                    endDate = today.toString();
                    break;
            }
        }
        
        Sort sort = Sort.by(direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, sort);
        
        // Gelişmiş filtreleme parametreleri ile işlemleri getir
        Page<Transaction> transactionPage = transactionService.findTransactionsAdvanced(
            type, startDate, endDate, customerId, productId, minAmount, maxAmount, pageable);
        
        // Dashboard istatistikleri
        Map<String, Object> dashboardStats = getDashboardStats();
        model.addAttribute("dashboardStats", dashboardStats);
        
        // Grafikler için veri
        Map<String, Object> chartData = getChartData();
        model.addAttribute("chartData", chartData);
        
        // Borç istatistikleri
        model.addAttribute("debtStats", debtService.getDebtStatistics());
        
        // Sayfalama ve filtreleme için gerekli veriler
        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactionPage.getTotalPages());
        model.addAttribute("totalItems", transactionPage.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        model.addAttribute("type", type);
        model.addAttribute("dateRange", dateRange);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("customerId", customerId);
        model.addAttribute("productId", productId);
        model.addAttribute("minAmount", minAmount);
        model.addAttribute("maxAmount", maxAmount);
        model.addAttribute("view", view != null ? view : "list");
        
        // Filtreler için gerekli veriler
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("transactionTypes", TransactionType.values());
        
        if ("calendar".equals(view)) {
            return "transactions-calendar";
        }
        
        return "transactions";
    }

    /**
     * Dashboard istatistiklerini hazırlar
     */
    private Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Toplam işlem sayısı
        long totalTransactions = transactionService.countAllTransactions();
        stats.put("totalTransactions", totalTransactions);
        
        // Günlük işlem sayısı
        long todayTransactions = transactionService.countTransactionsByDateRange(
            LocalDate.now().atStartOfDay(), 
            LocalDateTime.now()
        );
        stats.put("todayTransactions", todayTransactions);
        
        // Haftalık işlem sayısı
        LocalDate startOfWeek = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        long weeklyTransactions = transactionService.countTransactionsByDateRange(
            startOfWeek.atStartOfDay(), 
            LocalDateTime.now()
        );
        stats.put("weeklyTransactions", weeklyTransactions);
        
        // Aylık işlem sayısı
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        long monthlyTransactions = transactionService.countTransactionsByDateRange(
            startOfMonth.atStartOfDay(), 
            LocalDateTime.now()
        );
        stats.put("monthlyTransactions", monthlyTransactions);
        
        // Son 5 işlem
        List<Transaction> recentTransactions = transactionService.findRecentTransactions(5);
        stats.put("recentTransactions", recentTransactions);
        
        // En yüksek tutarlı işlemler
        List<Transaction> highestAmountTransactions = transactionService.findHighestAmountTransactions(5);
        stats.put("highestAmountTransactions", highestAmountTransactions);
        
        return stats;
    }

    /**
     * Grafikler için veri hazırlar
     */
    private Map<String, Object> getChartData() {
        Map<String, Object> chartData = new HashMap<>();
        
        // İşlem türlerine göre dağılım
        Map<TransactionType, Long> typeDistribution = transactionService.getTransactionCountByType();
        
        List<String> typeLabels = typeDistribution.keySet().stream()
            .map(TransactionType::name)
            .collect(Collectors.toList());
            
        List<Long> typeCounts = new ArrayList<>(typeDistribution.values());
        
        chartData.put("typeLabels", typeLabels);
        chartData.put("typeCounts", typeCounts);
        
        // Zaman içindeki işlem hacmi - son 30 gün
        Map<LocalDate, BigDecimal> volumeByDate = transactionService.getTransactionVolumeByDateRange(
            LocalDate.now().minusDays(30), 
            LocalDate.now()
        );
        
        List<String> dateLabels = volumeByDate.keySet().stream()
            .map(date -> date.format(DateTimeFormatter.ofPattern("dd/MM")))
            .collect(Collectors.toList());
            
        List<BigDecimal> volumeValues = new ArrayList<>(volumeByDate.values());
        
        chartData.put("dateLabels", dateLabels);
        chartData.put("volumeValues", volumeValues);
        
        // Müşterilere göre işlem dağılımı - en çok işlem yapılan 5 müşteri
        Map<Customer, Long> customerDistribution = transactionService.getTransactionCountByCustomer(5);
        
        List<String> customerLabels = customerDistribution.keySet().stream()
            .map(customer -> customer.getFirstName() + " " + customer.getLastName())
            .collect(Collectors.toList());
            
        List<Long> customerCounts = new ArrayList<>(customerDistribution.values());
        
        chartData.put("customerLabels", customerLabels);
        chartData.put("customerCounts", customerCounts);
        
        return chartData;
    }

    @GetMapping("/transactions/detail/{id}")
    public String showTransactionDetail(@PathVariable Long id, Model model) {
        Transaction transaction = transactionService.findById(id);
        if (transaction == null) {
            return "redirect:/transactions";
        }

        model.addAttribute("transaction", transaction);

        // İşlem tipine göre uygun detay sayfasına yönlendir
        switch (transaction.getType()) {
            case STOCK_IN:
            case STOCK_OUT:
                return "stock-transaction-detail";
            case SALE:
                return "redirect:/sales/detail/" + transaction.getSale().getId();
            case DEBT_IN:
            case DEBT_OUT:
            case DEBT_PAYMENT:
            case DEBT_COLLECTION:
                return "redirect:/debts/detail/" + transaction.getDebt().getId();
            default:
                return "transaction-detail"; // Genel detay sayfası
        }
    }
    
    /**
     * Modal için işlem detaylarını getirir
     */
    @GetMapping("/transactions/modal/{id}")
    @ResponseBody
    public ResponseEntity<Transaction> getTransactionForModal(@PathVariable Long id) {
        Transaction transaction = transactionService.findById(id);
        if (transaction == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(transaction, HttpStatus.OK);
    }
    
    /**
     * Takvim görünümü için işlem verilerini getirir
     */
    @GetMapping("/transactions/calendar-data")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getCalendarData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        if (start == null) {
            start = LocalDate.now().withDayOfMonth(1);
        }
        
        if (end == null) {
            end = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        }
        
        List<Transaction> transactions = transactionService.findTransactionsByDateRange(
            start.atStartOfDay(), 
            end.plusDays(1).atStartOfDay()
        );
        
        List<Map<String, Object>> calendarEvents = new ArrayList<>();
        
        for (Transaction transaction : transactions) {
            Map<String, Object> event = new HashMap<>();
            event.put("id", transaction.getId());
            event.put("title", transaction.getType() + " - " + transaction.getAmount() + " TL");
            event.put("start", transaction.getDate().format(DateTimeFormatter.ISO_DATE_TIME));
            
            // İşlem türüne göre renk belirleme
            String color = switch (transaction.getType()) {
                case SALE -> "#28a745"; // Yeşil
                case STOCK_IN -> "#007bff"; // Mavi
                case STOCK_OUT -> "#ffc107"; // Sarı
                case DEBT_IN, DEBT_PAYMENT -> "#dc3545"; // Kırmızı
                case DEBT_OUT, DEBT_COLLECTION -> "#6610f2"; // Mor
                default -> "#17a2b8"; // Açık mavi
            };
            
            event.put("color", color);
            event.put("url", "/transactions/detail/" + transaction.getId());
            
            calendarEvents.add(event);
        }
        
        return new ResponseEntity<>(calendarEvents, HttpStatus.OK);
    }
    
    /**
     * İşlemleri Excel olarak dışa aktar
     */
    @GetMapping("/transactions/export/excel")
    public void exportToExcel(HttpServletResponse response,
                             @RequestParam(required = false) String type,
                             @RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate,
                             @RequestParam(required = false) Long customerId,
                             @RequestParam(required = false) Long productId) throws IOException {
        List<Transaction> transactions = transactionService.findTransactionsForExport(
            type, startDate, endDate, customerId, productId);
        
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=transactions.xlsx");
        
        ExcelExporter exporter = new ExcelExporter(transactions);
        exporter.export(response);
    }
    
    /**
     * İşlemleri PDF olarak dışa aktar
     */
    @GetMapping("/transactions/export/pdf")
    public void exportToPDF(HttpServletResponse response,
                           @RequestParam(required = false) String type,
                           @RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String endDate,
                           @RequestParam(required = false) Long customerId,
                           @RequestParam(required = false) Long productId) throws IOException {
        List<Transaction> transactions = transactionService.findTransactionsForExport(
            type, startDate, endDate, customerId, productId);
        
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=transactions.pdf");
        
        PDFExporter exporter = new PDFExporter(transactions);
        exporter.export(response);
    }
    
    /**
     * İşlemleri CSV olarak dışa aktar
     */
    @GetMapping("/transactions/export/csv")
    public void exportToCSV(HttpServletResponse response,
                           @RequestParam(required = false) String type,
                           @RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String endDate,
                           @RequestParam(required = false) Long customerId,
                           @RequestParam(required = false) Long productId) throws IOException {
        List<Transaction> transactions = transactionService.findTransactionsForExport(
            type, startDate, endDate, customerId, productId);
        
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=transactions.csv");
        
        CSVExporter exporter = new CSVExporter(transactions);
        exporter.export(response);
    }
}

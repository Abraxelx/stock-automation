package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.service.DebtService;
import com.halilsahin.stockautomation.service.ProductService;
import com.halilsahin.stockautomation.service.SaleService;
import com.halilsahin.stockautomation.service.TransactionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/statistics")
public class StatisticsController {

    private final TransactionService transactionService;
    private final ProductService productService;
    private final SaleService saleService;
    private final DebtService debtService;

    @Autowired
    public StatisticsController(TransactionService transactionService, 
                               ProductService productService,
                               SaleService saleService,
                               DebtService debtService) {
        this.transactionService = transactionService;
        this.productService = productService;
        this.saleService = saleService;
        this.debtService = debtService;
    }

    @GetMapping
    public String showStatisticsPage(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {
        
        // Varsayılan tarih aralığı son 6 ay
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(6);
        
        if (period != null) {
            switch (period) {
                case "1M":
                    start = end.minusMonths(1);
                    break;
                case "3M":
                    start = end.minusMonths(3);
                    break;
                case "6M":
                    start = end.minusMonths(6);
                    break;
                case "1Y":
                    start = end.minusYears(1);
                    break;
                case "custom":
                    if (startDate != null && endDate != null) {
                        start = LocalDate.parse(startDate);
                        end = LocalDate.parse(endDate);
                    }
                    break;
            }
        }
        
        model.addAttribute("period", period != null ? period : "6M");
        model.addAttribute("startDate", start.toString());
        model.addAttribute("endDate", end.toString());
        
        // İstatistik verileri
        Map<String, Object> statistics = calculateStatistics(start, end);
        model.addAttribute("statistics", statistics);
        
        // Grafikler için veri hazırla
        Map<String, Object> chartData = prepareChartData(start, end);
        model.addAttribute("chartData", chartData);
        
        return "statistics";
    }
    
    @GetMapping("/api/chart-data")
    @ResponseBody
    public Map<String, Object> getChartData(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
            
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(6);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        return prepareChartData(start, end);
    }
    
    private Map<String, Object> calculateStatistics(LocalDate start, LocalDate end) {
        Map<String, Object> stats = new HashMap<>();
        
        // Tarih aralığını LocalDateTime'a çevir
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();
        
        // Tüm işlemleri getir
        List<Transaction> transactions = transactionService
            .findAllTransactionsBetweenDates(startDateTime, endDateTime);
        
        // İşlem tipleri sayısı
        Map<TransactionType, Long> transactionCounts = transactions.stream()
            .collect(Collectors.groupingBy(Transaction::getType, Collectors.counting()));
            
        // Satış işlemleri toplam tutarı
        double totalSalesAmount = transactions.stream()
            .filter(t -> t.getType() == TransactionType.SALE)
            .mapToDouble(t -> t.getAmount().doubleValue())
            .sum();
            
        // Stok giriş işlemleri toplam tutarı
        double totalStockInAmount = transactions.stream()
            .filter(t -> t.getType() == TransactionType.STOCK_IN)
            .mapToDouble(t -> t.getAmount().doubleValue())
            .sum();
            
        // Borç işlemleri toplam tutarı
        double totalDebtAmount = transactions.stream()
            .filter(t -> t.getType() == TransactionType.DEBT_IN || t.getType() == TransactionType.DEBT_OUT)
            .mapToDouble(t -> t.getAmount().doubleValue())
            .sum();
            
        // Son ay ve önceki ay karşılaştırması
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        LocalDateTime twoMonthsAgo = LocalDateTime.now().minusMonths(2);
        
        double currentMonthSales = transactions.stream()
            .filter(t -> t.getType() == TransactionType.SALE && t.getDate().isAfter(oneMonthAgo))
            .mapToDouble(t -> t.getAmount().doubleValue())
            .sum();
            
        double previousMonthSales = transactions.stream()
            .filter(t -> t.getType() == TransactionType.SALE && 
                   t.getDate().isAfter(twoMonthsAgo) && 
                   t.getDate().isBefore(oneMonthAgo))
            .mapToDouble(t -> t.getAmount().doubleValue())
            .sum();
            
        double salesGrowth = 0;
        if (previousMonthSales > 0) {
            salesGrowth = ((currentMonthSales - previousMonthSales) / previousMonthSales) * 100;
        }
        
        // İstatistikleri ekle
        stats.put("totalTransactions", transactions.size());
        stats.put("totalSalesTransactions", transactionCounts.getOrDefault(TransactionType.SALE, 0L));
        stats.put("totalStockInTransactions", transactionCounts.getOrDefault(TransactionType.STOCK_IN, 0L));
        stats.put("totalSalesAmount", totalSalesAmount);
        stats.put("totalStockInAmount", totalStockInAmount);
        stats.put("totalDebtAmount", totalDebtAmount);
        stats.put("currentMonthSales", currentMonthSales);
        stats.put("previousMonthSales", previousMonthSales);
        stats.put("salesGrowth", salesGrowth);
        
        // Borç istatistikleri
        Map<String, Object> debtStats = debtService.getDebtStatistics();
        stats.put("totalPayableAmount", debtStats.get("totalPayableAmount"));
        stats.put("totalReceivableAmount", debtStats.get("totalReceivableAmount"));
        
        return stats;
    }
    
    private Map<String, Object> prepareChartData(LocalDate start, LocalDate end) {
        Map<String, Object> chartData = new HashMap<>();
        
        // Aylık satış grafiği - son 12 ay
        Map<String, Double> monthlySales = new LinkedHashMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        
        // Başlangıç ve bitiş tarihleri arasındaki her ay için
        YearMonth startMonth = YearMonth.from(start);
        YearMonth endMonth = YearMonth.from(end);
        
        // Ayları doldur (her ay için bir giriş olsun)
        for (YearMonth month = startMonth; !month.isAfter(endMonth); month = month.plusMonths(1)) {
            String monthLabel = month.format(monthFormatter);
            monthlySales.put(monthLabel, 0.0);
        }
        
        // Tarih aralığındaki işlemleri al
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();
        
        List<Transaction> transactions = transactionService
            .findAllTransactionsBetweenDates(startDateTime, endDateTime);
        
        // Satış işlemlerini aylara göre grupla
        Map<YearMonth, Double> salesByMonth = transactions.stream()
            .filter(t -> t.getType() == TransactionType.SALE)
            .collect(Collectors.groupingBy(
                t -> YearMonth.from(t.getDate()),
                Collectors.summingDouble(t -> t.getAmount().doubleValue())
            ));
            
        // Haritayı doldur
        for (Map.Entry<YearMonth, Double> entry : salesByMonth.entrySet()) {
            String monthLabel = entry.getKey().format(monthFormatter);
            if (monthlySales.containsKey(monthLabel)) {
                monthlySales.put(monthLabel, entry.getValue());
            }
        }
        
        // İşlem tipine göre dağılım grafiği
        Map<String, Long> transactionsByType = transactions.stream()
            .collect(Collectors.groupingBy(
                t -> t.getType().toString(),
                Collectors.counting()
            ));
        
        // Ürün kategorilerine göre satış grafiği (örnek olarak en çok satılan 5 ürün)
        Map<String, Double> salesByProduct = transactions.stream()
            .filter(t -> t.getType() == TransactionType.SALE && t.getProduct() != null)
            .collect(Collectors.groupingBy(
                t -> t.getProduct().getName(),
                Collectors.summingDouble(t -> t.getAmount().doubleValue())
            ));
            
        // En çok satılan 5 ürünü al
        List<Map.Entry<String, Double>> topProducts = salesByProduct.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toList());
            
        Map<String, Double> topProductsMap = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : topProducts) {
            topProductsMap.put(entry.getKey(), entry.getValue());
        }
        
        // Günlük satış trendi - son 30 gün
        Map<String, Double> dailySales = new LinkedHashMap<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd MMM");
        
        // Son 30 günü belirle
        LocalDate initialThirtyDaysAgo = LocalDate.now().minusDays(30);
        // Start tarihi daha sonra ise onu kullan, değilse initialThirtyDaysAgo kullan
        final LocalDate thirtyDaysAgo = start.isAfter(initialThirtyDaysAgo) ? start : initialThirtyDaysAgo;
        
        // Günleri doldur
        for (LocalDate date = thirtyDaysAgo; !date.isAfter(end); date = date.plusDays(1)) {
            String dayLabel = date.format(dayFormatter);
            dailySales.put(dayLabel, 0.0);
        }
        
        // Satış işlemlerini günlere göre grupla
        Map<LocalDate, Double> salesByDay = transactions.stream()
            .filter(t -> t.getType() == TransactionType.SALE && 
                  t.getDate().isAfter(thirtyDaysAgo.atStartOfDay()))
            .collect(Collectors.groupingBy(
                t -> t.getDate().toLocalDate(),
                Collectors.summingDouble(t -> t.getAmount().doubleValue())
            ));
            
        // Haritayı doldur
        for (Map.Entry<LocalDate, Double> entry : salesByDay.entrySet()) {
            String dayLabel = entry.getKey().format(dayFormatter);
            if (dailySales.containsKey(dayLabel)) {
                dailySales.put(dayLabel, entry.getValue());
            }
        }
        
        // Chart verilerini ekle
        chartData.put("monthlySalesLabels", new ArrayList<>(monthlySales.keySet()));
        chartData.put("monthlySalesData", new ArrayList<>(monthlySales.values()));
        
        chartData.put("transactionTypeLabels", new ArrayList<>(transactionsByType.keySet()));
        chartData.put("transactionTypeData", new ArrayList<>(transactionsByType.values()));
        
        chartData.put("topProductLabels", new ArrayList<>(topProductsMap.keySet()));
        chartData.put("topProductData", new ArrayList<>(topProductsMap.values()));
        
        chartData.put("dailySalesLabels", new ArrayList<>(dailySales.keySet()));
        chartData.put("dailySalesData", new ArrayList<>(dailySales.values()));
        
        return chartData;
    }
} 
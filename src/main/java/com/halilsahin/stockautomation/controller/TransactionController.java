package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.service.TransactionService;
import com.halilsahin.stockautomation.service.DebtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.halilsahin.stockautomation.util.TransactionPDFExporter;
import com.halilsahin.stockautomation.util.TransactionExcelExporter;
import java.io.IOException;
import com.itextpdf.text.DocumentException;
import java.time.format.DateTimeFormatter;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class TransactionController {

    private final TransactionService transactionService;
    private final DebtService debtService;
    private static final int PAGE_SIZE = 20; // Sayfa başına gösterilecek işlem sayısı

    @Autowired
    public TransactionController(TransactionService transactionService, DebtService debtService) {
        this.transactionService = transactionService;
        this.debtService = debtService;
    }

    @GetMapping("/transactions")
    public String showTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {
        
        Sort sort = Sort.by(direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, sort);
        
        // İşlemleri getir
        Page<Transaction> transactionPage = transactionService.findTransactions(
            type, startDate, endDate, pageable);
        
        // Debug için
        System.out.println("Toplam işlem sayısı: " + transactionPage.getTotalElements());
        System.out.println("Mevcut sayfa içeriği: " + transactionPage.getContent().size());
        
        // Dashboard istatistiklerini ekle
        addDashboardStats(model);
        
        // Borç istatistiklerini ekle
        model.addAttribute("debtStats", debtService.getDebtStatistics());
        
        // Sayfalama bilgilerini modele ekle
        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactionPage.getTotalPages());
        model.addAttribute("totalItems", transactionPage.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        model.addAttribute("type", type);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        
        return "transactions";
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
     * Dashboard istatistiklerini hesaplar ve model'e ekler
     */
    private void addDashboardStats(Model model) {
        // Toplam işlem sayısı
        long totalTransactions = transactionService.countAllTransactions();
        model.addAttribute("totalTransactions", totalTransactions);
        
        // Günlük işlem sayısı
        long todayTransactions = transactionService.countTransactionsByDateRange(
            LocalDate.now().atStartOfDay(), 
            LocalDateTime.now()
        );
        model.addAttribute("todayTransactions", todayTransactions);
        
        // Haftalık işlem sayısı
        LocalDate startOfWeek = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        long weeklyTransactions = transactionService.countTransactionsByDateRange(
            startOfWeek.atStartOfDay(), 
            LocalDateTime.now()
        );
        model.addAttribute("weeklyTransactions", weeklyTransactions);
        
        // Aylık işlem sayısı
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        long monthlyTransactions = transactionService.countTransactionsByDateRange(
            startOfMonth.atStartOfDay(), 
            LocalDateTime.now()
        );
        model.addAttribute("monthlyTransactions", monthlyTransactions);
        
        // Son 5 işlem
        List<Transaction> recentTransactions = transactionService.findRecentTransactions(5);
        model.addAttribute("recentTransactions", recentTransactions);
        
        // En yüksek tutarlı işlemler
        List<Transaction> highestAmountTransactions = transactionService.findHighestAmountTransactions(5);
        model.addAttribute("highestAmountTransactions", highestAmountTransactions);
        
        // Günlük toplam tutar
        BigDecimal dailyTotal = transactionService.calculateDailyTotal();
        model.addAttribute("dailyTotal", dailyTotal);
    }

    // PDF İhracat
    @GetMapping("/export/pdf")
    public void exportToPDF(
            HttpServletResponse response,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount) throws IOException, DocumentException {
        
        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=islemler_" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".pdf";
        response.setHeader(headerKey, headerValue);

        // Filtreli verileri al
        List<Transaction> transactions = transactionService.findFilteredTransactions(
            type, startDate, endDate, minAmount, maxAmount);
        
        TransactionPDFExporter exporter = new TransactionPDFExporter(transactions);
        exporter.export(response);
    }

    // Excel İhracat
    @GetMapping("/export/excel")
    public void exportToExcel(
            HttpServletResponse response,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount) throws IOException {
        
        response.setContentType("application/octet-stream");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=islemler_" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".xlsx";
        response.setHeader(headerKey, headerValue);

        // Filtreli verileri al
        List<Transaction> transactions = transactionService.findFilteredTransactions(
            type, startDate, endDate, minAmount, maxAmount);
        
        TransactionExcelExporter exporter = new TransactionExcelExporter(transactions);
        exporter.export(response);
    }
}

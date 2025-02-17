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
}

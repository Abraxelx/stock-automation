package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class TransactionController {

    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/transactions")
    public String showTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {
        
        List<Transaction> transactions = transactionRepository.findAllByOrderByDateDesc();
        
        // Filtreleme işlemleri
        if (type != null && !type.isEmpty()) {
            transactions = transactions.stream()
                .filter(t -> t.getTransactionType().toString().equals(type))
                .toList();
        }
        
        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            transactions = transactions.stream()
                .filter(t -> t.getDate().isAfter(start))
                .toList();
        }
        
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
            transactions = transactions.stream()
                .filter(t -> t.getDate().isBefore(end))
                .toList();
        }
        
        model.addAttribute("transactions", transactions);
        return "transactions";
    }
}

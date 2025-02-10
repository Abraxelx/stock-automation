package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private static final int PAGE_SIZE = 20; // Sayfa başına gösterilecek işlem sayısı

    @Autowired
    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
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
        Page<Transaction> transactionPage;
        
        if (type != null && !type.isEmpty() || startDate != null && !startDate.isEmpty() || endDate != null && !endDate.isEmpty()) {
            LocalDateTime start = startDate != null && !startDate.isEmpty() ? 
                LocalDate.parse(startDate).atStartOfDay() : LocalDate.of(2000, 1, 1).atStartOfDay();
            LocalDateTime end = endDate != null && !endDate.isEmpty() ? 
                LocalDate.parse(endDate).atTime(23, 59, 59) : LocalDateTime.now().plusYears(10);
            
            if (type != null && !type.isEmpty()) {
                transactionPage = transactionRepository.findByTransactionTypeAndDateBetween(
                    TransactionType.valueOf(type), start, end, pageable);
            } else {
                transactionPage = transactionRepository.findByDateBetween(start, end, pageable);
            }
        } else {
            transactionPage = transactionRepository.findAll(pageable);
        }
        
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
}

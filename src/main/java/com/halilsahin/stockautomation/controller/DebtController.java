package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.constants.Constants;
import com.halilsahin.stockautomation.entity.Customer;
import com.halilsahin.stockautomation.entity.Debt;
import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import com.halilsahin.stockautomation.service.CustomerService;
import com.halilsahin.stockautomation.service.DebtService;
import com.halilsahin.stockautomation.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/debts")
public class DebtController {

    private final DebtService debtService;
    private final ProductService productService;
    private final CustomerService customerService;
    private final TransactionRepository transactionRepository;

    @Autowired
    public DebtController(DebtService debtService, ProductService productService, CustomerService customerService, TransactionRepository transactionRepository) {
        this.debtService = debtService;
        this.productService = productService;
        this.customerService = customerService;
        this.transactionRepository = transactionRepository;
    }

    // Borç Girişi
    @PostMapping("/add")
    public String addDebt(@RequestParam Long customerId,
                          @RequestParam double amount,
                          @RequestParam (required = false) Long productId,
                          @RequestParam String dueDate,
                          Model model) {
        Customer customerById = customerService.findCustomerById(customerId);
        Debt debt = new Debt();
        if(productId != null) {
            debt.setProduct(productService.findById(productId));
        }
        debt.setCustomer(customerById);
        debt.setAmount(amount);
        debt.setDueDate(LocalDateTime.parse(dueDate));
        debt.setPaid(false); // Başlangıçta borç ödenmemiş olacak

        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.DEBT_IN);
        transaction.setDescription(customerById.getFirstName().concat(" "+customerById.getLastName() +" ").concat(amount +" ").concat("BORÇ GİRİŞİ YAPMIŞTIR"));
        transaction.setDate(LocalDateTime.now());
        transaction.setRelatedEntity("DEBT");
        transactionRepository.save(transaction);
        debtService.addDebt(debt);

        model.addAttribute(Constants.DEBTS, debtService.getAllDebts());
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute(Constants.PRODUCTS, productService.findAll());// Ürünleri de model'e ekleyelim
        return Constants.DEBTS;
    }

    // Borçları listeleme
    @GetMapping
    public String getAllDebts(Model model) {
        model.addAttribute(Constants.DEBTS, debtService.getAllDebts());
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute(Constants.PRODUCTS, productService.findAll());  // Ürünleri de model'e ekleyelim
        return Constants.DEBTS;
    }

    // Müşteriye göre borç arama
    @GetMapping("/search")
    public String searchDebts(@RequestParam String customerName, Model model) {
        model.addAttribute(Constants.DEBTS, debtService.findDebtsByCustomerName(customerName));
        model.addAttribute(Constants.PRODUCTS, productService.findAll());  // Ürünleri de model'e ekleyelim
        return Constants.DEBTS;
    }

    // Ödeme işlemi
    @PostMapping("/pay/{id}")
    public String payDebt(@PathVariable Long id, Model model) {
        model.addAttribute(Constants.DEBTS, debtService.getAllDebts());
        model.addAttribute(Constants.PRODUCTS, productService.findAll());  // Ürünleri de model'e ekleyelim
        return Constants.DEBTS;
    }
}

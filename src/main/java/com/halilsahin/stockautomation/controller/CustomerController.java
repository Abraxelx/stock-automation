package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.entity.Customer;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import com.halilsahin.stockautomation.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }


    // Yeni müşteri ekleme sayfası
    @GetMapping("/add")
    public String showAddCustomerForm() {
        return "add-customer";
    }

    // Müşteri bilgilerini kaydetme
    @PostMapping("/save")
    public String saveCustomer(Customer customer) {
        customerService.saveCustomer(customer);
        return "redirect:/customers/add";
    }
}

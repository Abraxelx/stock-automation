package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.entity.Customer;
import com.halilsahin.stockautomation.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public String listCustomers(@RequestParam(required = false) String name,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String balanceStatus,
                              Model model) {
        List<Customer> customers;
        
        if (name != null || phone != null || balanceStatus != null) {
            customers = customerService.searchCustomers(name, phone, balanceStatus);
        } else {
            customers = customerService.getAllCustomers();
        }
        
        model.addAttribute("customers", customers);
        return "customers";
    }

    // Yeni müşteri ekleme sayfası
    @GetMapping("/add")
    public String showAddCustomerForm() {
        return "add-customer";
    }

    // Müşteri bilgilerini kaydetme
    @PostMapping("/save")
    public String saveCustomer(Customer customer) {
        customerService.save(customer);
        return "redirect:/customers/add";
    }

    @GetMapping("/detail/{id}")
    public String getCustomerDetail(@PathVariable Long id, Model model) {
        Customer customer = customerService.findCustomerById(id);
        if (customer == null) {
            return "redirect:/customers";
        }
        
        model.addAttribute("customer", customer);
        return "customer-detail"; // customer-detail.html oluşturacağız
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.findCustomerById(id);
        if (customer == null) {
            return "redirect:/customers";
        }
        
        model.addAttribute("customer", customer);
        return "edit-customer";
    }

    @PostMapping("/edit/{id}")
    public String updateCustomer(@PathVariable Long id, Customer customer) {
        customer.setId(id); // ID'yi set et
        customerService.save(customer);
        return "redirect:/customers/detail/" + id;
    }
}

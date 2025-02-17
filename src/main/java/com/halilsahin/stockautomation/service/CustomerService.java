package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Customer;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.CustomerRepository;
import com.halilsahin.stockautomation.transaction.TransactionContext;
import com.halilsahin.stockautomation.transaction.TransactionHandler;
import com.halilsahin.stockautomation.transaction.TransactionHandlerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TransactionHandlerFactory transactionHandlerFactory;

    @Autowired
    public CustomerService(CustomerRepository customerRepository, 
                         TransactionHandlerFactory transactionHandlerFactory) {
        this.customerRepository = customerRepository;
        this.transactionHandlerFactory = transactionHandlerFactory;
    }

    public Customer save(Customer customer) {
        boolean isNewCustomer = customer.getId() == null;
        Customer savedCustomer = customerRepository.save(customer);
        
        // Transaction context oluştur
        TransactionContext context = TransactionContext.builder()
            .relatedEntity("Customer")
            .amount(0.0) // Müşteri işlemlerinde tutar yok
            .date(LocalDateTime.now())
            .additionalData(Map.of(
                "customer", savedCustomer,
                "transactionType", isNewCustomer ? TransactionType.CUSTOMER_ADD : TransactionType.CUSTOMER_UPDATE
            ))
            .build();

        // İlgili handler'ı bul ve işlemi gerçekleştir
        TransactionHandler handler = transactionHandlerFactory.getHandler(
            isNewCustomer ? TransactionType.CUSTOMER_ADD : TransactionType.CUSTOMER_UPDATE
        );
        handler.handleTransaction(context);

        return savedCustomer;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer findCustomerById(Long id) {
        return customerRepository.findById(id).orElse(null);
    }

    public List<Customer> searchCustomers(String name, String phone, String balanceStatus) {
        List<Customer> customers = customerRepository.findAll();
        
        // Filtreleme işlemleri
        if (name != null && !name.trim().isEmpty()) {
            customers = customers.stream()
                .filter(c -> (c.getFirstName() + " " + c.getLastName())
                    .toLowerCase()
                    .contains(name.toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (phone != null && !phone.trim().isEmpty()) {
            customers = customers.stream()
                .filter(c -> c.getPhoneNumber().contains(phone))
                .collect(Collectors.toList());
        }
        
        if (balanceStatus != null) {
            switch (balanceStatus) {
                case "positive":
                    customers = customers.stream()
                        .filter(c -> c.getBalance().compareTo(BigDecimal.ZERO) > 0)
                        .collect(Collectors.toList());
                    break;
                case "negative":
                    customers = customers.stream()
                        .filter(c -> c.getBalance().compareTo(BigDecimal.ZERO) < 0)
                        .collect(Collectors.toList());
                    break;
                case "zero":
                    customers = customers.stream()
                        .filter(c -> c.getBalance().compareTo(BigDecimal.ZERO) == 0)
                        .collect(Collectors.toList());
                    break;
            }
        }
        
        return customers;
    }
}

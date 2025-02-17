package com.halilsahin.stockautomation.transaction.handler;

import com.halilsahin.stockautomation.entity.Customer;
import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import com.halilsahin.stockautomation.transaction.TransactionContext;
import com.halilsahin.stockautomation.transaction.TransactionDescriptionBuilder;
import com.halilsahin.stockautomation.transaction.TransactionHandler;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class CustomerTransactionHandler implements TransactionHandler {
    private final TransactionRepository transactionRepository;

    public CustomerTransactionHandler(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public TransactionType getType() {
        return TransactionType.CUSTOMER_ADD;
    }

    @Override
    public void handleTransaction(TransactionContext context) {
        Customer customer = (Customer) context.getAdditionalData().get("customer");
        TransactionType type = (TransactionType) context.getAdditionalData().get("transactionType");
        
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setCustomer(customer);
        transaction.setDescription(buildDescription(customer, type));
        transaction.setAmount(BigDecimal.ZERO);
        transaction.setDate(LocalDateTime.now());
        
        transactionRepository.save(transaction);
    }

    private String buildDescription(Customer customer, TransactionType type) {
        return new TransactionDescriptionBuilder()
            .appendCustomerInfo(customer, type)
            .build();
    }
} 
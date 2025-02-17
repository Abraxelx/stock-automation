package com.halilsahin.stockautomation.transaction.handler;

import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import com.halilsahin.stockautomation.transaction.TransactionContext;
import com.halilsahin.stockautomation.transaction.TransactionDescriptionBuilder;
import com.halilsahin.stockautomation.transaction.TransactionHandler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StockTransactionHandler implements TransactionHandler {
    private final TransactionRepository transactionRepository;

    public StockTransactionHandler(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public TransactionType getType() {
        return TransactionType.STOCK_IN; // Varsayılan olarak STOCK_IN
    }

    @Override
    public void handleTransaction(TransactionContext context) {
        Product product = (Product) context.getAdditionalData().get("product");
        Integer quantity = (Integer) context.getAdditionalData().get("quantity");
        TransactionType type = (TransactionType) context.getAdditionalData().get("transactionType");
        
        Transaction transaction = Transaction.createStockTransaction(
            product,
            quantity,
            BigDecimal.valueOf(context.getAmount())
        );
        
        transaction.setDescription(buildDescription(product, quantity, type));
        transactionRepository.save(transaction);
    }

    private String buildDescription(Product product, int quantity, TransactionType type) {
        return new TransactionDescriptionBuilder()
            .appendStockInfo(product, quantity, type)
            .build();
    }
} 
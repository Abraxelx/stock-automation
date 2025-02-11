package com.halilsahin.stockautomation.transaction.handler;

import com.halilsahin.stockautomation.entity.Sale;
import com.halilsahin.stockautomation.entity.SaleItem;
import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import com.halilsahin.stockautomation.transaction.TransactionDescriptionBuilder;
import com.halilsahin.stockautomation.transaction.TransactionHandler;
import org.springframework.stereotype.Component;
import com.halilsahin.stockautomation.transaction.TransactionContext;

import java.util.List;

@Component
public class SaleTransactionHandler implements TransactionHandler {
    private final TransactionRepository transactionRepository;

    public SaleTransactionHandler(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public TransactionType getType() {
        return TransactionType.SALE;
    }

    @Override
    public void handleTransaction(TransactionContext context) {
        Sale sale = (Sale) context.getAdditionalData().get("sale");
        List<SaleItem> items = (List<SaleItem>) context.getAdditionalData().get("items");
        
        Transaction transaction = Transaction.builder()
            .transactionType(TransactionType.SALE)
            .amount(context.getAmount())
            .date(context.getDate())
            .description(buildDescription(sale, items))
            .build();
            
        transactionRepository.save(transaction);
    }

    private String buildDescription(Sale sale, List<SaleItem> items) {
        return new TransactionDescriptionBuilder()
            .append("Satılan Ürünler: ")
            .appendItems(items)
            .appendDiscountInfo(sale.getDiscountRate())
            .build();
    }
} 
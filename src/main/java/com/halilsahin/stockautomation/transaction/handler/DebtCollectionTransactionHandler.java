package com.halilsahin.stockautomation.transaction.handler;

import com.halilsahin.stockautomation.entity.Debt;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.transaction.TransactionContext;
import com.halilsahin.stockautomation.transaction.TransactionHandler;
import org.springframework.stereotype.Component;

@Component
public class DebtCollectionTransactionHandler implements TransactionHandler {
    
    @Override
    public TransactionType getType() {
        return TransactionType.DEBT_COLLECTION;
    }

    @Override
    public void handleTransaction(TransactionContext context) {
        // Borç tahsilat işlemi
        Debt debt = (Debt) context.getAdditionalData().get("debt");
        // İşlemler...
    }
} 
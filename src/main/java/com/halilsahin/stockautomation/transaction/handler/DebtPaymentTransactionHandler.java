package com.halilsahin.stockautomation.transaction.handler;

import com.halilsahin.stockautomation.entity.Debt;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.transaction.TransactionContext;
import com.halilsahin.stockautomation.transaction.TransactionHandler;
import org.springframework.stereotype.Component;

@Component
public class DebtPaymentTransactionHandler implements TransactionHandler {
    
    @Override
    public TransactionType getType() {
        return TransactionType.DEBT_PAYMENT;
    }

    @Override
    public void handleTransaction(TransactionContext context) {
        // Borç ödeme işlemi
        Debt debt = (Debt) context.getAdditionalData().get("debt");
        // İşlemler...
    }
} 
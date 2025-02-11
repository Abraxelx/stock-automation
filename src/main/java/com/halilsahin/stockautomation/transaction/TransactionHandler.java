package com.halilsahin.stockautomation.transaction;

import com.halilsahin.stockautomation.enums.TransactionType;

public interface TransactionHandler {
    TransactionType getType();
    void handleTransaction(TransactionContext context);
} 
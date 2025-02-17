package com.halilsahin.stockautomation.transaction;

import com.halilsahin.stockautomation.enums.TransactionType;

public interface TransactionHandler<T> {
    TransactionType getType();
    void handleTransaction(TransactionContext<T> context);
    
    default boolean supports(TransactionType type) {
        return getType() == type;
    }
} 
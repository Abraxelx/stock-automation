package com.halilsahin.stockautomation.transaction;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
public class TransactionContext<T> {
    private String relatedEntity;
    private double amount;
    private LocalDateTime date;
    private Map<String, T> additionalData;

    @Builder
    public TransactionContext(String relatedEntity, double amount, LocalDateTime date, Map<String, T> additionalData) {
        this.relatedEntity = relatedEntity;
        this.amount = amount;
        this.date = date;
        this.additionalData = additionalData;
    }

    public T getData(String key) {
        return additionalData.get(key);
    }
} 
package com.halilsahin.stockautomation.transaction;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class TransactionContext {
    private final String relatedEntity;
    private final double amount;
    private final LocalDateTime date;
    private final Map<String, Object> additionalData;
    // Diğer gerekli alanlar
} 
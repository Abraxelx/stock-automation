package com.halilsahin.stockautomation.enums;

public enum PaymentMethod {
    CASH("Nakit"),
    CREDIT_CARD("Kredi Kartı"),
    BANK_TRANSFER("Havale/EFT");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 
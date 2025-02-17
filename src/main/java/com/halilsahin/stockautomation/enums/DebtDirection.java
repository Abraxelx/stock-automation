package com.halilsahin.stockautomation.enums;

import lombok.Getter;

@Getter
public enum DebtDirection {
    PAYABLE("Borç Al"),
    RECEIVABLE("Borç Ver");

    private final String displayName;

    DebtDirection(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 
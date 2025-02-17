package com.halilsahin.stockautomation.enums;

public enum UnitType {
    PIECE("ADET"),
    SET("TAKIM"),
    PACKAGE("PAKET");

    private final String displayName;

    UnitType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 
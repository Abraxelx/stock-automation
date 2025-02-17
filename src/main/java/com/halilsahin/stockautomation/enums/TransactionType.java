package com.halilsahin.stockautomation.enums;

import lombok.Getter;

@Getter
public enum TransactionType {
    // Borç işlemleri
    DEBT_IN("Borç Alma"),
    DEBT_OUT("Borç Verme"),
    DEBT_COLLECTION("Alacak Tahsilatı"),
    DEBT_PAYMENT("Borç Ödemesi"),
    
    // Stok işlemleri
    STOCK_IN("Stok Girişi"),
    STOCK_OUT("Stok Çıkışı"),
    
    // Satış/Alış işlemleri
    SALE("Satış"),
    PURCHASE("Alış"),
    
    // Müşteri işlemleri
    CUSTOMER_ADD("Müşteri Kaydı"),
    CUSTOMER_UPDATE("Müşteri Güncelleme");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }
}

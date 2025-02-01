package com.halilsahin.stockautomation.enums;

import lombok.Getter;

@Getter
public enum TransactionType {
        SALE("SATIŞ"),
        STOCK_IN("STOK GİRİŞİ"),
        STOCK_OUT("STOK ÇIKIŞI"),
        DEBT_IN("BORÇ GİRİŞİ"),
        DEBT_OUT("BORÇ ÇIKIŞI"),
        DEBT_RE_PAYMENT("BORÇ ÖDEME");

        private final String description;

        TransactionType(String description) {
            this.description = description;
        }

}

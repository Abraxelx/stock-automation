package com.halilsahin.stockautomation.enums;

public enum TransactionType {
        SALE("SATIŞ"),
        STOCK_IN("STOK GİRİŞİ"),
        STOCK_OUT("STOK ÇIKIŞI"),
        DEBT_IN("BORÇ GİRİŞİ"),
        DEBT_OUT("BORÇ ÇIKIŞI");

        private final String description;

        TransactionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
}

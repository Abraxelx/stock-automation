package com.halilsahin.stockautomation.enums;

import lombok.Getter;

@Getter
public enum DebtType {

    CASH("SATIŞ"),
    BILL("STOK GİRİŞİ"),
    PRODUCT("STOK ÇIKIŞI"),
    CHECK("BORÇ GİRİŞİ");

    private final String description;

    DebtType(String description) {
        this.description = description;
    }

}

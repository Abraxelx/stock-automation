package com.halilsahin.stockautomation.enums;

import lombok.Getter;

@Getter
public enum DebtType {

    CASH("Nakit"),
    BILL("Senet"),
    PRODUCT("Mal"),
    CREDIT_CARD("Kredi Kartı"),
    CHECK("Çek");

    //language=IgnoreLang
    private final String displayName;

    DebtType(String displayName) {
        this.displayName = displayName;
    }

}

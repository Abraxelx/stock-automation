package com.halilsahin.stockautomation.enums;

import lombok.Getter;

@Getter
public enum DebtType {

    CASH("NAKİT"),
    BILL("SENET"),
    PRODUCT("MAL"),
    CREDIT_CARD("CREDIT"),
    CHECK("ÇEK");

    //language=IgnoreLang
    private final String description;

    DebtType(String description) {
        this.description = description;
    }

}

package com.halilsahin.stockautomation.transaction;

import com.halilsahin.stockautomation.entity.SaleItem;
import java.util.List;

public class TransactionDescriptionBuilder {
    private final StringBuilder description = new StringBuilder();

    public TransactionDescriptionBuilder append(String text) {
        description.append(text);
        return this;
    }

    public TransactionDescriptionBuilder appendItems(List<SaleItem> items) {
        for (int i = 0; i < items.size(); i++) {
            SaleItem item = items.get(i);
            description.append(item.getQuantity())
                .append(" Adet ")
                .append(item.getProduct().getName());
            
            if (i < items.size() - 1) {
                description.append(", ");
            }
        }
        return this;
    }

    public TransactionDescriptionBuilder appendDiscountInfo(double discountRate) {
        if (discountRate > 0) {
            description.append(String.format(" (Toplam %%%.1f iskonto uygulandı)", discountRate));
        }
        return this;
    }

    public String build() {
        return description.toString();
    }
} 
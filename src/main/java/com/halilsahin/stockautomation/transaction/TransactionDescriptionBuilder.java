package com.halilsahin.stockautomation.transaction;

import com.halilsahin.stockautomation.entity.Debt;
import com.halilsahin.stockautomation.entity.SaleItem;
import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.entity.Customer;
import com.halilsahin.stockautomation.enums.TransactionType;
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

    public TransactionDescriptionBuilder appendDebtInfo(Debt debt, TransactionType type) {
        String action;
        switch (type) {
            case DEBT_IN:
                action = "Borç Alındı";
                break;
            case DEBT_OUT:
                action = "Borç Verildi";
                break;
            case DEBT_PAYMENT:
                action = "Borç Ödendi";
                break;
            case DEBT_COLLECTION:
                action = "Alacak Tahsil Edildi";
                break;
            default:
                action = "Borç İşlemi";
        }

        description.append(action)
            .append(" - ")
            .append(debt.getCustomer().getFirstName())
            .append(" ")
            .append(debt.getCustomer().getLastName())
            .append(" - ")
            .append(String.format("%.2f TL", debt.getAmount()));

        if (debt.getDebtType() != null) {
            description.append(" (")
                .append(debt.getDebtType().getDisplayName())
                .append(")");
        }

        return this;
    }

    public TransactionDescriptionBuilder appendStockInfo(Product product, int quantity, TransactionType type) {
        String action = type == TransactionType.STOCK_IN ? "Stok Girişi" : "Stok Çıkışı";
        
        description.append(action)
            .append(" - ")
            .append(product.getName())
            .append(" - ")
            .append(Math.abs(quantity))
            .append(" Adet");

        if (product.getPrice() != null) {
            description.append(" (Birim Fiyat: ")
                .append(String.format("%.2f TL", product.getPrice()))
                .append(")");
        }

        return this;
    }

    public TransactionDescriptionBuilder appendCustomerInfo(Customer customer, TransactionType type) {
        String action;
        switch (type) {
            case CUSTOMER_ADD:
                action = "Yeni Müşteri Kaydı";
                break;
            case CUSTOMER_UPDATE:
                action = "Müşteri Bilgileri Güncellendi";
                break;
            default:
                action = "Müşteri İşlemi";
        }

        description.append(action)
            .append(" - ")
            .append(customer.getFirstName())
            .append(" ")
            .append(customer.getLastName());

        if (customer.getPhoneNumber() != null) {
            description.append(" (")
                .append(customer.getPhoneNumber())
                .append(")");
        }

        return this;
    }

    public String build() {
        return description.toString();
    }
} 
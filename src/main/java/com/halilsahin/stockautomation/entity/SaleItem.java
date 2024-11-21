package com.halilsahin.stockautomation.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class SaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private double unitPrice;

    @Column(nullable = false)
    private double subtotal; // Ara toplam (quantity * unitPrice)

    public double getTotal() {
        return quantity * unitPrice;
    }
}


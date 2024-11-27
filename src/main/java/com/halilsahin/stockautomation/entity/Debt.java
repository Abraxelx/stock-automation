package com.halilsahin.stockautomation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Debt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private double amount;        // Borç miktarı
    private LocalDateTime dueDate; // Vadesi
    private LocalDateTime paymentDate; // Ödeme tarihi, eğer ödeme yapılmışsa
    private boolean isPaid;      // Ödeme durumu (ödenmiş/ödenmemiş)

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = true)
    private Product product;  // Ürün bilgisi
}

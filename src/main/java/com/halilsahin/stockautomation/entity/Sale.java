package com.halilsahin.stockautomation.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL)
    private List<SaleItem> items;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private double total;

    @Column
    private double discountRate = 0;

    @Column
    private double finalTotal;
}


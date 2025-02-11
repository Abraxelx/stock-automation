package com.halilsahin.stockautomation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SaleItem> saleItems = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private double total;

    @Column
    private double discountRate = 0;

    @Column
    private double finalTotal;

    // Yardımcı metod
    public void addSaleItem(SaleItem item) {
        saleItems.add(item);
        item.setSale(this);
    }
}


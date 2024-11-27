package com.halilsahin.stockautomation.entity;

import com.halilsahin.stockautomation.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    private String description;

    private double amount;

    private LocalDateTime date;

    private String relatedEntity;
}

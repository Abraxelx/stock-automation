package com.halilsahin.stockautomation.entity;

import com.halilsahin.stockautomation.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    // İlişkili entity referansları
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // İşlem detayları
    @Column(length = 1000)
    private String description;

    @Column(name = "previous_balance")
    private BigDecimal previousBalance;

    @Column(name = "new_balance")
    private BigDecimal newBalance;

    // Audit bilgileri
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Factory metodları
    public static Transaction createSaleTransaction(Sale sale, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.SALE);
        transaction.setAmount(amount);
        transaction.setSale(sale);
        transaction.setDate(LocalDateTime.now());
        return transaction;
    }

    public static Transaction createStockTransaction(Product product, int quantity, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setType(quantity > 0 ? TransactionType.STOCK_IN : TransactionType.STOCK_OUT);
        transaction.setAmount(amount);
        transaction.setProduct(product);
        transaction.setDate(LocalDateTime.now());
        return transaction;
    }

    public static Transaction createDebtTransaction(Customer customer, BigDecimal amount, 
            BigDecimal previousBalance, BigDecimal newBalance) {
        Transaction transaction = new Transaction();
        transaction.setType(amount.compareTo(BigDecimal.ZERO) > 0 ? 
            TransactionType.DEBT_IN : TransactionType.DEBT_OUT);
        transaction.setAmount(amount.abs());
        transaction.setCustomer(customer);
        transaction.setPreviousBalance(previousBalance);
        transaction.setNewBalance(newBalance);
        transaction.setDate(LocalDateTime.now());
        return transaction;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Transaction that = (Transaction) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}

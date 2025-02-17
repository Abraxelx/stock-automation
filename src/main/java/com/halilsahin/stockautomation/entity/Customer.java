package com.halilsahin.stockautomation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import jakarta.persistence.Column;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.PrePersist;
import jakarta.persistence.OneToMany;
import java.util.List;
import java.util.ArrayList;

import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String address;
    private String notes; // Müşteri hakkında notlar

    @Column(nullable = false)
    private LocalDateTime registrationDate;
    
    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Column(name = "credit_limit", nullable = false, precision = 10, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;
    
    @Column(name = "total_debt", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDebt = BigDecimal.ZERO;
    
    @Column(name = "total_credit", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCredit = BigDecimal.ZERO;
    
    @OneToMany(mappedBy = "customer")
    private List<Debt> debts = new ArrayList<>();
    
    @OneToMany(mappedBy = "customer")
    private List<Transaction> transactions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        registrationDate = LocalDateTime.now();
    }

    public boolean canTakeDebt(BigDecimal amount) {
        return totalDebt.add(amount).compareTo(creditLimit) <= 0;
    }

    public void updateBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Customer customer = (Customer) o;
        return getId() != null && Objects.equals(getId(), customer.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}

package com.halilsahin.stockautomation.entity;

import com.halilsahin.stockautomation.enums.DebtType;
import com.halilsahin.stockautomation.enums.PaymentMethod;
import com.halilsahin.stockautomation.enums.DebtDirection;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Debt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private double amount;        // Borç miktarı
    private LocalDateTime dueDate; // Vadesi
    @Column(name = "payment_date")
    private LocalDateTime paymentDate; // Ödeme tarihi
    private boolean isPaid;      // Ödeme durumu

    @Column(name = "last_payment_date")
    private LocalDateTime lastPaymentDate;

    private String description;   // Borç açıklaması
    private LocalDateTime createdAt; // Oluşturulma tarihi
    private String createdBy;     // Oluşturan kullanıcı

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;  // Ödeme yöntemi

    @Enumerated(EnumType.STRING)
    private DebtType debtType;  // Borç türü (MAL, ÇEK, SENET, PARA)

    @Enumerated(EnumType.STRING)
    private DebtDirection direction; // Borç yönü (PAYABLE/RECEIVABLE)

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;    // İşlem yapılan müşteri

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = true)
    private Product product;  // Ürün bilgisi (varsa)

    @OneToMany(mappedBy = "debt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "debt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Installment> installments;

    @OneToOne(mappedBy = "debt", fetch = FetchType.LAZY)
    private Transaction transaction;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Debt debt = (Debt) o;
        return getId() != null && Objects.equals(getId(), debt.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}

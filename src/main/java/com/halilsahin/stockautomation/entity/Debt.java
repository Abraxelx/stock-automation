package com.halilsahin.stockautomation.entity;

import com.halilsahin.stockautomation.enums.DebtType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
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
    private LocalDateTime paymentDate; // Ödeme tarihi, eğer ödeme yapılmışsa
    private boolean isPaid;      // Ödeme durumu (ödenmiş/ödenmemiş)

    @Enumerated(EnumType.STRING)
    private DebtType debtType;  // Borç türü (MAL, ÇEK, SENET, PARA)

    @ManyToOne
    @JoinColumn(name = "debtor_id")
    private Customer debtor; //Borç alan

    @ManyToOne
    @JoinColumn(name = "creditor_id")    // Borç veren kişi
    private Customer creditor;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = true)
    private Product product;  // Ürün bilgisi

    @OneToOne(mappedBy = "debt", cascade = CascadeType.ALL)
    private Document document; // Belge ilişkisi

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

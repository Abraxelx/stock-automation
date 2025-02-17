package com.halilsahin.stockautomation.transaction.handler;

import com.halilsahin.stockautomation.entity.Debt;
import com.halilsahin.stockautomation.entity.Transaction;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import com.halilsahin.stockautomation.transaction.TransactionContext;
import com.halilsahin.stockautomation.transaction.TransactionDescriptionBuilder;
import com.halilsahin.stockautomation.transaction.TransactionHandler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DebtTransactionHandler implements TransactionHandler {
    private final TransactionRepository transactionRepository;

    public DebtTransactionHandler(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public TransactionType getType() {
        return TransactionType.DEBT_IN; // Tek bir tip yerine birden fazla tipi destekleyelim
    }

    @Override
    public boolean supports(TransactionType type) {
        // Borç işlemlerinin tümünü destekle
        return type == TransactionType.DEBT_IN || 
               type == TransactionType.DEBT_OUT || 
               type == TransactionType.DEBT_PAYMENT || 
               type == TransactionType.DEBT_COLLECTION;
    }

    @Override
    public void handleTransaction(TransactionContext context) {
        Debt debt = (Debt) context.getAdditionalData().get("debt");
        TransactionType type = (TransactionType) context.getAdditionalData().get("transactionType");
        
        Transaction transaction = Transaction.createDebtTransaction(
            debt.getCustomer(),
            BigDecimal.valueOf(context.getAmount()),
            debt.getCustomer().getBalance(),
            calculateNewBalance(debt.getCustomer().getBalance(), context.getAmount(), type)
        );
        
        transaction.setType(type); // İşlem tipini ayarla
        transaction.setDescription(buildDescription(debt, type));
        transaction.setDebt(debt); // Borç referansını ekle
        transaction.setDate(LocalDateTime.now());
        
        transactionRepository.save(transaction);
    }

    private BigDecimal calculateNewBalance(BigDecimal currentBalance, double amount, TransactionType type) {
        BigDecimal transactionAmount = BigDecimal.valueOf(amount);
        
        switch (type) {
            case DEBT_IN:
            case DEBT_COLLECTION:
                return currentBalance.add(transactionAmount);
            case DEBT_OUT:
            case DEBT_PAYMENT:
                return currentBalance.subtract(transactionAmount);
            default:
                throw new IllegalArgumentException("Geçersiz işlem tipi: " + type);
        }
    }

    private String buildDescription(Debt debt, TransactionType type) {
        return new TransactionDescriptionBuilder()
            .appendDebtInfo(debt, type)
            .build();
    }
} 
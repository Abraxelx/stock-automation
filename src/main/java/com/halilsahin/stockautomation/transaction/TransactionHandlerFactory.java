package com.halilsahin.stockautomation.transaction;

import com.halilsahin.stockautomation.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TransactionHandlerFactory {
    private final List<TransactionHandler> handlers;

    public TransactionHandlerFactory(List<TransactionHandler> handlerList) {
        this.handlers = handlerList;
    }

    public TransactionHandler getHandler(TransactionType type) {
        return handlers.stream()
            .filter(handler -> handler.supports(type))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No handler found for type: " + type));
    }
} 
package com.halilsahin.stockautomation.transaction;

import com.halilsahin.stockautomation.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TransactionHandlerFactory {
    private final Map<TransactionType, TransactionHandler> handlers;

    public TransactionHandlerFactory(List<TransactionHandler> handlerList) {
        this.handlers = handlerList.stream()
            .collect(Collectors.toMap(
                TransactionHandler::getType,
                handler -> handler
            ));
    }

    public TransactionHandler getHandler(TransactionType type) {
        return handlers.get(type);
    }
} 
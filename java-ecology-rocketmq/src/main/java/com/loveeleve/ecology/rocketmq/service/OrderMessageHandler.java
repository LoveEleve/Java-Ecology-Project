package com.loveeleve.ecology.rocketmq.service;

import com.loveeleve.ecology.rocketmq.dto.OrderEvent;
import org.springframework.stereotype.Service;

@Service
public class OrderMessageHandler {

    private final ProcessedOrderStore store;

    public OrderMessageHandler(ProcessedOrderStore store) {
        this.store = store;
    }

    public void handle(OrderEvent event) {
        ProcessedOrderStore.ClaimResult claim = store.tryClaim(event.getOrderId());
        if (claim == ProcessedOrderStore.ClaimResult.SUCCESS) {
            return;
        }
        if (claim == ProcessedOrderStore.ClaimResult.IN_PROGRESS) {
            throw new IllegalStateException("order is being processed by another consumer");
        }
        try {
            if (event.isFail()) {
                throw new IllegalStateException("simulated RocketMQ downstream failure");
            }
            store.markSuccess(event.getOrderId());
        } catch (RuntimeException ex) {
            store.markFailed(event.getOrderId());
            throw ex;
        }
    }
}

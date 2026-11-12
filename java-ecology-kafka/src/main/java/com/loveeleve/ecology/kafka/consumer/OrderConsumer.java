package com.loveeleve.ecology.kafka.consumer;

import com.loveeleve.ecology.kafka.config.KafkaTopics;
import com.loveeleve.ecology.kafka.dto.OrderEvent;
import com.loveeleve.ecology.kafka.service.ProcessedOrderStore;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {

    private final ProcessedOrderStore store;

    public OrderConsumer(ProcessedOrderStore store) {
        this.store = store;
    }

    @KafkaListener(topics = KafkaTopics.ORDERS, groupId = "${app.kafka.consumer-group:ecology-kafka}")
    public void consume(OrderEvent event, Acknowledgment acknowledgment) {
        if (store.contains(event.getOrderId())) {
            acknowledgment.acknowledge();
            return;
        }
        if (event.isFail()) {
            throw new IllegalStateException("simulated downstream failure for order " + event.getOrderId());
        }
        store.markProcessedIfAbsent(event.getOrderId());
        acknowledgment.acknowledge();
    }
}

package com.loveeleve.ecology.kafka.producer;

import com.loveeleve.ecology.kafka.config.KafkaTopics;
import com.loveeleve.ecology.kafka.dto.OrderEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OrderProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(OrderEvent event) {
        try {
            kafkaTemplate.send(KafkaTopics.ORDERS, event.getOrderId(), event)
                    .get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to publish order event", ex);
        }
    }

    public void sendBatch(List<OrderEvent> events) {
        try {
            events.forEach(event -> kafkaTemplate.send(KafkaTopics.ORDERS_BATCH, event.getOrderId(), event));
            kafkaTemplate.flush();
        } catch (Exception ex) {
            throw new IllegalStateException("failed to publish batch order events", ex);
        }
    }
}

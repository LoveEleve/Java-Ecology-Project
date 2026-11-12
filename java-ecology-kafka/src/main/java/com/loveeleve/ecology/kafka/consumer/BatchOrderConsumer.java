package com.loveeleve.ecology.kafka.consumer;

import com.loveeleve.ecology.kafka.config.KafkaTopics;
import com.loveeleve.ecology.kafka.dto.OrderEvent;
import com.loveeleve.ecology.kafka.service.ProcessedOrderStore;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BatchOrderConsumer {

    private final ProcessedOrderStore store;

    public BatchOrderConsumer(ProcessedOrderStore store) {
        this.store = store;
    }

    @KafkaListener(
            topics = KafkaTopics.ORDERS_BATCH,
            groupId = "${app.kafka.batch-group:ecology-kafka-batch}",
            containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consume(List<OrderEvent> events, Acknowledgment acknowledgment) {
        for (OrderEvent event : events) {
            if (store.contains(event.getOrderId())) {
                continue;
            }
            if (event.isFail()) {
                throw new IllegalStateException("simulated batch failure for order " + event.getOrderId());
            }
            store.markProcessedIfAbsent(event.getOrderId());
        }
        acknowledgment.acknowledge();
    }
}

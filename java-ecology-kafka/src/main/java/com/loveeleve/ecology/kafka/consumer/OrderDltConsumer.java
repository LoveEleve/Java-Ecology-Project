package com.loveeleve.ecology.kafka.consumer;

import com.loveeleve.ecology.kafka.config.KafkaTopics;
import com.loveeleve.ecology.kafka.dto.OrderEvent;
import com.loveeleve.ecology.kafka.service.ProcessedOrderStore;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OrderDltConsumer {

    private final ProcessedOrderStore store;

    public OrderDltConsumer(ProcessedOrderStore store) {
        this.store = store;
    }

    @KafkaListener(topics = KafkaTopics.ORDERS_DLT, groupId = "${app.kafka.dlt-group:ecology-kafka-dlt}")
    public void consume(ConsumerRecord<String, OrderEvent> record, Acknowledgment acknowledgment) {
        OrderEvent event = record.value();
        if (event != null) {
            store.markFailed(event.getOrderId());
        }
        acknowledgment.acknowledge();
    }
}

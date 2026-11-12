package com.loveeleve.ecology.kafka.controller;

import com.loveeleve.ecology.kafka.dto.OrderEvent;
import com.loveeleve.ecology.kafka.producer.OrderProducer;
import com.loveeleve.ecology.kafka.service.ProcessedOrderStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/kafka")
public class KafkaController {

    private final OrderProducer producer;
    private final ProcessedOrderStore store;

    public KafkaController(OrderProducer producer, ProcessedOrderStore store) {
        this.producer = producer;
        this.store = store;
    }

    @PostMapping("/orders")
    public ResponseEntity<String> publish(@RequestBody OrderEvent event) {
        producer.send(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(event.getOrderId());
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of("processed", store.processedCount(), "failed", store.failedCount());
    }
}

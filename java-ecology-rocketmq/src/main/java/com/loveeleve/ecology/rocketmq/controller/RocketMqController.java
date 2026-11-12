package com.loveeleve.ecology.rocketmq.controller;

import com.loveeleve.ecology.rocketmq.dto.OrderEvent;
import com.loveeleve.ecology.rocketmq.producer.OrderProducer;
import com.loveeleve.ecology.rocketmq.service.ProcessedOrderStore;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/rocketmq")
public class RocketMqController {

    private final OrderProducer producer;
    private final ProcessedOrderStore store;

    public RocketMqController(OrderProducer producer, ProcessedOrderStore store) {
        this.producer = producer;
        this.store = store;
    }

    @PostMapping("/orders")
    public ResponseEntity<SendResult> sync(@RequestBody OrderEvent event) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(producer.syncSend(event));
    }

    @PostMapping("/orders/batch")
    public ResponseEntity<SendResult> batch(@RequestBody java.util.List<OrderEvent> events) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(producer.sendBatch(events));
    }

    @PostMapping("/orders/async")
    public ResponseEntity<String> async(@RequestBody OrderEvent event) {
        producer.asyncSend(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(event.getOrderId());
    }

    @PostMapping("/orders/oneway")
    public ResponseEntity<String> oneWay(@RequestBody OrderEvent event) {
        producer.sendOneWay(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(event.getOrderId());
    }

    @PostMapping("/orders/delay")
    public ResponseEntity<SendResult> delay(@RequestBody OrderEvent event,
                                            @RequestParam(defaultValue = "3") int level) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(producer.sendDelay(event, level));
    }

    @PostMapping("/orders/ordered")
    public ResponseEntity<SendResult> ordered(@RequestBody OrderEvent event) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(producer.sendOrdered(event));
    }

    @PostMapping("/orders/transaction")
    public ResponseEntity<SendResult> transaction(@RequestBody OrderEvent event) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(producer.sendTransaction(event));
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of("processed", store.processedCount());
    }
}

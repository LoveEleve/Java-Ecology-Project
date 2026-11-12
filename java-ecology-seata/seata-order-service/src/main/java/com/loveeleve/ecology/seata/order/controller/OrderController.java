package com.loveeleve.ecology.seata.order.controller;

import com.loveeleve.ecology.seata.api.CreateOrderRequest;
import com.loveeleve.ecology.seata.api.OrderResponse;
import com.loveeleve.ecology.seata.order.service.IdempotencyConflictException;
import com.loveeleve.ecology.seata.order.service.OrderInProgressException;
import com.loveeleve.ecology.seata.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderResponse create(@RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_REQUEST",
                "message", ex.getMessage()));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<Map<String, String>> idempotency(IdempotencyConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "IDEMPOTENCY_CONFLICT",
                "message", ex.getMessage()));
    }

    @ExceptionHandler(OrderInProgressException.class)
    public ResponseEntity<Map<String, String>> inProgress(OrderInProgressException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "ORDER_IN_PROGRESS",
                "message", ex.getMessage()));
    }

    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<Map<String, String>> remote(feign.FeignException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "REMOTE_PARTICIPANT_FAILED",
                "message", ex.getMessage()));
    }
}

package com.loveeleve.ecology.sharding.controller;

import com.loveeleve.ecology.sharding.common.ApiResponse;
import com.loveeleve.ecology.sharding.dto.CreateOrderRequest;
import com.loveeleve.ecology.sharding.dto.OrderResponse;
import com.loveeleve.ecology.sharding.exception.OrderNotFoundException;
import com.loveeleve.ecology.sharding.model.Order;
import com.loveeleve.ecology.sharding.repository.OrderRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody CreateOrderRequest request) {
        Order order = new Order(request.orderId(), request.userId(), request.amount(), request.status());
        orderRepository.insert(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(OrderResponse.from(order)));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> get(@PathVariable long orderId, @RequestParam long userId) {
        return ApiResponse.ok(orderRepository.find(orderId, userId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId)));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> list(@RequestParam long userId) {
        return ApiResponse.ok(orderRepository.findByUser(userId).stream().map(OrderResponse::from).toList());
    }

    @PutMapping("/{orderId}/status")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable long orderId,
                                                   @RequestParam long userId,
                                                   @RequestParam String status) {
        orderRepository.updateStatus(orderId, userId, status);
        return ApiResponse.ok(orderRepository.find(orderId, userId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId)));
    }
}

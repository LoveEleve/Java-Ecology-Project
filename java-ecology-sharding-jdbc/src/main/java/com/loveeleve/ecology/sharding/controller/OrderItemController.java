package com.loveeleve.ecology.sharding.controller;

import com.loveeleve.ecology.sharding.common.ApiResponse;
import com.loveeleve.ecology.sharding.dto.CreateOrderItemRequest;
import com.loveeleve.ecology.sharding.dto.OrderItemResponse;
import com.loveeleve.ecology.sharding.exception.OrderNotFoundException;
import com.loveeleve.ecology.sharding.model.OrderItem;
import com.loveeleve.ecology.sharding.repository.OrderItemRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/order-items")
public class OrderItemController {

    private final OrderItemRepository repository;

    public OrderItemController(OrderItemRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderItemResponse>> create(
            @Valid @RequestBody CreateOrderItemRequest request) {
        OrderItem item = new OrderItem(request.itemId(), request.orderId(), request.userId(),
                request.productName(), request.quantity());
        repository.insert(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(OrderItemResponse.from(item)));
    }

    @GetMapping("/{itemId}")
    public ApiResponse<OrderItemResponse> byItemId(@PathVariable long itemId) {
        return ApiResponse.ok(repository.findByItemId(itemId)
                .map(OrderItemResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(itemId)));
    }

    @GetMapping
    public ApiResponse<List<OrderItemResponse>> byOrder(@RequestParam long orderId,
                                                        @RequestParam long userId) {
        return ApiResponse.ok(repository.findByOrderId(orderId, userId).stream()
                .map(OrderItemResponse::from).toList());
    }
}

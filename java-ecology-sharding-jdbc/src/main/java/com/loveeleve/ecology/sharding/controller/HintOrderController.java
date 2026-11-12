package com.loveeleve.ecology.sharding.controller;

import com.loveeleve.ecology.sharding.common.ApiResponse;
import com.loveeleve.ecology.sharding.dto.CreateOrderRequest;
import com.loveeleve.ecology.sharding.dto.OrderResponse;
import com.loveeleve.ecology.sharding.exception.OrderNotFoundException;
import com.loveeleve.ecology.sharding.model.Order;
import com.loveeleve.ecology.sharding.service.HintOrderService;
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

@RestController
@RequestMapping("/api/hint/orders")
public class HintOrderController {

    private final HintOrderService hintOrderService;

    public HintOrderController(HintOrderService hintOrderService) {
        this.hintOrderService = hintOrderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestParam(defaultValue = "0") int database,
            @RequestParam(defaultValue = "0") int table) {
        Order order = new Order(request.orderId(), request.userId(), request.amount(), request.status());
        hintOrderService.insert(order, database, table);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(OrderResponse.from(order)));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> get(@PathVariable long orderId,
                                          @RequestParam(defaultValue = "0") int database,
                                          @RequestParam(defaultValue = "0") int table) {
        return ApiResponse.ok(hintOrderService.find(orderId, database, table)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId)));
    }
}

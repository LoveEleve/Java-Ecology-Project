package com.loveeleve.ecology.grpc.client.controller;

import com.loveeleve.ecology.grpc.api.CreateOrderRequest;
import com.loveeleve.ecology.grpc.api.OrderReply;
import com.loveeleve.ecology.grpc.client.service.OrderGrpcClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/grpc")
public class GrpcClientController {

    private final OrderGrpcClient client;

    public GrpcClientController(OrderGrpcClient client) {
        this.client = client;
    }

    @PostMapping("/orders")
    public OrderView create(@RequestBody OrderRequest request) {
        OrderReply reply = client.createOrder(CreateOrderRequest.newBuilder()
                .setRequestId(request.requestId())
                .setUserId(request.userId())
                .setProductId(request.productId())
                .setAmount(request.amount())
                .setQuantity(request.quantity())
                .build());
        return new OrderView(reply.getOrderId(), reply.getStatus(), reply.getRequestId());
    }

    public record OrderRequest(String requestId, long userId, long productId, String amount, int quantity) {
    }

    public record OrderView(long orderId, String status, String requestId) {
    }
}

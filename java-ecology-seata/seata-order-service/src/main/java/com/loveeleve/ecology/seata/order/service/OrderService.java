package com.loveeleve.ecology.seata.order.service;

import com.loveeleve.ecology.seata.api.AccountDebitRequest;
import com.loveeleve.ecology.seata.api.CreateOrderRequest;
import com.loveeleve.ecology.seata.api.OrderResponse;
import com.loveeleve.ecology.seata.api.StorageDeductRequest;
import com.loveeleve.ecology.seata.order.client.AccountClient;
import com.loveeleve.ecology.seata.order.client.StorageClient;
import com.loveeleve.ecology.seata.order.repository.OrderRecord;
import com.loveeleve.ecology.seata.order.repository.OrderRepository;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final AccountClient accountClient;
    private final StorageClient storageClient;

    public OrderService(OrderRepository orderRepository, AccountClient accountClient, StorageClient storageClient) {
        this.orderRepository = orderRepository;
        this.accountClient = accountClient;
        this.storageClient = storageClient;
    }

    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class, timeoutMills = 60000)
    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        validate(request);
        Optional<OrderRecord> existing = orderRepository.findByRequestId(request.requestId());
        if (existing.isPresent()) {
            return existingOrder(existing.get(), request);
        }

        long orderId;
        try {
            orderId = orderRepository.insert(request);
        } catch (DuplicateKeyException ex) {
            OrderRecord concurrent = orderRepository.findByRequestId(request.requestId())
                    .orElseThrow(() -> ex);
            return existingOrder(concurrent, request);
        }

        accountClient.debit(new AccountDebitRequest(request.requestId(), request.userId(), request.amount()));
        storageClient.deduct(new StorageDeductRequest(request.requestId(), request.productId(), request.quantity()));
        orderRepository.updateStatus(orderId, "CONFIRMED");
        return new OrderResponse(orderId, "CONFIRMED");
    }

    private OrderResponse existingOrder(OrderRecord existing, CreateOrderRequest request) {
        if (existing.userId() != request.userId()
                || existing.productId() != request.productId()
                || existing.amount().compareTo(request.amount()) != 0
                || existing.quantity() != request.quantity()) {
            throw new IdempotencyConflictException("requestId was used with another payload");
        }
        if (!"CONFIRMED".equals(existing.status())) {
            throw new OrderInProgressException("order request is not confirmed: " + existing.status());
        }
        return new OrderResponse(existing.orderId(), existing.status());
    }

    private void validate(CreateOrderRequest request) {
        if (request == null || !StringUtils.hasText(request.requestId())
                || request.userId() == null || request.productId() == null
                || request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0
                || request.quantity() == null || request.quantity() <= 0) {
            throw new IllegalArgumentException("invalid create order request");
        }
    }
}

package com.loveeleve.ecology.seata.order;

import com.loveeleve.ecology.seata.api.AccountDebitRequest;
import com.loveeleve.ecology.seata.api.CreateOrderRequest;
import com.loveeleve.ecology.seata.api.OrderResponse;
import com.loveeleve.ecology.seata.api.StorageDeductRequest;
import com.loveeleve.ecology.seata.order.client.AccountClient;
import com.loveeleve.ecology.seata.order.client.StorageClient;
import com.loveeleve.ecology.seata.order.repository.OrderRecord;
import com.loveeleve.ecology.seata.order.repository.OrderRepository;
import com.loveeleve.ecology.seata.order.service.IdempotencyConflictException;
import com.loveeleve.ecology.seata.order.service.OrderInProgressException;
import com.loveeleve.ecology.seata.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderServiceTests {

    private OrderRepository orderRepository;
    private AccountClient accountClient;
    private StorageClient storageClient;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        accountClient = mock(AccountClient.class);
        storageClient = mock(StorageClient.class);
        orderService = new OrderService(orderRepository, accountClient, storageClient);
    }

    @Test
    void createPersistsOrderThenCallsRemoteParticipantsInOrder() {
        CreateOrderRequest request = request("order-1");
        when(orderRepository.findByRequestId("order-1")).thenReturn(Optional.empty());
        when(orderRepository.insert(request)).thenReturn(100L);

        OrderResponse response = orderService.create(request);

        assertThat(response).isEqualTo(new OrderResponse(100L, "CONFIRMED"));
        InOrder inOrder = inOrder(orderRepository, accountClient, storageClient);
        inOrder.verify(orderRepository).insert(request);
        inOrder.verify(accountClient).debit(new AccountDebitRequest("order-1", 1L, new BigDecimal("100.00")));
        inOrder.verify(storageClient).deduct(new StorageDeductRequest("order-1", 2L, 3));
        inOrder.verify(orderRepository).updateStatus(100L, "CONFIRMED");
    }

    @Test
    void existingRequestIsIdempotent() {
        CreateOrderRequest request = request("order-2");
        when(orderRepository.findByRequestId("order-2"))
                .thenReturn(Optional.of(new OrderRecord(200L, "CONFIRMED", 1L, 2L,
                        new BigDecimal("100.00"), 3)));

        OrderResponse response = orderService.create(request);

        assertThat(response).isEqualTo(new OrderResponse(200L, "CONFIRMED"));
        verify(orderRepository, never()).insert(any());
        verifyNoInteractions(accountClient, storageClient);
    }

    @Test
    void reusingRequestIdWithDifferentPayloadIsRejected() {
        CreateOrderRequest request = request("order-4");
        when(orderRepository.findByRequestId("order-4"))
                .thenReturn(Optional.of(new OrderRecord(400L, "CONFIRMED", 1L, 99L,
                        new BigDecimal("100.00"), 3)));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(IdempotencyConflictException.class);
        verifyNoInteractions(accountClient, storageClient);
    }

    @Test
    void existingUnconfirmedOrderIsNotReportedAsSuccess() {
        CreateOrderRequest request = request("order-5");
        when(orderRepository.findByRequestId("order-5"))
                .thenReturn(Optional.of(new OrderRecord(500L, "CREATED", 1L, 2L,
                        new BigDecimal("100.00"), 3)));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(OrderInProgressException.class);
        verifyNoInteractions(accountClient, storageClient);
    }

    @Test
    void accountFailureStopsBeforeStorageCall() {
        CreateOrderRequest request = request("order-3");
        when(orderRepository.findByRequestId("order-3")).thenReturn(Optional.empty());
        when(orderRepository.insert(request)).thenReturn(300L);
        doThrow(new IllegalStateException("account down")).when(accountClient).debit(any());

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("account down");
        verify(storageClient, never()).deduct(any());
        verify(orderRepository, never()).updateStatus(300L, "CONFIRMED");
    }

    @Test
    void invalidRequestIsRejectedBeforePersisting() {
        assertThatThrownBy(() -> orderService.create(
                new CreateOrderRequest("", 1L, 2L, new BigDecimal("100.00"), 1)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(orderRepository, accountClient, storageClient);
    }

    private CreateOrderRequest request(String requestId) {
        return new CreateOrderRequest(requestId, 1L, 2L, new BigDecimal("100.00"), 3);
    }
}

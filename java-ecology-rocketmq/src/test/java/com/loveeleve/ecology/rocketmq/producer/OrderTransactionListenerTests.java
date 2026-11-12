package com.loveeleve.ecology.rocketmq.producer;

import com.loveeleve.ecology.rocketmq.dto.OrderEvent;
import com.loveeleve.ecology.rocketmq.service.TransactionOrderStore;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderTransactionListenerTests {

    private TransactionOrderStore store;
    private OrderTransactionListener listener;

    @BeforeEach
    void setUp() {
        store = mock(TransactionOrderStore.class);
        listener = new OrderTransactionListener(store);
    }

    @Test
    void executeCommitsWhenLocalTransactionSucceeds() {
        OrderEvent event = new OrderEvent("rmq-tx-1", "user-1", 100, false);

        RocketMQLocalTransactionState state = listener.executeLocalTransaction(
                MessageBuilder.withPayload(event).build(), event);

        assertThat(state).isEqualTo(RocketMQLocalTransactionState.COMMIT);
    }

    @Test
    void checkUsesCommittedStatus() {
        when(store.status("rmq-tx-2")).thenReturn(Optional.of("COMMITTED"));

        RocketMQLocalTransactionState state = listener.checkLocalTransaction(
                MessageBuilder.withPayload(new OrderEvent())
                        .setHeader(RocketMQHeaders.KEYS, "rmq-tx-2")
                        .build());

        assertThat(state).isEqualTo(RocketMQLocalTransactionState.COMMIT);
    }

    @Test
    void checkReturnsUnknownWhenStatusMissing() {
        when(store.status("rmq-tx-3")).thenReturn(Optional.empty());

        RocketMQLocalTransactionState state = listener.checkLocalTransaction(
                MessageBuilder.withPayload(new OrderEvent())
                        .setHeader(RocketMQHeaders.KEYS, "rmq-tx-3")
                        .build());

        assertThat(state).isEqualTo(RocketMQLocalTransactionState.UNKNOWN);
    }
}

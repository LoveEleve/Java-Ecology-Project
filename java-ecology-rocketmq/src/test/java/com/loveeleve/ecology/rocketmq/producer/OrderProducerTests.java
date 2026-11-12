package com.loveeleve.ecology.rocketmq.producer;

import com.loveeleve.ecology.rocketmq.config.RocketMqTopics;
import com.loveeleve.ecology.rocketmq.dto.OrderEvent;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderProducerTests {

    private RocketMQTemplate template;
    private OrderProducer producer;

    @BeforeEach
    void setUp() {
        template = mock(RocketMQTemplate.class);
        producer = new OrderProducer(template);
    }

    @Test
    void syncSendUsesOrderTopicTagAndKeys() {
        SendResult expected = new SendResult();
        when(template.syncSend(eq(RocketMqTopics.ORDERS + ":CREATE"), any(Message.class), eq(3000L)))
                .thenReturn(expected);

        SendResult result = producer.syncSend(event("rmq-producer-1"));

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<Message<?>> captor = ArgumentCaptor.forClass(Message.class);
        verify(template).syncSend(eq(RocketMqTopics.ORDERS + ":CREATE"), captor.capture(), eq(3000L));
        assertThat(captor.getValue().getHeaders())
                .containsEntry(RocketMQHeaders.KEYS, "rmq-producer-1")
                .containsEntry(RocketMQHeaders.TAGS, "CREATE");
    }

    @Test
    void orderedSendUsesOrderedTopic() {
        SendResult expected = new SendResult();
        when(template.syncSendOrderly(eq(RocketMqTopics.ORDERED), any(Message.class), eq("rmq-order-1")))
                .thenReturn(expected);

        assertThat(producer.sendOrdered(event("rmq-order-1"))).isSameAs(expected);
        verify(template).syncSendOrderly(eq(RocketMqTopics.ORDERED), any(Message.class), eq("rmq-order-1"));
    }

    @Test
    void transactionSendUsesTransactionTopic() {
        TransactionSendResult expected = new TransactionSendResult();
        when(template.sendMessageInTransaction(eq(RocketMqTopics.TRANSACTION + ":CREATE"), any(Message.class), any()))
                .thenReturn(expected);

        assertThat(producer.sendTransaction(event("rmq-tx-1"))).isSameAs(expected);
    }

    @Test
    void invalidDelayLevelIsRejected() {
        assertThatThrownBy(() -> producer.sendDelay(event("rmq-delay-1"), 19))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delayLevel");
    }

    private OrderEvent event(String orderId) {
        return new OrderEvent(orderId, "user-1", 100, false);
    }
}

package com.loveeleve.ecology.rocketmq.consumer;

import com.loveeleve.ecology.rocketmq.config.RocketMqTopics;
import com.loveeleve.ecology.rocketmq.dto.OrderEvent;
import com.loveeleve.ecology.rocketmq.service.OrderMessageHandler;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = RocketMqTopics.ORDERED,
        consumerGroup = "${app.rocketmq.ordered-group:ecology-rocketmq-ordered-consumer}",
        nameServer = "${rocketmq.name-server:}",
        tlsEnable = "${rocketmq.consumer.tls-enable:false}",
        namespace = "${rocketmq.consumer.namespace:}",
        consumeMode = ConsumeMode.ORDERLY,
        consumeThreadMax = 8,
        consumeThreadNumber = 8,
        maxReconsumeTimes = 3,
        suspendCurrentQueueTimeMillis = 1000,
        awaitTerminationMillisWhenShutdown = 5000
)
public class OrderlyOrderConsumer implements RocketMQListener<OrderEvent> {

    private final OrderMessageHandler handler;

    public OrderlyOrderConsumer(OrderMessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onMessage(OrderEvent event) {
        handler.handle(event);
    }
}

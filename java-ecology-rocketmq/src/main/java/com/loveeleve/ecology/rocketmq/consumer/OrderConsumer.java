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
        topic = RocketMqTopics.ORDERS,
        consumerGroup = "${app.rocketmq.consumer-group:ecology-rocketmq-consumer}",
        nameServer = "${rocketmq.name-server:}",
        tlsEnable = "${rocketmq.consumer.tls-enable:false}",
        namespace = "${rocketmq.consumer.namespace:}",
        selectorExpression = "CREATE",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING,
        consumeThreadMax = 32,
        consumeThreadNumber = 20,
        maxReconsumeTimes = 3,
        delayLevelWhenNextConsume = 1,
        awaitTerminationMillisWhenShutdown = 5000
)
public class OrderConsumer implements RocketMQListener<OrderEvent> {

    private final OrderMessageHandler handler;

    public OrderConsumer(OrderMessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onMessage(OrderEvent event) {
        handler.handle(event);
    }
}

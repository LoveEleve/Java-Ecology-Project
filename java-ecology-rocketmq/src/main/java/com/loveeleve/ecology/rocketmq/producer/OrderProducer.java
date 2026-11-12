package com.loveeleve.ecology.rocketmq.producer;

import com.loveeleve.ecology.rocketmq.config.RocketMqTopics;
import com.loveeleve.ecology.rocketmq.dto.OrderEvent;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class OrderProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);

    private final RocketMQTemplate rocketMQTemplate;

    public OrderProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public SendResult syncSend(OrderEvent event) {
        return rocketMQTemplate.syncSend(destination("CREATE"), message(event), 3000);
    }

    public void asyncSend(OrderEvent event) {
        rocketMQTemplate.asyncSend(destination("CREATE"), message(event), new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("rocketmq async send success orderId={} msgId={}",
                        event.getOrderId(), sendResult.getMsgId());
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("rocketmq async send failed orderId={}", event.getOrderId(), throwable);
            }
        });
    }

    public void sendOneWay(OrderEvent event) {
        rocketMQTemplate.sendOneWay(destination("CREATE"), message(event));
    }

    public SendResult sendDelay(OrderEvent event, int delayLevel) {
        if (delayLevel < 1 || delayLevel > 18) {
            throw new IllegalArgumentException("delayLevel must be between 1 and 18");
        }
        return rocketMQTemplate.syncSend(destination("CREATE"), message(event), 3000, delayLevel);
    }

    public SendResult sendBatch(java.util.List<OrderEvent> events) {
        java.util.List<Message<OrderEvent>> messages = events.stream()
                .map(this::message)
                .toList();
        return rocketMQTemplate.syncSend(destination("CREATE"), messages, 3000);
    }

    public SendResult sendOrdered(OrderEvent event) {
        return rocketMQTemplate.syncSendOrderly(RocketMqTopics.ORDERED, message(event), event.getOrderId());
    }

    public TransactionSendResult sendTransaction(OrderEvent event) {
        return rocketMQTemplate.sendMessageInTransaction(
                RocketMqTopics.TRANSACTION + ":CREATE", message(event), event);
    }

    private String destination(String tag) {
        return RocketMqTopics.ORDERS + ":" + tag;
    }

    private Message<OrderEvent> message(OrderEvent event) {
        return MessageBuilder.withPayload(event)
                .setHeader(RocketMQHeaders.KEYS, event.getOrderId())
                .setHeader(RocketMQHeaders.TAGS, "CREATE")
                .build();
    }
}

package com.loveeleve.ecology.rocketmq.producer;

import com.loveeleve.ecology.rocketmq.dto.OrderEvent;
import com.loveeleve.ecology.rocketmq.service.TransactionOrderStore;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;

@RocketMQTransactionListener
public class OrderTransactionListener implements RocketMQLocalTransactionListener {

    private final TransactionOrderStore store;

    public OrderTransactionListener(TransactionOrderStore store) {
        this.store = store;
    }

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        OrderEvent event = (OrderEvent) arg;
        try {
            store.mark(event.getOrderId(), "COMMITTED");
            return RocketMQLocalTransactionState.COMMIT;
        } catch (RuntimeException ex) {
            store.mark(event.getOrderId(), "ROLLBACK");
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String orderId = (String) msg.getHeaders().get(RocketMQHeaders.KEYS);
        return store.status(orderId)
                .map(status -> "COMMITTED".equals(status)
                        ? RocketMQLocalTransactionState.COMMIT
                        : RocketMQLocalTransactionState.ROLLBACK)
                .orElse(RocketMQLocalTransactionState.UNKNOWN);
    }
}

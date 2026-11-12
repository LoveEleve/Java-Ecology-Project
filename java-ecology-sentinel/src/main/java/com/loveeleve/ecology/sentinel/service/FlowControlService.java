package com.loveeleve.ecology.sentinel.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FlowControlService {

    private final AtomicInteger sequence = new AtomicInteger();

    @SentinelResource(value = "flow-order", blockHandler = "orderBlocked")
    public String order(String userId) {
        return "ok:" + userId + ":" + sequence.incrementAndGet();
    }

    public String orderBlocked(String userId, BlockException ex) {
        return "blocked:flow-order:" + ex.getClass().getSimpleName();
    }

    @SentinelResource(value = "flow-warmup", blockHandler = "warmUpBlocked")
    public String warmUp(String userId) {
        return "ok:warmup:" + userId;
    }

    public String warmUpBlocked(String userId, BlockException ex) {
        return "blocked:flow-warmup:" + ex.getClass().getSimpleName();
    }

    @SentinelResource(value = "flow-thread", blockHandler = "threadBlocked")
    public String occupyThread(long millis) throws InterruptedException {
        Thread.sleep(millis);
        return "ok:thread";
    }

    public String threadBlocked(long millis, BlockException ex) {
        return "blocked:flow-thread:" + ex.getClass().getSimpleName();
    }
}

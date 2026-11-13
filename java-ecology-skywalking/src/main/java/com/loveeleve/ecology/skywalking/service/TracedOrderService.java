package com.loveeleve.ecology.skywalking.service;

import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.Tag;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TracedOrderService {

    @Trace(operationName = "order.process")
    @Tag(key = "component", value = "order")
    public String process(String orderId, BigDecimal amount) {
        if (orderId == null || orderId.isBlank() || amount == null || amount.signum() <= 0) {
            IllegalArgumentException ex = new IllegalArgumentException("invalid order");
            ActiveSpan.error(ex);
            throw ex;
        }
        ActiveSpan.tag("order.id", orderId);
        ActiveSpan.tag("order.amount", String.valueOf(amount));
        sleep(ThreadLocalRandom.current().nextLong(5, 20));
        return "ok:" + orderId;
    }

    @Trace(operationName = "order.risk-check")
    public boolean riskCheck(String orderId) {
        ActiveSpan.tag("order.id", orderId);
        return !orderId.startsWith("risk-");
    }

    public String traceId() {
        return TraceContext.traceId();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", ex);
        }
    }
}

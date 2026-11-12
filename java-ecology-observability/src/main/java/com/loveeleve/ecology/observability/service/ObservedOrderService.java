package com.loveeleve.ecology.observability.service;

import com.loveeleve.ecology.observability.metrics.OrderMetrics;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ObservedOrderService {

    private final OrderMetrics orderMetrics;

    public ObservedOrderService(OrderMetrics orderMetrics) {
        this.orderMetrics = orderMetrics;
    }

    @Timed(value = "demo.order.timed", percentiles = {0.5, 0.95, 0.99}, histogram = true)
    @Counted("demo.order.counted")
    @Observed(name = "demo.order.observed", contextualName = "order-processing")
    public String process(String orderId) {
        long started = System.nanoTime();
        try {
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("orderId must not be blank");
            }
            long elapsed = ThreadLocalRandom.current().nextLong(5, 25);
            sleep(elapsed);
            BigDecimal amount = BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(10, 1000));
            orderMetrics.success(amount, (System.nanoTime() - started) / 1_000_000);
            return "ok:" + orderId;
        } catch (RuntimeException ex) {
            orderMetrics.failure();
            throw ex;
        }
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

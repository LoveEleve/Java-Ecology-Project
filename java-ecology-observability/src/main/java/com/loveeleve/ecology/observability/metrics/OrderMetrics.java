package com.loveeleve.ecology.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Component
public class OrderMetrics {

    private final Counter created;
    private final Counter failed;
    private final Timer latency;
    private final DistributionSummary amount;

    public OrderMetrics(MeterRegistry registry) {
        this.created = Counter.builder("demo.orders.created")
                .description("number of successfully created orders")
                .register(registry);
        this.failed = Counter.builder("demo.orders.failed")
                .description("number of failed orders")
                .register(registry);
        this.latency = Timer.builder("demo.orders.latency")
                .description("order processing latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);
        this.amount = DistributionSummary.builder("demo.orders.amount")
                .description("order amount distribution")
                .baseUnit("currency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void success(BigDecimal value, long elapsedMillis) {
        created.increment();
        latency.record(elapsedMillis, TimeUnit.MILLISECONDS);
        amount.record(value.doubleValue());
    }

    public void failure() {
        failed.increment();
    }
}

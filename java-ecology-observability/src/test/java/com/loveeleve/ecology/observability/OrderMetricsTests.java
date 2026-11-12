package com.loveeleve.ecology.observability;

import com.loveeleve.ecology.observability.metrics.OrderMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMetricsTests {

    @Test
    void recordsSuccessFailureLatencyAndAmount() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OrderMetrics metrics = new OrderMetrics(registry);

        metrics.success(new BigDecimal("99.50"), 12);
        metrics.failure();

        assertThat(registry.counter("demo.orders.created").count()).isEqualTo(1.0);
        assertThat(registry.counter("demo.orders.failed").count()).isEqualTo(1.0);
        assertThat(registry.timer("demo.orders.latency").count()).isEqualTo(1);
        assertThat(registry.summary("demo.orders.amount").count()).isEqualTo(1);
        assertThat(registry.summary("demo.orders.amount").totalAmount()).isEqualTo(99.5);
    }
}

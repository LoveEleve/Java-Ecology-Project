package com.loveeleve.ecology.skywalking;

import com.loveeleve.ecology.skywalking.service.TracedOrderService;
import org.apache.skywalking.apm.toolkit.trace.Tag;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TracedOrderServiceTests {

    private final TracedOrderService service = new TracedOrderService();

    @Test
    void tracedMethodsRemainBusinessCallableWithoutAgent() {
        assertThat(service.process("order-1", new BigDecimal("100.00"))).isEqualTo("ok:order-1");
        assertThat(service.riskCheck("risk-1")).isFalse();
        assertThat(service.riskCheck("order-1")).isTrue();
    }

    @Test
    void customSpansAreAnnotated() throws Exception {
        assertThat(TracedOrderService.class.getMethod("process", String.class, BigDecimal.class)
                .getAnnotation(Trace.class).operationName()).isEqualTo("order.process");
        assertThat(TracedOrderService.class.getMethod("process", String.class, BigDecimal.class)
                .getAnnotation(Tag.class).value()).isEqualTo("order");
        assertThat(TracedOrderService.class.getMethod("riskCheck", String.class)
                .getAnnotation(Trace.class).operationName()).isEqualTo("order.risk-check");
    }

    @Test
    void invalidOrderIsRejectedAndBusinessCanContinue() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.process("", new BigDecimal("100.00")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.process("order-2", new BigDecimal("1.00"))).isEqualTo("ok:order-2");
    }

    @Test
    void traceIdIsQueryableWithoutAgent() {
        assertThat(service.traceId()).isNotNull();
    }
}

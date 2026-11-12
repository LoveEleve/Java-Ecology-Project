package com.loveeleve.ecology.observability;

import com.loveeleve.ecology.observability.service.ObservedOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "management.tracing.enabled=false")
@AutoConfigureMockMvc
class ObservabilityApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObservedOrderService orderService;

    @Test
    void healthAndInfoExposeApplicationDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.orderHealth.status").value("UP"))
                .andExpect(jsonPath("$.components.orderHealth.details.latencyBudgetMs").value(100));

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.module").value("java-ecology-observability"));
    }

    @Test
    void prometheusEndpointContainsCustomAndJvmMetrics() throws Exception {
        mockMvc.perform(get("/api/observability/orders/order-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok:order-1"));
        mockMvc.perform(get("/api/observability/slow"))
                .andExpect(status().isOk());
        assertThatThrownBy(() -> orderService.process(" "))
                .isInstanceOf(IllegalArgumentException.class);

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("demo_orders_created_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("demo_orders_failed_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("demo_orders_amount_currency_count")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("demo_order_timed_seconds_count")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("demo_order_counted_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("demo_order_observed_seconds_count")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("http_server_requests_seconds_count")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("application=\"java-ecology-observability\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_memory_used_bytes")));
    }

    @Test
    void metricsEndpointIsExposed() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }
}

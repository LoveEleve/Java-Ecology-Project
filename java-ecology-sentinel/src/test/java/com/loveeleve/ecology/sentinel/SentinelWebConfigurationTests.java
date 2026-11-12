package com.loveeleve.ecology.sentinel;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.adapter.web.common.UrlCleaner;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SentinelWebConfigurationTests {

    @Test
    void urlCleanerNormalizesOrderId() {
        UrlCleaner cleaner = new com.loveeleve.ecology.sentinel.config.SentinelWebConfiguration().urlCleaner();

        assertThat(cleaner.clean("/api/sentinel/orders/10086")).isEqualTo("/api/sentinel/orders/{orderId}");
        assertThat(cleaner.clean("/api/sentinel/flow/order")).isEqualTo("/api/sentinel/flow/order");
    }

    @Test
    void blockExceptionHandlerReturnsJson429() throws Exception {
        BlockExceptionHandler handler = new com.loveeleve.ecology.sentinel.config.SentinelWebConfiguration()
                .blockExceptionHandler(new ObjectMapper());
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, "flow-order", new FlowException("test"));

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("\"code\":\"SENTINEL_BLOCKED\"");
        assertThat(response.getContentAsString()).contains("\"resource\":\"flow-order\"");
    }
}

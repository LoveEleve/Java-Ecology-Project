package com.loveeleve.ecology.sentinel.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser;
import com.alibaba.csp.sentinel.adapter.web.common.UrlCleaner;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Map;

@Configuration
public class SentinelWebConfiguration {

    @Bean
    public RequestOriginParser requestOriginParser() {
        return request -> {
            String origin = request.getHeader("X-Origin");
            return StringUtils.hasText(origin) ? origin : "anonymous";
        };
    }

    @Bean
    public UrlCleaner urlCleaner() {
        return url -> url.replaceAll("/api/sentinel/orders/[^/]+", "/api/sentinel/orders/{orderId}");
    }

    @Bean
    public BlockExceptionHandler blockExceptionHandler(ObjectMapper objectMapper) {
        return (HttpServletRequest request, HttpServletResponse response,
                String resourceName, com.alibaba.csp.sentinel.slots.block.BlockException ex) -> {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "code", "SENTINEL_BLOCKED",
                    "resource", resourceName,
                    "rule", ex.getClass().getSimpleName(),
                    "message", "request blocked by sentinel"
            ));
        };
    }
}

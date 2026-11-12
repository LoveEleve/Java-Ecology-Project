package com.loveeleve.ecology.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TraceGlobalFilter implements GlobalFilter, Ordered {

    public static final String REQUEST_ID = "X-Request-Id";
    public static final String TRACE_ID = "X-Trace-Id";
    private static final Logger log = LoggerFactory.getLogger(TraceGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = request.getHeaders().getFirst(REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        String finalRequestId = requestId;
        ServerHttpRequest mutated = request.mutate()
                .header(REQUEST_ID, finalRequestId)
                .header(TRACE_ID, finalRequestId)
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(mutated).build();
        mutatedExchange.getResponse().getHeaders().set(REQUEST_ID, finalRequestId);
        mutatedExchange.getResponse().getHeaders().set(TRACE_ID, finalRequestId);

        long start = System.nanoTime();
        return chain.filter(mutatedExchange)
                .doFinally(signal -> log.info("gateway {} {} -> {} cost={}ms requestId={}",
                        request.getMethod(), request.getURI().getPath(),
                        mutatedExchange.getResponse().getStatusCode(),
                        (System.nanoTime() - start) / 1_000_000,
                        finalRequestId));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

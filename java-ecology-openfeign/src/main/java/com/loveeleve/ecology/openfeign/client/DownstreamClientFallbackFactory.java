package com.loveeleve.ecology.openfeign.client;

import com.loveeleve.ecology.openfeign.dto.EchoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class DownstreamClientFallbackFactory implements FallbackFactory<DownstreamClient> {

    private static final Logger log = LoggerFactory.getLogger(DownstreamClientFallbackFactory.class);

    @Override
    public DownstreamClient create(Throwable cause) {
        log.warn("downstream fallback activated: {}", cause == null ? "unknown" : cause.getMessage());
        return new DownstreamClient() {
            @Override
            public String hello(String name) {
                return "fallback:hello:" + name;
            }

            @Override
            public EchoResponse echo(String message) {
                return new EchoResponse("fallback:" + message, "fallback");
            }

            @Override
            public String notFound() {
                return "fallback:not-found";
            }

            @Override
            public String slow(long millis) {
                return "fallback:slow:" + millis;
            }
        };
    }
}

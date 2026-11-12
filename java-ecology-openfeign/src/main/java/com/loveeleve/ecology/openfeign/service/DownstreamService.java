package com.loveeleve.ecology.openfeign.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.loveeleve.ecology.openfeign.client.DownstreamClient;
import com.loveeleve.ecology.openfeign.dto.EchoResponse;
import org.springframework.stereotype.Service;

@Service
public class DownstreamService {

    private final DownstreamClient client;

    public DownstreamService(DownstreamClient client) {
        this.client = client;
    }

    @SentinelResource(value = "downstream.hello", fallback = "helloFallback")
    public String hello(String name) {
        return client.hello(name);
    }

    public String helloFallback(String name, Throwable cause) {
        return "sentinel-fallback:hello:" + name;
    }

    @SentinelResource(value = "downstream.echo", fallback = "echoFallback")
    public EchoResponse echo(String message) {
        return client.echo(message);
    }

    public EchoResponse echoFallback(String message, Throwable cause) {
        return new EchoResponse("sentinel-fallback:" + message, "fallback");
    }

    @SentinelResource(value = "downstream.not-found", fallback = "notFoundFallback")
    public String notFound() {
        return client.notFound();
    }

    public String notFoundFallback(Throwable cause) {
        return "sentinel-fallback:not-found";
    }

    @SentinelResource(value = "downstream.slow", fallback = "slowFallback")
    public String slow(long millis) {
        return client.slow(millis);
    }

    public String slowFallback(long millis, Throwable cause) {
        return "sentinel-fallback:slow:" + millis;
    }
}

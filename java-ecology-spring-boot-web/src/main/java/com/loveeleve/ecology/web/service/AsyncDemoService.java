package com.loveeleve.ecology.web.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Service
public class AsyncDemoService {

    @Async("demoTaskExecutor")
    public CompletableFuture<String> compute(String input) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("async task interrupted", ex);
        }
        return CompletableFuture.completedFuture("async:" + input + "@" + Instant.now());
    }
}

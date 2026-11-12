package com.loveeleve.ecology.sentinel.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.springframework.stereotype.Service;

@Service
public class DegradeService {

    @SentinelResource(value = "degrade-order", fallback = "slowFallback",
            exceptionsToIgnore = IllegalArgumentException.class)
    public String slow(long delayMs) throws InterruptedException {
        if (delayMs < 0) {
            throw new IllegalArgumentException("delayMs must be positive");
        }
        Thread.sleep(delayMs);
        return "ok:slow:" + delayMs;
    }

    public String slowFallback(long delayMs, Throwable ex) {
        if (ex instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return "fallback:degrade-order:" + ex.getClass().getSimpleName();
    }

    @SentinelResource(value = "degrade-exception", fallback = "exceptionFallback")
    public String exception() {
        throw new IllegalStateException("simulated downstream failure");
    }

    public String exceptionFallback(Throwable ex) {
        return "fallback:degrade-exception:" + ex.getClass().getSimpleName();
    }
}

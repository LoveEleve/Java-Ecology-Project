package com.loveeleve.ecology.arthas.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Service
public class DiagnosticService {

    public String slow(int millis) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(Math.max(0, millis)));
        return "slow:" + millis;
    }

    public long hotLoop(int iterations) {
        long sum = 0;
        for (int i = 0; i < Math.max(0, iterations); i++) {
            sum += (long) i * i;
        }
        return sum;
    }

    public String fail(String message) {
        throw new IllegalStateException("simulated failure: " + message);
    }

    public String nested(String value) {
        return "nested:" + reverse(value);
    }

    private String reverse(String value) {
        return new StringBuilder(value == null ? "" : value).reverse().toString();
    }
}

package com.loveeleve.ecology.redisson.config;

import java.util.regex.Pattern;

public final class RedissonKeys {

    private static final Pattern SAFE_PART = Pattern.compile("[a-zA-Z0-9:_\\-]{1,64}");
    private static final String PREFIX = "ecology:redisson:";

    private RedissonKeys() {
    }

    public static String redLock(String name) {
        return PREFIX + "red-lock:" + safe(name);
    }

    public static String async(String name) {
        return PREFIX + "async:" + safe(name);
    }

    public static String lock(String name) {
        return PREFIX + "lock:" + safe(name);
    }

    public static String tryLock(String name) {
        return PREFIX + "try-lock:" + safe(name);
    }

    public static String fairLock(String name) {
        return PREFIX + "fair-lock:" + safe(name);
    }

    public static String readWriteLock(String name) {
        return PREFIX + "rw-lock:" + safe(name);
    }

    public static String rateLimiter(String name) {
        return PREFIX + "rate:" + safe(name);
    }

    public static String semaphore(String name) {
        return PREFIX + "semaphore:" + safe(name);
    }

    public static String map(String name) {
        return PREFIX + "map:" + safe(name);
    }

    public static String lockState(String name) {
        return PREFIX + "lock-state:" + safe(name);
    }

    private static String safe(String part) {
        if (part == null || !SAFE_PART.matcher(part).matches()) {
            throw new IllegalArgumentException("invalid Redisson key part");
        }
        return part;
    }
}

package com.loveeleve.ecology.redis.config;

import java.util.regex.Pattern;

public final class RedisKeys {

    private static final Pattern SAFE_PART = Pattern.compile("[a-zA-Z0-9:_\\-]{1,64}");
    private static final String PREFIX = "ecology:redis:";

    private RedisKeys() {
    }

    public static String counter(String name) {
        return PREFIX + "counter:" + safe(name);
    }

    public static String stock(String sku) {
        return PREFIX + "stock:" + safe(sku);
    }

    public static String rateLimit(String key) {
        return PREFIX + "rate:" + safe(key);
    }

    public static String hash(String name) {
        return PREFIX + "hash:" + safe(name);
    }

    public static String leaderboard(String board) {
        return PREFIX + "zset:" + safe(board);
    }

    public static String pipeline(String name) {
        return PREFIX + "pipeline:" + safe(name);
    }

    private static String safe(String part) {
        if (part == null || !SAFE_PART.matcher(part).matches()) {
            throw new IllegalArgumentException("invalid Redis key part");
        }
        return part;
    }
}

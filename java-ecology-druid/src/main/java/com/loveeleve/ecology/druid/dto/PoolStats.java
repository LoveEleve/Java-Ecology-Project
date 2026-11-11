package com.loveeleve.ecology.druid.dto;

public record PoolStats(
        String name,
        int activeCount,
        int poolingCount,
        int maxActive,
        int minIdle,
        int waitThreadCount,
        long createCount,
        long destroyCount,
        long connectCount,
        long errorCount,
        long discardCount
) {
}

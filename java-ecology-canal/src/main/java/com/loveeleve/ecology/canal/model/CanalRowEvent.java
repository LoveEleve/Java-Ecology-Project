package com.loveeleve.ecology.canal.model;

import java.util.Map;

public record CanalRowEvent(
        String destination,
        String database,
        String table,
        String eventType,
        Map<String, String> before,
        Map<String, String> after,
        long executeTime,
        long batchId,
        long entryId) {
}

package com.loveeleve.ecology.sharding.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ShardExpansionService {

    public Map<String, String> plan(long userId, long orderId, int targetDatabaseCount, int targetTableCount) {
        if (targetDatabaseCount <= 0 || targetTableCount <= 0) {
            throw new IllegalArgumentException("target shard counts must be positive");
        }
        Map<String, String> plan = new LinkedHashMap<>();
        plan.put("currentDatabase", "ds_" + Math.floorMod(userId, 2));
        plan.put("targetDatabase", "ds_" + Math.floorMod(userId, targetDatabaseCount));
        plan.put("currentTable", "t_order_" + Math.floorMod(orderId, 2));
        plan.put("targetTable", "t_order_" + Math.floorMod(orderId, targetTableCount));
        plan.put("shadowDatabase", "ds_shadow_" + Math.floorMod(userId, targetDatabaseCount));
        plan.put("shadowTable", "t_order_shadow_" + Math.floorMod(orderId, targetTableCount));
        plan.put("note", "dual-write + backfill + checksum + shadow-read + cutover + rollback");
        return plan;
    }
}

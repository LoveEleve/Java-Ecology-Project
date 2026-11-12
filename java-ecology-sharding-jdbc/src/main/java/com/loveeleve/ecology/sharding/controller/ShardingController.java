package com.loveeleve.ecology.sharding.controller;

import com.loveeleve.ecology.sharding.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sharding")
public class ShardingController {

    @GetMapping("/plan")
    public ApiResponse<Map<String, String>> plan(@RequestParam long orderId, @RequestParam long userId) {
        Map<String, String> plan = new LinkedHashMap<>();
        plan.put("database", "ds_" + Math.floorMod(userId, 2));
        plan.put("table", "t_order_" + Math.floorMod(orderId, 2));
        plan.put("sql", "SELECT/INSERT via logical table t_order");
        return ApiResponse.ok(plan);
    }
}

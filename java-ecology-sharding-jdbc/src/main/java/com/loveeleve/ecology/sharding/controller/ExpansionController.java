package com.loveeleve.ecology.sharding.controller;

import com.loveeleve.ecology.sharding.common.ApiResponse;
import com.loveeleve.ecology.sharding.service.ShardExpansionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sharding/expansion")
public class ExpansionController {

    private final ShardExpansionService expansionService;

    public ExpansionController(ShardExpansionService expansionService) {
        this.expansionService = expansionService;
    }

    @GetMapping("/plan")
    public ApiResponse<Map<String, String>> plan(@RequestParam long userId,
                                                 @RequestParam long orderId,
                                                 @RequestParam(defaultValue = "4") int targetDatabases,
                                                 @RequestParam(defaultValue = "4") int targetTables) {
        return ApiResponse.ok(expansionService.plan(userId, orderId, targetDatabases, targetTables));
    }
}

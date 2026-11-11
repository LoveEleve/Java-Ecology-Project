package com.loveeleve.ecology.druid.controller;

import com.loveeleve.ecology.druid.common.ApiResponse;
import com.loveeleve.ecology.druid.dto.CreateUserRequest;
import com.loveeleve.ecology.druid.dto.PoolStats;
import com.loveeleve.ecology.druid.dto.UserResponse;
import com.loveeleve.ecology.druid.service.PoolStatsService;
import com.loveeleve.ecology.druid.service.PoolUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PoolController {

    private final PoolUserService userService;
    private final PoolStatsService statsService;

    public PoolController(PoolUserService userService, PoolStatsService statsService) {
        this.userService = userService;
        this.statsService = statsService;
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(userService.create(request)));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<UserResponse> get(@PathVariable long id) {
        return ApiResponse.ok(userService.get(id));
    }

    @GetMapping("/users/count")
    public ApiResponse<Long> count() {
        return ApiResponse.ok(userService.count());
    }

    @GetMapping("/pool")
    public ApiResponse<PoolStats> pool() {
        return ApiResponse.ok(statsService.snapshot());
    }
}

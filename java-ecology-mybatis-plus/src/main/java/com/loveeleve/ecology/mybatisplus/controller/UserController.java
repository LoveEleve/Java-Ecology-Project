package com.loveeleve.ecology.mybatisplus.controller;

import com.loveeleve.ecology.mybatisplus.common.ApiResponse;
import com.loveeleve.ecology.mybatisplus.dto.PageResult;
import com.loveeleve.ecology.mybatisplus.dto.UserBatchCreateRequest;
import com.loveeleve.ecology.mybatisplus.dto.UserCreateRequest;
import com.loveeleve.ecology.mybatisplus.dto.UserResponse;
import com.loveeleve.ecology.mybatisplus.dto.UserUpdateRequest;
import com.loveeleve.ecology.mybatisplus.enums.UserStatus;
import com.loveeleve.ecology.mybatisplus.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(userService.create(request)));
    }

    @PostMapping("/batch")
    public ApiResponse<Integer> createBatch(@Valid @RequestBody UserBatchCreateRequest request) {
        return ApiResponse.ok(userService.createBatch(request.users()));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@PathVariable long id) {
        return ApiResponse.ok(userService.get(id));
    }

    @GetMapping("/status/{status}")
    public ApiResponse<List<UserResponse>> selectByStatus(@PathVariable UserStatus status) {
        return ApiResponse.ok(userService.selectByStatus(status));
    }

    @GetMapping
    public ApiResponse<PageResult<UserResponse>> query(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(userService.query(username, email, status, minAge, maxAge, page, size));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

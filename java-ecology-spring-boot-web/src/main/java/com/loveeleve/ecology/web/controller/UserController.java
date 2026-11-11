package com.loveeleve.ecology.web.controller;

import com.loveeleve.ecology.web.common.ApiResponse;
import com.loveeleve.ecology.web.dto.UserCreateRequest;
import com.loveeleve.ecology.web.dto.UserResponse;
import com.loveeleve.ecology.web.dto.UserUpdateRequest;
import com.loveeleve.ecology.web.service.UserService;
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
        UserResponse response = UserResponse.from(userService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@PathVariable long id) {
        return ApiResponse.ok(UserResponse.from(userService.get(id)));
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<UserResponse> users = userService.list(page, size).stream().map(UserResponse::from).toList();
        return ApiResponse.ok(users);
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok(UserResponse.from(userService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.loveeleve.ecology.web.dto;

import com.loveeleve.ecology.web.model.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Integer age,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.name(), user.email(), user.age(), user.createdAt());
    }
}

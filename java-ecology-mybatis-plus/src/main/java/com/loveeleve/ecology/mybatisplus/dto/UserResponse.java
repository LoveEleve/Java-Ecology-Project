package com.loveeleve.ecology.mybatisplus.dto;

import com.loveeleve.ecology.mybatisplus.entity.User;
import com.loveeleve.ecology.mybatisplus.enums.UserStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        UserStatus status,
        Integer age,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                user.getAge(),
                user.getVersion(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

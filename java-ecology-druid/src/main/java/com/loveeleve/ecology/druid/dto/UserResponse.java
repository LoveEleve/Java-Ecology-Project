package com.loveeleve.ecology.druid.dto;

import com.loveeleve.ecology.druid.model.PoolUser;

public record UserResponse(
        Long id,
        String username,
        String email,
        Integer age
) {

    public static UserResponse from(PoolUser user) {
        return new UserResponse(user.id(), user.username(), user.email(), user.age());
    }
}

package com.loveeleve.ecology.mybatis.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 64) String username,
        @Email @Size(max = 128) String email,
        @Min(0) @Max(150) Integer age
) {
}

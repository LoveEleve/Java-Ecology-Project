package com.loveeleve.ecology.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 32, message = "name must be at most 32 characters")
        String name,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email format is invalid")
        @Size(max = 128, message = "email must be at most 128 characters")
        String email,

        @Min(value = 1, message = "age must be at least 1")
        @Max(value = 120, message = "age must be at most 120")
        Integer age
) {
}

package com.loveeleve.ecology.mybatisplus.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserBatchCreateRequest(
        @NotEmpty @Size(max = 100) List<@Valid UserCreateRequest> users
) {
}

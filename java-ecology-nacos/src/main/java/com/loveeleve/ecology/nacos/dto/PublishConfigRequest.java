package com.loveeleve.ecology.nacos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublishConfigRequest(
        @NotBlank @Size(max = 128) String dataId,
        @NotBlank @Size(max = 128) String group,
        @NotBlank @Size(max = 4096) String content,
        @NotBlank @Size(max = 32) String type
) {
}

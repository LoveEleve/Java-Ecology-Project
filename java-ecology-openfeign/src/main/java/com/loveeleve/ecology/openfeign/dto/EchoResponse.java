package com.loveeleve.ecology.openfeign.dto;

public record EchoResponse(
        String message,
        String traceId
) {
}

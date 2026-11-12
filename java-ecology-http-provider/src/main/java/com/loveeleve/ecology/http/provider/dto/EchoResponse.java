package com.loveeleve.ecology.http.provider.dto;

public record EchoResponse(
        String message,
        String traceId
) {
}

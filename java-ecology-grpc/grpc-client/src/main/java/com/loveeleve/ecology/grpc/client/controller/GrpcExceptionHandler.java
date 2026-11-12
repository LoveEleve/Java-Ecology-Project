package com.loveeleve.ecology.grpc.client.controller;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GrpcExceptionHandler {

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<Map<String, String>> grpcStatus(StatusRuntimeException ex) {
        HttpStatus httpStatus = switch (ex.getStatus().getCode()) {
            case INVALID_ARGUMENT, FAILED_PRECONDITION, ALREADY_EXISTS -> HttpStatus.BAD_REQUEST;
            case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_GATEWAY;
        };
        return ResponseEntity.status(httpStatus).body(Map.of(
                "code", ex.getStatus().getCode().name(),
                "message", ex.getStatus().getDescription() == null
                        ? "grpc call failed" : ex.getStatus().getDescription()));
    }
}

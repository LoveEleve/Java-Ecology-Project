package com.loveeleve.ecology.openfeign.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DownstreamException.class)
    public ResponseEntity<Map<String, Object>> handleDownstream(DownstreamException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("code", "DOWNSTREAM_ERROR", "status", ex.getStatus(), "message", ex.getMessage()));
    }
}

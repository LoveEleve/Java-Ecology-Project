package com.loveeleve.ecology.webflux.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TimeoutException.class)
    public Mono<ProblemDetail> timeout(TimeoutException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT, "reactive timeout");
        detail.setTitle("Timeout");
        return Mono.just(detail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ProblemDetail> invalid(IllegalArgumentException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("Invalid request");
        return Mono.just(detail);
    }
}

package com.loveeleve.ecology.webflux.controller;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/reactive")
public class ReactiveController {

    private final AtomicLong sequence = new AtomicLong();

    @GetMapping("/hello")
    public Mono<String> hello() {
        return Mono.just("hello-webflux");
    }

    @GetMapping("/items")
    public Flux<Item> items() {
        return Flux.range(1, 5)
                .delayElements(Duration.ofMillis(20))
                .map(index -> new Item(index, "item-" + index));
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sse() {
        return Flux.interval(Duration.ofMillis(100))
                .take(3)
                .map(tick -> ServerSentEvent.<String>builder()
                        .id(String.valueOf(sequence.incrementAndGet()))
                        .event("tick")
                        .data("tick-" + tick + "-" + Instant.now())
                        .build());
    }

    @PostMapping("/echo")
    public Mono<EchoResponse> echo(@RequestBody EchoRequest request) {
        if (request == null || request.value() == null || request.value().isBlank()) {
            return Mono.error(new IllegalArgumentException("value must not be blank"));
        }
        return Mono.just(new EchoResponse(request.value(), request.value().toUpperCase()));
    }

    @GetMapping("/timeout/{millis}")
    public Mono<String> timeout(@PathVariable long millis) {
        if (millis < 0) {
            return Mono.error(new IllegalArgumentException("millis must not be negative"));
        }
        return Mono.delay(Duration.ofMillis(millis))
                .timeout(Duration.ofMillis(100))
                .map(ignored -> "completed");
    }

    public record Item(int index, String value) {
    }

    public record EchoRequest(String value) {
    }

    public record EchoResponse(String input, String output) {
    }
}

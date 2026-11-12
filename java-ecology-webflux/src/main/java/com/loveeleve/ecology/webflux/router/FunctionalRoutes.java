package com.loveeleve.ecology.webflux.router;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Configuration
public class FunctionalRoutes {

    @Bean
    public RouterFunction<ServerResponse> routes() {
        return route()
                .GET("/functional/ping", request -> ok().bodyValue(Map.of("message", "pong")))
                .GET("/functional/echo/{value}", request -> ok().bodyValue(Map.of(
                        "value", request.pathVariable("value"))))
                .POST("/functional/validate", request -> request.bodyToMono(ValidateRequest.class)
                        .flatMap(body -> body.value() == null || body.value().isBlank()
                                ? ServerResponse.badRequest().bodyValue(Map.of("error", "value required"))
                                : ok().bodyValue(Map.of("value", body.value())))
                        .switchIfEmpty(ServerResponse.badRequest().bodyValue(Map.of("error", "body required"))))
                .build();
    }

    public record ValidateRequest(String value) {
    }
}

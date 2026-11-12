package com.loveeleve.ecology.webflux;

import com.loveeleve.ecology.webflux.controller.ReactiveController;
import com.loveeleve.ecology.webflux.router.FunctionalRoutes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class WebFluxApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private WebClient webClient;

    @LocalServerPort
    private int port;

    @Test
    void monoEndpointReturnsSingleValue() {
        webTestClient.get().uri("/api/reactive/hello")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("hello-webflux");
    }

    @Test
    void fluxEndpointReturnsItems() {
        webTestClient.get().uri("/api/reactive/items")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ReactiveController.Item.class).hasSize(5);
    }

    @Test
    void postEchoAndTimeoutAreReactive() {
        webTestClient.post().uri("/api/reactive/echo")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReactiveController.EchoRequest("abc"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.input").isEqualTo("abc")
                .jsonPath("$.output").isEqualTo("ABC");

        webTestClient.get().uri("/api/reactive/timeout/500")
                .exchange()
                .expectStatus().isEqualTo(504)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Timeout");

        webTestClient.get().uri("/api/reactive/timeout/10")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("completed");

        webTestClient.post().uri("/api/reactive/echo")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReactiveController.EchoRequest(" "))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
    }

    @Test
    void reactiveWebClientCanCallTheReactorNettyServer() {
        String body = webClient.get()
                .uri("http://127.0.0.1:" + port + "/api/reactive/hello")
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(3));
        assertThat(body).isEqualTo("hello-webflux");
    }

    @Test
    void sseStreamProducesEvents() {
        List<String> events = webTestClient.get().uri("/api/reactive/sse")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(String.class)
                .getResponseBody()
                .take(3)
                .collectList()
                .block(Duration.ofSeconds(3));
        assertThat(events).hasSize(3);
    }

    @Test
    void webClientParsesSseEventFields() {
        List<ServerSentEvent<String>> events = webClient.get()
                .uri("http://127.0.0.1:" + port + "/api/reactive/sse")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() { })
                .take(3)
                .collectList()
                .block(Duration.ofSeconds(3));

        assertThat(events).hasSize(3);
        assertThat(events).allSatisfy(event -> {
            assertThat(event.id()).isNotBlank();
            assertThat(event.event()).isEqualTo("tick");
            assertThat(event.data()).startsWith("tick-");
        });
    }

    @Test
    void functionalRoutesWork() {
        webTestClient.get().uri("/functional/ping")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.message").isEqualTo("pong");

        webTestClient.post().uri("/functional/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new FunctionalRoutes.ValidateRequest(""))
                .exchange()
                .expectStatus().isBadRequest();
    }
}

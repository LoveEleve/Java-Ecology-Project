package com.loveeleve.ecology.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void helloEndpointWorks() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/hello?name=test", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("hello test");
    }

    @Test
    void requestIdHeaderIsReturned() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/hello", String.class);

        assertThat(response.getHeaders().getFirst("X-Request-Id")).isNotBlank();
        assertThat(response.getBody()).contains("requestId");
    }

    @Test
    void validationFailureReturnsBadRequest() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/users",
                Map.of("name", "", "email", "not-an-email", "age", 0),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("VALIDATION_FAILED");
    }

    @Test
    void createAndReadUser() {
        ResponseEntity<String> created = restTemplate.postForEntity(
                "/api/users",
                Map.of("name", "Dave", "email", "dave@example.com", "age", 30),
                String.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).contains("Dave");

        ResponseEntity<String> list = restTemplate.getForEntity("/api/users?page=1&size=10", String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).contains("Dave");
    }

    @Test
    void unknownPathReturnsJson404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/not-exists", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getBody()).contains("requestId");
    }

    @Test
    void malformedJsonReturnsBadRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/users",
                new HttpEntity<>("{not-json}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("INVALID_REQUEST_BODY");
    }

    @Test
    void asyncEndpointWorks() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/async?input=test", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("async:test");
    }

    @Test
    void actuatorHealthIsAvailable() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }
}

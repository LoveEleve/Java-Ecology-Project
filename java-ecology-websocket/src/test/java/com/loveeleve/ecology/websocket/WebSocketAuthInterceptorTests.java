package com.loveeleve.ecology.websocket;

import com.loveeleve.ecology.websocket.config.WebSocketProperties;
import com.loveeleve.ecology.websocket.interceptor.WebSocketAuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebSocketAuthInterceptorTests {

    private final WebSocketAuthInterceptor interceptor;

    WebSocketAuthInterceptorTests() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setAuthToken("test-token");
        interceptor = new WebSocketAuthInterceptor(properties);
    }

    @Test
    void acceptsValidQueryTokenAndExtractsUser() {
        Map<String, Object> attributes = new HashMap<>();
        MockHttpServletRequest rawRequest = new MockHttpServletRequest();
        rawRequest.setRequestURI("/ws/chat");
        rawRequest.setQueryString("token=test-token&userId=u1");
        ServletServerHttpResponse response = new ServletServerHttpResponse(new MockHttpServletResponse());

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(rawRequest),
                response, mock(WebSocketHandler.class), attributes);

        assertThat(accepted).isTrue();
        assertThat(attributes).containsEntry(WebSocketAuthInterceptor.USER_ID_ATTRIBUTE, "u1");
    }

    @Test
    void acceptsBearerHeaderToken() {
        Map<String, Object> attributes = new HashMap<>();
        MockHttpServletRequest rawRequest = new MockHttpServletRequest();
        rawRequest.setRequestURI("/ws/chat");
        rawRequest.addHeader("Authorization", "Bearer test-token");
        rawRequest.addHeader("X-User-Id", "u2");
        ServletServerHttpResponse response = new ServletServerHttpResponse(new MockHttpServletResponse());

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(rawRequest), response, mock(WebSocketHandler.class), attributes);

        assertThat(accepted).isTrue();
        assertThat(attributes).containsEntry(WebSocketAuthInterceptor.USER_ID_ATTRIBUTE, "u2");
    }

    @Test
    void rejectsMissingOrWrongToken() {
        MockHttpServletRequest rawRequest = new MockHttpServletRequest();
        rawRequest.setRequestURI("/ws/chat");
        rawRequest.setQueryString("token=wrong");
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(rawResponse);

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(rawRequest),
                response, mock(WebSocketHandler.class), new HashMap<>());

        assertThat(accepted).isFalse();
        assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }
}

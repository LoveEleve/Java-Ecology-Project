package com.loveeleve.ecology.websocket.interceptor;

import com.loveeleve.ecology.websocket.config.WebSocketProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTRIBUTE = "userId";
    private final WebSocketProperties properties;

    public WebSocketAuthInterceptor(WebSocketProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (!constantTimeEquals(properties.getAuthToken(), token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId == null || userId.isBlank()) {
            userId = UriComponentsBuilder.fromUri(request.getURI()).build()
                    .getQueryParams().getFirst("userId");
        }
        attributes.put(USER_ID_ATTRIBUTE, userId == null || userId.isBlank() ? "anonymous" : userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length());
        }
        return UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst("token");
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}

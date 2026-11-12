package com.loveeleve.ecology.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loveeleve.ecology.websocket.dto.WsMessage;
import com.loveeleve.ecology.websocket.interceptor.WebSocketAuthInterceptor;
import com.loveeleve.ecology.websocket.service.WebSocketSessionRegistry;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final WebSocketSessionRegistry registry;

    public ChatWebSocketHandler(ObjectMapper objectMapper, WebSocketSessionRegistry registry) {
        this.objectMapper = objectMapper;
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = (String) session.getAttributes().getOrDefault(
                WebSocketAuthInterceptor.USER_ID_ATTRIBUTE, "anonymous");
        if (!registry.register(session, userId)) {
            session.close(CloseStatus.SESSION_NOT_RELIABLE.withReason("server is full"));
            return;
        }
        registry.send(session, new WsMessage("WELCOME", "server", userId, "connected"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            WsMessage incoming = objectMapper.readValue(message.getPayload(), WsMessage.class);
            if ("PING".equals(incoming.type())) {
                registry.send(session, new WsMessage("PONG", "server", incoming.from(), "pong"));
                return;
            }
            if ("CHAT".equals(incoming.type())) {
                String authenticatedUser = (String) session.getAttributes().getOrDefault(
                        WebSocketAuthInterceptor.USER_ID_ATTRIBUTE, "anonymous");
                WsMessage outgoing = new WsMessage("CHAT", authenticatedUser,
                        incoming.to(), incoming.payload());
                if (incoming.to() != null && !incoming.to().isBlank()) {
                    registry.sendToUser(incoming.to(), outgoing);
                } else {
                    registry.broadcast(outgoing);
                }
                return;
            }
            registry.send(session, new WsMessage("ERROR", "server", incoming.from(), "unknown type"));
        } catch (Exception ex) {
            registry.send(session, new WsMessage("ERROR", "server", "unknown", "invalid message"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("websocket transport error session={}", session.getId(), exception);
        registry.unregister(session);
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException ignored) {
            // best effort
        }
    }

    @Scheduled(fixedDelayString = "${app.websocket.heartbeat-millis:25000}")
    public void heartbeat() {
        registry.broadcast(new WsMessage("PING", "server", null, "heartbeat"));
    }

    @PreDestroy
    public void shutdown() {
        registry.closeAll();
    }
}

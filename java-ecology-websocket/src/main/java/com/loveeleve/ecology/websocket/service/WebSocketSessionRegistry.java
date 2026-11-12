package com.loveeleve.ecology.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loveeleve.ecology.websocket.config.WebSocketProperties;
import com.loveeleve.ecology.websocket.dto.WsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionRegistry.class);

    private final Map<String, ClientSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final WebSocketProperties properties;

    public WebSocketSessionRegistry(ObjectMapper objectMapper, WebSocketProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public boolean register(WebSocketSession session, String userId) {
        if (sessions.size() >= properties.getMaxSessions()) {
            return false;
        }
        WebSocketSession decorated = new ConcurrentWebSocketSessionDecorator(
                session, (int) properties.getSendTimeLimitMillis(), properties.getSendBufferSizeBytes());
        ClientSession client = new ClientSession(decorated, userId);
        sessions.put(session.getId(), client);
        userSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session.getId());
        return true;
    }

    public void unregister(WebSocketSession session) {
        ClientSession removed = sessions.remove(session.getId());
        if (removed != null) {
            Set<String> ids = userSessions.get(removed.userId());
            if (ids != null) {
                ids.remove(session.getId());
                if (ids.isEmpty()) {
                    userSessions.remove(removed.userId());
                }
            }
        }
    }

    public void send(WebSocketSession session, WsMessage message) {
        ClientSession client = sessions.get(session.getId());
        WebSocketSession target = client == null ? session : client.session();
        try {
            if (target.isOpen()) {
                target.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            }
        } catch (IOException ex) {
            closeQuietly(target);
        } catch (RuntimeException ex) {
            log.warn("websocket send failed session={}", target.getId(), ex);
            closeQuietly(target);
        }
    }

    public int sendToUser(String userId, WsMessage message) {
        Set<String> ids = userSessions.get(userId);
        if (ids == null) {
            return 0;
        }
        int sent = 0;
        for (String id : ids) {
            ClientSession client = sessions.get(id);
            if (client != null) {
                send(client.session(), message);
                sent++;
            }
        }
        return sent;
    }

    public int broadcast(WsMessage message) {
        int sent = 0;
        for (ClientSession client : sessions.values()) {
            send(client.session(), message);
            sent++;
        }
        return sent;
    }

    public Collection<String> userIds() {
        return List.copyOf(userSessions.keySet());
    }

    public int sessionCount() {
        return sessions.size();
    }

    public Set<String> sessionsOf(String userId) {
        return Set.copyOf(userSessions.getOrDefault(userId, Set.of()));
    }

    public void closeAll() {
        for (ClientSession client : sessions.values()) {
            try {
                client.session().close(CloseStatus.NORMAL);
            } catch (IOException ignored) {
                // best effort
            }
        }
        sessions.clear();
        userSessions.clear();
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.SESSION_NOT_RELIABLE);
        } catch (IOException ignored) {
            // best effort
        }
    }

    public record ClientSession(WebSocketSession session, String userId) {
    }
}

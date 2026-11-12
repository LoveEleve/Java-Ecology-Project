package com.loveeleve.ecology.websocket.controller;

import com.loveeleve.ecology.websocket.dto.WsMessage;
import com.loveeleve.ecology.websocket.service.WebSocketSessionRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ws")
public class WebSocketController {

    private final WebSocketSessionRegistry registry;

    public WebSocketController(WebSocketSessionRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("sessions", registry.sessionCount(), "users", registry.userIds());
    }

    @PostMapping("/broadcast")
    public Map<String, Integer> broadcast(@RequestBody WsMessage message) {
        return Map.of("sent", registry.broadcast(message));
    }

    @PostMapping("/send")
    public Map<String, Integer> send(@RequestParam String userId, @RequestBody WsMessage message) {
        return Map.of("sent", registry.sendToUser(userId, message));
    }
}

package com.loveeleve.ecology.jraft.controller;

import com.loveeleve.ecology.jraft.config.JraftProperties;
import com.loveeleve.ecology.jraft.model.KvCommand;
import com.loveeleve.ecology.jraft.service.JraftNode;
import com.loveeleve.ecology.jraft.service.JraftNodeLifecycle;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/jraft")
public class JraftController {

    private final JraftProperties properties;
    private final JraftNodeLifecycle lifecycle;

    public JraftController(JraftProperties properties, JraftNodeLifecycle lifecycle) {
        this.properties = properties;
        this.lifecycle = lifecycle;
    }

    @PostMapping("/put")
    public ResponseEntity<Map<String, String>> put(@RequestBody KvCommand command) throws Exception {
        if (command == null || command.op() == null || command.key() == null) {
            return ResponseEntity.badRequest().body(Map.of("code", "INVALID_COMMAND"));
        }
        JraftNode node = lifecycle.node();
        if (node == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("code", "NOT_RUNNING"));
        }
        if (!node.isLeader()) {
            return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                    .body(Map.of("code", "NOT_LEADER", "leader", String.valueOf(node.leaderId())));
        }
        return ResponseEntity.ok(Map.of("value", node.apply(command)));
    }

    @GetMapping("/get")
    public ResponseEntity<Map<String, String>> get(@RequestParam String key) throws Exception {
        if (key == null || key.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", "INVALID_KEY"));
        }
        JraftNode node = lifecycle.node();
        if (node == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("code", "NOT_RUNNING"));
        }
        if (!node.isLeader()) {
            return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                    .body(Map.of("code", "NOT_LEADER", "leader", String.valueOf(node.leaderId())));
        }
        String value = node.apply(KvCommand.get(key));
        if (value == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", "NOT_FOUND"));
        }
        return ResponseEntity.ok(Map.of("value", value));
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        JraftNode node = lifecycle.node();
        return Map.of(
                "enabled", properties.isEnabled(),
                "running", lifecycle.isRunning(),
                "node", properties.getNodeId(),
                "group", properties.getGroupId(),
                "leader", node == null ? "" : String.valueOf(node.leaderId()));
    }
}

package com.loveeleve.ecology.canal.controller;

import com.loveeleve.ecology.canal.config.CanalProperties;
import com.loveeleve.ecology.canal.service.CanalClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/canal")
public class CanalController {

    private final CanalProperties properties;
    private final CanalClient client;

    public CanalController(CanalProperties properties, CanalClient client) {
        this.properties = properties;
        this.client = client;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "enabled", properties.isEnabled(),
                "running", client.isRunning(),
                "destination", properties.getDestination(),
                "filter", properties.getFilter(),
                "processedEvents", client.processedEvents(),
                "lastBatchId", client.lastBatchId(),
                "lastError", client.lastError() == null ? "" : client.lastError());
    }
}

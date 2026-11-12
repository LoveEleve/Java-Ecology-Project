package com.loveeleve.ecology.websocket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.websocket")
public class WebSocketProperties {

    private String path = "/ws/chat";
    private String authToken = "dev-token";
    private List<String> allowedOrigins = List.of("http://localhost:3000", "http://localhost:8107");
    private int maxSessions = 1000;
    private int maxTextMessageBytes = 64 * 1024;
    private long idleTimeoutMillis = 300_000;
    private long sendTimeLimitMillis = 5_000;
    private int sendBufferSizeBytes = 512 * 1024;
    private long heartbeatMillis = 25_000;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public int getMaxSessions() {
        return maxSessions;
    }

    public void setMaxSessions(int maxSessions) {
        this.maxSessions = maxSessions;
    }

    public int getMaxTextMessageBytes() {
        return maxTextMessageBytes;
    }

    public void setMaxTextMessageBytes(int maxTextMessageBytes) {
        this.maxTextMessageBytes = maxTextMessageBytes;
    }

    public long getIdleTimeoutMillis() {
        return idleTimeoutMillis;
    }

    public void setIdleTimeoutMillis(long idleTimeoutMillis) {
        this.idleTimeoutMillis = idleTimeoutMillis;
    }

    public long getSendTimeLimitMillis() {
        return sendTimeLimitMillis;
    }

    public void setSendTimeLimitMillis(long sendTimeLimitMillis) {
        this.sendTimeLimitMillis = sendTimeLimitMillis;
    }

    public int getSendBufferSizeBytes() {
        return sendBufferSizeBytes;
    }

    public void setSendBufferSizeBytes(int sendBufferSizeBytes) {
        this.sendBufferSizeBytes = sendBufferSizeBytes;
    }

    public long getHeartbeatMillis() {
        return heartbeatMillis;
    }

    public void setHeartbeatMillis(long heartbeatMillis) {
        this.heartbeatMillis = heartbeatMillis;
    }
}

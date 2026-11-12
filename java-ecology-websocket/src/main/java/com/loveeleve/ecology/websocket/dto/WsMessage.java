package com.loveeleve.ecology.websocket.dto;

public record WsMessage(String type, String from, String to, String payload, long timestamp) {

    public WsMessage(String type, String from, String to, String payload) {
        this(type, from, to, payload, System.currentTimeMillis());
    }
}

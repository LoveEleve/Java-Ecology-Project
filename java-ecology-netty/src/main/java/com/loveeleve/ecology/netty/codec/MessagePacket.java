package com.loveeleve.ecology.netty.codec;

public record MessagePacket(String type, String requestId, String payload) {
}

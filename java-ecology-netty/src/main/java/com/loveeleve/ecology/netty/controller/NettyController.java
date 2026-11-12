package com.loveeleve.ecology.netty.controller;

import com.loveeleve.ecology.netty.codec.MessagePacket;
import com.loveeleve.ecology.netty.server.NettyTcpServer;
import io.netty.channel.Channel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/netty")
public class NettyController {

    private final NettyTcpServer server;

    public NettyController(NettyTcpServer server) {
        this.server = server;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("running", server.isRunning(), "port", server.getPort(),
                "connections", server.activeConnections());
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> send(@RequestBody MessagePacket packet) {
        if (packet == null || packet.requestId() == null || packet.requestId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", "INVALID_REQUEST"));
        }
        Channel channel = server.clients().stream().filter(Channel::isActive).findFirst().orElse(null);
        if (channel == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("code", "NO_CONNECTION"));
        }
        if (!channel.isWritable()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("code", "WRITE_BUFFER_FULL"));
        }
        channel.writeAndFlush(packet);
        return ResponseEntity.accepted().body(Map.of("requestId", String.valueOf(packet.requestId())));
    }

    @PostMapping("/broadcast")
    public Map<String, Integer> broadcast(@RequestBody MessagePacket packet) {
        int sent = 0;
        for (Channel channel : server.clients()) {
            if (channel.isActive() && channel.isWritable()) {
                channel.writeAndFlush(packet);
                sent++;
            }
        }
        return Map.of("sent", sent);
    }
}

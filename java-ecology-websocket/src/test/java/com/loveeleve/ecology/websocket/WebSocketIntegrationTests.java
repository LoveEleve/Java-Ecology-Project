package com.loveeleve.ecology.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.websocket.auth-token=test-token",
                "app.websocket.allowed-origins=http://localhost",
                "app.websocket.heartbeat-millis=200"
        })
class WebSocketIntegrationTests {

    @LocalServerPort
    private int port;

    @Test
    void rejectsBadTokenAndOrigin() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        String base = "ws://127.0.0.1:" + port + "/ws/chat";

        assertThatThrownBy(() -> client.newWebSocketBuilder()
                .header("Origin", "http://localhost")
                .buildAsync(URI.create(base + "?token=wrong&userId=u1"), new NoopListener())
                .get(3, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class);

        assertThatThrownBy(() -> client.newWebSocketBuilder()
                .header("Origin", "http://evil.example")
                .buildAsync(URI.create(base + "?token=test-token&userId=u1"), new NoopListener())
                .get(3, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    void privateMessageReachesOnlyTheTargetUser() throws Exception {
        List<String> u1Messages = new CopyOnWriteArrayList<>();
        List<String> u2Messages = new CopyOnWriteArrayList<>();
        WebSocket u1 = connect("u1", u1Messages);
        WebSocket u2 = connect("u2", u2Messages);
        try {
            awaitMessage(u1Messages, "WELCOME");
            awaitMessage(u2Messages, "WELCOME");

            u1.sendText("{\"type\":\"CHAT\",\"from\":\"spoofed\",\"to\":\"u2\",\"payload\":\"private\"}", true)
                    .get(3, TimeUnit.SECONDS);

            awaitMessage(u2Messages, "\"payload\":\"private\"");
            assertThat(u2Messages).anyMatch(message -> message.contains("\"from\":\"u1\""));
            assertThat(u1Messages).noneMatch(message -> message.contains("\"payload\":\"private\""));
        } finally {
            u1.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(3, TimeUnit.SECONDS);
            u2.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(3, TimeUnit.SECONDS);
        }
    }

    @Test
    void connectsReceivesWelcomeAndEchoesChatAndPing() throws Exception {
        List<String> messages = new CopyOnWriteArrayList<>();
        CompletableFuture<WebSocket> connected = new CompletableFuture<>();
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                connected.complete(webSocket);
                webSocket.request(1);
            }

            @Override
            public CompletableFuture<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                messages.add(data.toString());
                webSocket.request(1);
                return null;
            }
        };
        WebSocket socket = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build()
                .newWebSocketBuilder()
                .header("Origin", "http://localhost")
                .connectTimeout(Duration.ofSeconds(3))
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/ws/chat?token=test-token&userId=u1"), listener)
                .get(3, TimeUnit.SECONDS);
        try {
            assertThat(connected.get(3, TimeUnit.SECONDS)).isNotNull();
            awaitMessage(messages, "WELCOME");

            socket.sendText("{\"type\":\"CHAT\",\"from\":\"u1\",\"to\":\"\",\"payload\":\"hello\"}", true)
                    .get(3, TimeUnit.SECONDS);
            awaitMessage(messages, "\"type\":\"CHAT\"");

            socket.sendText("{\"type\":\"PING\",\"from\":\"u1\",\"to\":\"\",\"payload\":\"ping\"}", true)
                    .get(3, TimeUnit.SECONDS);
            awaitMessage(messages, "\"type\":\"PONG\"");
            awaitMessage(messages, "\"type\":\"PING\"");
        } finally {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(3, TimeUnit.SECONDS);
        }
    }

    private WebSocket connect(String userId, List<String> messages) throws Exception {
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                webSocket.request(1);
            }

            @Override
            public CompletableFuture<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                messages.add(data.toString());
                webSocket.request(1);
                return null;
            }
        };
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build()
                .newWebSocketBuilder()
                .header("Origin", "http://localhost")
                .connectTimeout(Duration.ofSeconds(3))
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/ws/chat?token=test-token&userId=" + userId),
                        listener)
                .get(3, TimeUnit.SECONDS);
    }

    private static final class NoopListener implements WebSocket.Listener {
        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }
    }

    private void awaitMessage(List<String> messages, String expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            if (messages.stream().anyMatch(message -> message.contains(expected))) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("expected websocket message containing " + expected + " but got " + messages);
    }
}

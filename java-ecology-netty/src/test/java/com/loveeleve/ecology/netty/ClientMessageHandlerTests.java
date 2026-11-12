package com.loveeleve.ecology.netty;

import com.loveeleve.ecology.netty.client.ClientMessageHandler;
import com.loveeleve.ecology.netty.codec.MessagePacket;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ClientMessageHandlerTests {

    @Test
    void completesPendingRequestByRequestId() {
        Map<String, CompletableFuture<MessagePacket>> pending = new ConcurrentHashMap<>();
        CompletableFuture<MessagePacket> future = new CompletableFuture<>();
        pending.put("req-1", future);
        EmbeddedChannel channel = new EmbeddedChannel(
                new ClientMessageHandler(pending, () -> { }));

        channel.writeInbound(new MessagePacket("ECHO", "req-1", "hello"));

        assertThat(future.join()).isEqualTo(new MessagePacket("ECHO", "req-1", "hello"));
        assertThat(pending).doesNotContainKey("req-1");
    }

    @Test
    void failsPendingRequestsWhenConnectionClosesAndTriggersReconnectCallback() {
        Map<String, CompletableFuture<MessagePacket>> pending = new ConcurrentHashMap<>();
        CompletableFuture<MessagePacket> future = new CompletableFuture<>();
        pending.put("req-2", future);
        AtomicInteger callbacks = new AtomicInteger();
        EmbeddedChannel channel = new EmbeddedChannel(
                new ClientMessageHandler(pending, callbacks::incrementAndGet));

        channel.close();

        assertThat(future).isCompletedExceptionally();
        assertThat(callbacks.get()).isEqualTo(1);
    }
}

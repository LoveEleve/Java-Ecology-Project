package com.loveeleve.ecology.netty;

import com.loveeleve.ecology.netty.codec.MessagePacket;
import com.loveeleve.ecology.netty.handler.ServerMessageHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServerMessageHandlerTests {

    @Test
    void echoRequestProducesEchoResponse() {
        EmbeddedChannel channel = new EmbeddedChannel(
                new ServerMessageHandler(new DefaultChannelGroup(GlobalEventExecutor.INSTANCE), 10));

        channel.writeInbound(new MessagePacket("ECHO", "req-1", "hello"));

        MessagePacket response = channel.readOutbound();
        assertThat(response).isEqualTo(new MessagePacket("ECHO", "req-1", "hello"));
    }

    @Test
    void pingRequestProducesPongResponse() {
        EmbeddedChannel channel = new EmbeddedChannel(
                new ServerMessageHandler(new DefaultChannelGroup(GlobalEventExecutor.INSTANCE), 10));

        channel.writeInbound(new MessagePacket("PING", "req-2", "ping"));

        MessagePacket response = channel.readOutbound();
        assertThat(response).isEqualTo(new MessagePacket("PONG", "req-2", "pong"));
    }

    @Test
    void unknownTypeProducesErrorResponse() {
        EmbeddedChannel channel = new EmbeddedChannel(
                new ServerMessageHandler(new DefaultChannelGroup(GlobalEventExecutor.INSTANCE), 10));

        channel.writeInbound(new MessagePacket("UNKNOWN", "req-3", "x"));

        MessagePacket response = channel.readOutbound();
        assertThat(response.type()).isEqualTo("ERROR");
        assertThat(response.requestId()).isEqualTo("req-3");
    }
}

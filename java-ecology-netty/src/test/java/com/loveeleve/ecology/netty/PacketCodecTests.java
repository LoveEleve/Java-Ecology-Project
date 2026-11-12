package com.loveeleve.ecology.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loveeleve.ecology.netty.codec.MessagePacket;
import com.loveeleve.ecology.netty.codec.PacketDecoder;
import com.loveeleve.ecology.netty.codec.PacketEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PacketCodecTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void encoderWritesLengthPrefixedJson() {
        EmbeddedChannel channel = new EmbeddedChannel(new PacketEncoder(objectMapper));

        assertThat(channel.writeOutbound(new MessagePacket("ECHO", "req-1", "hello"))).isTrue();
        ByteBuf encoded = channel.readOutbound();
        try {
            int length = encoded.readInt();
            byte[] body = new byte[length];
            encoded.readBytes(body);
            assertThat(length).isPositive();
            assertThat(new String(body, StandardCharsets.UTF_8)).contains("\"requestId\":\"req-1\"");
        } finally {
            encoded.release();
        }
    }

    @Test
    void encoderRejectsAnOversizedBody() {
        EmbeddedChannel channel = new EmbeddedChannel(new PacketEncoder(objectMapper));

        assertThatThrownBy(() -> channel.writeOutbound(new MessagePacket(
                "ECHO", "big", "x".repeat(PacketEncoder.MAX_BODY_LENGTH + 1))))
                .isInstanceOf(Exception.class);
    }

    @Test
    void decoderRejectsInvalidJson() {
        EmbeddedChannel channel = new EmbeddedChannel(new PacketDecoder(objectMapper));

        assertThatThrownBy(() -> channel.writeInbound(
                Unpooled.copiedBuffer("{invalid", StandardCharsets.UTF_8)))
                .isInstanceOf(Exception.class);
    }

    @Test
    void decoderHandlesAFragmentedFrame() {
        EmbeddedChannel channel = new EmbeddedChannel(
                new LengthFieldBasedFrameDecoder(PacketEncoder.MAX_FRAME_LENGTH, 0, 4, 0, 4),
                new PacketDecoder(objectMapper));
        byte[] body = ("{\"type\":\"ECHO\",\"requestId\":\"req-2\",\"payload\":\"world\"}")
                .getBytes(StandardCharsets.UTF_8);
        ByteBuf frame = Unpooled.buffer(4 + body.length);
        frame.writeInt(body.length);
        frame.writeBytes(body);

        ByteBuf firstPart = frame.readRetainedSlice(3);
        ByteBuf secondPart = frame.readRetainedSlice(frame.readableBytes());
        assertThat(channel.writeInbound(firstPart)).isFalse();
        assertThat(channel.writeInbound(secondPart)).isTrue();

        MessagePacket packet = channel.readInbound();
        assertThat(packet).isEqualTo(new MessagePacket("ECHO", "req-2", "world"));
        frame.release();
    }
}

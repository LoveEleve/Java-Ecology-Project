package com.loveeleve.ecology.netty.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public final class FrameCodec {

    private FrameCodec() {
    }

    public static void install(ChannelPipeline pipeline, ObjectMapper objectMapper) {
        pipeline.addLast("frameDecoder",
                new LengthFieldBasedFrameDecoder(PacketEncoder.MAX_FRAME_LENGTH, 0, 4, 0, 4));
        pipeline.addLast("packetDecoder", new PacketDecoder(objectMapper));
        pipeline.addLast("packetEncoder", new PacketEncoder(objectMapper));
    }
}

package com.loveeleve.ecology.netty.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PacketEncoder extends MessageToByteEncoder<MessagePacket> {

    public static final int MAX_FRAME_LENGTH = 1024 * 1024;
    public static final int MAX_BODY_LENGTH = MAX_FRAME_LENGTH - Integer.BYTES;

    private final ObjectMapper objectMapper;

    public PacketEncoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, MessagePacket msg, ByteBuf out) throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(msg);
        if (body.length > MAX_BODY_LENGTH) {
            throw new IllegalArgumentException("packet too large: " + body.length);
        }
        out.writeInt(body.length);
        out.writeBytes(body);
    }
}

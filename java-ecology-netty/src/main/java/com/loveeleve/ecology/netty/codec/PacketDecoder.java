package com.loveeleve.ecology.netty.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class PacketDecoder extends MessageToMessageDecoder<ByteBuf> {

    private final ObjectMapper objectMapper;

    public PacketDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (!msg.isReadable()) {
            return;
        }
        try {
            byte[] bytes = new byte[msg.readableBytes()];
            msg.readBytes(bytes);
            out.add(objectMapper.readValue(new String(bytes, StandardCharsets.UTF_8), MessagePacket.class));
        } catch (Exception ex) {
            throw new CorruptedFrameException("invalid packet JSON", ex);
        }
    }
}

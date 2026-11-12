package com.loveeleve.ecology.netty.handler;

import com.loveeleve.ecology.netty.codec.MessagePacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.timeout.IdleStateEvent;

public class ServerMessageHandler extends SimpleChannelInboundHandler<MessagePacket> {

    private final ChannelGroup clients;
    private final int maxConnections;

    public ServerMessageHandler(ChannelGroup clients, int maxConnections) {
        this.clients = clients;
        this.maxConnections = maxConnections;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (clients.size() >= maxConnections) {
            ctx.close();
            return;
        }
        clients.add(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessagePacket request) {
        if (!ctx.channel().isWritable()) {
            ctx.close();
            return;
        }
        String type = request.type() == null ? "" : request.type();
        MessagePacket response = switch (type) {
            case "PING" -> new MessagePacket("PONG", request.requestId(), "pong");
            case "ECHO" -> new MessagePacket("ECHO", request.requestId(), request.payload());
            default -> new MessagePacket("ERROR", request.requestId(), "unknown type: " + type);
        };
        ctx.writeAndFlush(response).addListener(future -> {
            if (!future.isSuccess()) {
                ctx.close();
            }
        });
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.close();
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}

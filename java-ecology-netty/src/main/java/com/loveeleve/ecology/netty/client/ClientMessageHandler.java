package com.loveeleve.ecology.netty.client;

import com.loveeleve.ecology.netty.codec.MessagePacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ClientMessageHandler extends SimpleChannelInboundHandler<MessagePacket> {

    private final Map<String, CompletableFuture<MessagePacket>> pending;
    private final Runnable onDisconnected;

    public ClientMessageHandler(Map<String, CompletableFuture<MessagePacket>> pending, Runnable onDisconnected) {
        this.pending = pending;
        this.onDisconnected = onDisconnected;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessagePacket response) {
        if (response.requestId() != null) {
            CompletableFuture<MessagePacket> future = pending.remove(response.requestId());
            if (future != null) {
                future.complete(response);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        pending.forEach((id, future) -> future.completeExceptionally(
                new IllegalStateException("connection closed")));
        pending.clear();
        onDisconnected.run();
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.writeAndFlush(new MessagePacket("PING", null, "client-heartbeat"));
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}

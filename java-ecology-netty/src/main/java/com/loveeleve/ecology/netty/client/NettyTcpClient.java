package com.loveeleve.ecology.netty.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loveeleve.ecology.netty.codec.FrameCodec;
import com.loveeleve.ecology.netty.codec.MessagePacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnProperty(prefix = "app.netty.client", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(NettyClientProperties.class)
public class NettyTcpClient {

    private final NettyClientProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, CompletableFuture<MessagePacket>> pending = new ConcurrentHashMap<>();
    private final EventLoopGroup group = new NioEventLoopGroup(1);
    private final AtomicInteger reconnectAttempts = new AtomicInteger();
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean();
    private volatile Channel channel;
    private volatile boolean closed;

    public NettyTcpClient(NettyClientProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void start() {
        connect();
    }

    public synchronized void connect() {
        if (closed || (channel != null && channel.isActive())) {
            return;
        }
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMillis())
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.ALLOCATOR, io.netty.buffer.PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(0, 30, 0));
                        FrameCodec.install(ch.pipeline(), objectMapper);
                        ch.pipeline().addLast("clientHandler",
                                new ClientMessageHandler(pending, NettyTcpClient.this::scheduleReconnect));
                    }
                });
        bootstrap.connect(properties.getHost(), properties.getPort()).addListener(future -> {
            if (future.isSuccess()) {
                Channel connected = ((io.netty.channel.ChannelFuture) future).channel();
                if (closed) {
                    connected.close();
                    return;
                }
                channel = connected;
                reconnectAttempts.set(0);
                reconnectScheduled.set(false);
            } else {
                scheduleReconnect();
            }
        });
    }

    public CompletableFuture<MessagePacket> send(MessagePacket packet) {
        if (packet == null || packet.requestId() == null || packet.requestId().isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("requestId must not be blank"));
        }
        Channel current = channel;
        if (current == null || !current.isActive()) {
            return CompletableFuture.failedFuture(new IllegalStateException("netty client is not connected"));
        }
        CompletableFuture<MessagePacket> future = new CompletableFuture<>();
        pending.put(packet.requestId(), future);
        current.writeAndFlush(packet).addListener(write -> {
            if (!write.isSuccess()) {
                pending.remove(packet.requestId());
                future.completeExceptionally(write.cause());
            }
        });
        return future.orTimeout(properties.getRequestTimeoutSeconds(), TimeUnit.SECONDS)
                .whenComplete((result, error) -> pending.remove(packet.requestId()));
    }

    private void scheduleReconnect() {
        if (closed || !properties.isEnabled() || !reconnectScheduled.compareAndSet(false, true)) {
            return;
        }
        int attempts = reconnectAttempts.incrementAndGet();
        long delay = Math.min(30L, (long) properties.getReconnectSeconds() * attempts);
        if (group.isShuttingDown()) {
            reconnectScheduled.set(false);
            return;
        }
        group.schedule(() -> {
            reconnectScheduled.set(false);
            connect();
        }, delay, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void close() {
        closed = true;
        Channel current = channel;
        if (current != null) {
            current.close();
        }
        pending.forEach((id, future) -> future.completeExceptionally(
                new IllegalStateException("netty client closed")));
        pending.clear();
        group.shutdownGracefully(0, 5, TimeUnit.SECONDS).awaitUninterruptibly(5, TimeUnit.SECONDS);
    }

    public boolean isConnected() {
        return channel != null && channel.isActive();
    }
}

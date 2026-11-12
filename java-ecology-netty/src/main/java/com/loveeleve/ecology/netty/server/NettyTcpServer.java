package com.loveeleve.ecology.netty.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loveeleve.ecology.netty.codec.FrameCodec;
import com.loveeleve.ecology.netty.handler.ServerMessageHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetSocketAddress;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@EnableConfigurationProperties(NettyServerProperties.class)
public class NettyTcpServer implements SmartLifecycle {

    private final NettyServerProperties properties;
    private final ObjectMapper objectMapper;
    private final ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean running;

    public NettyTcpServer(NettyServerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void start() {
        if (!properties.isEnabled() || running) {
            return;
        }
        int workers = properties.getWorkerThreads() > 0
                ? properties.getWorkerThreads()
                : Math.max(2, Runtime.getRuntime().availableProcessors() * 2);
        bossGroup = new NioEventLoopGroup(properties.getBossThreads());
        workerGroup = new NioEventLoopGroup(workers);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.ALLOCATOR, io.netty.buffer.PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                            new WriteBufferWaterMark(32 * 1024, 64 * 1024))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast("idleStateHandler",
                                    new IdleStateHandler(0, 0, properties.getIdleSeconds()));
                            ch.pipeline().addLast("writeTimeoutHandler",
                                    new WriteTimeoutHandler(properties.getWriteTimeoutSeconds()));
                            FrameCodec.install(ch.pipeline(), objectMapper);
                            ch.pipeline().addLast("serverHandler",
                                    new ServerMessageHandler(clients, properties.getMaxConnections()));
                        }
                    });
            serverChannel = bootstrap.bind(properties.getBindAddress(), properties.getPort()).sync().channel();
            running = true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            shutdownGroups();
            throw new IllegalStateException("netty server interrupted during start", ex);
        } catch (RuntimeException ex) {
            shutdownGroups();
            throw ex;
        }
    }

    @Override
    public void stop() {
        running = false;
        if (serverChannel != null) {
            serverChannel.close().awaitUninterruptibly(5, TimeUnit.SECONDS);
            serverChannel = null;
        }
        clients.close().awaitUninterruptibly(5, TimeUnit.SECONDS);
        shutdownGroups();
    }

    private void shutdownGroups() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).awaitUninterruptibly(5, TimeUnit.SECONDS);
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).awaitUninterruptibly(5, TimeUnit.SECONDS);
            workerGroup = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        if (serverChannel != null && serverChannel.localAddress() instanceof InetSocketAddress address) {
            return address.getPort();
        }
        return properties.getPort();
    }

    public int activeConnections() {
        return clients.size();
    }

    public ChannelGroup clients() {
        return clients;
    }
}

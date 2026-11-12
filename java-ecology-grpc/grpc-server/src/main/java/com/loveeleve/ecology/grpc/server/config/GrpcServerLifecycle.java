package com.loveeleve.ecology.grpc.server.config;

import com.loveeleve.ecology.grpc.server.interceptor.RequestIdServerInterceptor;
import com.loveeleve.ecology.grpc.server.service.OrderGrpcService;
import io.grpc.Server;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.services.HealthStatusManager;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@EnableConfigurationProperties(GrpcServerProperties.class)
public class GrpcServerLifecycle implements SmartLifecycle {

    private final GrpcServerProperties properties;
    private Server server;
    private HealthStatusManager healthStatusManager;
    private volatile boolean running;

    public GrpcServerLifecycle(GrpcServerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void start() {
        if (!properties.isEnabled() || running) {
            return;
        }
        healthStatusManager = new HealthStatusManager();
        try {
            server = NettyServerBuilder.forPort(properties.getPort())
                    .addService(new OrderGrpcService())
                    .addService(healthStatusManager.getHealthService())
                    .addService(ProtoReflectionService.newInstance())
                    .intercept(new RequestIdServerInterceptor())
                    .maxInboundMessageSize(properties.getMaxInboundMessageSize())
                    .keepAliveTime(properties.getKeepAliveSeconds(), TimeUnit.SECONDS)
                    .permitKeepAliveTime(properties.getPermitKeepAliveSeconds(), TimeUnit.SECONDS)
                    .permitKeepAliveWithoutCalls(true)
                    .build()
                    .start();
            healthStatusManager.setStatus("", HealthCheckResponse.ServingStatus.SERVING);
            running = true;
        } catch (Exception ex) {
            throw new IllegalStateException("failed to start grpc server", ex);
        }
    }

    @Override
    public void stop() {
        running = false;
        if (healthStatusManager != null) {
            healthStatusManager.setStatus("", HealthCheckResponse.ServingStatus.NOT_SERVING);
        }
        if (server != null) {
            server.shutdown();
            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                server.shutdownNow();
            }
            server = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return server != null ? server.getPort() : properties.getPort();
    }
}

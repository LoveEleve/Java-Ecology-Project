package com.loveeleve.ecology.grpc.client.config;

import com.loveeleve.ecology.grpc.client.interceptor.RequestIdClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(GrpcClientProperties.class)
public class GrpcClientConfiguration {

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel orderServiceChannel(GrpcClientProperties properties) {
        Map<String, Object> retryPolicy = Map.of(
                "maxAttempts", (double) Math.max(2, properties.getMaxRetryAttempts()),
                "initialBackoff", "0.1s",
                "maxBackoff", "1s",
                "backoffMultiplier", 2.0,
                "retryableStatusCodes", List.of("UNAVAILABLE"));
        Map<String, Object> methodConfig = Map.of(
                "name", List.of(Map.of("service", "ecology.grpc.OrderService")),
                "retryPolicy", retryPolicy);
        return NettyChannelBuilder.forAddress(properties.getHost(), properties.getPort())
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .enableRetry()
                .defaultServiceConfig(Map.of("methodConfig", List.of(methodConfig)))
                .intercept(new RequestIdClientInterceptor())
                .build();
    }
}

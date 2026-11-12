package com.loveeleve.ecology.grpc.server;

import com.loveeleve.ecology.grpc.api.CreateOrderRequest;
import com.loveeleve.ecology.grpc.api.OrderReply;
import com.loveeleve.ecology.grpc.api.OrderServiceGrpc;
import com.loveeleve.ecology.grpc.server.config.GrpcServerLifecycle;
import com.loveeleve.ecology.grpc.server.config.GrpcServerProperties;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class GrpcServerLifecycleTests {

    @Test
    void startsOnEphemeralPortAndServesUnaryCall() {
        GrpcServerProperties properties = new GrpcServerProperties();
        properties.setPort(0);
        GrpcServerLifecycle lifecycle = new GrpcServerLifecycle(properties);
        lifecycle.start();
        ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", lifecycle.getPort())
                .usePlaintext()
                .build();
        try {
            OrderReply reply = OrderServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .createOrder(CreateOrderRequest.newBuilder()
                            .setRequestId("netty-grpc-1")
                            .setUserId(1)
                            .setProductId(2)
                            .setAmount("100.00")
                            .setQuantity(1)
                            .build());
            assertThat(reply.getStatus()).isEqualTo("CONFIRMED");
            assertThat(reply.getRequestId()).isEqualTo("netty-grpc-1");
        } finally {
            channel.shutdownNow();
            lifecycle.stop();
        }
    }
}

package com.loveeleve.ecology.grpc.client;

import com.loveeleve.ecology.grpc.api.CreateOrderRequest;
import com.loveeleve.ecology.grpc.api.OrderReply;
import com.loveeleve.ecology.grpc.api.OrderServiceGrpc;
import com.loveeleve.ecology.grpc.client.config.GrpcClientConfiguration;
import com.loveeleve.ecology.grpc.client.config.GrpcClientProperties;
import com.loveeleve.ecology.grpc.client.service.OrderGrpcClient;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GrpcClientConfigurationTests {

    @Test
    void nettyClientConfigurationCanCallARealServer() throws Exception {
        Server server = NettyServerBuilder.forPort(0)
                .addService(new OrderServiceGrpc.OrderServiceImplBase() {
                    @Override
                    public void createOrder(CreateOrderRequest request, StreamObserver<OrderReply> observer) {
                        observer.onNext(OrderReply.newBuilder()
                                .setOrderId(1)
                                .setStatus("CONFIRMED")
                                .setRequestId(request.getRequestId())
                                .build());
                        observer.onCompleted();
                    }
                })
                .build()
                .start();
        GrpcClientProperties properties = new GrpcClientProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(server.getPort());
        properties.setDeadlineMillis(3000);
        ManagedChannel channel = new GrpcClientConfiguration().orderServiceChannel(properties);
        OrderGrpcClient client = new OrderGrpcClient(channel, properties);
        try {
            OrderReply reply = client.createOrder(CreateOrderRequest.newBuilder()
                    .setRequestId("client-config-1")
                    .setUserId(1)
                    .setProductId(2)
                    .setAmount("10.00")
                    .setQuantity(1)
                    .build());
            assertThat(reply.getStatus()).isEqualTo("CONFIRMED");
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }
}

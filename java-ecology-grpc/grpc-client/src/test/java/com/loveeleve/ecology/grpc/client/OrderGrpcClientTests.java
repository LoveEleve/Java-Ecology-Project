package com.loveeleve.ecology.grpc.client;

import com.loveeleve.ecology.grpc.api.CreateOrderRequest;
import com.loveeleve.ecology.grpc.api.OrderReply;
import com.loveeleve.ecology.grpc.api.OrderServiceGrpc;
import com.loveeleve.ecology.grpc.api.StreamOrdersRequest;
import com.loveeleve.ecology.grpc.client.config.GrpcClientProperties;
import com.loveeleve.ecology.grpc.client.service.OrderGrpcClient;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderGrpcClientTests {

    private Server server;
    private ManagedChannel channel;
    private OrderGrpcClient client;

    @BeforeEach
    void setUp() throws Exception {
        String name = "client-order-" + System.nanoTime();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
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

                    @Override
                    public void streamOrders(StreamOrdersRequest request, StreamObserver<OrderReply> observer) {
                        for (int i = 0; i < request.getCount(); i++) {
                            observer.onNext(OrderReply.newBuilder()
                                    .setOrderId(i + 1L)
                                    .setStatus("STREAMED")
                                    .setRequestId(String.valueOf(i))
                                    .build());
                        }
                        observer.onCompleted();
                    }
                })
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        GrpcClientProperties properties = new GrpcClientProperties();
        properties.setDeadlineMillis(2000);
        client = new OrderGrpcClient(channel, properties);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void unaryClientReturnsReply() {
        OrderReply reply = client.createOrder(CreateOrderRequest.newBuilder()
                .setRequestId("client-1")
                .setUserId(1)
                .setProductId(2)
                .setAmount("10.00")
                .setQuantity(1)
                .build());

        assertThat(reply.getOrderId()).isEqualTo(1);
        assertThat(reply.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void serverStreamingClientCollectsAllReplies() {
        List<OrderReply> replies = client.streamOrders(3);

        assertThat(replies).hasSize(3);
        assertThat(replies).extracting(OrderReply::getOrderId).containsExactly(1L, 2L, 3L);
    }
}

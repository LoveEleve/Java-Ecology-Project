package com.loveeleve.ecology.grpc.server;

import com.loveeleve.ecology.grpc.api.ChatMessage;
import com.loveeleve.ecology.grpc.api.CreateOrderRequest;
import com.loveeleve.ecology.grpc.api.OrderReply;
import com.loveeleve.ecology.grpc.api.OrderServiceGrpc;
import com.loveeleve.ecology.grpc.api.StreamOrdersRequest;
import com.loveeleve.ecology.grpc.api.UploadSummary;
import com.loveeleve.ecology.grpc.server.interceptor.RequestIdServerInterceptor;
import com.loveeleve.ecology.grpc.server.service.OrderGrpcService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderGrpcServiceTests {

    private Server server;
    private ManagedChannel channel;
    private OrderServiceGrpc.OrderServiceBlockingStub blockingStub;
    private OrderServiceGrpc.OrderServiceStub asyncStub;

    @BeforeEach
    void setUp() throws Exception {
        String name = "order-service-" + System.nanoTime();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(new OrderGrpcService())
                .intercept(new RequestIdServerInterceptor())
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        blockingStub = OrderServiceGrpc.newBlockingStub(channel);
        asyncStub = OrderServiceGrpc.newStub(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void unaryCreateOrderReturnsConfirmedReply() {
        OrderReply reply = blockingStub.createOrder(request("grpc-1"));

        assertThat(reply.getOrderId()).isPositive();
        assertThat(reply.getStatus()).isEqualTo("CONFIRMED");
        assertThat(reply.getRequestId()).isEqualTo("grpc-1");
    }

    @Test
    void invalidRequestMapsToInvalidArgument() {
        CreateOrderRequest invalid = request("grpc-2").toBuilder().setQuantity(0).build();

        assertThatThrownBy(() -> blockingStub.createOrder(invalid))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(ex -> assertThat(((StatusRuntimeException) ex).getStatus().getCode())
                        .isEqualTo(Status.Code.INVALID_ARGUMENT));
    }

    @Test
    void serverStreamingReturnsRequestedNumberOfReplies() {
        Iterator<OrderReply> iterator = blockingStub.streamOrders(
                StreamOrdersRequest.newBuilder().setCount(3).build());
        List<OrderReply> replies = new ArrayList<>();
        iterator.forEachRemaining(replies::add);

        assertThat(replies).hasSize(3);
        assertThat(replies).allMatch(reply -> reply.getStatus().equals("STREAMED"));
    }

    @Test
    void clientStreamingSummarizesAcceptedRequests() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<UploadSummary> summary = new AtomicReference<>();
        StreamObserver<CreateOrderRequest> requestObserver = asyncStub.uploadOrders(new StreamObserver<>() {
            @Override
            public void onNext(UploadSummary value) {
                summary.set(value);
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });

        requestObserver.onNext(request("upload-1"));
        requestObserver.onNext(request("upload-2"));
        requestObserver.onCompleted();

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(summary.get().getAccepted()).isEqualTo(2);
    }

    @Test
    void bidirectionalStreamingEchoesEachMessage() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        List<ChatMessage> replies = new ArrayList<>();
        StreamObserver<ChatMessage> requestObserver = asyncStub.chat(new StreamObserver<>() {
            @Override
            public void onNext(ChatMessage value) {
                replies.add(value);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }

            @Override
            public void onCompleted() {
            }
        });

        requestObserver.onNext(ChatMessage.newBuilder().setUser("u1").setText("a").build());
        requestObserver.onNext(ChatMessage.newBuilder().setUser("u1").setText("b").build());
        requestObserver.onCompleted();

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(replies).extracting(ChatMessage::getText).containsExactly("echo:a", "echo:b");
    }

    private CreateOrderRequest request(String requestId) {
        return CreateOrderRequest.newBuilder()
                .setRequestId(requestId)
                .setUserId(1)
                .setProductId(2)
                .setAmount("100.00")
                .setQuantity(1)
                .build();
    }
}

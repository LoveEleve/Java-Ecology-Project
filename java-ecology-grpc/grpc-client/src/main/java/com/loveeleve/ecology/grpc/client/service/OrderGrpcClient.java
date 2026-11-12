package com.loveeleve.ecology.grpc.client.service;

import com.loveeleve.ecology.grpc.api.ChatMessage;
import com.loveeleve.ecology.grpc.api.CreateOrderRequest;
import com.loveeleve.ecology.grpc.api.OrderReply;
import com.loveeleve.ecology.grpc.api.OrderServiceGrpc;
import com.loveeleve.ecology.grpc.api.StreamOrdersRequest;
import com.loveeleve.ecology.grpc.api.UploadSummary;
import com.loveeleve.ecology.grpc.client.config.GrpcClientProperties;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class OrderGrpcClient {

    private final ManagedChannel channel;
    private final GrpcClientProperties properties;
    private final OrderServiceGrpc.OrderServiceBlockingStub blockingStub;
    private final OrderServiceGrpc.OrderServiceStub asyncStub;

    public OrderGrpcClient(ManagedChannel channel, GrpcClientProperties properties) {
        this.channel = channel;
        this.properties = properties;
        this.blockingStub = OrderServiceGrpc.newBlockingStub(channel);
        this.asyncStub = OrderServiceGrpc.newStub(channel);
    }

    public OrderReply createOrder(CreateOrderRequest request) {
        return blockingStub.withDeadlineAfter(properties.getDeadlineMillis(), TimeUnit.MILLISECONDS)
                .createOrder(request);
    }

    public List<OrderReply> streamOrders(int count) {
        Iterator<OrderReply> iterator = blockingStub
                .withDeadlineAfter(properties.getDeadlineMillis(), TimeUnit.MILLISECONDS)
                .streamOrders(StreamOrdersRequest.newBuilder().setCount(count).build());
        List<OrderReply> replies = new ArrayList<>();
        iterator.forEachRemaining(replies::add);
        return replies;
    }

    public UploadSummary uploadOrders(List<CreateOrderRequest> requests) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<UploadSummary> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        StreamObserver<CreateOrderRequest> requestObserver = asyncStub
                .withDeadlineAfter(properties.getDeadlineMillis(), TimeUnit.MILLISECONDS)
                .uploadOrders(new StreamObserver<>() {
                    @Override
                    public void onNext(UploadSummary summary) {
                        result.set(summary);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        error.set(throwable);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }
                });
        for (CreateOrderRequest request : requests) {
            requestObserver.onNext(request);
        }
        requestObserver.onCompleted();
        if (!latch.await(properties.getDeadlineMillis(), TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("upload timed out");
        }
        if (error.get() != null) {
            throw new IllegalStateException(error.get());
        }
        return result.get();
    }

    public List<ChatMessage> chat(List<ChatMessage> messages) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<ChatMessage> replies = new CopyOnWriteArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        StreamObserver<ChatMessage> requestObserver = asyncStub
                .withDeadlineAfter(properties.getDeadlineMillis(), TimeUnit.MILLISECONDS)
                .chat(new StreamObserver<>() {
                    @Override
                    public void onNext(ChatMessage message) {
                        replies.add(message);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        error.set(throwable);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }
                });
        messages.forEach(requestObserver::onNext);
        requestObserver.onCompleted();
        if (!latch.await(properties.getDeadlineMillis(), TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("chat timed out");
        }
        if (error.get() != null) {
            throw new IllegalStateException(error.get());
        }
        return replies;
    }
}

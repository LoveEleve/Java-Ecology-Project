package com.loveeleve.ecology.grpc.server.service;

import com.loveeleve.ecology.grpc.api.ChatMessage;
import com.loveeleve.ecology.grpc.api.CreateOrderRequest;
import com.loveeleve.ecology.grpc.api.OrderReply;
import com.loveeleve.ecology.grpc.api.OrderServiceGrpc;
import com.loveeleve.ecology.grpc.api.StreamOrdersRequest;
import com.loveeleve.ecology.grpc.api.UploadSummary;
import com.loveeleve.ecology.grpc.server.interceptor.RequestIdServerInterceptor;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

public class OrderGrpcService extends OrderServiceGrpc.OrderServiceImplBase {

    private final AtomicLong orderIds = new AtomicLong(1000);

    @Override
    public void createOrder(CreateOrderRequest request, StreamObserver<OrderReply> responseObserver) {
        String error = validate(request);
        if (error != null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(error).asRuntimeException());
            return;
        }
        OrderReply reply = OrderReply.newBuilder()
                .setOrderId(orderIds.incrementAndGet())
                .setStatus("CONFIRMED")
                .setRequestId(request.getRequestId())
                .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void streamOrders(StreamOrdersRequest request, StreamObserver<OrderReply> responseObserver) {
        int count = Math.max(0, Math.min(request.getCount(), 1000));
        for (int i = 0; i < count; i++) {
            String requestId = RequestIdServerInterceptor.REQUEST_ID_CONTEXT.get();
            responseObserver.onNext(OrderReply.newBuilder()
                    .setOrderId(orderIds.incrementAndGet())
                    .setStatus("STREAMED")
                    .setRequestId(requestId + "-" + i)
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<CreateOrderRequest> uploadOrders(StreamObserver<UploadSummary> responseObserver) {
        return new StreamObserver<>() {
            private int accepted;

            @Override
            public void onNext(CreateOrderRequest request) {
                if (validate(request) == null) {
                    accepted++;
                }
            }

            @Override
            public void onError(Throwable throwable) {
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(UploadSummary.newBuilder()
                        .setAccepted(accepted)
                        .setRequestId(RequestIdServerInterceptor.REQUEST_ID_CONTEXT.get())
                        .build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessage> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(ChatMessage message) {
                responseObserver.onNext(ChatMessage.newBuilder()
                        .setUser(message.getUser())
                        .setText("echo:" + message.getText())
                        .build());
            }

            @Override
            public void onError(Throwable throwable) {
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    private String validate(CreateOrderRequest request) {
        if (request.getRequestId().isBlank()) {
            return "request_id must not be blank";
        }
        if (request.getUserId() <= 0 || request.getProductId() <= 0) {
            return "user_id and product_id must be positive";
        }
        if (request.getQuantity() <= 0) {
            return "quantity must be positive";
        }
        try {
            if (new BigDecimal(request.getAmount()).signum() <= 0) {
                return "amount must be positive";
            }
        } catch (NumberFormatException ex) {
            return "amount must be a valid decimal";
        }
        return null;
    }
}

package com.loveeleve.ecology.grpc.client.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import java.util.UUID;

public class RequestIdClientInterceptor implements ClientInterceptor {

    public static final Metadata.Key<String> REQUEST_ID_HEADER =
            Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        String requestId = UUID.randomUUID().toString();
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(REQUEST_ID_HEADER, requestId);
                super.start(responseListener, headers);
            }
        };
    }
}

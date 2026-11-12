package com.loveeleve.ecology.grpc.server.interceptor;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

import java.util.UUID;

public class RequestIdServerInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> REQUEST_ID_HEADER =
            Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER);

    public static final Context.Key<String> REQUEST_ID_CONTEXT = Context.key("request-id");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String requestId = headers.get(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        Context context = Context.current().withValue(REQUEST_ID_CONTEXT, requestId);
        return Contexts.interceptCall(context, call, headers, next);
    }
}

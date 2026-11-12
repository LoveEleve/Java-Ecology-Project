package com.loveeleve.ecology.dubbo.api;

import java.util.concurrent.CompletableFuture;

public interface OrderService {

    OrderView create(OrderRequest request);

    OrderView get(long orderId);

    String ping(String message);

    CompletableFuture<OrderView> createAsync(OrderRequest request);

    String echoAttachment();
}

package com.loveeleve.ecology.dubbo.consumer;

import com.loveeleve.ecology.dubbo.api.OrderRequest;
import com.loveeleve.ecology.dubbo.api.OrderService;
import com.loveeleve.ecology.dubbo.api.OrderView;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class OrderConsumer {

    @DubboReference(
            version = "1.0.0",
            group = "ecology",
            check = false,
            init = false,
            lazy = true,
            timeout = 3000,
            retries = 2,
            loadbalance = "roundrobin",
            cluster = "failover",
            validation = "true"
    )
    private OrderService orderService;

    public OrderView create(OrderRequest request) {
        return orderService.create(request);
    }

    public OrderView get(long orderId) {
        return orderService.get(orderId);
    }

    public String ping(String message) {
        return orderService.ping(message);
    }

    public CompletableFuture<OrderView> createAsync(OrderRequest request) {
        return orderService.createAsync(request);
    }

    public String echoAttachment() {
        try {
            RpcContext.getClientAttachment().setAttachment("tenant", "ecology");
            return orderService.echoAttachment();
        } finally {
            RpcContext.removeContext();
        }
    }
}

package com.loveeleve.ecology.dubbo.provider;

import com.loveeleve.ecology.dubbo.api.OrderRequest;
import com.loveeleve.ecology.dubbo.api.OrderService;
import com.loveeleve.ecology.dubbo.api.OrderView;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@DubboService(
        version = "1.0.0",
        group = "ecology",
        timeout = 3000,
        retries = 2,
        loadbalance = "roundrobin",
        validation = "true"
)
public class OrderServiceImpl implements OrderService {

    private final Map<Long, OrderView> orders = new ConcurrentHashMap<>();

    @Override
    public OrderView create(OrderRequest request) {
        OrderView order = new OrderView(request.getOrderId(), request.getUserId(),
                request.getAmount(), "CREATED");
        orders.put(order.getOrderId(), order);
        return order;
    }

    @Override
    public OrderView get(long orderId) {
        return orders.get(orderId);
    }

    @Override
    public String ping(String message) {
        return "pong:" + message;
    }

    @Override
    public CompletableFuture<OrderView> createAsync(OrderRequest request) {
        return CompletableFuture.completedFuture(create(request));
    }

    @Override
    public String echoAttachment() {
        String tenant = RpcContext.getServerAttachment().getAttachment("tenant");
        return tenant == null ? "missing" : tenant;
    }
}

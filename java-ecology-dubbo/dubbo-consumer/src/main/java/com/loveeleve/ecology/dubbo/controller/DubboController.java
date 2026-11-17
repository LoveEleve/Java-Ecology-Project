package com.loveeleve.ecology.dubbo.controller;

import com.loveeleve.ecology.dubbo.api.OrderRequest;
import com.loveeleve.ecology.dubbo.api.OrderView;
import com.loveeleve.ecology.dubbo.consumer.GenericOrderInvoker;
import com.loveeleve.ecology.dubbo.consumer.InstanceConsumer;
import com.loveeleve.ecology.dubbo.consumer.OrderConsumer;
import com.loveeleve.ecology.dubbo.consumer.SlowOrderConsumer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/dubbo")
public class DubboController {

    private final OrderConsumer orderConsumer;
    private final GenericOrderInvoker genericOrderInvoker;
    private final SlowOrderConsumer slowOrderConsumer;
    private final InstanceConsumer instanceConsumer;

    public DubboController(OrderConsumer orderConsumer,
                           GenericOrderInvoker genericOrderInvoker,
                           SlowOrderConsumer slowOrderConsumer,
                           InstanceConsumer instanceConsumer) {
        this.orderConsumer = orderConsumer;
        this.genericOrderInvoker = genericOrderInvoker;
        this.slowOrderConsumer = slowOrderConsumer;
        this.instanceConsumer = instanceConsumer;
    }

    @PostMapping("/orders")
    public OrderView create(@RequestBody OrderRequest request) {
        return orderConsumer.create(request);
    }

    @GetMapping("/orders/{orderId}")
    public OrderView get(@PathVariable long orderId) {
        return orderConsumer.get(orderId);
    }

    @GetMapping("/ping")
    public String ping(@RequestParam(defaultValue = "hello") String message) {
        return orderConsumer.ping(message);
    }

    @GetMapping("/instance")
    public String instance() {
        return instanceConsumer.instance();
    }

    @GetMapping("/slow")
    public String slow(@RequestParam(defaultValue = "hello") String message,
                       @RequestParam(defaultValue = "1000") long delayMs) {
        return slowOrderConsumer.slowPing(message, delayMs);
    }

    @PostMapping("/async/orders")
    public CompletableFuture<OrderView> createAsync(@RequestBody OrderRequest request) {
        return orderConsumer.createAsync(request);
    }

    @GetMapping("/attachment")
    public String attachment() {
        return orderConsumer.echoAttachment();
    }

    @GetMapping("/generic/ping")
    public Object genericPing(@RequestParam(defaultValue = "hello") String message) {
        return genericOrderInvoker.invoke("ping", new String[]{"java.lang.String"}, new Object[]{message});
    }
}

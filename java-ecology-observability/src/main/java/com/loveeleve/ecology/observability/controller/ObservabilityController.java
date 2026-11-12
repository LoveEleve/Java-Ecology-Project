package com.loveeleve.ecology.observability.controller;

import com.loveeleve.ecology.observability.service.ObservedOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {

    private final ObservedOrderService orderService;

    public ObservabilityController(ObservedOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders/{orderId}")
    public String order(@PathVariable String orderId) {
        return orderService.process(orderId);
    }

    @GetMapping("/slow")
    public String slow() throws InterruptedException {
        Thread.sleep(150);
        return "ok:slow";
    }
}

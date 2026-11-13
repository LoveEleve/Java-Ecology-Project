package com.loveeleve.ecology.skywalking.controller;

import com.loveeleve.ecology.skywalking.service.TracedOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/skywalking")
public class TracedOrderController {

    private final TracedOrderService service;

    public TracedOrderController(TracedOrderService service) {
        this.service = service;
    }

    @GetMapping("/orders/{orderId}")
    public Map<String, Object> order(@PathVariable String orderId,
                                     @RequestParam(defaultValue = "100.00") BigDecimal amount) {
        return Map.of(
                "result", service.process(orderId, amount),
                "riskOk", service.riskCheck(orderId),
                "traceId", service.traceId());
    }

    @GetMapping("/trace-id")
    public Map<String, String> traceId() {
        return Map.of("traceId", service.traceId());
    }
}

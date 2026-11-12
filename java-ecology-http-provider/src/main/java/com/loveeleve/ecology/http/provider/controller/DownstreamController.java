package com.loveeleve.ecology.http.provider.controller;

import com.loveeleve.ecology.http.provider.dto.EchoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/downstream")
public class DownstreamController {

    @GetMapping("/hello")
    public String hello(@RequestParam String name) {
        return "hello " + name;
    }

    @GetMapping("/echo")
    public EchoResponse echo(@RequestParam String message,
                             @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        return new EchoResponse(message, traceId);
    }

    @GetMapping("/not-found")
    public String notFound() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
    }

    @GetMapping("/slow")
    public String slow(@RequestParam(defaultValue = "2000") long millis) throws InterruptedException {
        Thread.sleep(Math.min(Math.max(millis, 0), 10_000));
        return "slow:" + millis;
    }
}

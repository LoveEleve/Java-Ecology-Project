package com.loveeleve.ecology.openfeign.controller;

import com.loveeleve.ecology.openfeign.dto.EchoResponse;
import com.loveeleve.ecology.openfeign.service.DownstreamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feign")
public class FeignController {

    private final DownstreamService downstreamService;

    public FeignController(DownstreamService downstreamService) {
        this.downstreamService = downstreamService;
    }

    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "feign") String name) {
        return downstreamService.hello(name);
    }

    @GetMapping("/echo")
    public EchoResponse echo(@RequestParam(defaultValue = "hello") String message) {
        return downstreamService.echo(message);
    }

    @GetMapping("/not-found")
    public String notFound() {
        return downstreamService.notFound();
    }

    @GetMapping("/slow")
    public String slow(@RequestParam(defaultValue = "5000") long millis) {
        return downstreamService.slow(millis);
    }
}

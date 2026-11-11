package com.loveeleve.ecology.web.controller;

import com.loveeleve.ecology.web.common.ApiResponse;
import com.loveeleve.ecology.web.common.RequestIds;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/hello")
public class HelloController {

    @GetMapping
    public ApiResponse<Map<String, Object>> hello(
            @RequestParam(defaultValue = "world") String name,
            @RequestHeader(value = "X-Demo", required = false) String demoHeader) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "hello " + name);
        data.put("demoHeader", demoHeader);
        data.put("requestId", RequestIds.current());
        data.put("serverTime", Instant.now());
        return ApiResponse.ok(data);
    }

    @GetMapping("/{name}")
    public ApiResponse<String> helloPath(@PathVariable String name) {
        return ApiResponse.ok("hello " + name);
    }

    @PostMapping("/echo")
    public ApiResponse<Map<String, Object>> echo(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(body);
    }
}

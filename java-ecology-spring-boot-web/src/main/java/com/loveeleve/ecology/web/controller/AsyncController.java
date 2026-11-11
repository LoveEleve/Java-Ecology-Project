package com.loveeleve.ecology.web.controller;

import com.loveeleve.ecology.web.common.ApiResponse;
import com.loveeleve.ecology.web.service.AsyncDemoService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/async")
public class AsyncController {

    private final AsyncDemoService asyncDemoService;
    private final ThreadPoolTaskExecutor taskExecutor;

    public AsyncController(AsyncDemoService asyncDemoService,
                           @Qualifier("demoTaskExecutor") ThreadPoolTaskExecutor taskExecutor) {
        this.asyncDemoService = asyncDemoService;
        this.taskExecutor = taskExecutor;
    }

    @GetMapping
    public CompletableFuture<ApiResponse<String>> async(@RequestParam(defaultValue = "demo") String input) {
        return asyncDemoService.compute(input).thenApply(ApiResponse::ok);
    }

    @GetMapping("/deferred")
    public DeferredResult<ApiResponse<String>> deferred(@RequestParam(defaultValue = "demo") String input) {
        DeferredResult<ApiResponse<String>> result = new DeferredResult<>(5000L);
        taskExecutor.execute(() -> {
            try {
                Thread.sleep(200);
                result.setResult(ApiResponse.ok("deferred:" + input + "@" + Instant.now()));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                result.setErrorResult(ex);
            }
        });
        result.onTimeout(() -> result.setErrorResult(new IllegalStateException("deferred timeout")));
        return result;
    }
}

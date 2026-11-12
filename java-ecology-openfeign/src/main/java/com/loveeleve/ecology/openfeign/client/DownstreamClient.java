package com.loveeleve.ecology.openfeign.client;

import com.loveeleve.ecology.openfeign.config.DemoFeignConfig;
import com.loveeleve.ecology.openfeign.dto.EchoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "ecology-http-provider",
        path = "/api/downstream",
        configuration = DemoFeignConfig.class,
        fallbackFactory = DownstreamClientFallbackFactory.class
)
public interface DownstreamClient {

    @GetMapping("/hello")
    String hello(@RequestParam("name") String name);

    @GetMapping("/echo")
    EchoResponse echo(@RequestParam("message") String message);

    @GetMapping("/not-found")
    String notFound();

    @GetMapping("/slow")
    String slow(@RequestParam("millis") long millis);
}

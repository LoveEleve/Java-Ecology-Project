package com.loveeleve.ecology.openfeign.config;

import com.loveeleve.ecology.openfeign.exception.DownstreamException;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Configuration(proxyBeanMethods = false)
public class DemoFeignConfig {

    @Bean
    public RequestInterceptor traceIdInterceptor() {
        return template -> {
            template.header("X-Trace-Id", UUID.randomUUID().toString());
            template.header("X-Demo-Token", "ecology-demo");
        };
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        ErrorDecoder.Default delegate = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            if (response.status() == 404) {
                return new DownstreamException(404, "downstream endpoint not found: " + methodKey);
            }
            return delegate.decode(methodKey, response);
        };
    }

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 500, 2);
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(1000, TimeUnit.MILLISECONDS,
                3000, TimeUnit.MILLISECONDS, true);
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}

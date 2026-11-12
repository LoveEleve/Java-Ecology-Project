package com.loveeleve.ecology.redisson.config;

import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RedissonConfig {

    @Bean
    public RedissonAutoConfigurationCustomizer redissonCustomizer() {
        return config -> {
            config.setCodec(new JsonJacksonCodec());
            config.setLockWatchdogTimeout(30_000);
            if (config.isSingleConfig()) {
                config.useSingleServer()
                        .setConnectionMinimumIdleSize(2)
                        .setConnectionPoolSize(16)
                        .setSubscriptionConnectionPoolSize(4)
                        .setRetryAttempts(3)
                        .setRetryInterval(1500);
            }
        };
    }
}

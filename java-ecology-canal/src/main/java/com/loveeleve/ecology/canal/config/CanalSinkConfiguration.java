package com.loveeleve.ecology.canal.config;

import com.loveeleve.ecology.canal.service.CanalSink;
import com.loveeleve.ecology.canal.service.LoggingCanalSink;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CanalSinkConfiguration {

    @Bean
    @ConditionalOnMissingBean(CanalSink.class)
    public CanalSink loggingCanalSink() {
        return new LoggingCanalSink();
    }
}

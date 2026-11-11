package com.loveeleve.ecology.mybatis.config;

import com.loveeleve.ecology.mybatis.interceptor.SlowSqlInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@MapperScan("com.loveeleve.ecology.mybatis.mapper")
@EnableConfigurationProperties(SlowSqlProperties.class)
public class MyBatisConfig {

    @Bean
    public SlowSqlInterceptor slowSqlInterceptor(SlowSqlProperties properties) {
        return new SlowSqlInterceptor(properties);
    }
}

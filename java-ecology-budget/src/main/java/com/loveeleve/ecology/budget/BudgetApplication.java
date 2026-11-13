package com.loveeleve.ecology.budget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 预算预扣服务启动类。
 *
 * <p>生产部署时预算服务通常作为独立微服务部署，依赖 MySQL/Redis 与配置中心；
 * 本模块用 H2/MySQL profile 展示完整 Spring Boot 接入方式。</p>
 */
@EnableScheduling
@SpringBootApplication
public class BudgetApplication {

    public static void main(String[] args) {
        SpringApplication.run(BudgetApplication.class, args);
    }
}

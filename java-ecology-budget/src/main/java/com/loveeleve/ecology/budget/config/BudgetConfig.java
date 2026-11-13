package com.loveeleve.ecology.budget.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 预算服务基础配置。
 */
@Configuration
@EnableConfigurationProperties(BudgetProperties.class)
public class BudgetConfig {

    /**
     * 显式 TransactionTemplate，便于在“多来源逐个尝试”时给每个来源独立事务：
     * 来源 A 不足回滚后，还能继续尝试来源 B。
     */
    @Bean
    public TransactionTemplate budgetTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}

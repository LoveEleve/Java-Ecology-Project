package com.loveeleve.ecology.mybatisplus.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.loveeleve.ecology.mybatisplus.advanced.DataPermissionHandlerImpl;
import com.loveeleve.ecology.mybatisplus.advanced.DynamicTableNameHandlerImpl;
import com.loveeleve.ecology.mybatisplus.advanced.TenantLineHandlerImpl;
import com.loveeleve.ecology.mybatisplus.injector.CustomSqlInjector;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@MapperScan("com.loveeleve.ecology.mybatisplus.mapper")
@EnableConfigurationProperties(AdvancedMybatisPlusProperties.class)
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(AdvancedMybatisPlusProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        if (properties.isTenantEnabled()) {
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
                    new TenantLineHandlerImpl(properties)));
        }
        if (properties.isDataPermissionEnabled()) {
            interceptor.addInnerInterceptor(new DataPermissionInterceptor(
                    new DataPermissionHandlerImpl(properties)));
        }
        if (properties.isDynamicTableEnabled()) {
            interceptor.addInnerInterceptor(new DynamicTableNameInnerInterceptor(
                    new DynamicTableNameHandlerImpl(properties)));
        }
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    @Bean
    public ISqlInjector customSqlInjector() {
        return new CustomSqlInjector();
    }
}

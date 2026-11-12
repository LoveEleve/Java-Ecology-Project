package com.loveeleve.ecology.sentinel.config;

import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Properties;

@Configuration
@ConditionalOnProperty(prefix = "app.sentinel.nacos", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SentinelNacosProperties.class)
public class SentinelNacosRuleConfiguration {

    private final SentinelNacosProperties properties;
    private final ObjectMapper objectMapper;

    public SentinelNacosRuleConfiguration(SentinelNacosProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Bean(destroyMethod = "close")
    public NacosDataSource<List<FlowRule>> flowRuleDataSource() {
        NacosDataSource<List<FlowRule>> source = source("flow-rules", new TypeReference<>() {
        });
        FlowRuleManager.register2Property(source.getProperty());
        return source;
    }

    @Bean(destroyMethod = "close")
    public NacosDataSource<List<DegradeRule>> degradeRuleDataSource() {
        NacosDataSource<List<DegradeRule>> source = source("degrade-rules", new TypeReference<>() {
        });
        DegradeRuleManager.register2Property(source.getProperty());
        return source;
    }

    @Bean(destroyMethod = "close")
    public NacosDataSource<List<ParamFlowRule>> paramFlowRuleDataSource() {
        NacosDataSource<List<ParamFlowRule>> source = source("param-flow-rules", new TypeReference<>() {
        });
        ParamFlowRuleManager.register2Property(source.getProperty());
        return source;
    }

    @Bean(destroyMethod = "close")
    public NacosDataSource<List<SystemRule>> systemRuleDataSource() {
        NacosDataSource<List<SystemRule>> source = source("system-rules", new TypeReference<>() {
        });
        SystemRuleManager.register2Property(source.getProperty());
        return source;
    }

    @Bean(destroyMethod = "close")
    public NacosDataSource<List<AuthorityRule>> authorityRuleDataSource() {
        NacosDataSource<List<AuthorityRule>> source = source("authority-rules", new TypeReference<>() {
        });
        AuthorityRuleManager.register2Property(source.getProperty());
        return source;
    }

    private <T> NacosDataSource<T> source(String suffix, TypeReference<T> type) {
        return new NacosDataSource<>(nacosProperties(), properties.getGroupId(),
                properties.getDataIdPrefix() + "-" + suffix, converter(type));
    }

    private Properties nacosProperties() {
        Properties result = new Properties();
        result.setProperty(PropertyKeyConst.SERVER_ADDR, properties.getServerAddr());
        if (StringUtils.hasText(properties.getNamespace())) {
            result.setProperty(PropertyKeyConst.NAMESPACE, properties.getNamespace());
        }
        if (StringUtils.hasText(properties.getUsername())) {
            result.setProperty(PropertyKeyConst.USERNAME, properties.getUsername());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            result.setProperty(PropertyKeyConst.PASSWORD, properties.getPassword());
        }
        if (StringUtils.hasText(properties.getAccessKey())) {
            result.setProperty(PropertyKeyConst.ACCESS_KEY, properties.getAccessKey());
        }
        if (StringUtils.hasText(properties.getSecretKey())) {
            result.setProperty(PropertyKeyConst.SECRET_KEY, properties.getSecretKey());
        }
        return result;
    }

    private <T> Converter<String, T> converter(TypeReference<T> type) {
        return source -> {
            try {
                return objectMapper.readValue(source, type);
            } catch (JsonProcessingException ex) {
                throw new IllegalArgumentException("invalid sentinel rule JSON", ex);
            }
        };
    }
}

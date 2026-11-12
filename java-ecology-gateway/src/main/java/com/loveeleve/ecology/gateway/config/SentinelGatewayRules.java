package com.loveeleve.ecology.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.sentinel", name = "local-rules-enabled",
        havingValue = "true", matchIfMissing = true)
public class SentinelGatewayRules {

    @PostConstruct
    public void loadRules() {
        GatewayFlowRule providerRule = new GatewayFlowRule("http-provider")
                .setCount(100)
                .setIntervalSec(1);
        GatewayRuleManager.loadRules(Set.of(providerRule));
    }
}

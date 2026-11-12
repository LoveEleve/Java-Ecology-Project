package com.loveeleve.ecology.openfeign.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.sentinel", name = "local-rules-enabled",
        havingValue = "true", matchIfMissing = true)
public class SentinelRulesConfig {

    @PostConstruct
    public void loadRules() {
        FlowRule helloFlow = new FlowRule("downstream.hello")
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(20)
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        FlowRule echoFlow = new FlowRule("downstream.echo")
                .setGrade(RuleConstant.FLOW_GRADE_THREAD)
                .setCount(20);
        FlowRuleManager.loadRules(List.of(helloFlow, echoFlow));

        DegradeRule helloDegrade = new DegradeRule("downstream.hello")
                .setGrade(RuleConstant.DEGRADE_GRADE_RT)
                .setCount(1000)
                .setTimeWindow(10)
                .setMinRequestAmount(5)
                .setSlowRatioThreshold(0.5);
        DegradeRuleManager.loadRules(List.of(helloDegrade));
    }
}

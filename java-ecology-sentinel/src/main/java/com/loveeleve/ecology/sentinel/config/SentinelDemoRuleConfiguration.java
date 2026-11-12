package com.loveeleve.ecology.sentinel.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
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
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnExpression("${app.sentinel.demo-rules:true} && ${app.sentinel.nacos.enabled:false} == false")
public class SentinelDemoRuleConfiguration {

    @PostConstruct
    public void loadDemoRules() {
        FlowRule flow = new FlowRule("flow-order");
        flow.setGrade(RuleConstant.FLOW_GRADE_QPS);
        flow.setCount(5);
        flow.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER);
        flow.setMaxQueueingTimeMs(200);

        FlowRule warmUp = new FlowRule("flow-warmup");
        warmUp.setGrade(RuleConstant.FLOW_GRADE_QPS);
        warmUp.setCount(5);
        warmUp.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_WARM_UP);
        warmUp.setWarmUpPeriodSec(10);

        FlowRule thread = new FlowRule("flow-thread");
        thread.setGrade(RuleConstant.FLOW_GRADE_THREAD);
        thread.setCount(2);

        DegradeRule slow = new DegradeRule("degrade-order");
        slow.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        slow.setCount(200);
        slow.setTimeWindow(10);
        slow.setMinRequestAmount(5);
        slow.setStatIntervalMs(1000);
        slow.setSlowRatioThreshold(0.5);

        DegradeRule exception = new DegradeRule("degrade-exception");
        exception.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        exception.setCount(0.5);
        exception.setTimeWindow(10);
        exception.setMinRequestAmount(3);
        exception.setStatIntervalMs(1000);

        ParamFlowRule hot = new ParamFlowRule("hot-param");
        hot.setParamIdx(0);
        hot.setCount(2);
        hot.setDurationInSec(1);

        SystemRule system = new SystemRule();
        system.setQps(500);

        AuthorityRule authority = new AuthorityRule();
        authority.setResource("authority-order");
        authority.setLimitApp("trusted-client");
        authority.setStrategy(RuleConstant.AUTHORITY_WHITE);

        FlowRuleManager.loadRules(List.of(flow, warmUp, thread));
        DegradeRuleManager.loadRules(List.of(slow, exception));
        ParamFlowRuleManager.loadRules(List.of(hot));
        SystemRuleManager.loadRules(List.of(system));
        AuthorityRuleManager.loadRules(List.of(authority));
    }
}

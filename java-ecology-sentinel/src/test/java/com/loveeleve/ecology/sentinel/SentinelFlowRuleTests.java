package com.loveeleve.ecology.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SentinelFlowRuleTests {

    private List<FlowRule> originalRules;

    @BeforeEach
    void captureRules() {
        originalRules = FlowRuleManager.getRules();
    }

    @AfterEach
    void restoreRules() {
        FlowRuleManager.loadRules(originalRules);
    }

    @Test
    void qpsRuleBlocksTheSecondRequestInTheSameSecond() throws Exception {
        FlowRule rule = new FlowRule("test-flow-qps");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(1);
        FlowRuleManager.loadRules(List.of(rule));

        Entry first = SphU.entry("test-flow-qps");
        first.exit();

        assertThatThrownBy(() -> {
            Entry second = SphU.entry("test-flow-qps");
            second.exit();
        }).isInstanceOf(BlockException.class);
    }

    @Test
    void threadRuleBlocksWhenConcurrencyIsExceeded() throws Exception {
        FlowRule rule = new FlowRule("test-flow-thread");
        rule.setGrade(RuleConstant.FLOW_GRADE_THREAD);
        rule.setCount(1);
        FlowRuleManager.loadRules(List.of(rule));

        Entry first = SphU.entry("test-flow-thread");
        try {
            assertThatThrownBy(() -> {
                Entry second = SphU.entry("test-flow-thread");
                second.exit();
            }).isInstanceOf(BlockException.class);
        } finally {
            first.exit();
        }
    }

    @Test
    void annotationUsesBlockHandler() throws Exception {
        FlowRule rule = new FlowRule("annotated-flow");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(1);
        FlowRuleManager.loadRules(List.of(rule));

        AspectJProxyFactory factory = new AspectJProxyFactory(new AnnotatedFlowService());
        factory.addAspect(SentinelResourceAspect.class);
        AnnotatedFlowService proxy = factory.getProxy();

        assertThat(proxy.call("u1")).startsWith("ok:");
        assertThat(proxy.call("u1")).startsWith("blocked:");
    }

    static class AnnotatedFlowService {

        @SentinelResource(value = "annotated-flow", blockHandler = "blocked")
        public String call(String userId) {
            return "ok:" + userId;
        }

        public String blocked(String userId, BlockException ex) {
            return "blocked:" + ex.getClass().getSimpleName();
        }
    }
}

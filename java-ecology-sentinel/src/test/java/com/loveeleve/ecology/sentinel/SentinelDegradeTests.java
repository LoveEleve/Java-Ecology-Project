package com.loveeleve.ecology.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.loveeleve.ecology.sentinel.service.DegradeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SentinelDegradeTests {

    private List<DegradeRule> originalRules;

    @BeforeEach
    void captureRules() {
        originalRules = DegradeRuleManager.getRules();
    }

    @AfterEach
    void restoreRules() {
        DegradeRuleManager.loadRules(originalRules);
    }

    @Test
    void exceptionRatioRuleOpensCircuitAfterFailure() throws Exception {
        DegradeRule rule = new DegradeRule("degrade-test");
        rule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        rule.setCount(0.5);
        rule.setMinRequestAmount(1);
        rule.setStatIntervalMs(1000);
        rule.setTimeWindow(10);
        DegradeRuleManager.loadRules(List.of(rule));

        Entry entry = SphU.entry("degrade-test");
        try {
            try {
                throw new IllegalStateException("simulated failure");
            } catch (IllegalStateException ex) {
                Tracer.traceEntry(ex, entry);
            }
        } finally {
            entry.exit();
        }

        assertThatThrownBy(() -> {
            Entry blocked = SphU.entry("degrade-test");
            blocked.exit();
        }).isInstanceOf(BlockException.class);
    }

    @Test
    void slowCallRuleOpensCircuitAndUsesFallback() throws Exception {
        DegradeRule rule = new DegradeRule("degrade-order");
        rule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        rule.setCount(10);
        rule.setMinRequestAmount(1);
        rule.setStatIntervalMs(1000);
        rule.setTimeWindow(10);
        rule.setSlowRatioThreshold(1.0);
        DegradeRuleManager.loadRules(List.of(rule));

        AspectJProxyFactory factory = new AspectJProxyFactory(new DegradeService());
        factory.addAspect(SentinelResourceAspect.class);
        DegradeService proxy = factory.getProxy();

        assertThat(proxy.slow(20)).startsWith("ok:slow:");
        assertThat(proxy.slow(20)).startsWith("fallback:degrade-order:");
    }

    @Test
    void annotationFallbackHandlesException() throws Exception {
        AspectJProxyFactory factory = new AspectJProxyFactory(new DegradeService());
        factory.addAspect(SentinelResourceAspect.class);
        DegradeService proxy = factory.getProxy();

        assertThat(proxy.exception()).startsWith("fallback:degrade-exception:");
    }
}

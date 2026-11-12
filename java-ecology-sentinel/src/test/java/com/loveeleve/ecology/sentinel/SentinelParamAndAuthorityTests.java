package com.loveeleve.ecology.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SentinelParamAndAuthorityTests {

    private List<ParamFlowRule> originalParamRules;
    private List<AuthorityRule> originalAuthorityRules;

    @BeforeEach
    void captureRules() {
        originalParamRules = ParamFlowRuleManager.getRules();
        originalAuthorityRules = AuthorityRuleManager.getRules();
    }

    @AfterEach
    void restoreRules() {
        ParamFlowRuleManager.loadRules(originalParamRules);
        AuthorityRuleManager.loadRules(originalAuthorityRules);
    }

    @Test
    void sameHotParameterIsLimitedButAnotherParameterIsAllowed() throws Exception {
        ParamFlowRule rule = new ParamFlowRule("hot-test");
        rule.setParamIdx(0);
        rule.setCount(1);
        rule.setDurationInSec(1);
        ParamFlowRuleManager.loadRules(List.of(rule));

        Entry first = SphU.entry("hot-test", EntryType.IN, 1, "user-1");
        first.exit();

        assertThatThrownBy(() -> {
            Entry second = SphU.entry("hot-test", EntryType.IN, 1, "user-1");
            second.exit();
        }).isInstanceOf(BlockException.class);

        Entry another = SphU.entry("hot-test", EntryType.IN, 1, "user-2");
        another.exit();
    }

    @Test
    void authorityRuleRejectsUnknownOrigin() {
        AuthorityRule rule = new AuthorityRule();
        rule.setResource("authority-test");
        rule.setLimitApp("trusted-client");
        rule.setStrategy(RuleConstant.AUTHORITY_WHITE);
        AuthorityRuleManager.loadRules(List.of(rule));

        ContextUtil.enter("authority-test-context", "unknown-client");
        try {
            assertThatThrownBy(() -> {
                Entry entry = SphU.entry("authority-test");
                entry.exit();
            }).isInstanceOf(BlockException.class);
        } finally {
            ContextUtil.exit();
        }
    }
}

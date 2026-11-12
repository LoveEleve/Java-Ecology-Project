package com.loveeleve.ecology.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SentinelSystemRuleTests {

    private List<SystemRule> originalRules;

    @BeforeEach
    void captureRules() {
        originalRules = SystemRuleManager.getRules();
    }

    @AfterEach
    void restoreRules() {
        SystemRuleManager.loadRules(originalRules);
    }

    @Test
    void systemQpsRuleBlocksAfterGlobalThreshold() throws Exception {
        Thread.sleep(1100);
        SystemRule rule = new SystemRule();
        rule.setQps(1.5);
        SystemRuleManager.loadRules(List.of(rule));

        Entry first = SphU.entry("system-test", EntryType.IN);
        first.exit();

        assertThatThrownBy(() -> {
            Entry second = SphU.entry("system-test", EntryType.IN);
            second.exit();
        }).isInstanceOf(BlockException.class);
    }
}

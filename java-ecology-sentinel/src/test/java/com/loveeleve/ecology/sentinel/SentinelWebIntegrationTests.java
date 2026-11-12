package com.loveeleve.ecology.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SentinelWebIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    private List<FlowRule> originalFlowRules;

    @BeforeEach
    void captureFlowRules() {
        originalFlowRules = FlowRuleManager.getRules();
    }

    @AfterEach
    void restoreFlowRules() {
        FlowRuleManager.loadRules(originalFlowRules);
    }

    @Test
    void authorityRejectsAnonymousAndAllowsTrustedOrigin() throws Exception {
        mockMvc.perform(get("/api/sentinel/authority"))
                .andExpect(status().isOk())
                .andExpect(content().string("blocked:authority-order:AuthorityException"));

        mockMvc.perform(get("/api/sentinel/authority").header("X-Origin", "trusted-client"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok:authority"));
    }

    @Test
    void hotParameterIsBlockedAfterThreshold() throws Exception {
        mockMvc.perform(get("/api/sentinel/hot").param("userId", "hot-user"))
                .andExpect(content().string("ok:hot:hot-user"));
        mockMvc.perform(get("/api/sentinel/hot").param("userId", "hot-user"))
                .andExpect(content().string("ok:hot:hot-user"));
        mockMvc.perform(get("/api/sentinel/hot").param("userId", "hot-user"))
                .andExpect(content().string("blocked:hot-param:hot-user:ParamFlowException"));
    }

    @Test
    void urlRuleUsesCleanedResourceAndReturnsJson429() throws Exception {
        FlowRule rule = new FlowRule("/api/sentinel/orders/{orderId}");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(1);
        FlowRuleManager.loadRules(List.of(rule));

        mockMvc.perform(get("/api/sentinel/orders/10086"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sentinel/orders/10087"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":\"SENTINEL_BLOCKED\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"resource\":\"/api/sentinel/orders/{orderId}\"")));
    }

    @Test
    void rulesEndpointExposesCurrentRules() throws Exception {
        mockMvc.perform(get("/api/sentinel/rules"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"flow\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"authority\"")));
    }
}

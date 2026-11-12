package com.loveeleve.ecology.sentinel.controller;

import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.loveeleve.ecology.sentinel.service.AuthorityService;
import com.loveeleve.ecology.sentinel.service.DegradeService;
import com.loveeleve.ecology.sentinel.service.FlowControlService;
import com.loveeleve.ecology.sentinel.service.HotParamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sentinel")
public class SentinelController {

    private final FlowControlService flowControlService;
    private final DegradeService degradeService;
    private final HotParamService hotParamService;
    private final AuthorityService authorityService;

    public SentinelController(FlowControlService flowControlService,
                              DegradeService degradeService,
                              HotParamService hotParamService,
                              AuthorityService authorityService) {
        this.flowControlService = flowControlService;
        this.degradeService = degradeService;
        this.hotParamService = hotParamService;
        this.authorityService = authorityService;
    }

    @GetMapping("/flow/order")
    public String order(@RequestParam(defaultValue = "user-1") String userId) {
        return flowControlService.order(userId);
    }

    @GetMapping("/flow/warmup")
    public String warmUp(@RequestParam(defaultValue = "user-1") String userId) {
        return flowControlService.warmUp(userId);
    }

    @GetMapping("/flow/thread")
    public String thread(@RequestParam(defaultValue = "500") long millis) throws InterruptedException {
        return flowControlService.occupyThread(millis);
    }

    @GetMapping("/degrade/slow")
    public String slow(@RequestParam(defaultValue = "0") long delayMs) throws InterruptedException {
        return degradeService.slow(delayMs);
    }

    @GetMapping("/degrade/exception")
    public String exception() {
        return degradeService.exception();
    }

    @GetMapping("/hot")
    public String hot(@RequestParam(defaultValue = "user-1") String userId) {
        return hotParamService.query(userId);
    }

    @GetMapping("/authority")
    public String authority() {
        return authorityService.order();
    }

    @GetMapping("/orders/{orderId}")
    public String orderById(@PathVariable String orderId) {
        return "ok:order:" + orderId;
    }

    @GetMapping("/rules")
    public Map<String, Object> rules() {
        return Map.of(
                "flow", FlowRuleManager.getRules(),
                "degrade", DegradeRuleManager.getRules(),
                "param", ParamFlowRuleManager.getRules(),
                "system", SystemRuleManager.getRules(),
                "authority", AuthorityRuleManager.getRules()
        );
    }
}

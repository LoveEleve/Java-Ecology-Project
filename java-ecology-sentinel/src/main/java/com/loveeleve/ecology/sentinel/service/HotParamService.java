package com.loveeleve.ecology.sentinel.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.stereotype.Service;

@Service
public class HotParamService {

    @SentinelResource(value = "hot-param", blockHandler = "hotBlocked")
    public String query(String userId) {
        return "ok:hot:" + userId;
    }

    public String hotBlocked(String userId, BlockException ex) {
        return "blocked:hot-param:" + userId + ":" + ex.getClass().getSimpleName();
    }
}

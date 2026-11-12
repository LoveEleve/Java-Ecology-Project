package com.loveeleve.ecology.sentinel.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.stereotype.Service;

@Service
public class AuthorityService {

    @SentinelResource(value = "authority-order", blockHandler = "authorityBlocked")
    public String order() {
        return "ok:authority";
    }

    public String authorityBlocked(BlockException ex) {
        return "blocked:authority-order:" + ex.getClass().getSimpleName();
    }
}

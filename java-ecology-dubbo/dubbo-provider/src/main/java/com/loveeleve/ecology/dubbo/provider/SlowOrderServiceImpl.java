package com.loveeleve.ecology.dubbo.provider;

import com.loveeleve.ecology.dubbo.api.SlowOrderService;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 仅用于演示 consumer 侧 timeout/failfast，不是生产业务接口。
 */
@DubboService(
        version = "1.0.0",
        group = "ecology",
        timeout = 1000,
        retries = 0
)
public class SlowOrderServiceImpl implements SlowOrderService {

    @Override
    public String slowPing(String message, long delayMs) {
        if (delayMs < 0) {
            throw new IllegalArgumentException("delayMs must be >= 0");
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("slowPing interrupted", e);
        }
        return "slow:" + message;
    }
}

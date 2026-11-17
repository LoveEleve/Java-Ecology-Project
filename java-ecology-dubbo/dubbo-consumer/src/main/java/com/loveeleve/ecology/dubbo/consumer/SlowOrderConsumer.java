package com.loveeleve.ecology.dubbo.consumer;

import com.loveeleve.ecology.dubbo.api.SlowOrderService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * 超时/快速失败示例：单次 500ms 超时，不重试，避免非幂等操作被重复执行。
 */
@Component
public class SlowOrderConsumer {

    @DubboReference(
            version = "1.0.0",
            group = "ecology",
            check = false,
            init = false,
            lazy = true,
            timeout = 500,
            retries = 0,
            cluster = "failfast"
    )
    private SlowOrderService slowOrderService;

    public String slowPing(String message, long delayMs) {
        return slowOrderService.slowPing(message, delayMs);
    }
}

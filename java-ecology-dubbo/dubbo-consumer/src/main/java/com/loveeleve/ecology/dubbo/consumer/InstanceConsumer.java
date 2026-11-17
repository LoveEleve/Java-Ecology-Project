package com.loveeleve.ecology.dubbo.consumer;

import com.loveeleve.ecology.dubbo.api.InstanceService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

@Component
public class InstanceConsumer {

    @DubboReference(
            version = "1.0.0",
            group = "ecology",
            check = false,
            init = false,
            lazy = true,
            timeout = 1000,
            retries = 0,
            loadbalance = "roundrobin"
    )
    private InstanceService instanceService;

    public String instance() {
        return instanceService.instance();
    }
}

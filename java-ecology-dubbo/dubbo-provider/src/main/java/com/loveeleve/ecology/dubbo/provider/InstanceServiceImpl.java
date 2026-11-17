package com.loveeleve.ecology.dubbo.provider;

import com.loveeleve.ecology.dubbo.api.InstanceService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;

@DubboService(
        version = "1.0.0",
        group = "ecology",
        timeout = 1000,
        retries = 0
)
public class InstanceServiceImpl implements InstanceService {

    private final String instance;

    public InstanceServiceImpl(@Value("${server.port:8090}") String instance) {
        this.instance = instance;
    }

    @Override
    public String instance() {
        return instance;
    }
}

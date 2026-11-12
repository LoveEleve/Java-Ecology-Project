package com.loveeleve.ecology.dubbo.consumer;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.stereotype.Component;

@Component
public class GenericOrderInvoker {

    @DubboReference(
            interfaceName = "com.loveeleve.ecology.dubbo.api.OrderService",
            version = "1.0.0",
            group = "ecology",
            check = false,
            init = false,
            lazy = true
    )
    private GenericService genericService;

    public Object invoke(String method, String[] parameterTypes, Object[] args) {
        return genericService.$invoke(method, parameterTypes, args);
    }
}

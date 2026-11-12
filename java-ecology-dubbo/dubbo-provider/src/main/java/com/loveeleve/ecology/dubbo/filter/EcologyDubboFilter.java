package com.loveeleve.ecology.dubbo.filter;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, order = 100)
public class EcologyDubboFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(EcologyDubboFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long start = System.nanoTime();
        try {
            return invoker.invoke(invocation);
        } finally {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            log.debug("dubbo {}#{} cost={}ms", invoker.getInterface().getName(),
                    invocation.getMethodName(), costMs);
        }
    }
}

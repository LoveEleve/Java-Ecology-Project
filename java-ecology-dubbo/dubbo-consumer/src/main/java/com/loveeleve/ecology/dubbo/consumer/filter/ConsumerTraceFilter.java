package com.loveeleve.ecology.dubbo.consumer.filter;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumer 侧 Filter 示例：统一写入来源标识，供 Provider 审计/限流使用。
 *
 * <p>Filter 在 RPC 调用线程上执行，只做轻量操作，不做 IO。</p>
 */
@Activate(group = CommonConstants.CONSUMER, order = -1000)
public class ConsumerTraceFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ConsumerTraceFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        RpcContext.getClientAttachment().setAttachment("x-client-app", "dubbo-consumer");
        log.debug("dubbo consumer call {}#{}", invoker.getInterface().getName(),
                invocation.getMethodName());
        return invoker.invoke(invocation);
    }
}

package com.loveeleve.ecology.seata.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.seata.core.context.RootContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SeataFeignXidInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String xid = RootContext.getXID();
        if (StringUtils.hasText(xid)) {
            template.header(RootContext.KEY_XID, xid);
        }
    }
}

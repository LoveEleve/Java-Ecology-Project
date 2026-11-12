package com.loveeleve.ecology.seata.order.config;

import feign.RequestTemplate;
import org.apache.seata.core.context.RootContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class SeataFeignXidInterceptorTests {

    private final SeataFeignXidInterceptor interceptor = new SeataFeignXidInterceptor();

    @AfterEach
    void clearContext() {
        RootContext.unbind();
    }

    @Test
    void propagatesXidAsTxXidHeader() {
        RootContext.bind("xid-feign");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        Collection<String> values = template.headers().get(RootContext.KEY_XID);
        assertThat(values).containsExactly("xid-feign");
    }

    @Test
    void doesNotAddHeaderWithoutXid() {
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey(RootContext.KEY_XID);
    }
}

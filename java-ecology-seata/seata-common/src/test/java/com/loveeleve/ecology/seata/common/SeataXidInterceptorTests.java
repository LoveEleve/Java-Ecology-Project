package com.loveeleve.ecology.seata.common;

import org.apache.seata.core.context.RootContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SeataXidInterceptorTests {

    private final SeataXidInterceptor interceptor = new SeataXidInterceptor();

    @AfterEach
    void clearContext() {
        RootContext.unbind();
    }

    @Test
    void bindsXidFromIncomingRequestAndClearsAfterCompletion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RootContext.KEY_XID, "xid-1");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        assertThat(RootContext.getXID()).isEqualTo("xid-1");

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
        assertThat(RootContext.getXID()).isNull();
    }

    @Test
    void leavesContextUntouchedWithoutXidHeader() throws Exception {
        interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());

        assertThat(RootContext.getXID()).isNull();
    }

    @Test
    void doesNotUnbindAContextItDidNotBind() throws Exception {
        RootContext.bind("local-global-tx");
        MockHttpServletRequest request = new MockHttpServletRequest();

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);

        assertThat(RootContext.getXID()).isEqualTo("local-global-tx");
    }
}

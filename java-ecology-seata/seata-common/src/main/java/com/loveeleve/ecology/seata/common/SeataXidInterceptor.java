package com.loveeleve.ecology.seata.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.seata.core.context.RootContext;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

public class SeataXidInterceptor implements HandlerInterceptor {

    private static final String BOUND_ATTRIBUTE = SeataXidInterceptor.class.getName() + ".BOUND";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String xid = request.getHeader(RootContext.KEY_XID);
        if (StringUtils.hasText(xid)) {
            RootContext.bind(xid);
            request.setAttribute(BOUND_ATTRIBUTE, Boolean.TRUE);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (Boolean.TRUE.equals(request.getAttribute(BOUND_ATTRIBUTE))) {
            RootContext.unbind();
        }
    }
}

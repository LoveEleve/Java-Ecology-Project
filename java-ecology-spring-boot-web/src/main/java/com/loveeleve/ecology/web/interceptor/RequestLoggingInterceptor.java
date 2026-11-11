package com.loveeleve.ecology.web.interceptor;

import com.loveeleve.ecology.web.common.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String target = handler instanceof HandlerMethod handlerMethod
                ? handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName()
                : handler.getClass().getSimpleName();
        log.debug("request start id={} method={} uri={} handler={}",
                RequestIds.current(), request.getMethod(), request.getRequestURI(), target);
        request.setAttribute("requestStartNanos", System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object start = request.getAttribute("requestStartNanos");
        long elapsedMs = start instanceof Long startNanos
                ? (System.nanoTime() - startNanos) / 1_000_000
                : -1;
        log.debug("request end id={} method={} uri={} status={} elapsedMs={}",
                RequestIds.current(), request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
    }
}

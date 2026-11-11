package com.loveeleve.ecology.web.filter;

import com.loveeleve.ecology.web.common.RequestIds;
import com.loveeleve.ecology.web.config.DemoProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private final DemoProperties properties;

    public RequestIdFilter(DemoProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader(properties.getRequestIdHeader());
        if (!StringUtils.hasText(requestId)) {
            requestId = RequestIds.newId();
        }
        MDC.put(RequestIds.MDC_KEY, requestId);
        response.setHeader(properties.getRequestIdHeader(), requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestIds.MDC_KEY);
        }
    }
}

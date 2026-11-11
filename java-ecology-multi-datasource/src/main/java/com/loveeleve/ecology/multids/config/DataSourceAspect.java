package com.loveeleve.ecology.multids.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataSourceAspect {

    @Around("@annotation(com.loveeleve.ecology.multids.config.DataSource) || "
            + "@within(com.loveeleve.ecology.multids.config.DataSource)")
    public Object route(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        DataSource dataSource = AnnotatedElementUtils.findMergedAnnotation(
                signature.getMethod(), DataSource.class);
        if (dataSource == null) {
            dataSource = AnnotatedElementUtils.findMergedAnnotation(
                    joinPoint.getTarget().getClass(), DataSource.class);
        }
        DataSourceKey previous = DataSourceContextHolder.get();
        DataSourceContextHolder.set(dataSource.value());
        try {
            return joinPoint.proceed();
        } finally {
            if (previous == null) {
                DataSourceContextHolder.clear();
            } else {
                DataSourceContextHolder.set(previous);
            }
        }
    }
}

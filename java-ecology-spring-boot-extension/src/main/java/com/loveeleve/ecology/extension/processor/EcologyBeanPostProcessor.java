package com.loveeleve.ecology.extension.processor;

import com.loveeleve.ecology.extension.EcologyMarker;
import com.loveeleve.ecology.extension.imports.EcologyImportedMarker;
import com.loveeleve.ecology.extension.imports.EcologyRegistrarMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

public class EcologyBeanPostProcessor implements BeanPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(EcologyBeanPostProcessor.class);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof EcologyMarker
                || bean instanceof EcologyImportedMarker
                || bean instanceof EcologyRegistrarMarker
                || bean instanceof EcologyRegistryMarker) {
            log.debug("ecology extension bean initialized: {}", beanName);
        }
        return bean;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

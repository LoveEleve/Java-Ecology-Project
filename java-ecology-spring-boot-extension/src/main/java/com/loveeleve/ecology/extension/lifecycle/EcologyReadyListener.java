package com.loveeleve.ecology.extension.lifecycle;

import com.loveeleve.ecology.extension.properties.EcologyExtensionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

public class EcologyReadyListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(EcologyReadyListener.class);

    private final EcologyExtensionProperties properties;

    public EcologyReadyListener(EcologyExtensionProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("ecology extension ready: {}", properties.getMessage());
    }
}

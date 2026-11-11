package com.loveeleve.ecology.extension.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationImportEvent;
import org.springframework.boot.autoconfigure.AutoConfigurationImportListener;

public class EcologyAutoConfigurationImportListener implements AutoConfigurationImportListener {

    private static final Logger log = LoggerFactory.getLogger(EcologyAutoConfigurationImportListener.class);

    @Override
    public void onAutoConfigurationImportEvent(AutoConfigurationImportEvent event) {
        log.debug("ecology extension auto-configuration candidates: {}", event.getCandidateConfigurations().size());
    }
}

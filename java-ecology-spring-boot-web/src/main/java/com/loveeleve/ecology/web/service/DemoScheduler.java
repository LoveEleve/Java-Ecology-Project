package com.loveeleve.ecology.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DemoScheduler {

    private static final Logger log = LoggerFactory.getLogger(DemoScheduler.class);

    @Scheduled(fixedDelayString = "${app.demo.schedule-delay-ms:3600000}", initialDelayString = "60000")
    public void heartbeat() {
        log.debug("scheduled heartbeat");
    }
}

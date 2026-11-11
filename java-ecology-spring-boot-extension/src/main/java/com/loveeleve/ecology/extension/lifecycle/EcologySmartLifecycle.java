package com.loveeleve.ecology.extension.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

public class EcologySmartLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(EcologySmartLifecycle.class);

    private volatile boolean running;

    @Override
    public void start() {
        running = true;
        log.info("ecology extension SmartLifecycle started");
    }

    @Override
    public void stop() {
        running = false;
        log.info("ecology extension SmartLifecycle stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}

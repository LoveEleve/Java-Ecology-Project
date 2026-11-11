package com.loveeleve.ecology.extension.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.time.Duration;

public class EcologyRunListener implements SpringApplicationRunListener {

    private static final Logger log = LoggerFactory.getLogger(EcologyRunListener.class);

    public EcologyRunListener(SpringApplication application, String[] args) {
    }

    @Override
    public void starting(ConfigurableBootstrapContext bootstrapContext) {
        log.info("ecology extension run listener: starting");
    }

    @Override
    public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext,
                                    ConfigurableEnvironment environment) {
        log.debug("ecology extension run listener: environment prepared");
    }

    @Override
    public void started(ConfigurableApplicationContext context, Duration timeTaken) {
        log.info("ecology extension run listener: started in {} ms", timeTaken.toMillis());
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        log.error("ecology extension run listener: failed", exception);
    }
}

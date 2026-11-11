package com.loveeleve.ecology.extension.env;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

public class EcologyEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String DEFAULTS_LOCATION = "ecology-defaults.properties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ClassPathResource resource = new ClassPathResource(DEFAULTS_LOCATION);
        if (!resource.exists()) {
            return;
        }
        try {
            environment.getPropertySources()
                    .addLast(new ResourcePropertySource("ecology-defaults", resource));
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load " + DEFAULTS_LOCATION, ex);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

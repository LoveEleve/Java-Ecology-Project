package com.loveeleve.ecology.extension.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.Arrays;

public class EcologyAutoConfigurationImportFilter implements AutoConfigurationImportFilter, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        Arrays.fill(matches, true);
        boolean enabled = environment == null
                || environment.getProperty("ecology.extension.enabled", Boolean.class, true);
        if (!enabled) {
            for (int i = 0; i < autoConfigurationClasses.length; i++) {
                if (EcologyExtensionAutoConfiguration.class.getName().equals(autoConfigurationClasses[i])) {
                    matches[i] = false;
                }
            }
        }
        return matches;
    }
}

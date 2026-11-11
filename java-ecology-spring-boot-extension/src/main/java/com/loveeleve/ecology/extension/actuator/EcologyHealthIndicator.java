package com.loveeleve.ecology.extension.actuator;

import com.loveeleve.ecology.extension.properties.EcologyExtensionProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class EcologyHealthIndicator implements HealthIndicator {

    private final EcologyExtensionProperties properties;

    public EcologyHealthIndicator(EcologyExtensionProperties properties) {
        this.properties = properties;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("enabled", properties.isEnabled())
                .withDetail("message", properties.getMessage())
                .build();
    }
}

package com.loveeleve.ecology.observability.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("orderHealth")
public class OrderHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
                .withDetail("component", "order")
                .withDetail("latencyBudgetMs", 100)
                .build();
    }
}

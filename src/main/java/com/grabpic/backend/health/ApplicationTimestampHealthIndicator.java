package com.grabpic.backend.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ApplicationTimestampHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
                .withDetail("timestamp", Instant.now().toString())
                .build();
    }
}

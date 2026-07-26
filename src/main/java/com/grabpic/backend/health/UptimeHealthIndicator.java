package com.grabpic.backend.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class UptimeHealthIndicator implements HealthIndicator {

    private final long startTimeMillis = System.currentTimeMillis();

    @Override
    public Health health() {
        long uptimeMillis = System.currentTimeMillis() - startTimeMillis;
        return Health.up()
                .withDetail("milliseconds", uptimeMillis)
                .withDetail("formatted", formatUptime(uptimeMillis))
                .build();
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public static String formatUptime(long uptimeMillis) {
        long seconds = uptimeMillis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, secs);
        }
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        }
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        }
        return String.format("%ds", secs);
    }
}

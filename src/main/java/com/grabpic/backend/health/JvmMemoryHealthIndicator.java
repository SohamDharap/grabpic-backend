package com.grabpic.backend.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

@Component
public class JvmMemoryHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();

        long used = heap.getUsed();
        long max = heap.getMax();
        double usageRatio = max > 0 ? (double) used / max : 0.0;

        Health.Builder builder = usageRatio < 0.9 ? Health.up() : Health.down();
        return builder
                .withDetail("used", formatBytes(used))
                .withDetail("max", formatBytes(max))
                .withDetail("free", formatBytes(max - used))
                .withDetail("usagePercent", String.format("%.2f%%", usageRatio * 100))
                .build();
    }

    static String formatBytes(long bytes) {
        if (bytes < 0) {
            return "unknown";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

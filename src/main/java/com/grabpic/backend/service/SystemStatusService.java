package com.grabpic.backend.service;

import com.grabpic.backend.dto.response.SystemStatusResponse;
import com.grabpic.backend.health.JvmMemoryHealthIndicator;
import com.grabpic.backend.health.UptimeHealthIndicator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SystemStatusService {

    private final DataSource dataSource;
    private final UptimeHealthIndicator uptimeHealthIndicator;
    private final Optional<BuildProperties> buildProperties;

    @Value("${app.display.name:GrabPic Backend}")
    private String applicationName;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    public SystemStatusResponse getSystemStatus() {
        DatabaseSnapshot database = fetchDatabaseSnapshot();
        long uptimeMillis = System.currentTimeMillis() - uptimeHealthIndicator.getStartTimeMillis();

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        File root = new File(".");
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        boolean isHealthy = "UP".equals(database.status());

        return SystemStatusResponse.builder()
                .application(applicationName)
                .status(isHealthy ? "UP" : "DOWN")
                .version(resolveVersion())
                .javaVersion(System.getProperty("java.version"))
                .springBootVersion(SpringBootVersion.getVersion())
                .uptime(UptimeHealthIndicator.formatUptime(uptimeMillis))
                .database(SystemStatusResponse.DatabaseInfo.builder()
                        .status(database.status())
                        .name(database.name())
                        .version(database.version())
                        .host(database.host())
                        .build())
                .system(SystemStatusResponse.SystemInfo.builder()
                        .cpuCores(osBean.getAvailableProcessors())
                        .freeMemory(JvmMemoryHealthIndicator.formatBytes(maxMemory - usedMemory))
                        .totalMemory(JvmMemoryHealthIndicator.formatBytes(totalMemory))
                        .maxMemory(JvmMemoryHealthIndicator.formatBytes(maxMemory))
                        .build())
                .disk(SystemStatusResponse.DiskInfo.builder()
                        .totalSpace(JvmMemoryHealthIndicator.formatBytes(root.getTotalSpace()))
                        .freeSpace(JvmMemoryHealthIndicator.formatBytes(root.getFreeSpace()))
                        .build())
                .time(Instant.now().toString())
                .build();
    }

    private String resolveVersion() {
        return buildProperties
                .map(BuildProperties::getVersion)
                .orElse("0.0.1-SNAPSHOT");
    }

    private DatabaseSnapshot fetchDatabaseSnapshot() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT version()")) {

            String version = resultSet.next() ? resultSet.getString(1) : "unknown";
            return new DatabaseSnapshot("UP", parseDatabaseName(), version, parseDatabaseHost());
        } catch (Exception ex) {
            return new DatabaseSnapshot("DOWN", parseDatabaseName(), "unavailable", parseDatabaseHost());
        }
    }

    private String parseDatabaseName() {
        try {
            URI uri = URI.create(datasourceUrl.replace("jdbc:", ""));
            String path = uri.getPath();
            if (path != null && path.startsWith("/")) {
                return path.substring(1);
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "unknown";
    }

    private String parseDatabaseHost() {
        try {
            URI uri = URI.create(datasourceUrl.replace("jdbc:", ""));
            return uri.getHost() != null ? uri.getHost() : "unknown";
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private record DatabaseSnapshot(String status, String name, String version, String host) {}
}

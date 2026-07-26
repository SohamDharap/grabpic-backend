package com.grabpic.backend.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
@RequiredArgsConstructor
public class DatabaseVersionHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT version()")) {

            if (resultSet.next()) {
                String version = resultSet.getString(1);
                return Health.up().withDetail("version", version).build();
            }
            return Health.down().withDetail("version", "unknown").build();
        } catch (Exception ex) {
            return Health.down(ex).withDetail("version", "unavailable").build();
        }
    }
}

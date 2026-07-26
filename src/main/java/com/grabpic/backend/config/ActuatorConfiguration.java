package com.grabpic.backend.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ActuatorConfiguration implements InfoContributor {

    private final Optional<BuildProperties> buildProperties;

    public ActuatorConfiguration(Optional<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("application", buildProperties
                .map(BuildProperties::getName)
                .orElse("backend"));

        builder.withDetail("version", buildProperties
                .map(BuildProperties::getVersion)
                .orElse("0.0.1-SNAPSHOT"));

        builder.withDetail("buildTime", buildProperties
                .map(BuildProperties::getTime)
                .map(Object::toString)
                .orElse("unknown"));

        builder.withDetail("javaVersion", System.getProperty("java.version"));
    }
}

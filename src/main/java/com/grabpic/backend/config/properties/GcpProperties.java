package com.grabpic.backend.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "gcp")
public class GcpProperties {
    private String projectId;
}

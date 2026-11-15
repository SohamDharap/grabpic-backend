package com.grabpic.backend.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "python.face.service")
public class FaceServiceProperties {

    @NotBlank(message = "Python face service base URL must be configured")
    private String baseUrl = "http://face-service:8001";

    private String embedPath = "/embed";

    @Min(1000)
    private int connectTimeoutMs = 5000;

    @Min(1000)
    private int readTimeoutMs = 120000;

    public String getEmbedUrl() {
        String base = baseUrl != null ? baseUrl.trim() : "http://face-service:8001";
        String path = embedPath != null ? embedPath.trim() : "/embed";
        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        } else if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        }
        return base + path;
    }
}

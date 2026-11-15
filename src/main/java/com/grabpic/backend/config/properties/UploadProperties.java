package com.grabpic.backend.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    @Min(1)
    private int maxFiles = 50;

    @Min(1)
    private long maxZipSizeMb = 500;

    @Min(1)
    private long maxImageSizeMb = 25;

    private List<String> allowedContentTypes = List.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private String zipTempDir;
}

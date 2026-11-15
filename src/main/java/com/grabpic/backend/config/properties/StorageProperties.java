package com.grabpic.backend.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    @NotBlank
    private String provider = "gcs";

    @Valid
    private LocalStorageProperties local = new LocalStorageProperties();

    private boolean deleteOnUploadFailure = true;

    private boolean generateSignedUrlsForThumbnails = true;

    private boolean generateOriginalSignedUrlOnDemand = true;

    private boolean useSignedUrlsForUploadResponses = true;

    @Data
    public static class LocalStorageProperties {
        private String baseDir = "/storage";
    }
}

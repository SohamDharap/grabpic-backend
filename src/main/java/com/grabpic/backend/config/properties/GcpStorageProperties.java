package com.grabpic.backend.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "gcp.storage")
public class GcpStorageProperties {

    @NotBlank(message = "GCS bucket name must be configured")
    private String bucket = "grabpic-media";

    private String location = "asia-south1";

    private String authenticationMode = "adc";

    private String objectPrefix = "events";

    private String originalDirectory = "original";

    private String thumbnailDirectory = "thumbnail";

    private String signedUrlVersion = "V4";

    @Valid
    private SignedUrlProperties signedUrl = new SignedUrlProperties();

    @Data
    public static class SignedUrlProperties {
        private boolean enabled = true;
        private int durationMinutes = 15;
        private int maxDurationMinutes = 60;
        @NotBlank(message = "Signing service account must be configured for IAM-based signed URL generation")
        private String signingServiceAccount = "529807292953-compute@developer.gserviceaccount.com";
    }
}

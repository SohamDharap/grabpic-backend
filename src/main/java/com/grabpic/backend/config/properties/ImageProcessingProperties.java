package com.grabpic.backend.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.image")
public class ImageProcessingProperties {

    @Valid
    private ThumbnailProperties thumbnail = new ThumbnailProperties();

    @Data
    public static class ThumbnailProperties {
        private boolean enabled = true;

        @Min(50)
        @Max(2000)
        private int width = 400;

        @Min(50)
        @Max(2000)
        private int height = 400;

        @Min(0)
        @Max(1)
        private double quality = 0.80;

        private String format = "jpeg";

        private boolean preserveAspectRatio = true;
    }
}

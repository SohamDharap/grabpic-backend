package com.grabpic.backend.config;

import com.grabpic.backend.config.properties.*;
import com.grabpic.backend.service.StorageService;
import com.grabpic.backend.service.impl.GcsStorageService;
import com.grabpic.backend.service.impl.LocalStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        GcpProperties.class,
        GcpStorageProperties.class,
        ImageProcessingProperties.class,
        UploadProperties.class,
        FaceServiceProperties.class,
        StorageProperties.class
})
@Slf4j
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "app.storage.provider", havingValue = "gcs", matchIfMissing = true)
    public StorageService gcsStorageService(GcpStorageProperties storageProperties, GcpProperties gcpProperties) {
        log.info("Configuring Google Cloud Storage (GCS) provider for bucket '{}'", storageProperties.getBucket());
        return new GcsStorageService(storageProperties, gcpProperties);
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.provider", havingValue = "local")
    public StorageService localStorageService(StorageProperties storageProperties) {
        log.info("Configuring Local Storage provider with base-dir '{}'", storageProperties.getLocal().getBaseDir());
        return new LocalStorageService(storageProperties);
    }
}

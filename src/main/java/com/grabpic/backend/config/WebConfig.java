package com.grabpic.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadAbsolutePath = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        String uploadsFolder = Paths.get("uploads").toAbsolutePath().normalize().toString();
        String storageFolder = Paths.get("storage").toAbsolutePath().normalize().toString();
        
        registry.addResourceHandler("/uploads/**", "/storage/**")
                .addResourceLocations(
                        "file:" + uploadAbsolutePath + "/",
                        "file:" + uploadsFolder + "/",
                        "file:" + storageFolder + "/"
                );
    }
}

package com.grabpic.backend.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        // maxFileSize = 50MB (52428800 bytes), maxRequestSize = 500MB (524288000 bytes)
        return new MultipartConfigElement(null, 50L * 1024 * 1024, 500L * 1024 * 1024, 0);
    }
}



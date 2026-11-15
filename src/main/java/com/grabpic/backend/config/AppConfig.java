package com.grabpic.backend.config;

import com.grabpic.backend.config.properties.FaceServiceProperties;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(FaceServiceProperties faceServiceProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofMillis(faceServiceProperties.getConnectTimeoutMs()).toMillis());
        factory.setReadTimeout((int) Duration.ofMillis(faceServiceProperties.getReadTimeoutMs()).toMillis());
        return new RestTemplate(factory);
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        // maxFileSize = 50MB (52428800 bytes), maxRequestSize = 500MB (524288000 bytes)
        return new MultipartConfigElement(null, 50L * 1024 * 1024, 500L * 1024 * 1024, 0);
    }
}

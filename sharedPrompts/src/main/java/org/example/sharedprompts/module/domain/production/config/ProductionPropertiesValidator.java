package org.example.sharedprompts.module.domain.production.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.config.properties.ProductionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(ProductionProperties.class)
public class ProductionPropertiesValidator {
    
    private final ProductionProperties properties;
    
    @PostConstruct
    public void validate() {
        if (properties.getStorage().getS3().getBucket() == null || 
            properties.getStorage().getS3().getBucket().isBlank()) {
            throw new IllegalStateException("production.storage.s3.bucket is required");
        }
    }
}


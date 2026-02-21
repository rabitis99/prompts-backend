package org.example.sharedprompts.module.domain.production.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "production")
public class ProductionProperties {
    
    private Storage storage = new Storage();
    
    @Getter
    @Setter
    public static class Storage {
        private S3 s3 = new S3();
    }
    
    @Getter
    @Setter
    public static class S3 {
        private String bucket;
        private String prefix;
    }
}


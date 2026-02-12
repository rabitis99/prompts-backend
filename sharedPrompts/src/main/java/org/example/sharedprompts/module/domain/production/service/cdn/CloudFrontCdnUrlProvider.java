package org.example.sharedprompts.module.domain.production.service.cdn;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cdn.enabled", havingValue = "true")
@Slf4j
public class CloudFrontCdnUrlProvider implements CdnUrlProvider {

    private final String domain;

    public CloudFrontCdnUrlProvider(@Value("${cdn.domain}") String domain) {
        this.domain = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
        log.info("CloudFront CDN enabled - domain: {}", this.domain);
    }

    @Override
    public String generateUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        String key = objectKey.startsWith("/") ? objectKey : "/" + objectKey;
        return domain + key;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

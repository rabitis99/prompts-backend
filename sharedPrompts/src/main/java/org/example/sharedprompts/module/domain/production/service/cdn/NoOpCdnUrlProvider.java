package org.example.sharedprompts.module.domain.production.service.cdn;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cdn.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpCdnUrlProvider implements CdnUrlProvider {

    @Override
    public String generateUrl(String objectKey) {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}

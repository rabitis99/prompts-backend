package org.example.sharedprompts.module.domain.production.service.cdn;

public interface CdnUrlProvider {

    String generateUrl(String objectKey);

    boolean isEnabled();
}

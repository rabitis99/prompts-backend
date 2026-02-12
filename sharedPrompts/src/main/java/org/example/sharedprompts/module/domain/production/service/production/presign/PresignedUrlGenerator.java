package org.example.sharedprompts.module.domain.production.service.production.presign;

import java.time.Duration;

public interface PresignedUrlGenerator {

    String generate(String bucket, String key, Duration ttl);
}

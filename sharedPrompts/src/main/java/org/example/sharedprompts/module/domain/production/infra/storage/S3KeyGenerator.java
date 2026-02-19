package org.example.sharedprompts.module.domain.production.infra.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * S3 키 생성 유틸리티
 * 테넌트 인식 S3 키 생성을 담당합니다.
 */
@Component
public class S3KeyGenerator {

    @Value("${production.storage.s3.prefix:production}")
    private String prefix;

    /**
     * 테넌트가 없는 경우의 S3 키를 생성합니다.
     */
    public String generateKey(Long userId, String jobId, String fileName) {
        return String.format("%s/%d/%s/%s", prefix, userId, jobId, fileName);
    }

    /**
     * 테넌트 인식 S3 키를 생성합니다.
     * tenantId가 null이거나 비어있으면 테넌트 없는 키를 생성합니다.
     */
    public String generateKey(String tenantId, Long userId, String jobId, String fileName) {
        if (tenantId == null || tenantId.isBlank()) {
            return generateKey(userId, jobId, fileName);
        }
        return String.format("%s/%s/%d/%s/%s", prefix, tenantId, userId, jobId, fileName);
    }
}


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
        return String.format("%s/%d/%s/%s", prefix, userId, jobId, sanitizeFileName(fileName));
    }

    /**
     * 테넌트 인식 S3 키를 생성합니다.
     * tenantId가 null이거나 비어있으면 테넌트 없는 키를 생성합니다.
     */
    public String generateKey(String tenantId, Long userId, String jobId, String fileName) {
        if (tenantId == null || tenantId.isBlank()) {
            return generateKey(userId, jobId, fileName);
        }
        return String.format("%s/%s/%d/%s/%s", prefix, tenantId, userId, jobId, sanitizeFileName(fileName));
    }

    /**
     * 파일명을 정리하여 경로 탐색 공격을 방지합니다.
     * 경로 구분자(/, \)와 상대 경로 패턴(..)을 제거하고 파일명만 추출합니다.
     *
     * @param fileName 원본 파일명
     * @return 정리된 파일명 (null이거나 비어있으면 "output" 반환)
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "output";
        }

        // Windows 경로 구분자를 Unix 스타일로 변환
        String sanitized = fileName.replace("\\", "/");

        // 경로가 포함된 경우 마지막 파일명만 추출
        int lastSlash = sanitized.lastIndexOf('/');
        if (lastSlash >= 0) {
            sanitized = sanitized.substring(lastSlash + 1);
        }

        // 상대 경로 패턴 제거
        sanitized = sanitized.replace("..", "");

        // 빈 문자열이면 기본값 반환
        if (sanitized.isBlank()) {
            return "output";
        }

        return sanitized;
    }
}


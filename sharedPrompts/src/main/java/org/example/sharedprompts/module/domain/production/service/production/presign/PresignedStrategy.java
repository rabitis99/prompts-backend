package org.example.sharedprompts.module.domain.production.service.production.presign;

import java.time.Duration;

/**
 * 파일 타입 기반 Presigned URL 생성 전략 인터페이스
 */
public interface PresignedStrategy {
    
    /**
     * 이 전략이 지원하는 Content-Type 패턴 확인
     */
    boolean supports(String contentType);
    
    /**
     * Presigned URL 생성
     * 
     * @param bucket S3 버킷 이름
     * @param key S3 객체 키
     * @param contentType 파일의 Content-Type
     * @param ttl URL 유효 기간
     * @return Presigned URL
     */
    String generatePresignedUrl(String bucket, String key, String contentType, Duration ttl);
}


package org.example.sharedprompts.module.domain.production.service.production.presign;

import java.time.Duration;

/**
 * Presigned URL 생성기 인터페이스
 * S3와 같은 스토리지에서 임시 접근 URL을 생성합니다.
 */
public interface PresignedUrlGenerator {

    /**
     * Presigned URL 생성
     * 
     * @param bucket 스토리지 버킷 이름
     * @param key 스토리지 객체 키
     * @param ttl URL 유효 기간
     * @return Presigned URL 문자열
     * @throws IllegalArgumentException bucket, key가 null이거나 blank이거나, ttl이 유효하지 않은 경우
     */
    String generate(String bucket, String key, Duration ttl);
}

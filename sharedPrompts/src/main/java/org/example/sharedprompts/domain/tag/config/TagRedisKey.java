package org.example.sharedprompts.domain.tag.config;

import java.time.Duration;

/**
 * 태그 관련 Redis Key 네이밍 표준화
 * - prefix 표준화: sharedprompts:tag:*
 * - TTL 정책 관리
 */
public final class TagRedisKey {

    private static final String PREFIX = "sharedprompts:tag";
    
    /**
     * 태그 카운트 키
     * 형식: sharedprompts:tag:count:<tagName>
     */
    public static String countKey(String tagName) {
        return String.format("%s:count:%s", PREFIX, tagName);
    }
    
    /**
     * 태그 카운트 TTL (30일)
     */
    public static Duration countTtl() {
        return Duration.ofDays(30);
    }
    
    /**
     * 지연 이벤트 큐 키
     * 형식: sharedprompts:tag:retry:queue
     */
    public static String retryQueueKey() {
        return PREFIX + ":retry:queue";
    }
    
    /**
     * Dead Letter Queue 키 prefix
     * 형식: sharedprompts:tag:dlq:<timestamp>
     */
    public static String dlqKeyPrefix() {
        return PREFIX + ":dlq:";
    }
    
    /**
     * DLQ TTL (7일)
     */
    public static Duration dlqTtl() {
        return Duration.ofDays(7);
    }
    
    private TagRedisKey() {
        throw new UnsupportedOperationException("Utility class");
    }
}


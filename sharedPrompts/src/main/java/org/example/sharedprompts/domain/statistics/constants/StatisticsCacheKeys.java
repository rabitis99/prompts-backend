package org.example.sharedprompts.domain.statistics.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 통계 캐시 키 상수 클래스
 * 
 * 통계 관련 캐시 키를 중앙화하여 관리합니다.
 * 캐시 키 변경 시 이 클래스만 수정하면 되므로 유지보수성이 향상됩니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StatisticsCacheKeys {

    /**
     * 통계 캐시 이름
     */
    public static final String CACHE_NAME = "statistics";

    /**
     * 전체 통계 캐시 키
     */
    public static final String ALL = "all";

    /**
     * 사용자 통계 캐시 키
     */
    public static final String USER = "user";

    /**
     * 프롬프트 통계 캐시 키
     */
    public static final String PROMPT = "prompt";

    /**
     * AI 호출 통계 캐시 키
     */
    public static final String AI_CALL = "ai-call";

    /**
     * 개인 통계 캐시 키 생성
     * 
     * @param userId 사용자 ID
     * @return 개인 통계 캐시 키 (예: "my:123")
     */
    public static String userStatistics(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        return "my:" + userId;
    }
}


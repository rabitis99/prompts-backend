package org.example.sharedprompts.domain.rate.ratelimitlog.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Rate Limit 로그 관련 상수 정의
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RateLimitLogConstants {

    /**
     * 통계 조회 관련 상수
     */
    public static final class Statistics {
        /**
         * 기본 통계 조회 기간 (일)
         */
        public static final int DEFAULT_STATISTICS_DAYS = 7;

        /**
         * 최다 위반자 조회 기본 개수
         */
        public static final int DEFAULT_TOP_VIOLATORS_LIMIT = 10;

        private Statistics() {
        }
    }

    /**
     * 로그 정리 관련 상수
     */
    public static final class Cleanup {
        /**
         * 기본 로그 보관 기간 (일)
         */
        public static final int DEFAULT_RETENTION_DAYS = 90;

        /**
         * 기본 스케줄 (매일 새벽 3시)
         */
        public static final String DEFAULT_SCHEDULE = "0 0 3 * * ?";

        private Cleanup() {
        }
    }
}







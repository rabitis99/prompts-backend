package org.example.sharedprompts.auth.rate;

/**
 * Rate Limiting 알고리즘 인터페이스
 * 
 * 다양한 Rate Limiting 알고리즘을 구현할 수 있도록 인터페이스로 정의합니다.
 * - FixedWindowRateLimiter: 고정 윈도우 방식
 * - SlidingWindowRateLimiter: 슬라이딩 윈도우 방식
 * - TokenBucketRateLimiter: 토큰 버킷 방식
 */
public interface RateLimiter {
    
    /**
     * 주어진 key에 대해 1토큰을 소비하면서 요청 허용 여부를 판단합니다.
     *
     * @param key          rate limit key (예: rate:login:ip:1.2.3.4)
     * @param capacity     윈도우 내 허용 횟수
     * @param windowSeconds 윈도우(초)
     * @return RateLimitResult (허용 여부, 현재 카운트, Retry-After 초)
     */
    RateLimitResult consume(String key, long capacity, long windowSeconds);
    
    /**
     * Rate Limit 결과
     */
    record RateLimitResult(
            boolean allowed,
            long currentCount,
            long retryAfterSeconds
    ) {
        /**
         * Rate Limit이 초과되었는지 확인합니다.
         * 
         * @return 초과 여부
         */
        public boolean isExceeded() {
            return !allowed;
        }

        /**
         * Rate Limit 체크가 실패했는지 확인합니다.
         * (null 체크를 위한 편의 메서드)
         * 
         * @param result RateLimitResult (null 가능)
         * @return 실패 여부 (null이면 true)
         */
        public static boolean isFailed(RateLimitResult result) {
            return result == null;
        }

        /**
         * Retry-After 값을 반환합니다 (최소값 보장).
         * 
         * @param minRetryAfterSeconds 최소 Retry-After 값 (초)
         * @return Retry-After 값 (초)
         */
        public long getRetryAfter(long minRetryAfterSeconds) {
            return Math.max(retryAfterSeconds, minRetryAfterSeconds);
        }
    }
}


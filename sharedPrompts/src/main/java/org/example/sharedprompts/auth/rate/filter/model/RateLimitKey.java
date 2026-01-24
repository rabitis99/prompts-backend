package org.example.sharedprompts.auth.rate.filter.model;

import org.example.sharedprompts.domain.rate.ratelimitlog.enums.RateLimitType;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

/**
 * Rate Limit 키 Value Object
 * Rate Limit 키의 값과 타입을 함께 관리합니다.
 * 타입 정보를 포함하여 로그/메트릭/DB 저장 시 의미를 명확히 합니다.
 * 
 * @param value 키 값 (예: "rate:login:ip:1.2.3.4")
 * @param type Rate Limit 타입 (IP 또는 USER)
 */
public record RateLimitKey(
        String value,
        RateLimitType type
) {
    /**
     * Rate Limit 키를 생성합니다.
     * 
     * @param value 키 값
     * @param type Rate Limit 타입
     * @return RateLimitKey
     * @throws ApiException value가 null이거나 비어있는 경우, 또는 type이 null인 경우
     */
    public RateLimitKey {
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "value");
        }
        if (type == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "type");
        }
    }

    public static RateLimitKey of(String value, RateLimitType type) {
        return new RateLimitKey(value, type);
    }

    /**
     * IP 기반 Rate Limit 키를 생성합니다.
     */
    public static RateLimitKey forIp(String value) {
        return of(value, RateLimitType.IP);
    }

    /**
     * 사용자 기반 Rate Limit 키를 생성합니다.
     */
    public static RateLimitKey forUser(String value) {
        return of(value, RateLimitType.USER);
    }

    /**
     * 키가 IP 기반인지 확인합니다.
     */
    public boolean isIpBased() {
        return type == RateLimitType.IP;
    }

    /**
     * 키가 사용자 기반인지 확인합니다.
     */
    public boolean isUserBased() {
        return type == RateLimitType.USER;
    }

    /**
     * 로그 가독성을 위한 문자열 표현
     * 
     * @return "TYPE:value" 형식의 문자열 (예: "IP:rate:login:ip:1.2.3.4")
     */
    @Override
    public String toString() {
        return type + ":" + value;
    }
}


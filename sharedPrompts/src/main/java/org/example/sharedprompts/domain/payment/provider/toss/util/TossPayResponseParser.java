package org.example.sharedprompts.domain.payment.provider.toss.util;

import lombok.extern.slf4j.Slf4j;

import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * TossPay API 응답 파서
 *
 * <p>책임:
 * <ul>
 *   <li>TossPay API 응답 구조 해석</li>
 *   <li>필수 필드 검증 및 타입/포맷 보장</li>
 * </ul>
 *
 * <p>주의:
 * Toss API의 시간 필드는 정산 및 감사에 사용되므로
 * 파싱 실패 시 fallback 값을 사용하지 않고 즉시 실패한다.
 */
@Slf4j
@Component
public class TossPayResponseParser {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * 승인 시간 파싱 (필수)
     */
    public LocalDateTime parseApprovedAt(Map<String, Object> body) {
        return parseRequiredDateTime(body, "approvedAt");
    }

    /**
     * 취소 시간 파싱 (필수)
     */
    public LocalDateTime parseCanceledAt(Map<String, Object> body) {
        return parseRequiredDateTime(body, "canceledAt");
    }

    /**
     * 취소 금액 파싱
     *
     * <p>cancels 배열의 마지막 요소를 가장 최근 취소로 간주한다.
     * 금액은 중요하지만 Toss 응답 구조 변경 가능성을 고려해
     * 파싱 실패 시 요청 금액으로 fallback 한다.
     */
    public long parseCanceledAmount(Map<String, Object> body, long requestedAmount) {
        try {
            Object cancelsObj = body.get("cancels");
            if (!(cancelsObj instanceof List<?> cancels) || cancels.isEmpty()) {
                return requestedAmount;
            }

            Object lastCancel = cancels.get(cancels.size() - 1);
            if (!(lastCancel instanceof Map<?, ?> cancelMap)) {
                return requestedAmount;
            }

            Object cancelAmountObj = cancelMap.get("cancelAmount");
            if (!(cancelAmountObj instanceof Number number)) {
                return requestedAmount;
            }

            return number.longValue();
        } catch (Exception e) {
            log.warn("취소 금액 파싱 실패, 요청 금액 사용", e);
            return requestedAmount;
        }
    }

    /**
     * 필수 DateTime 필드 파싱 공통 로직
     */
    private LocalDateTime parseRequiredDateTime(Map<String, Object> body, String fieldName) {
        Object value = body.get(fieldName);

        if (value == null) {
            throw new ApiException(
                    ErrorCode.PAYMENT_PROVIDER_RESPONSE_INVALID,
                    "Toss response missing required field: " + fieldName
            );
        }

        try {
            return OffsetDateTime
                    .parse(value.toString(), ISO_FORMATTER)
                    .toLocalDateTime();
        } catch (Exception e) {
            throw new ApiException(
                    ErrorCode.PAYMENT_PROVIDER_RESPONSE_INVALID,
                    "Failed to parse Toss datetime field '" + fieldName + "': " + value,
                    e
            );
        }
    }
}

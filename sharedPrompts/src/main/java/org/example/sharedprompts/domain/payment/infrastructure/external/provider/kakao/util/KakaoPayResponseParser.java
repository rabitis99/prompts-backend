package org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * KakaoPay 응답 파싱 유틸리티
 * 
 * <p>단일 책임: KakaoPay API 응답 파싱만 담당
 */
@Slf4j
@Component
public class KakaoPayResponseParser {

    /**
     * 승인 시간 파싱
     */
    public LocalDateTime parseApprovedAt(Map<String, Object> body) {
        return parseDateTime(body, "approved_at", "승인");
    }

    /**
     * 취소 시간 파싱
     */
    public LocalDateTime parseCanceledAt(Map<String, Object> body) {
        return parseDateTime(body, "canceled_at", "취소");
    }

    /**
     * 공통 날짜/시간 파싱 헬퍼 메서드
     */
    private LocalDateTime parseDateTime(Map<String, Object> body, String fieldName, String fieldDescription) {
        try {
            Object dateTimeObj = body.get(fieldName);
            if (dateTimeObj != null) {
                String dateTimeStr = dateTimeObj.toString();
                // KakaoPay는 Unix timestamp (초 단위) 또는 ISO 8601 형식 사용
                try {
                    long timestamp = Long.parseLong(dateTimeStr);
                    return LocalDateTime.ofEpochSecond(timestamp, 0, java.time.ZoneOffset.of("+09:00"));
                } catch (NumberFormatException e) {
                    // ISO 8601 형식 파싱 시도
                    try {
                        // 타임존 정보가 있는 경우
                        OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
                        return offsetDateTime.toLocalDateTime();
                    } catch (Exception e2) {
                        // 타임존 정보가 없는 경우 (예: 2024-04-17T18:50:07)
                        // KakaoPay는 KST(+09:00)로 해석해야 함
                        try {
                            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of("Asia/Seoul"));
                            return zonedDateTime.toLocalDateTime();
                        } catch (Exception e3) {
                            // 모든 파싱 실패 시 예외를 다시 던짐
                            throw new IllegalArgumentException("날짜/시간 형식을 파싱할 수 없습니다: " + dateTimeStr, e3);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("{} 시간 파싱 실패: {}", fieldDescription, e.getMessage());
        }
        return LocalDateTime.now();
    }
}


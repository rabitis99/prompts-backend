package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
     * 
     * @param body 응답 본문
     * @return LocalDateTime (파싱 실패 시 현재 시간)
     */
    public LocalDateTime parseApprovedAt(Map<String, Object> body) {
        try {
            Object approvedAtObj = body.get("approved_at");
            if (approvedAtObj != null) {
                String approvedAtStr = approvedAtObj.toString();
                // KakaoPay는 Unix timestamp (초 단위) 또는 ISO 8601 형식 사용
                try {
                    long timestamp = Long.parseLong(approvedAtStr);
                    return LocalDateTime.ofEpochSecond(timestamp, 0, java.time.ZoneOffset.of("+09:00"));
                } catch (NumberFormatException e) {
                    return OffsetDateTime.parse(approvedAtStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
                }
            }
        } catch (Exception e) {
            log.warn("승인 시간 파싱 실패: {}", e.getMessage());
        }
        return LocalDateTime.now();
    }

    /**
     * 취소 시간 파싱
     * 
     * @param body 응답 본문
     * @return LocalDateTime (파싱 실패 시 현재 시간)
     */
    public LocalDateTime parseCanceledAt(Map<String, Object> body) {
        try {
            Object canceledAtObj = body.get("canceled_at");
            if (canceledAtObj != null) {
                String canceledAtStr = canceledAtObj.toString();
                try {
                    long timestamp = Long.parseLong(canceledAtStr);
                    return LocalDateTime.ofEpochSecond(timestamp, 0, java.time.ZoneOffset.of("+09:00"));
                } catch (NumberFormatException e) {
                    return OffsetDateTime.parse(canceledAtStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
                }
            }
        } catch (Exception e) {
            log.warn("취소 시간 파싱 실패: {}", e.getMessage());
        }
        return LocalDateTime.now();
    }
}


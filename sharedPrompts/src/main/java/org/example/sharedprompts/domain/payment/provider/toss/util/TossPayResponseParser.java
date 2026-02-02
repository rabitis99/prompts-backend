package org.example.sharedprompts.domain.payment.provider.toss.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * TossPay API 응답 파서
 *
 * <p>단일 책임: TossPay API 응답에서 필요한 정보 추출
 */
@Slf4j
@Component
public class TossPayResponseParser {

    /**
     * 승인 시간 파싱
     */
    public LocalDateTime parseApprovedAt(Map<String, Object> body) {
        try {
            Object approvedAtObj = body.get("approvedAt");
            if (approvedAtObj != null) {
                String approvedAtStr = approvedAtObj.toString();
                return OffsetDateTime.parse(approvedAtStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
            }
        } catch (Exception e) {
            log.warn("승인 시간 파싱 실패: {}", e.getMessage());
        }
        return LocalDateTime.now();
    }

    /**
     * 취소 시간 파싱
     */
    public LocalDateTime parseCanceledAt(Map<String, Object> body) {
        try {
            Object canceledAtObj = body.get("canceledAt");
            if (canceledAtObj != null) {
                String canceledAtStr = canceledAtObj.toString();
                return OffsetDateTime.parse(canceledAtStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
            }
        } catch (Exception e) {
            log.warn("취소 시간 파싱 실패: {}", e.getMessage());
        }
        return LocalDateTime.now();
    }
}

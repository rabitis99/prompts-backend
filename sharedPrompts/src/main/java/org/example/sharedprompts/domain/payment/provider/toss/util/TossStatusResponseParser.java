package org.example.sharedprompts.domain.payment.provider.toss.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.provider.toss.dto.TossStatusResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * TossPay 결제 상태 조회 응답 파서
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossStatusResponseParser {

    private final TossPayResponseParser responseParser;
    private final TossPayJsonConverter jsonConverter;

    public TossStatusResponse parse(Map<String, Object> body) {
        String status = getRequiredString(body, "status");
        BigDecimal totalAmount = parseTotalAmount(body);
        String currency = getRequiredString(body, "currency");
        String orderId = getRequiredString(body, "orderId");

        log.debug("TossPay 결제 상태 조회 성공: orderId={}, status={}", orderId, status);
        return new TossStatusResponse(
                status,
                totalAmount,
                currency,
                orderId,
                responseParser.parseApprovedAt(body),
                jsonConverter.convertToJson(body)
        );
    }

    private String getRequiredString(Map<String, Object> body, String fieldName) {
        Object value = body.get(fieldName);
        if (value == null || value.toString().isEmpty()) {
            throw new RuntimeException("TossPay status 응답에 " + fieldName + "가 없습니다");
        }
        return value.toString();
    }

    private BigDecimal parseTotalAmount(Map<String, Object> body) {
        Object totalAmountObj = body.get("totalAmount");
        if (totalAmountObj == null) {
            throw new RuntimeException("TossPay status 응답에 totalAmount가 없습니다");
        }
        try {
            return new BigDecimal(totalAmountObj.toString());
        } catch (NumberFormatException e) {
            log.error("TossPay status 응답의 totalAmount 형식이 올바르지 않습니다: {}", totalAmountObj);
            throw new RuntimeException("TossPay status 응답의 totalAmount 형식이 올바르지 않습니다: " + totalAmountObj, e);
        }
    }
}


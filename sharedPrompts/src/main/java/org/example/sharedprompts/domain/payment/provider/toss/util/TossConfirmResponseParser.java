package org.example.sharedprompts.domain.payment.provider.toss.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.provider.toss.dto.TossConfirmResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * TossPay 결제 승인 응답 파서
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossConfirmResponseParser {

    private final TossPayResponseParser responseParser;
    private final TossPayJsonConverter jsonConverter;

    public TossConfirmResponse parse(String paymentKey, Map<String, Object> body) {
        String status = getRequiredString(body, "status");
        BigDecimal totalAmount = parseTotalAmount(body);
        String currency = getRequiredString(body, "currency");
        String orderId = getRequiredString(body, "orderId");

        log.info("TossPay 결제 승인 성공: paymentKey={}, orderId={}, status={}", paymentKey, orderId, status);
        return new TossConfirmResponse(
                paymentKey,
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
            throw new RuntimeException("TossPay confirm 응답에 " + fieldName + "가 없습니다");
        }
        return value.toString();
    }

    private BigDecimal parseTotalAmount(Map<String, Object> body) {
        Object totalAmountObj = body.get("totalAmount");
        if (totalAmountObj == null) {
            throw new RuntimeException("TossPay confirm 응답에 totalAmount가 없습니다");
        }
        try {
            return new BigDecimal(totalAmountObj.toString());
        } catch (NumberFormatException e) {
            log.error("TossPay confirm 응답의 totalAmount 형식이 올바르지 않습니다: {}", totalAmountObj);
            throw new RuntimeException("TossPay confirm 응답의 totalAmount 형식이 올바르지 않습니다: " + totalAmountObj, e);
        }
    }
}


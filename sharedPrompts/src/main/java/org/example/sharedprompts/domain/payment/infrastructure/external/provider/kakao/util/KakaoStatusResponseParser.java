package org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.dto.KakaoStatusResponse;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * KakaoPay 결제 상태 조회 응답 파서
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoStatusResponseParser {

    private final KakaoPayJsonConverter jsonConverter;

    public KakaoStatusResponse parse(Map<String, Object> body) {
        if (body == null) {
            throw new ApiException(
                    ErrorCode.PAYMENT_PROVIDER_RESPONSE_INVALID,
                    "KakaoPay status 응답 body가 없습니다"
            );
        }
        String status = requireText(body.get("status"), "status");
        String orderId = requireText(body.get("partner_order_id"), "partner_order_id");

        Object amountObj = body.get("amount");
        if (amountObj == null) {
            throw new ApiException(
                    ErrorCode.PAYMENT_PROVIDER_RESPONSE_INVALID,
                    "KakaoPay status 응답에 amount가 없습니다"
            );
        }
        if (!(amountObj instanceof Map)) {
            throw new ApiException(
                    ErrorCode.PAYMENT_PROVIDER_RESPONSE_INVALID,
                    "KakaoPay status 응답의 amount 형식이 올바르지 않습니다"
            );
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> amountMap = (Map<String, Object>) amountObj;

        long totalAmount = parseLong(amountMap.get("total"), "amount.total");
        long taxFreeAmount = amountMap.get("tax_free") != null
                ? parseLong(amountMap.get("tax_free"), "amount.tax_free")
                : 0L;

        return new KakaoStatusResponse(
                status,
                orderId,
                totalAmount,
                taxFreeAmount,
                jsonConverter.convertToJson(body)
        );
    }

    private long parseLong(Object value, String fieldName) {
        if (value == null) {
            throw new ApiException(
                    ErrorCode.PAYMENT_PROVIDER_RESPONSE_INVALID,
                    "KakaoPay status 응답에 " + fieldName + "이 없습니다"
            );
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            throw new ApiException(
                    ErrorCode.PAYMENT_PROVIDER_RESPONSE_INVALID,
                    "KakaoPay status 응답의 " + fieldName + " 형식이 올바르지 않습니다: " + value,
                    e
            );
        }
    }

    private String requireText(Object value, String fieldName) {
        if (value == null || value.toString().isBlank()) {
            throw new ApiException(
                    ErrorCode.PAYMENT_PROVIDER_RESPONSE_INVALID,
                    "KakaoPay status 응답에 " + fieldName + "이 없습니다"
            );
        }
        return value.toString();
    }
}


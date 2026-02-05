package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoStatusResponse;
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
        String status = requireText(body.get("status"), "status");
        String orderId = requireText(body.get("partner_order_id"), "partner_order_id");

        @SuppressWarnings("unchecked")
        Map<String, Object> amountMap = (Map<String, Object>) body.get("amount");
        if (amountMap == null) {
            throw new RuntimeException("KakaoPay status 응답에 amount가 없습니다");
        }

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
            throw new RuntimeException("KakaoPay status 응답에 " + fieldName + "이 없습니다");
        }
        return Long.parseLong(value.toString());
    }

    private String requireText(Object value, String fieldName) {
        if (value == null || value.toString().isBlank()) {
            throw new RuntimeException("KakaoPay status 응답에 " + fieldName + "이 없습니다");
        }
        return value.toString();
    }
}


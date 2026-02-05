package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoReadyResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * KakaoPay 결제 준비 응답 파서
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoReadyResponseParser {

    private final KakaoPayJsonConverter jsonConverter;

    public KakaoReadyResponse parse(String orderId, Map<String, Object> body) {
        String tid = getRequiredString(body, "tid");
        String redirectUrl = getRequiredString(body, "next_redirect_pc_url");

        log.info("KakaoPay ready success: tid={}, orderId={}", tid, orderId);

        return new KakaoReadyResponse(
                tid,
                redirectUrl,
                jsonConverter.convertToJson(body)
        );
    }

    private String getRequiredString(Map<String, Object> body, String fieldName) {
        Object value = body.get(fieldName);
        if (value == null || value.toString().isBlank()) {
            throw new RuntimeException("KakaoPay ready 응답에 " + fieldName + "이 없습니다");
        }
        return value.toString();
    }
}


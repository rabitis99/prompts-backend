package org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * KakaoPay JSON 변환 유틸리티
 */
@Slf4j
@Component
public class KakaoPayJsonConverter {

    private final ObjectMapper objectMapper;

    public KakaoPayJsonConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Map을 JSON 문자열로 변환
     */
    public String convertToJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("JSON 변환 실패: {}", e.getMessage());
            return "{}";
        }
    }
}


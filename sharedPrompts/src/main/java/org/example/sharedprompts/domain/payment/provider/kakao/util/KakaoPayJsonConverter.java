package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * KakaoPay JSON 변환 유틸리티
 * 
 * <p>단일 책임: JSON 변환만 담당
 */
@Slf4j
@Component
public class KakaoPayJsonConverter {

    /**
     * Map을 JSON 문자열로 변환
     * 
     * @param map 변환할 Map
     * @return JSON 문자열 (실패 시 "{}")
     */
    public String convertToJson(Map<String, Object> map) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("JSON 변환 실패: {}", e.getMessage());
            return "{}";
        }
    }
}


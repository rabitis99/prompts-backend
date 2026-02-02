package org.example.sharedprompts.domain.payment.provider.toss.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * TossPay JSON 변환기
 *
 * <p>단일 책임: Map 데이터를 JSON 문자열로 변환
 */
@Slf4j
@Component
public class TossPayJsonConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

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

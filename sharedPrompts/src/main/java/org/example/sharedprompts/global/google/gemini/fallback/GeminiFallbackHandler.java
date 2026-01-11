package org.example.sharedprompts.global.google.gemini.fallback;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Gemini API 호출 실패 시 Fallback 메시지를 생성하는 클래스
 */
@Component
public class GeminiFallbackHandler {

    private static final String FALLBACK_INDICATOR = "[AI 응답 생성에 실패했습니다";

    /**
     * Fallback 프롬프트를 생성합니다.
     * AI 호출 실패 시 사용자에게 의미 있는 기본 프롬프트를 제공합니다.
     *
     * @param originalPrompt 원본 프롬프트
     * @return Fallback 메시지
     */
    public String createFallbackMessage(String originalPrompt) {
        String timestamp = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        return String.format("""
            [AI 응답 생성에 실패했습니다 - %s]
            
            시스템이 일시적으로 응답을 생성할 수 없습니다. 아래 기본 가이드를 참고해주세요.
            
            원본 요청:
            %s
            
            참고사항:
            - 네트워크 연결을 확인해주세요
            - 잠시 후 다시 시도해주세요
            - 문제가 지속되면 관리자에게 문의해주세요
            """, timestamp, originalPrompt);
    }

    /**
     * 주어진 메시지가 Fallback 메시지인지 확인합니다.
     *
     * @param message 확인할 메시지
     * @return Fallback 메시지인 경우 true
     */
    public boolean isFallbackMessage(String message) {
        return message != null && message.startsWith(FALLBACK_INDICATOR);
    }
}


package org.example.sharedprompts.global.google.gemini;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.google.gemini.service.GoogleGeminiService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 리액티브 GoogleGeminiService를 감싸 동기 API를 제공하는 어댑터.
 * block 호출은 이 계층에만 한정하고, 도메인 서비스는 동기 API만 사용한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SyncGoogleGeminiClientImpl implements SyncGoogleGeminiClient {

    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(60);

    private final GoogleGeminiService googleGeminiService;

    @Override
    public String chatSync(String prompt) {
        try {
            Mono<String> aiResponseMono = googleGeminiService.chat(prompt);
            return aiResponseMono.block(BLOCK_TIMEOUT);
        } catch (Exception e) {
            log.error("Google Gemini 동기 호출 실패: {}", e.getMessage(), e);
            throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
        }
    }
}



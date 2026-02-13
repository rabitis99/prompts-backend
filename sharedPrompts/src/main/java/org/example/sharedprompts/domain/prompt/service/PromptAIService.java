package org.example.sharedprompts.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.service.guideline.PromptGuidelineBuilder;
import org.example.sharedprompts.dto.prompt.request.InputRequestDto;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.global.google.gemini.SyncGoogleGeminiClient;
import org.example.sharedprompts.global.util.ValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptAIService {

    private final PromptGenerator promptGenerator;
    private final SyncGoogleGeminiClient syncGoogleGeminiClient;
    private final PromptGuidelineBuilder promptGuidelineBuilder;

    /**
     * 트랜잭션 없이 AI 호출 및 Guideline 적용 수행
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String generateContentSync(PromptRequestDto request) {
        ValidationUtils.requireNonNull(request, "request");

        InputRequestDto dto = request.toInputRequestDto();
        ValidationUtils.requireNonNull(dto.getLanguage(), "language");

        // 1. Prompt 생성
        String promptText = promptGenerator.generatePrompt(dto);
        log.debug("AI 프롬프트 생성 완료: language={}", dto.getLanguage());

        // 2. AI 호출 (Google Gemini 사용)
        // 참고: Spring AI는 구조만 유지하고 실제로는 사용하지 않음 (토큰 비용 때문)
        String aiGeneratedContent = syncGoogleGeminiClient.chatSync(promptText);
        log.debug("Google Gemini 응답 수신 완료: contentLength={}", aiGeneratedContent != null ? aiGeneratedContent.length() : 0);
        
        if (aiGeneratedContent == null || aiGeneratedContent.isEmpty()) {
            log.error("AI 서비스 응답이 null입니다: language={}", dto.getLanguage());
            throw new IllegalStateException("AI 서비스 응답이 null입니다.");
        }

        // 3. Guideline 적용
        String result = promptGuidelineBuilder.build(aiGeneratedContent, dto);
        log.debug("Guideline 적용 완료: language={}", dto.getLanguage());

        return result;
    }
}


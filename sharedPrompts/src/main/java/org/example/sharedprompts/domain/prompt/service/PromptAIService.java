package org.example.sharedprompts.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.service.guideline.GuidelineBuilderFactory;
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
    private final GuidelineBuilderFactory guidelineBuilderFactory;

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

        // 2. AI 호출
        String aiGeneratedContent = syncGoogleGeminiClient.chatSync(promptText);
        if (aiGeneratedContent == null) {
            log.error("AI 서비스 응답이 null입니다: language={}", dto.getLanguage());
            throw new IllegalStateException("AI 서비스 응답이 null입니다.");
        }
        log.debug("AI 서비스 응답 수신 완료: contentLength={}", aiGeneratedContent.length());

        // 3. Guideline 적용
        PromptGuidelineBuilder builder = guidelineBuilderFactory.getBuilder(dto.getLanguage());
        ValidationUtils.requireNonNull(builder, "GuidelineBuilder for language: " + dto.getLanguage());

        String result = builder.build(aiGeneratedContent, dto);
        log.debug("Guideline 적용 완료: language={}", dto.getLanguage());

        return result;
    }
}


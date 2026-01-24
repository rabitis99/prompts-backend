package org.example.sharedprompts.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.service.guideline.GuidelineBuilderFactory;
import org.example.sharedprompts.domain.prompt.service.guideline.PromptGuidelineBuilder;
import org.example.sharedprompts.dto.prompt.request.InputRequestDto;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.global.google.gemini.SyncGoogleGeminiClient;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptAIService {

    private final PromptGenerator promptGenerator;
    private final SyncGoogleGeminiClient syncGoogleGeminiClient;
    private final GuidelineBuilderFactory guidelineBuilderFactory;

    /**
     * PromptRequestDto를 InputRequestDto로 변환하고,
     * AI 모델을 호출한 뒤 Guideline을 적용한 최종 프롬프트 텍스트를 동기 방식으로 생성한다.
     * <p>
     * 외부 AI 호출 실패 / 타임아웃 시 SyncGoogleGeminiClient에서 도메인 예외로 변환한다.
     */
    public String generateContentSync(PromptRequestDto request) {
        Objects.requireNonNull(request, "request must not be null");

        InputRequestDto dto = request.toInputRequestDto();
        Objects.requireNonNull(dto.getLanguage(), "language must not be null");

        String promptText = promptGenerator.generatePrompt(dto);
        log.debug("AI 프롬프트 생성 완료: language={}", dto.getLanguage());

        String aiGeneratedContent = syncGoogleGeminiClient.chatSync(promptText);
        if (aiGeneratedContent == null) {
            log.error("AI 서비스 응답이 null입니다: language={}", dto.getLanguage());
            throw new IllegalStateException("AI 서비스 응답이 null입니다.");
        }
        log.debug("AI 서비스 응답 수신 완료: contentLength={}", aiGeneratedContent.length());

        PromptGuidelineBuilder builder = guidelineBuilderFactory.getBuilder(dto.getLanguage());
        Objects.requireNonNull(builder, "GuidelineBuilder must not be null for language: " + dto.getLanguage());

        String result = builder.build(aiGeneratedContent, dto);
        log.debug("Guideline 적용 완료: language={}", dto.getLanguage());
        return result;
    }
}


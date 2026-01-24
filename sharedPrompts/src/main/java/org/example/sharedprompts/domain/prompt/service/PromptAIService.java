package org.example.sharedprompts.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.service.guideline.GuidelineBuilderFactory;
import org.example.sharedprompts.domain.prompt.service.guideline.PromptGuidelineBuilder;
import org.example.sharedprompts.dto.prompt.request.InputRequestDto;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.global.google.gemini.SyncGoogleGeminiClient;
import org.springframework.stereotype.Service;

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
        InputRequestDto dto = request.toInputRequestDto();
        String promptText = promptGenerator.generatePrompt(dto);

        String aiGeneratedContent = syncGoogleGeminiClient.chatSync(promptText);

        PromptGuidelineBuilder builder =
                guidelineBuilderFactory.getBuilder(dto.getLanguage());
        return builder.build(aiGeneratedContent, dto);
    }
}


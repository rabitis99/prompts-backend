package org.example.sharedprompts.domain.prompt.facade;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.port.in.CreatePromptUseCase;
import org.example.sharedprompts.domain.prompt.service.PromptAIService;
import org.example.sharedprompts.domain.prompt.service.PromptPersistenceService;
import org.example.sharedprompts.domain.prompt.service.PromptSanitizationService;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프롬프트 생성 유즈케이스 전체 흐름을 담당하는 클래스.
 * <p>
 * - 입력값 Sanitization
 * - AI 모델 호출 및 Guideline 적용
 * - 트랜잭션 내 프롬프트 저장 및 태그 처리
 * - 이벤트 발행 및 후처리
 */
@Component
@RequiredArgsConstructor
public class PromptCreationFlow implements CreatePromptUseCase {

    private final PromptSanitizationService promptSanitizationService;
    private final PromptAIService promptAIService;
    private final PromptPersistenceService promptPersistenceService;

    @Override
    @Transactional(timeout = 30)
    public PromptResponseDto create(PromptRequestDto request, Long userId) {
        // 1. 입력값 Sanitization
        PromptRequestDto sanitizedRequest = promptSanitizationService.sanitize(request);

        // 2. AI 호출 및 Guideline 적용 (트랜잭션 외부)
        String aiContent = promptAIService.generateContentSync(sanitizedRequest);

        // 3. DB 저장 (트랜잭션 내부)
        return promptPersistenceService.savePrompt(sanitizedRequest, userId, aiContent);
    }
}



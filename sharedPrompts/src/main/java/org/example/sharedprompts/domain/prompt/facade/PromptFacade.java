package org.example.sharedprompts.domain.prompt.facade;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptFacade {

    private final PromptCreationFlow promptCreationFlow;

    /**
     * 프롬프트 생성에 대한 단일 진입점.
     * 유즈케이스 수준의 책임만 가지며, 실제 생성 흐름은 {@link PromptCreationFlow}에서 처리한다.
     */
    public PromptResponseDto createPrompt(PromptRequestDto request, Long userId) {
        return promptCreationFlow.create(request, userId);
    }
}

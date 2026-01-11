package org.example.sharedprompts.domain.prompt.facade;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.service.PromptService;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PromptFacade {

    private final PromptService promptService;

    @Transactional
    public PromptResponseDto createPrompt(PromptRequestDto request, Long userId) {
        // 리액티브 → 동기 변환 (block 허용)
        return promptService.createPrompt(request, userId)
            .block(Duration.ofSeconds(60));
    }
}


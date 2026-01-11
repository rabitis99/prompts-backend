package org.example.sharedprompts.domain.prompt.facade;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.service.PromptService;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PromptFacade {

    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(60);

    private final PromptService promptService;

    /**
     * 외부 AI API 호출을 포함한 프롬프트 생성
     * 
     * 외부 API 호출은 트랜잭션 경계 밖에서 수행하고,
     * DB 작업만 savePrompt 메서드에서 @Transactional로 보장합니다.
     * 이를 통해 DB 커넥션 풀 고갈을 방지합니다.
     */
    public PromptResponseDto createPrompt(PromptRequestDto request, Long userId) {
        // 리액티브 → 동기 변환 (block 허용)
        // 외부 AI API 호출이 완료된 후, savePrompt 메서드의 @Transactional에서 DB 작업 수행
        return promptService.createPrompt(request, userId)
            .block(BLOCK_TIMEOUT);
    }
}


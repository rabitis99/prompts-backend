package org.example.sharedprompts.domain.prompt.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.service.orchestration.PromptServiceImpl;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptCommandPort;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptQueryPort;
import org.example.sharedprompts.domain.prompt.infrastructure.persistence.PromptRepository;
import org.example.sharedprompts.domain.prompt.infrastructure.persistence.PromptSearchContext;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 프롬프트 조회·저장·삭제를 Repository에 위임하는 아웃바운드 어댑터.
 * <p>헥사고날: {@link PromptQueryPort}, {@link PromptCommandPort} 구현체.
 * Application 계층({@link PromptServiceImpl} 등)은 Port에만 의존하고,
 * 본 어댑터는 {@link PromptRepository}를 주입받아 Port 메서드를 구현한다.</p>
 */
@Component
@RequiredArgsConstructor
public class PromptPersistenceAdapter implements PromptQueryPort, PromptCommandPort {

    private final PromptRepository promptRepository;

    @Override
    public Optional<Prompt> findById(Long promptId) {
        return promptRepository.findById(promptId);
    }

    @Override
    public Page<Prompt> searchPrompts(PromptSearchCondition condition, Long viewerId) {
        PromptSearchContext context = PromptSearchContext.of(condition, viewerId);
        return promptRepository.searchPrompts(context);
    }

    @Override
    public Page<Prompt> searchMyPrompts(Long userId, PromptSearchCondition condition) {
        return promptRepository.searchMyPrompts(userId, condition);
    }

    @Override
    public Page<Prompt> searchUserPrompts(Long userId, PromptSearchCondition condition, Long viewerId) {
        return promptRepository.searchUserPrompts(userId, condition, viewerId);
    }

    @Override
    public Prompt save(Prompt prompt) {
        return promptRepository.save(prompt);
    }

    @Override
    public void delete(Prompt prompt) {
        promptRepository.delete(prompt);
    }
}

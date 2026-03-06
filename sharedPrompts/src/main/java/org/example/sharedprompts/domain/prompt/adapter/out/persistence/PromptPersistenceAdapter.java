package org.example.sharedprompts.domain.prompt.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptCommandPort;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptQueryPort;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptSearchQuery;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.prompt.infrastructure.persistence.PromptRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 프롬프트 조회·저장·삭제를 Repository에 위임하는 아웃바운드 어댑터.
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
    public Page<Prompt> search(PromptSearchQuery query) {
        return promptRepository.search(query);
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

package org.example.sharedprompts.domain.prompt.adapter.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptCommandPort;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptQueryPort;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptSearchQuery;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.prompt.infrastructure.persistence.PromptRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Dependency-direction adapter (no semantic translation)
/**
 * Prompt 영속성 어댑터
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
        return promptRepository.saveAndFlush(prompt);
    }

    @Override
    public void delete(Prompt prompt) {
        promptRepository.delete(prompt);
    }
}
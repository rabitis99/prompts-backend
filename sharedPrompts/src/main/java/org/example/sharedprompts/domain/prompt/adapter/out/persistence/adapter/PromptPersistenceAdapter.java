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

/**
 * Prompt 영속성 어댑터
 */
@Component
@RequiredArgsConstructor
public class PromptPersistenceAdapter implements PromptQueryPort, PromptCommandPort {

    private final PromptRepository promptRepository;

    @Override
    public Optional<Prompt> findById(Long promptId) {

        // 프롬프트 단건 조회
        return promptRepository.findById(promptId);
    }

    @Override
    public Page<Prompt> search(PromptSearchQuery query) {

        // 프롬프트 검색
        return promptRepository.search(query);
    }

    @Override
    public Prompt save(Prompt prompt) {

        // 프롬프트 저장
        return promptRepository.saveAndFlush(prompt);
    }

    @Override
    public void delete(Prompt prompt) {

        // 프롬프트 삭제
        promptRepository.delete(prompt);
    }
}
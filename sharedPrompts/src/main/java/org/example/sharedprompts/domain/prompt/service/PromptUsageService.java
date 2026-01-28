package org.example.sharedprompts.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromptUsageService {

    private final PromptRepository promptRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementUsageCount(Long promptId) {
        promptRepository.incrementUsageCount(promptId);
    }
}

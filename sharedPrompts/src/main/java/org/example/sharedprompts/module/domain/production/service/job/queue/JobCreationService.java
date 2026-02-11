package org.example.sharedprompts.module.domain.production.service.job.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.service.job.idempotencykey.IdempotencyKeyGenerator;
import org.example.sharedprompts.module.domain.production.service.job.JobEntityCreationService;
import org.example.sharedprompts.module.domain.production.service.job.JobStateService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobCreationService {

    private final JobEntityCreationService jobEntityCreationService;
    private final JobStateService jobStateService;
    private final IdempotencyKeyGenerator idempotencyKeyGenerator;

    public JobEntity createJob(Long promptId, Long userId, ProductionCommand command, String userInput) {
        String key = idempotencyKeyGenerator.generate(promptId, userId, command, userInput);
        log.info("Creating job with idempotencyKey: {}", key);
        return jobEntityCreationService.createJob(promptId, userId, command, userInput, key);
    }

    public JobEntity getJob(String jobId) {
        return jobStateService.getJob(jobId);
    }
}

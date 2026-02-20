package org.example.sharedprompts.module.domain.production.service.job.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.job.Job;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueService {

    private final JobCreationService jobCreationService;
    private final JobQueuePublisher jobQueuePublisher;
    private final JobMapper jobMapper;

    @Transactional
    public String enqueueJob(Long promptId, Long userId, ProductionCommand command, String userInput) {
        var jobEntity = jobCreationService.createJob(promptId, userId, command, userInput);
        jobQueuePublisher.publishJob(jobEntity.getJobId(), 3);
        return jobEntity.getJobId();
    }

    @Transactional(readOnly = true)
    public Job getJob(String jobId) {
        var jobEntity = jobCreationService.getJob(jobId);
        return jobMapper.toJob(jobEntity);
    }
}

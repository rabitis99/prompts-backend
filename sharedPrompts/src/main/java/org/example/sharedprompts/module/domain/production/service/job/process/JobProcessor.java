package org.example.sharedprompts.module.domain.production.service.job.process;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.service.job.JobStateService;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.JobExceptionHandler;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.RecoveryException;
import org.example.sharedprompts.module.domain.production.service.job.process.util.CommandDeserializer;
import org.example.sharedprompts.module.domain.production.service.job.queue.JobQueuePublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobProcessor {

    private final JobStateService jobStateService;
    private final JobExceptionHandler exceptionHandler;
    private final CommandDeserializer commandDeserializer;
    private final JobProcessorDelegate jobProcessorDelegate;
    private final JobQueuePublisher jobQueuePublisher;

    public void processJob(String jobId) {
        jobProcessorDelegate.processJob(jobId);
    }

    public void recoverJob(String jobId) {
        JobEntity job = jobStateService.getJob(jobId);
        JobStatus status = job.getStatus();

        if (status != JobStatus.FAILED) {
            log.warn("Cannot recover job - only FAILED status can be recovered. jobId: {}, status: {}", jobId, status);
            return;
        }

        log.info("Recovering failed job - jobId: {}, status: {}", jobId, status);

        try {
            jobStateService.retryJob(jobId);
            jobQueuePublisher.publishJob(jobId, 3);
        } catch (Exception e) {
            log.error("Failed to recover job - jobId: {}, status: {}", jobId, status, e);
            ProductionCommand command = commandDeserializer.deserialize(job);
            exceptionHandler.handleRecoveryException(jobId, command.getCommandType().name(), 
                    new RecoveryException("Recovery failed", e));
        }
    }
}


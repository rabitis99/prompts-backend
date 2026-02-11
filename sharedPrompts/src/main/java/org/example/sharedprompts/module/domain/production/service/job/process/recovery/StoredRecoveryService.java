package org.example.sharedprompts.module.domain.production.service.job.process.recovery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.service.job.JobStateService;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.JobExceptionHandler;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.RecoveryException;
import org.example.sharedprompts.module.domain.production.service.job.process.util.CommandDeserializer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoredRecoveryService {

    private final JobStateService jobStateService;
    private final CommandDeserializer commandDeserializer;
    private final JobExceptionHandler exceptionHandler;

    public void recover(String jobId) {
        JobEntity job = jobStateService.getJob(jobId);

        if (job.getStatus() != JobStatus.STORED) {
            throw new RecoveryException("Cannot retry from STORED: current status is " + job.getStatus());
        }

        try {
            jobStateService.markCompleted(jobId);
            log.info("Job recovered from STORED successfully - jobId: {}", jobId);
        } catch (Exception e) {
            log.error("Recovery failed from STORED - jobId: {}", jobId, e);
            ProductionCommand command = commandDeserializer.deserialize(job);
            exceptionHandler.handleRecoveryException(jobId, command.getCommandType().name(), new RecoveryException("Recovery failed", e));
        }
    }
}


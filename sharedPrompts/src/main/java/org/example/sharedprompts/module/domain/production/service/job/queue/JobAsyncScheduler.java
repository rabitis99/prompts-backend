package org.example.sharedprompts.module.domain.production.service.job.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.job.process.JobProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobAsyncScheduler {

    private final JobProcessor jobProcessor;

    public void schedule(String jobId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Transaction committed, starting async job - jobId: {}", jobId);
                jobProcessor.processJobAsync(jobId);
            }
        });
    }
}

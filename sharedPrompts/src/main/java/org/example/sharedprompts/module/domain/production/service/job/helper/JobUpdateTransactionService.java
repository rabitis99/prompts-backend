package org.example.sharedprompts.module.domain.production.service.job.helper;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.service.job.transaction.JobTransactionBoundary;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JobUpdateTransactionService {

    private final JobTransactionBoundary transactionBoundary;

    public void updateJobInTransaction(String jobId, Consumer<org.example.sharedprompts.module.domain.production.entity.job.JobEntity> updater) {
        transactionBoundary.executeInTransaction(jobId, updater);
    }

    public <T> T updateJobAndReturnInTransaction(String jobId, Function<org.example.sharedprompts.module.domain.production.entity.job.JobEntity, T> updater) {
        return transactionBoundary.executeInTransaction(jobId, updater);
    }
}


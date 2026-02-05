package org.example.sharedprompts.domain.payment.infrastructure.transaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class PaymentTransactionManager {

    private final DistributedLockService distributedLockService;
    private final TransactionTemplate transactionTemplate;

    public PaymentTransactionManager(
            DistributedLockService distributedLockService,
            PlatformTransactionManager transactionManager) {
        this.distributedLockService = distributedLockService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public <T> T executeWithLockAndTransaction(String lockKey, Supplier<T> task) {
        return distributedLockService.executeWithLock(lockKey, () ->
                transactionTemplate.execute(status -> task.get())
        );
    }

    public <T> T executeInTransaction(Supplier<T> task) {
        return transactionTemplate.execute(status -> task.get());
    }

    public <T> T executeWithLock(String lockKey, Supplier<T> task) {
        return distributedLockService.executeWithLock(lockKey, task);
    }
}


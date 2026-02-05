package org.example.sharedprompts.domain.payment.application.command.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.infrastructure.transaction.DistributedLockService;
import org.example.sharedprompts.domain.payment.infrastructure.transaction.PaymentTransactionManager;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTransactionOrchestrator {

    private final PaymentTransactionManager transactionManager;
    private final DistributedLockService distributedLockService;

    public <T> T executeWithLockAndTransaction(String lockKey, Supplier<T> task) {
        return transactionManager.executeWithLockAndTransaction(lockKey, task);
    }


    public String createLockKey(String prefix, Long id) {
        return distributedLockService.createLockKey(prefix, id);
    }
}


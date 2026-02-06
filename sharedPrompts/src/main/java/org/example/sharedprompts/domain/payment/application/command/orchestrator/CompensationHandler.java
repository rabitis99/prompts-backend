package org.example.sharedprompts.domain.payment.application.command.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.compensation.CompensationQueue;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.compensation.CompensationTask;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.compensation.CompensationTaskType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationHandler {

    private final CompensationQueue compensationQueue;

    public void handlePostProcessFailure(CompensationTaskType taskType, Long paymentId, Long userId,
                                        BigDecimal amount, String errorMessage) {
        handlePostProcessFailure(taskType, paymentId, userId, amount, null, errorMessage);
    }

    public void handlePostProcessFailure(CompensationTaskType taskType, Long paymentId, Long userId,
                                        BigDecimal amount, BigDecimal originalAmount, String errorMessage) {
        log.warn("후처리 실패 보상 작업 생성: taskType={}, paymentId={}, userId={}", taskType, paymentId, userId);
        
        CompensationTask task = originalAmount != null
                ? CompensationTask.withOriginalAmount(taskType, paymentId, userId, amount, originalAmount, errorMessage)
                : CompensationTask.of(taskType, paymentId, userId, amount, errorMessage);
        compensationQueue.enqueue(task);
    }
}


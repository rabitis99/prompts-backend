package org.example.sharedprompts.module.domain.production.service.ai.retry;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class AiPollingScheduler {

    private final ScheduledExecutorService scheduledExecutorService;

    public AiPollingScheduler(@Qualifier("retryScheduledExecutor") ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public CompletableFuture<Void> scheduleDelay(long delayMs) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            scheduledExecutorService.schedule(() -> {
                future.complete(null);
            }, delayMs, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            future.completeExceptionally(e);
        }
        return future;
    }
}


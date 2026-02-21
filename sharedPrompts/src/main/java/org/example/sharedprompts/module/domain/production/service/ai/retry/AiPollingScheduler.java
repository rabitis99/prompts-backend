package org.example.sharedprompts.module.domain.production.service.ai.retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiPollingScheduler {
    
    @Qualifier("retryScheduledExecutor")
    private final ScheduledExecutorService scheduledExecutorService;
    
    public CompletableFuture<Void> scheduleDelay(long delayMs) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        scheduledExecutorService.schedule(() -> {
            future.complete(null);
        }, delayMs, TimeUnit.MILLISECONDS);
        return future;
    }
}


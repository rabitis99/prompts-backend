package org.example.sharedprompts.module.domain.production.service.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "storage.lifecycle.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class StorageCleanupScheduler {

    private final StorageLifecycleService storageLifecycleService;

    @Scheduled(cron = "${storage.lifecycle.cleanup-cron:0 0 3 * * ?}")
    @SchedulerLock(name = "storageLifecycleCleanup", lockAtMostFor = "PT1H", lockAtLeastFor = "PT5M")
    public void runLifecycleProcessing() {
        log.info("Storage lifecycle cleanup scheduler started");

        try {
            int processed = storageLifecycleService.processLifecycleTransitions();
            log.info("Storage lifecycle cleanup completed - processed: {} objects", processed);
        } catch (Exception e) {
            log.error("Storage lifecycle cleanup failed", e);
        }
    }
}

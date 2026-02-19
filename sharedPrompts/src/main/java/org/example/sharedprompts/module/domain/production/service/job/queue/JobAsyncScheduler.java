package org.example.sharedprompts.module.domain.production.service.job.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.tenant.TenantContext;
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
        // Capture tenant context from the current thread BEFORE the transaction commits
        // This is critical because TenantContextFilter may clear it in its finally block
        // which runs after the request completes, potentially before afterCommit
        // tenant_id must come from X-Tenant-Id header - DO NOT create or generate tenant_id
        String tenantId = TenantContext.getCurrentTenantId();
        
        if (tenantId == null || tenantId.isBlank()) {
            log.error("Tenant context is REQUIRED when scheduling job - jobId: {}. " +
                    "X-Tenant-Id header must be provided when creating jobs.", jobId);
            throw new IllegalStateException(
                    "Tenant context is required when scheduling job. X-Tenant-Id header must be provided when creating jobs.");
        }
        
        log.debug("Captured tenant context for async job - jobId: {}, tenantId: {}", jobId, tenantId);
        
        // Capture tenantId as final variable for use in the lambda
        final String capturedTenantId = tenantId;
        
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Transaction committed, starting async job - jobId: {}", jobId);
                
                // CRITICAL: Set tenant context BEFORE calling async method
                // SecurityContextTaskDecorator.decorate() is called when processJobAsync is invoked,
                // and it captures the tenant context at that exact moment from ThreadLocal.
                // We must ensure it's set here so SecurityContextTaskDecorator can capture it.
                String previousTenantId = TenantContext.getCurrentTenantId();
                try {
                    // Set tenant context - it was already validated to be non-null when schedule() was called
                    TenantContext.setCurrentTenantId(capturedTenantId);
                    log.debug("Tenant context set in afterCommit - jobId: {}, tenantId: {}", jobId, capturedTenantId);
                    
                    // Double-check that tenant context is actually set
                    String currentTenantId = TenantContext.getCurrentTenantId();
                    if (currentTenantId == null) {
                        log.error("Failed to set tenant context in afterCommit - jobId: {}, expected tenantId: {}. " +
                                "This indicates a ThreadLocal issue.", jobId, capturedTenantId);
                        throw new IllegalStateException(
                                "Failed to set tenant context in afterCommit for jobId: " + jobId);
                    } else if (!currentTenantId.equals(capturedTenantId)) {
                        log.warn("Tenant context mismatch in afterCommit - jobId: {}, expected: {}, actual: {}", 
                                jobId, capturedTenantId, currentTenantId);
                    }
                    
                    // Call async method - SecurityContextTaskDecorator.decorate() will be called
                    // and will capture the tenant context from ThreadLocal at this moment
                    jobProcessor.processJobAsync(jobId);
                } finally {
                    // Restore previous tenant context (if any) after async task is submitted
                    // Note: The actual cleanup in the async thread will be done by SecurityContextTaskDecorator
                    if (previousTenantId != null) {
                        TenantContext.setCurrentTenantId(previousTenantId);
                    } else {
                        TenantContext.clear();
                    }
                }
            }
        });
    }
}

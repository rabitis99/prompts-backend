package org.example.sharedprompts.module.domain.production.service.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.factory.JobEntityFactory;
import org.example.sharedprompts.module.domain.production.util.TenantContextValidator;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobEntityCreationService {

    private final JobRepository jobRepository;
    private final JobEntityPersistenceService persistenceService;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobEntity createJob(
            Long promptId,
            Long userId,
            ProductionCommand command,
            String userInput,
            String idempotencyKey
    ) {
        try {
            String commandJson = objectMapper.writeValueAsString(command);
            String commandType = command.getCommandType() != null
                    ? command.getCommandType().name()
                    : "UNKNOWN";

            // Capture tenant ID from TenantContext when creating the job
            // tenant_id must come from X-Tenant-Id header - DO NOT create or generate tenant_id
            String tenantId = TenantContextValidator.requireTenantContext(
                    String.format("creating job - idempotencyKey: %s, userId: %s", idempotencyKey, userId));

            JobEntity job = JobEntityFactory.create(
                    promptId,
                    userId,
                    commandType,
                    commandJson,
                    userInput,
                    idempotencyKey,
                    tenantId
            );

            // Save in separate transaction so constraint violation only aborts that transaction
            // (PostgreSQL-compatible: caller transaction remains valid for findByIdempotencyKeyForUpdate)
            JobEntity savedJob = persistenceService.saveInNewTransaction(job);
            log.info("Job created - jobId: {}, idempotencyKey: {}",
                    savedJob.getJobId(), idempotencyKey);

            return savedJob;

        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate idempotencyKey detected - idempotencyKey: {}", idempotencyKey);

            // P0-1: Use pessimistic lock to prevent race condition when reading existing job
            // This ensures we get the latest state even if another thread is modifying the job
            JobEntity existingJob = jobRepository.findByIdempotencyKeyForUpdate(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "Job with idempotencyKey not found after duplicate exception: " + idempotencyKey, e));

            if (existingJob.isFailed()) {
                existingJob.resetForIdempotencyRetry();
                log.info("Reset failed job for retry - jobId: {}, idempotencyKey: {}",
                        existingJob.getJobId(), idempotencyKey);
                return jobRepository.save(existingJob);
            }

            return existingJob;

        } catch (Exception e) {
            log.error("Failed to create job - idempotencyKey: {}", idempotencyKey, e);
            throw new RuntimeException("Failed to create job: " + e.getMessage(), e);
        }
    }
}


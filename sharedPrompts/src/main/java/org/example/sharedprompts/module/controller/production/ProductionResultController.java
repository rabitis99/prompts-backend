package org.example.sharedprompts.module.controller.production;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.model.job.Job;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactRepository;
import org.example.sharedprompts.module.domain.production.service.job.process.recovery.JobRecoveryService;
import org.example.sharedprompts.module.domain.production.service.job.queue.JobQueueService;
import org.example.sharedprompts.module.dto.response.production.JobResponseDto;
import org.example.sharedprompts.module.dto.response.production.ProductionResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProductionResultController {

    private final ProductionArtifactRepository productionArtifactRepository;
    private final JobQueueService jobQueueService;
    private final JobRecoveryService jobRecoveryService;

    @GetMapping("/production/{productionId}")
    public ResponseEntity<CustomResponse<ProductionResponseDto>> getProductionResult(
            @PathVariable Long productionId,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Production result requested - artifactId: {}, userId: {}",
                productionId, authUser.getId());

        ProductionArtifactEntity artifact = productionArtifactRepository
                .findByIdWithDetail(productionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Production artifact not found: " + productionId));

        if (!artifact.getUserId().equals(authUser.getId())) {
            throw new IllegalArgumentException("Access denied to production: " + productionId);
        }

        return CustomResponseHelper.ok(ProductionResponseDto.from(artifact));
    }
    /**
     * Job 상태 조회
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<CustomResponse<JobResponseDto>> getJobStatus(
            @PathVariable String jobId,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Job status requested - jobId: {}, userId: {}", jobId, authUser.getId());

        Job job = jobQueueService.getJob(jobId);

        // 권한 확인 (본인의 Job만 조회 가능)
        if (!job.getUserId().equals(authUser.getId())) {
            throw new IllegalArgumentException("Access denied to job: " + jobId);
        }

        return CustomResponseHelper.ok(JobResponseDto.from(job));
    }

    /**
     * Job 재시도 (PARSED 또는 PARSE_FAILED 상태에서)
     * 파싱 단계부터 재시작하여 렌더링 및 저장까지 수행
     */
    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<CustomResponse<String>> retryJob(
            @PathVariable String jobId,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Job retry requested - jobId: {}, userId: {}", jobId, authUser.getId());

        // Job 조회 및 권한 확인
        var job = jobQueueService.getJob(jobId);
        if (!job.getUserId().equals(authUser.getId())) {
            throw new IllegalArgumentException("Access denied to job: " + jobId);
        }

        // 재시도 실행
        jobRecoveryService.recoverFromParsed(jobId);

        return CustomResponseHelper.ok("Job retry started successfully");
    }
}


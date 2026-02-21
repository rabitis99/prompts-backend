package org.example.sharedprompts.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.admin.response.AdminProductionJobSummaryDto;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.dto.common.PageResponse;
import org.example.sharedprompts.global.annotation.AdminOnly;
import org.example.sharedprompts.module.domain.production.application.ProductionJobAdminQueryService;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.service.job.process.JobProcessor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자용 Production Job 모니터링/재시도 API (DEPLOYMENT_ISSUES 5.1 - 실패 Job 모니터링)
 */
@Slf4j
@AdminOnly
@RestController
@RequestMapping("/admin/production/jobs")
@RequiredArgsConstructor
public class AdminProductionJobController {

    private final ProductionJobAdminQueryService jobAdminQueryService;
    private final JobProcessor jobProcessor;

    /**
     * 실패(FAILED) Job 목록 조회
     * GET /admin/production/jobs/failed?page=0&size=50
     */
    @GetMapping("/failed")
    public ResponseEntity<CustomResponse<PageResponse<AdminProductionJobSummaryDto>>> getFailedJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Admin failed jobs list - userId: {}, page: {}, size: {}", authUser.getId(), page, size);
        Page<JobEntity> entityPage = jobAdminQueryService.findFailedJobs(page, size);
        Page<AdminProductionJobSummaryDto> dtoPage = entityPage.map(this::toSummaryDto);
        return CustomResponseHelper.ok(PageResponse.of(dtoPage));
    }

    /**
     * 상태 불명(UNKNOWN) Job 목록 조회
     * GET /admin/production/jobs/unknown?page=0&size=50
     */
    @GetMapping("/unknown")
    public ResponseEntity<CustomResponse<PageResponse<AdminProductionJobSummaryDto>>> getUnknownJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Admin unknown jobs list - userId: {}, page: {}, size: {}", authUser.getId(), page, size);
        Page<JobEntity> entityPage = jobAdminQueryService.findUnknownJobs(page, size);
        Page<AdminProductionJobSummaryDto> dtoPage = entityPage.map(this::toSummaryDto);
        return CustomResponseHelper.ok(PageResponse.of(dtoPage));
    }

    /**
     * 관리자 재시도 (FAILED Job만 가능)
     * POST /admin/production/jobs/{jobId}/retry
     */
    @PostMapping("/{jobId}/retry")
    public ResponseEntity<CustomResponse<Void>> retryJob(
            @PathVariable String jobId,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Admin job retry - jobId: {}, userId: {}", jobId, authUser.getId());
        jobProcessor.recoverJob(jobId);
        return CustomResponseHelper.ok(null);
    }

    private AdminProductionJobSummaryDto toSummaryDto(JobEntity e) {
        return AdminProductionJobSummaryDto.builder()
                .jobId(e.getJobId())
                .status(e.getStatus().name())
                .errorMessage(e.getErrorMessage())
                .createdAt(e.getCreatedAt())
                .userId(e.getUserId())
                .retryCount(e.getRetryCount())
                .build();
    }
}

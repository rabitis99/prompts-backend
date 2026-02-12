package org.example.sharedprompts.module.controller.production;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.module.domain.production.application.ProductionResultApplicationService;
import org.example.sharedprompts.module.dto.response.production.JobResponseDto;
import org.example.sharedprompts.module.dto.response.production.ProductionResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProductionResultController {

    private final ProductionResultApplicationService applicationService;

    @GetMapping("/production/{productionId}")
    public ResponseEntity<CustomResponse<ProductionResponseDto>> getProductionResult(
            @PathVariable Long productionId,
            @CurrentUser AuthUser authUser
    ) {
        ProductionResponseDto response = applicationService.getProductionResult(
                productionId, 
                authUser.getId()
        );
        
        return CustomResponseHelper.ok(response);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<CustomResponse<JobResponseDto>> getJobStatus(
            @PathVariable String jobId,
            @CurrentUser AuthUser authUser
    ) {
        JobResponseDto response = applicationService.getJobStatus(jobId, authUser.getId());
        
        return CustomResponseHelper.ok(response);
    }

    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<CustomResponse<String>> retryJob(
            @PathVariable String jobId,
            @CurrentUser AuthUser authUser
    ) {
        applicationService.retryJob(jobId, authUser.getId());
        
        return CustomResponseHelper.ok("Job retry started successfully");
    }
}


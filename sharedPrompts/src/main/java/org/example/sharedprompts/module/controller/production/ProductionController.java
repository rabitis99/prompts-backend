package org.example.sharedprompts.module.controller.production;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.module.domain.production.application.ProductionApplicationService;
import org.example.sharedprompts.module.dto.request.production.*;
import org.example.sharedprompts.module.dto.response.production.JobResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 통합 Production Controller
 */
@RestController
@RequestMapping("/prompts/{promptId}/production")
@RequiredArgsConstructor
@Slf4j
public class ProductionController {
    
    private final ProductionApplicationService applicationService;
    
    /**
     * Text Production 생성 요청
     */
    @PostMapping("/text")
    public ResponseEntity<CustomResponse<JobResponseDto>> produceText(
            @PathVariable Long promptId,
            @Valid @RequestBody TextProductionRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Text production requested - promptId: {}, userId: {}", 
                promptId, authUser.getId());
        
        JobResponseDto response = applicationService.produce(
                promptId,
                authUser.getId(),
                request
        );
        
        return CustomResponseHelper.ok(response);
    }
    
    /**
     * Image Production 생성 요청
     */
    @PostMapping("/image")
    public ResponseEntity<CustomResponse<JobResponseDto>> produceImage(
            @PathVariable Long promptId,
            @Valid @RequestBody ImageProductionRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Image production requested - promptId: {}, userId: {}", 
                promptId, authUser.getId());
        
        JobResponseDto response = applicationService.produce(
                promptId,
                authUser.getId(),
                request
        );
        
        return CustomResponseHelper.ok(response);
    }
    
    /**
     * Email Production 생성 요청
     */
    @PostMapping("/email")
    public ResponseEntity<CustomResponse<JobResponseDto>> produceEmail(
            @PathVariable Long promptId,
            @Valid @RequestBody EmailProductionRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Email production requested - promptId: {}, userId: {}", 
                promptId, authUser.getId());
        
        JobResponseDto response = applicationService.produce(
                promptId,
                authUser.getId(),
                request
        );
        
        return CustomResponseHelper.ok(response);
    }
    
    /**
     * Blog Production 생성 요청
     */
    @PostMapping("/blog")
    public ResponseEntity<CustomResponse<JobResponseDto>> produceBlog(
            @PathVariable Long promptId,
            @Valid @RequestBody BlogProductionRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Blog production requested - promptId: {}, userId: {}", 
                promptId, authUser.getId());
        
        JobResponseDto response = applicationService.produce(
                promptId,
                authUser.getId(),
                request
        );
        
        return CustomResponseHelper.ok(response);
    }
    
    /**
     * Document Production 생성 요청
     */
    @PostMapping("/document")
    public ResponseEntity<CustomResponse<JobResponseDto>> produceDocument(
            @PathVariable Long promptId,
            @Valid @RequestBody DocumentProductionRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Document production requested - promptId: {}, userId: {}", 
                promptId, authUser.getId());
        
        JobResponseDto response = applicationService.produce(
                promptId,
                authUser.getId(),
                request
        );
        
        return CustomResponseHelper.ok(response);
    }
}


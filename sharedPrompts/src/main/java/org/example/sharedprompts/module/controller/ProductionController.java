package org.example.sharedprompts.module.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.prompt.facade.PromptProductionDeliveryFacade;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.module.dto.request.production.ProductionCommandMapper;
import org.example.sharedprompts.module.dto.request.production.ProductionRequestDto;
import org.example.sharedprompts.module.dto.response.production.ProductionResponseDto;
import org.example.sharedprompts.module.domain.production.api.model.ProductionResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ProductionController {
    
    private final PromptProductionDeliveryFacade productionDeliveryFacade;
    private final ProductionCommandMapper productionCommandMapper;
    
    @PostMapping("/prompts/{promptId}/production")
    public ResponseEntity<CustomResponse<ProductionResponseDto>> executeProduction(
            @PathVariable Long promptId,
            @Valid @RequestBody ProductionRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        ProductionResult result = productionDeliveryFacade.executeProduction(
            promptId,
            authUser.getId(),
            productionCommandMapper.toProductionCommand(request.getCommand()),
            request.getUserInput() != null ? 
                request.getUserInput().getInput() : null
        );
        
        // 경쟁 조건을 피하기 위해 result에서 직접 artifact ID를 가져옵니다
        Long productionEntityId = result.getArtifactId();
        
        ProductionResponseDto response = ProductionResponseDto.from(result, productionEntityId);
        return CustomResponseHelper.ok(response);
    }
    
    @GetMapping("/production/{productionArtifactId}")
    public ResponseEntity<CustomResponse<ProductionResponseDto>> getProductionResult(
            @PathVariable Long productionArtifactId,
            @CurrentUser AuthUser authUser
    ) {
        ProductionResult result = productionDeliveryFacade.getProductionResult(productionArtifactId, authUser.getId());
        
        ProductionResponseDto response = ProductionResponseDto.from(result, productionArtifactId);
        return CustomResponseHelper.ok(response);
    }
}


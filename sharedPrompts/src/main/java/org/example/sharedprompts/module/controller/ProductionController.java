package org.example.sharedprompts.module.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.prompt.facade.PromptProductionDeliveryFacade;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.module.dto.request.production.ProductionRequestDto;
import org.example.sharedprompts.module.dto.response.production.ProductionResponseDto;
import org.example.sharedprompts.module.domain.production.api.model.ProductionResult;
import org.example.sharedprompts.module.domain.production.repository.ProductionArtifactRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ProductionController {
    
    private final PromptProductionDeliveryFacade productionDeliveryFacade;
    private final ProductionArtifactRepository productionArtifactRepository;
    
    @PostMapping("/prompts/{promptId}/production")
    public ResponseEntity<CustomResponse<ProductionResponseDto>> executeProduction(
            @PathVariable Long promptId,
            @Valid @RequestBody ProductionRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        ProductionResult result = productionDeliveryFacade.executeProduction(
            promptId,
            authUser.getId(),
            request.toProductionCommand(),
            request.getUserInput() != null ? 
                request.getUserInput().getInput() : null
        );
        
        Long productionEntityId = productionArtifactRepository
                .findTop1000ByUserIdOrderByCreatedAtDesc(authUser.getId())
                .stream()
                .filter(entity -> entity.getCommandType().equals(request.getCommandType()))
                .findFirst()
                .map(entity -> entity.getId())
                .orElse(null);
        
        ProductionResponseDto response = ProductionResponseDto.from(result, productionEntityId);
        return CustomResponseHelper.ok(response);
    }
    
    @GetMapping("/production/{productionId}")
    public ResponseEntity<CustomResponse<ProductionResponseDto>> getProductionResult(
            @PathVariable String productionId,
            @CurrentUser AuthUser authUser
    ) {
        ProductionResult result = productionDeliveryFacade.getProductionResult(productionId, authUser.getId());
        
        Long entityId = productionArtifactRepository.findByProductionId(productionId)
                .map(entity -> entity.getId())
                .orElse(null);
        
        ProductionResponseDto response = ProductionResponseDto.from(result, entityId);
        return CustomResponseHelper.ok(response);
    }
}


package org.example.sharedprompts.domain.prompt.adapter.in.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request.UnifiedGeneratePromptRequest;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.BadgeDto;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.UnifiedGeneratePromptResponse;
import org.example.sharedprompts.domain.prompt.application.port.in.GenerateUnifiedPromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.UnifiedGeneratePromptResult;
import org.example.sharedprompts.domain.prompt.domain.value.quality.QualityBadge;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * 통합 프롬프트 생성 엔진 컨트롤러.
 *
 * <p>외부에는 단일 엔드포인트(/api/prompts/generate)와 단일 DTO만 노출한다.</p>
 */
@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class UnifiedPromptEngineController {

    private static final long ASYNC_TIMEOUT_MS = 60_000L;

    private static final Logger log = LoggerFactory.getLogger(UnifiedPromptEngineController.class);

    private final GenerateUnifiedPromptUseCase unifiedPromptUseCase;

    @PostMapping("/generate")
    public WebAsyncTask<ResponseEntity<CustomResponse<UnifiedGeneratePromptResponse>>> generate(
            @Valid @RequestBody UnifiedGeneratePromptRequest request,
            @CurrentUser AuthUser authUser
    ) {
        WebAsyncTask<ResponseEntity<CustomResponse<UnifiedGeneratePromptResponse>>> asyncTask =
                getResponseEntityWebAsyncTask(request, authUser);

        asyncTask.onTimeout(() -> {
            ApiException timeoutEx = new ApiException(
                    ErrorCode.AI_GENERATION_TIMEOUT,
                    "프롬프트 생성이 시간 초과되었습니다. 잠시 후 다시 시도해주세요.");
            return ResponseEntity.status(timeoutEx.getErrorCode().getHttpStatus())
                    .body(CustomResponse.fail(timeoutEx));
        });

        asyncTask.onError(() -> {
            ApiException ex = new ApiException(
                    ErrorCode.AI_GENERATION_FAILED,
                    "프롬프트 생성 중 오류가 발생했습니다.");
            return ResponseEntity.status(ex.getErrorCode().getHttpStatus())
                    .body(CustomResponse.fail(ex));
        });

        return asyncTask;
    }

    private @NotNull WebAsyncTask<ResponseEntity<CustomResponse<UnifiedGeneratePromptResponse>>> getResponseEntityWebAsyncTask(
            UnifiedGeneratePromptRequest request,
            AuthUser authUser
    ) {
        if (authUser == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        UnifiedGeneratePromptCommand command = request.toCommand(authUser.getId());

        Callable<ResponseEntity<CustomResponse<UnifiedGeneratePromptResponse>>> callable = () -> {
            try {
                UnifiedGeneratePromptResult result = unifiedPromptUseCase.generate(command);
                UnifiedGeneratePromptResponse response = toResponse(result);
                return CustomResponseHelper.created(response);
            } catch (Exception e) {
                log.warn("Unified prompt generation failed: {}", e.getMessage(), e);
                throw e;
            }
        };

        return new WebAsyncTask<>(ASYNC_TIMEOUT_MS, callable);
    }

    private UnifiedGeneratePromptResponse toResponse(UnifiedGeneratePromptResult result) {
        List<BadgeDto> badgeDtos = result.qualityBadges().stream()
                .map(this::toBadgeDto)
                .toList();

        return new UnifiedGeneratePromptResponse(
                result.output(),
                result.requestedEngineMode(),
                result.effectiveEngineMode(),
                result.engineProfile(),
                result.resolvedDomain(),
                result.objective(),
                result.outputNeeds(),
                result.intent(),
                result.variant(),
                result.coreRole(),
                result.domainRole(),
                badgeDtos,
                result.verifyPassed(),
                result.repairCount(),
                result.finallyPassed(),
                result.schemaContractFailed(),
                result.schemaFailureReasons(),
                result.appliedRuleIds(),
                result.routingReasons()
        );
    }

    private BadgeDto toBadgeDto(QualityBadge badge) {
        return new BadgeDto(badge.name(), badge.getDisplayName());
    }
}


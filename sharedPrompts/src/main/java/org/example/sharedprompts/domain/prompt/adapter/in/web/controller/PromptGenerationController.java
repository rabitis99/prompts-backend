package org.example.sharedprompts.domain.prompt.adapter.in.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request.ConfirmedGeneratePromptRequest;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request.UnifiedGeneratePromptRequest;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.UnifiedGeneratePromptResponse;
import org.example.sharedprompts.domain.prompt.adapter.in.web.mapper.UnifiedPromptResponseMapper;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptFromConfirmedAxesUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.GenerateUnifiedPromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.ConfirmedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.UnifiedGeneratePromptResult;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.util.concurrent.Callable;

/**
 * Unified prompt generation controller: one-shot generation and confirmed-axes generation.
 * POST /prompts/generate — existing one-shot flow (unchanged).
 * POST /prompts/generate/confirmed — generate using pre-confirmed axes from recommendation flow.
 */
@RestController
@RequestMapping("/prompts")
@RequiredArgsConstructor
public class PromptGenerationController {

    private static final long ASYNC_TIMEOUT_MS = 60_000L;
    private static final Logger log = LoggerFactory.getLogger(PromptGenerationController.class);

    private final GenerateUnifiedPromptUseCase unifiedPromptUseCase;
    private final GeneratePromptFromConfirmedAxesUseCase generateFromConfirmedAxesUseCase;
    private final UnifiedPromptResponseMapper responseMapper;

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
                UnifiedGeneratePromptResponse response = responseMapper.toResponse(result);
                return CustomResponseHelper.created(response);
            } catch (ApiException ex) {
                return ResponseEntity.status(ex.getErrorCode().getHttpStatus())
                        .body(CustomResponse.fail(ex));
            } catch (Exception e) {
                log.warn("Unified prompt generation failed: {}", e.getMessage(), e);
                throw e;
            }
        };

        return new WebAsyncTask<>(ASYNC_TIMEOUT_MS, callable);
    }

    @PostMapping("/generate/confirmed")
    public WebAsyncTask<ResponseEntity<CustomResponse<UnifiedGeneratePromptResponse>>> generateConfirmed(
            @Valid @RequestBody ConfirmedGeneratePromptRequest request,
            @CurrentUser AuthUser authUser
    ) {
        if (authUser == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        ConfirmedGeneratePromptCommand command = request.toCommand(authUser.getId());

        Callable<ResponseEntity<CustomResponse<UnifiedGeneratePromptResponse>>> callable = () -> {
            try {
                UnifiedGeneratePromptResult result = generateFromConfirmedAxesUseCase.generate(command);
                UnifiedGeneratePromptResponse response = responseMapper.toResponse(result);
                return CustomResponseHelper.created(response);
            } catch (ApiException ex) {
                return ResponseEntity.status(ex.getErrorCode().getHttpStatus())
                        .body(CustomResponse.fail(ex));
            } catch (Exception e) {
                log.warn("Confirmed prompt generation failed: {}", e.getMessage(), e);
                throw e;
            }
        };

        WebAsyncTask<ResponseEntity<CustomResponse<UnifiedGeneratePromptResponse>>> task =
                new WebAsyncTask<>(ASYNC_TIMEOUT_MS, callable);
        task.onTimeout(() -> {
            ApiException timeoutEx = new ApiException(
                    ErrorCode.AI_GENERATION_TIMEOUT,
                    "프롬프트 생성이 시간 초과되었습니다. 잠시 후 다시 시도해주세요.");
            return ResponseEntity.status(timeoutEx.getErrorCode().getHttpStatus())
                    .body(CustomResponse.fail(timeoutEx));
        });
        task.onError(() -> {
            ApiException ex = new ApiException(
                    ErrorCode.AI_GENERATION_FAILED,
                    "프롬프트 생성 중 오류가 발생했습니다.");
            return ResponseEntity.status(ex.getErrorCode().getHttpStatus())
                    .body(CustomResponse.fail(ex));
        });
        return task;
    }
}

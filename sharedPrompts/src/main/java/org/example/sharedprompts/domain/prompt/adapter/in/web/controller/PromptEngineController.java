package org.example.sharedprompts.domain.prompt.adapter.in.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.BadgeResponseAssembler;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request.GeneratePromptRequest;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.GeneratePromptResponse;
import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptUseCase;
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

import java.util.concurrent.Callable;

/**
 * 정확도 중심 프롬프트 생성 엔진 컨트롤러 (v2).
 */
@RestController
@RequestMapping("/v2/prompts")
@RequiredArgsConstructor
public class PromptEngineController {

    private static final long ASYNC_TIMEOUT_MS = 60_000L;

    private final GeneratePromptUseCase generatePromptUseCase;
    private final BadgeResponseAssembler badgeResponseAssembler;

    /**
     * 프롬프트 생성 (정확도 엔진 v2).
     * 4단계 파이프라인(Clarify → Solve → Verify → Repair)을 실행하고
     * 배지 형태의 품질 정보와 함께 결과를 반환한다.
     */
    @PostMapping("/generate")
    public WebAsyncTask<ResponseEntity<CustomResponse<GeneratePromptResponse>>> generate(
            @Valid @RequestBody GeneratePromptRequest request,
            @CurrentUser AuthUser authUser
    ) {
        WebAsyncTask<ResponseEntity<CustomResponse<GeneratePromptResponse>>> asyncTask = getResponseEntityWebAsyncTask(request, authUser);

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

    private @NotNull WebAsyncTask<ResponseEntity<CustomResponse<GeneratePromptResponse>>> getResponseEntityWebAsyncTask(GeneratePromptRequest request, AuthUser authUser) {
        if (authUser == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        GeneratePromptCommand command = request.toCommand(authUser.getId());

        Callable<ResponseEntity<CustomResponse<GeneratePromptResponse>>> callable = () -> {
            GeneratePromptResult result = generatePromptUseCase.generate(command);
            GeneratePromptResponse response = badgeResponseAssembler.assemble(result);
            return CustomResponseHelper.created(response);
        };

        return new WebAsyncTask<>(ASYNC_TIMEOUT_MS, callable);
    }
}

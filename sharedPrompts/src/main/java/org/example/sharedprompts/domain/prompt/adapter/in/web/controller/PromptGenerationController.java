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
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.global.async.AsyncWebTaskExecutor;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.WebAsyncTask;

@RestController
@RequestMapping("/prompts")
@RequiredArgsConstructor
public class PromptGenerationController {

    private static final String OP_UNIFIED = "프롬프트 생성";
    private static final String OP_CONFIRMED = "확정 축 기반 프롬프트 생성";

    private final GenerateUnifiedPromptUseCase unifiedPromptUseCase;
    private final GeneratePromptFromConfirmedAxesUseCase generateFromConfirmedAxesUseCase;
    private final UnifiedPromptResponseMapper responseMapper;
    private final AsyncWebTaskExecutor asyncWebTaskExecutor;

    @PostMapping("/generate")
    public WebAsyncTask<ResponseEntity<CustomResponse<UnifiedGeneratePromptResponse>>> generate(
            @Valid @RequestBody UnifiedGeneratePromptRequest request,
            @CurrentUser AuthUser authUser
    ) {
        validateAuth(authUser);

        return asyncWebTaskExecutor.execute(OP_UNIFIED, authUser.getId(), () -> {
            var command = request.toCommand(authUser.getId());
            var result = unifiedPromptUseCase.generate(command);
            return CustomResponseHelper.created(responseMapper.toResponse(result));
        });
    }

    @PostMapping("/generate/confirmed")
    public WebAsyncTask<ResponseEntity<CustomResponse<UnifiedGeneratePromptResponse>>> generateConfirmed(
            @Valid @RequestBody ConfirmedGeneratePromptRequest request,
            @CurrentUser AuthUser authUser
    ) {
        validateAuth(authUser);

        return asyncWebTaskExecutor.execute(OP_CONFIRMED, authUser.getId(), () -> {
            var command = request.toCommand(authUser.getId());
            var result = generateFromConfirmedAxesUseCase.generate(command);
            return CustomResponseHelper.created(responseMapper.toResponse(result));
        });
    }

    private void validateAuth(AuthUser authUser) {
        if (authUser == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }
}

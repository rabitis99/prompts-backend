package org.example.sharedprompts.controller.prompt;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.prompt.application.port.in.command.CreatePromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.PromptCommandUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptQueryUseCase;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.dto.common.PageResponse;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.example.sharedprompts.dto.prompt.request.PromptUpdateDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.global.config.PromptCreationProperties;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.security.SecurityContextService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequestMapping("/prompts")
public class PromptController {

    private final PromptQueryUseCase promptQueryUseCase;
    private final PromptCommandUseCase promptCommandUseCase;
    private final CreatePromptUseCase createPromptUseCase;
    private final PromptCreationProperties promptCreationProperties;
    private final ExecutorService aiCallTaskExecutorWithSecurityContext;

    public PromptController(
            PromptQueryUseCase promptQueryUseCase,
            PromptCommandUseCase promptCommandUseCase,
            CreatePromptUseCase createPromptUseCase,
            PromptCreationProperties promptCreationProperties,
            @Qualifier("aiCallTaskExecutorWithSecurityContext") ExecutorService aiCallTaskExecutorWithSecurityContext
    ) {
        this.promptQueryUseCase = promptQueryUseCase;
        this.promptCommandUseCase = promptCommandUseCase;
        this.createPromptUseCase = createPromptUseCase;
        this.promptCreationProperties = promptCreationProperties;
        this.aiCallTaskExecutorWithSecurityContext = aiCallTaskExecutorWithSecurityContext;
    }

    @PostMapping
    public WebAsyncTask<ResponseEntity<CustomResponse<PromptResponseDto>>> createPrompt(
            @Valid @RequestBody PromptRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        Long userId = authUser.getId();

        long timeoutMs = promptCreationProperties.getTimeoutMs();
        long futureTimeoutMs = promptCreationProperties.getFutureTimeoutMs();

        final AtomicReference<Future<PromptResponseDto>> futureHolder = new AtomicReference<>();

        Callable<ResponseEntity<CustomResponse<PromptResponseDto>>> callable = () -> {
            try {
                futureHolder.set(aiCallTaskExecutorWithSecurityContext.submit(
                        () -> createPromptUseCase.create(request, userId)
                ));
                Future<PromptResponseDto> future = futureHolder.get();

                PromptResponseDto result = future.get(futureTimeoutMs, TimeUnit.MILLISECONDS);

                ResponseEntity<CustomResponse<PromptResponseDto>> response = CustomResponseHelper.created(result);

                SecurityContextService.logSecurityContextDebug("응답 생성 완료");

                return response;

            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof ApiException apiException) {
                    return createFailResponse(apiException);
                }
                return createFailResponse(new ApiException(ErrorCode.AI_GENERATION_FAILED));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Future<PromptResponseDto> f = futureHolder.get();
                if (f != null) {
                    f.cancel(true);
                }
                return createFailResponse(new ApiException(ErrorCode.AI_GENERATION_FAILED));

            } catch (TimeoutException e) {
                cancelFuture(futureHolder);
                return createFailResponse(timeoutApiException());

            } catch (Exception e) {
                log.error("[PromptController] Unexpected error during prompt creation", e);
                return createFailResponse(new ApiException(ErrorCode.AI_GENERATION_FAILED));
            }
        };

        WebAsyncTask<ResponseEntity<CustomResponse<PromptResponseDto>>> asyncTask =
                new WebAsyncTask<>(timeoutMs, callable);

        asyncTask.onTimeout(() -> {
            cancelFuture(futureHolder);
            return createFailResponse(timeoutApiException());
        });

        asyncTask.onError(() -> createFailResponse(new ApiException(ErrorCode.INTERNAL_SERVER_ERROR)));

        return asyncTask;
    }

    @GetMapping
    public ResponseEntity<CustomResponse<PageResponse<PromptResponseDto>>> getPrompts(
            @Valid @ModelAttribute("condition") PromptSearchCondition condition,
            @CurrentUser AuthUser authUser
    ) {
        Long viewerId = authUser != null ? authUser.getId() : null;
        PageResponse<PromptResponseDto> response = promptQueryUseCase.getPrompts(condition, viewerId);
        return CustomResponseHelper.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<PromptResponseDto>> getPromptDetail(
            @PathVariable Long id,
            @CurrentUser AuthUser authUser
    ) {
        Long viewerId = authUser != null ? authUser.getId() : null;
        return CustomResponseHelper.ok(promptQueryUseCase.getPromptDetail(id, viewerId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CustomResponse<PromptResponseDto>> updatePrompt(
            @PathVariable Long id,
            @Valid @RequestBody PromptUpdateDto promptUpdateDto,
            @CurrentUser AuthUser authUser
    ) {
        return CustomResponseHelper.ok(
                promptCommandUseCase.updatePrompt(id, promptUpdateDto, authUser.getId())
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrompt(
            @PathVariable Long id,
            @CurrentUser AuthUser authUser
    ) {
        promptCommandUseCase.deletePrompt(id, authUser.getId());
        return CustomResponseHelper.noContent();
    }

    @GetMapping("/me")
    public ResponseEntity<CustomResponse<PageResponse<PromptResponseDto>>> getMyPrompts(
            @CurrentUser AuthUser authUser,
            @Valid @ModelAttribute("condition") PromptSearchCondition condition
    ) {
        PageResponse<PromptResponseDto> response = promptQueryUseCase.getMyPrompts(authUser.getId(), condition);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 다른 사용자의 프롬프트 목록 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<CustomResponse<PageResponse<PromptResponseDto>>> getUserPrompts(
            @PathVariable Long userId,
            @Valid @ModelAttribute("condition") PromptSearchCondition condition,
            @CurrentUser AuthUser authUser
    ) {
        Long viewerId = authUser != null ? authUser.getId() : null;
        PageResponse<PromptResponseDto> response = promptQueryUseCase.getUserPrompts(userId, condition, viewerId);
        return CustomResponseHelper.ok(response);
    }

    private void cancelFuture(AtomicReference<Future<PromptResponseDto>> futureHolder) {
        Future<PromptResponseDto> f = futureHolder.get();
        if (f != null) {
            f.cancel(true);
        }
    }

    private ApiException timeoutApiException() {
        return new ApiException(
                ErrorCode.AI_GENERATION_TIMEOUT,
                "프롬프트 생성이 시간 초과되었습니다. 잠시 후 다시 시도해주세요."
        );
    }

    private ResponseEntity<CustomResponse<PromptResponseDto>> createFailResponse(ApiException e) {
        return CustomResponseHelper.fail(e);
    }
}

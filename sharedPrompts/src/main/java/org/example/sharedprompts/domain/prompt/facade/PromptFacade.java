package org.example.sharedprompts.domain.prompt.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.global.config.PromptCreationProperties;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptFacade {

    private final PromptCreationFlow promptCreationFlow;
    private final PromptCreationProperties promptCreationProperties;

    @Qualifier("aiCallTaskExecutorWithSecurityContext")
    private final ExecutorService aiCallTaskExecutorWithSecurityContext;

    public WebAsyncTask<ResponseEntity<CustomResponse<PromptResponseDto>>> createPromptAsyncWeb(
            PromptRequestDto request,
            Long userId
    ) {

        long timeoutMs = promptCreationProperties.getTimeoutMs();
        long futureTimeoutMs = promptCreationProperties.getFutureTimeoutMs();
        
        Callable<ResponseEntity<CustomResponse<PromptResponseDto>>> callable = () -> {
            try {
                log.debug("프롬프트 생성 시작: userId={}, title={}", userId, request.getTitle());

                Future<PromptResponseDto> future = aiCallTaskExecutorWithSecurityContext.submit(
                        () -> promptCreationFlow.create(request, userId)
                );

                PromptResponseDto result = future.get(futureTimeoutMs, TimeUnit.MILLISECONDS);

                log.info("프롬프트 생성 완료: userId={}, promptId={}", userId, result.getId());
                
                ResponseEntity<CustomResponse<PromptResponseDto>> response = CustomResponseHelper.created(result);
                
                if (log.isDebugEnabled()) {
                    SecurityContext context = SecurityContextHolder.getContext();
                    log.debug("[SecurityContext] 응답 생성 완료 - Thread: {}, hasAuth: {}", 
                            Thread.currentThread().getName(),
                            context.getAuthentication() != null);
                }
                
                return response;

            } catch (ApiException e) {
                log.warn("프롬프트 생성 비즈니스 실패: userId={}, errorCode={}", userId, e.getErrorCode(), e);
                return createFailResponse(e);

            } catch (TimeoutException e) {
                log.warn("프롬프트 생성 Future 타임아웃: userId={}, timeout={}ms", userId, futureTimeoutMs, e);
                return createFailResponse(new ApiException(
                        ErrorCode.AI_GENERATION_FAILED,
                        "프롬프트 생성이 시간 초과되었습니다. 잠시 후 다시 시도해주세요."
                ));

            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof ApiException apiException) {
                    log.warn("프롬프트 생성 비즈니스 실패: userId={}, errorCode={}", userId, apiException.getErrorCode(), e);
                    return createFailResponse(apiException);
                }
                log.error("프롬프트 생성 실행 실패: userId={}", userId, e);
                return createFailResponse(new ApiException(ErrorCode.AI_GENERATION_FAILED));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("프롬프트 생성 인터럽트: userId={}", userId, e);
                return createFailResponse(new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));

            } catch (Exception e) {
                log.error("프롬프트 생성 시스템 실패: userId={}", userId, e);
                return createFailResponse(new ApiException(ErrorCode.AI_GENERATION_FAILED));
            }
        };

        WebAsyncTask<ResponseEntity<CustomResponse<PromptResponseDto>>> asyncTask =
                new WebAsyncTask<>(timeoutMs, callable);

        asyncTask.onTimeout(() -> {
            log.warn("프롬프트 생성 WebAsyncTask 타임아웃: userId={}, timeout={}ms", userId, timeoutMs);
            return createFailResponse(new ApiException(
                    ErrorCode.AI_GENERATION_FAILED,
                    "프롬프트 생성이 시간 초과되었습니다. 잠시 후 다시 시도해주세요."
            ));
        });

        asyncTask.onError(() -> {
            log.error("프롬프트 생성 WebAsyncTask 에러: userId={}", userId);
            return createFailResponse(new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
        });

        return asyncTask;
    }

    private ResponseEntity<CustomResponse<PromptResponseDto>> createFailResponse(ApiException e) {
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(CustomResponse.fail(e));
    }
}

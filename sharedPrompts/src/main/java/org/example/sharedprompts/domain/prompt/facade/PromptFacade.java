package org.example.sharedprompts.domain.prompt.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptFacade {

    private final PromptCreationFlow promptCreationFlow;

    @Qualifier("aiCallTaskExecutor")
    private final Executor aiCallTaskExecutor;

    /**
     * Controller에서 바로 사용할 수 있는 WebAsyncTask 래퍼
     *
     * Controller에서는 이 메서드만 호출하면 되고,
     * 내부에서 CompletableFuture, 타임아웃, 예외 처리 모두 통합
     * 
     * 동작 원리:
     * 1. WebAsyncTask를 사용하여 Tomcat 요청 스레드를 즉시 반환
     * 2. 별도 스레드에서 AI 호출 및 프롬프트 생성 수행
     * 3. 타임아웃 설정으로 무한 대기 방지
     * 4. 예외 발생 시 CustomResponse 구조 유지
     */
    public WebAsyncTask<ResponseEntity<CustomResponse<PromptResponseDto>>> createPromptAsyncWeb(
            PromptRequestDto request,
            Long userId
    ) {
        long timeoutMs = 40_000L; // AI 호출 최대 + 여유

        WebAsyncTask<ResponseEntity<CustomResponse<PromptResponseDto>>> asyncTask = 
                new WebAsyncTask<>(timeoutMs, () -> {
                    try {
                        // 비동기 실행
                        CompletableFuture<PromptResponseDto> future = createPromptAsync(request, userId);
                        // timeout 적용, 별도 스레드에서 블로킹
                        PromptResponseDto result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                        return CustomResponseHelper.created(result);

                    } catch (TimeoutException e) {
                        log.error("프롬프트 생성 타임아웃: userId={}, timeout={}ms", userId, timeoutMs, e);
                        return convertFailResponse(new ApiException(
                                ErrorCode.AI_GENERATION_FAILED,
                                "프롬프트 생성이 시간 초과되었습니다. 잠시 후 다시 시도해주세요."
                        ));

                    } catch (Exception e) {
                        log.error("프롬프트 생성 실패: userId={}", userId, e);
                        ApiException apiException = (e.getCause() instanceof ApiException cause) ? cause :
                                new ApiException(ErrorCode.AI_GENERATION_FAILED);
                        return convertFailResponse(apiException);
                    }
                });

        // 타임아웃 핸들러: WebAsyncTask 타임아웃 발생 시 호출
        asyncTask.onTimeout(() -> {
            log.warn("WebAsyncTask 타임아웃: userId={}, timeout={}ms", userId, timeoutMs);
            return convertFailResponse(new ApiException(
                    ErrorCode.AI_GENERATION_FAILED,
                    "요청 처리 시간이 초과되었습니다. 잠시 후 다시 시도해주세요."
            ));
        });

        // 에러 핸들러: 예외 발생 시 호출
        asyncTask.onError(() -> {
            log.error("WebAsyncTask 에러 발생: userId={}", userId);
            return convertFailResponse(new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
        });

        return asyncTask;
    }

    /**
     * CustomResponseHelper.fail()은 ResponseEntity<CustomResponse<Void>>를 반환하지만,
     * 우리는 ResponseEntity<CustomResponse<PromptResponseDto>>를 반환해야 함.
     * 타입 변환 헬퍼 메서드
     */
    @SuppressWarnings("unchecked")
    private ResponseEntity<CustomResponse<PromptResponseDto>> convertFailResponse(ApiException e) {
        ResponseEntity<CustomResponse<Void>> failResponse = CustomResponseHelper.fail(e);
        // 타입 변환: CustomResponse<Void>를 CustomResponse<PromptResponseDto>로 변환
        // data가 null이므로 타입 안전함
        return (ResponseEntity<CustomResponse<PromptResponseDto>>) (ResponseEntity<?>) failResponse;
    }

    /**
     * 프롬프트 생성 (비동기)
     *
     * CompletableFuture를 사용하여 AI 호출 전용 스레드 풀에서 실행
     */
    public CompletableFuture<PromptResponseDto> createPromptAsync(
            PromptRequestDto request,
            Long userId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("프롬프트 생성 시작: userId={}, title={}", userId, request.getTitle());
                PromptResponseDto result = promptCreationFlow.create(request, userId);
                log.debug("프롬프트 생성 완료: userId={}, promptId={}", userId, result.getId());
                return result;

            } catch (Exception e) {
                log.error("프롬프트 생성 중 예외 발생: userId={}", userId, e);
                throw new RuntimeException(e);
            }
        }, aiCallTaskExecutor);
    }
}

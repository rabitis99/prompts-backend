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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptFacade {

    private final PromptCreationFlow promptCreationFlow;

    @Qualifier("aiCallTaskExecutor")
    private final ExecutorService aiCallTaskExecutor;

    /**
     * Controller에서 바로 사용할 수 있는 WebAsyncTask 래퍼
     *
     * Controller에서는 이 메서드만 호출하면 되고,
     * 내부에서 Future, 타임아웃, 예외 처리 모두 통합
     * 
     * 동작 원리:
     * 1. WebAsyncTask를 사용하여 Tomcat 요청 스레드를 즉시 반환
     * 2. aiCallTaskExecutor를 명시하여 bounded 스레드 풀 사용 (스레드 급증 방지)
     * 3. 별도 스레드에서 AI 호출 및 프롬프트 생성 수행
     * 4. 타임아웃 설정으로 무한 대기 방지
     * 5. 타임아웃 발생 시 Future.cancel(true)로 스레드 인터럽트 시도 (리소스 해제)
     * 6. 예외 발생 시 CustomResponse 구조 유지
     * 
     * 주의사항:
     * - Future.cancel(true)는 스레드 인터럽트를 시도하지만, 블로킹 I/O 작업(AI API 호출)은
     *   인터럽트에 응답하지 않을 수 있습니다. 이 경우 작업은 계속 실행되지만,
     *   최소한 스레드 풀 리소스 해제를 위해 취소를 시도합니다.
     */
    public WebAsyncTask<ResponseEntity<CustomResponse<PromptResponseDto>>> createPromptAsyncWeb(
            PromptRequestDto request,
            Long userId
    ) {
        long timeoutMs = 40_000L; // AI 호출 최대 + 여유

        // Future를 외부에서 참조할 수 있도록 AtomicReference로 저장
        // (람다 내부에서 final 변수만 접근 가능하므로)
        // ExecutorService.submit()을 사용하여 Future를 얻고, 이를 통해 더 나은 취소 지원
        AtomicReference<Future<PromptResponseDto>> futureRef = new AtomicReference<>();

        Callable<ResponseEntity<CustomResponse<PromptResponseDto>>> callable = () -> {
                    try {
                        // ExecutorService.submit()을 사용하여 Future를 얻음
                        // Future.cancel(true)는 CompletableFuture.cancel()보다 더 효과적임
                        Future<PromptResponseDto> future = aiCallTaskExecutor.submit(() -> {
                            try {
                                log.debug("프롬프트 생성 시작: userId={}, title={}", userId, request.getTitle());
                                PromptResponseDto result = promptCreationFlow.create(request, userId);
                                log.debug("프롬프트 생성 완료: userId={}, promptId={}", userId, result.getId());
                                return result;
                            } catch (Exception e) {
                                log.error("프롬프트 생성 중 예외 발생: userId={}", userId, e);
                                throw new RuntimeException(e);
                            }
                        });
                        futureRef.set(future); // 참조 저장
                        
                        // timeout 적용, 별도 스레드에서 블로킹
                        PromptResponseDto result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                        return CustomResponseHelper.created(result);

                    } catch (TimeoutException e) {
                        log.error("프롬프트 생성 타임아웃: userId={}, timeout={}ms", userId, timeoutMs, e);
                        
                        // 타임아웃 발생 시 Future 취소 시도
                        // Future.cancel(true)는 스레드 인터럽트를 시도하여 CompletableFuture.cancel()보다 효과적
                        // 주의: 블로킹 I/O 작업(AI API 호출)은 인터럽트에 응답하지 않을 수 있음
                        Future<PromptResponseDto> future = futureRef.get();
                        if (future != null && !future.isDone()) {
                            boolean cancelled = future.cancel(true);
                            log.warn("Future 취소 시도: userId={}, cancelled={}, isDone={}", 
                                    userId, cancelled, future.isDone());
                            
                            // 취소가 성공했는지 확인하고 로깅
                            if (cancelled) {
                                log.debug("Future가 성공적으로 취소되었습니다: userId={}", userId);
                            } else {
                                log.warn("Future 취소 실패 (이미 완료되었거나 취소 불가능): userId={}", userId);
                            }
                        }
                        
                        return convertFailResponse(new ApiException(
                                ErrorCode.AI_GENERATION_FAILED,
                                "프롬프트 생성이 시간 초과되었습니다. 잠시 후 다시 시도해주세요."
                        ));

                    } catch (Exception e) {
                        log.error("프롬프트 생성 실패: userId={}", userId, e);
                        
                        // 예외 발생 시에도 Future 취소 시도
                        Future<PromptResponseDto> future = futureRef.get();
                        if (future != null && !future.isDone()) {
                            future.cancel(true);
                        }
                        
                        ApiException apiException = (e.getCause() instanceof ApiException cause) ? cause :
                                new ApiException(ErrorCode.AI_GENERATION_FAILED);
                        return convertFailResponse(apiException);
                    }
                };

        // WebAsyncTask 생성: 타임아웃과 callable 설정
        // 주의: WebAsyncTask의 callable 실행은 Spring의 기본 AsyncTaskExecutor를 사용하지만,
        // 실제 AI 작업은 aiCallTaskExecutor.submit()을 통해 bounded 스레드 풀에서 실행됨
        // 따라서 future.get()의 블로킹은 발생하지만, 실제 작업은 bounded 풀에서 관리됨
        WebAsyncTask<ResponseEntity<CustomResponse<PromptResponseDto>>> asyncTask = 
                new WebAsyncTask<>(timeoutMs, callable);

        // 타임아웃 핸들러: WebAsyncTask 타임아웃 발생 시 호출
        asyncTask.onTimeout(() -> {
            log.warn("WebAsyncTask 타임아웃: userId={}, timeout={}ms", userId, timeoutMs);
            
            // 타임아웃 발생 시 Future 취소 시도
            Future<PromptResponseDto> future = futureRef.get();
            if (future != null && !future.isDone()) {
                boolean cancelled = future.cancel(true);
                log.warn("WebAsyncTask 타임아웃 시 Future 취소 시도: userId={}, cancelled={}, isDone={}", 
                        userId, cancelled, future.isDone());
                
                if (cancelled) {
                    log.debug("WebAsyncTask 타임아웃 시 Future가 성공적으로 취소되었습니다: userId={}", userId);
                } else {
                    log.warn("WebAsyncTask 타임아웃 시 Future 취소 실패: userId={}", userId);
                }
            }
            
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
     * 
     * 참고: 이 메서드는 외부 호출을 위해 CompletableFuture를 반환하지만,
     * 내부적으로는 createPromptAsyncWeb()에서 ExecutorService.submit()을 사용하여
     * 더 나은 취소 지원을 제공합니다.
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
                if (e instanceof ApiException) {
                    throw (ApiException) e;
                }
                throw new RuntimeException(e);
            }
        }, aiCallTaskExecutor);
    }
}

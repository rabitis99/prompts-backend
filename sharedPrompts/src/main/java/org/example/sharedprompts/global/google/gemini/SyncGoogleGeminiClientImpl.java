package org.example.sharedprompts.global.google.gemini;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.google.gemini.service.GoogleGeminiService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 리액티브 GoogleGeminiService를 감싸 동기 API를 제공하는 어댑터.
 * 
 * 책임 분리:
 * - AI 호출 책임: GoogleGeminiService에 위임
 * - 스레드 관리 책임: aiCallTaskExecutor에 위임
 * - 동기/비동기 변환 책임: 이 클래스가 담당
 * 
 * 운영 원칙:
 * - Mono.block() 사용 금지: Tomcat 요청 스레드를 block 하지 않음
 * - 별도 스레드 풀(aiCallTaskExecutor)에서 실행하여 Tomcat 스레드 풀 보호
 * - 타임아웃 설정으로 무한 대기 방지
 * 
 * 장애 시나리오:
 * - AI 호출 지연/타임아웃: 별도 스레드 풀에서 격리되어 Tomcat 스레드 풀에 영향 없음
 * - 스레드 풀 포화: RejectedExecutionException 발생 → Circuit Breaker Fallback 처리
 * - AI 호출 실패: GoogleGeminiService의 Circuit Breaker가 Fallback 메시지 반환
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SyncGoogleGeminiClientImpl implements SyncGoogleGeminiClient {

    /**
     * AI 호출 타임아웃 (초)
     * GoogleGeminiProperties.timeoutSeconds + 여유 시간(5초)
     */
    private static final long AI_CALL_TIMEOUT_SECONDS = 35;

    private final GoogleGeminiService googleGeminiService;
    
    /**
     * AI 호출 전용 스레드 풀
     * Tomcat 요청 스레드를 보호하기 위해 별도 스레드 풀 사용
     */
    @Qualifier("aiCallTaskExecutor")
    private final Executor aiCallTaskExecutor;

    /**
     * 동기 방식으로 AI 호출을 수행합니다.
     * 
     * 구현 전략:
     * 1. Mono를 CompletableFuture로 변환
     * 2. 별도 스레드 풀에서 실행
     * 3. Future.get()으로 결과 대기 (타임아웃 설정)
     * 
     * Tomcat 스레드 풀 보호:
     * - aiCallTaskExecutor에서 실행하므로 Tomcat 요청 스레드는 즉시 반환
     * - Future.get()은 별도 스레드에서 대기하므로 Tomcat 스레드 풀에 영향 없음
     * 
     * @param prompt AI에 전달할 프롬프트
     * @return AI 생성 결과
     * @throws ApiException AI 호출 실패 시 (타임아웃, 네트워크 오류 등)
     */
    @Override
    public String chatSync(String prompt) {
        try {
            // 1. Mono를 CompletableFuture로 변환
            CompletableFuture<String> future = monoToCompletableFuture(
                    googleGeminiService.chat(prompt)
            );
            
            // 2. 별도 스레드 풀에서 실행하고 결과 대기 (타임아웃 설정)
            return future.get(AI_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
        } catch (TimeoutException e) {
            log.error("Google Gemini 호출 타임아웃: timeout={}s, promptLength={}", 
                    AI_CALL_TIMEOUT_SECONDS, prompt != null ? prompt.length() : 0, e);
            throw new ApiException(ErrorCode.AI_GENERATION_FAILED, "AI 호출이 타임아웃되었습니다.");
            
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // 스레드 풀 포화 시 Circuit Breaker Fallback이 처리하도록 예외 전파
            log.error("AI call task executor pool is saturated. Circuit Breaker will provide fallback.", e);
            throw new ApiException(ErrorCode.AI_GENERATION_FAILED, "AI 호출 서비스가 과부하 상태입니다.");
            
        } catch (Exception e) {
            log.error("Google Gemini 동기 호출 실패: {}", e.getMessage(), e);
            throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    /**
     * Mono를 CompletableFuture로 변환합니다.
     * 
     * 변환 전략:
     * - Mono.subscribe()를 별도 스레드 풀에서 실행
     * - 결과를 CompletableFuture에 설정
     * - 에러 발생 시 CompletableFuture에 예외 설정
     * 
     * @param mono 변환할 Mono
     * @return CompletableFuture
     */
    private CompletableFuture<String> monoToCompletableFuture(Mono<String> mono) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        // 별도 스레드 풀에서 Mono 실행
        aiCallTaskExecutor.execute(() -> {
            mono.subscribe(
                    result -> {
                        // 성공 시 결과 설정
                        future.complete(result);
                    },
                    error -> {
                        // 에러 시 예외 설정
                        future.completeExceptionally(error);
                    }
            );
        });
        
        return future;
    }
}



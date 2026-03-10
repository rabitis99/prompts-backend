package org.example.sharedprompts.global.async;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.exception.ApiException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.util.concurrent.Callable;

/**
 * 비동기 웹 요청을 위한 공통 Executor
 * 타임아웃 및 예외 처리(로깅)를 일관되게 적용합니다.
 */
@Slf4j
@Component
public class AsyncWebTaskExecutor {

    private static final long DEFAULT_TIMEOUT_MS = 60_000L;

    public <T> WebAsyncTask<T> execute(String operation, Long userId, Callable<T> callable) {
        return execute(DEFAULT_TIMEOUT_MS, operation, userId, callable);
    }

    public <T> WebAsyncTask<T> execute(long timeoutMs, String operation, Long userId, Callable<T> callable) {
        Callable<T> wrappedCallable = () -> {
            log.info("[{}] 시작 userId={}", operation, userId);
            try {
                T result = callable.call();
                log.info("[{}] 성공 userId={}", operation, userId);
                return result;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[{}] 인터럽트됨 userId={}", operation, userId);
                throw e;
            } catch (ApiException e) {
                log.warn("[{}] 비즈니스 예외 userId={}, code={}", operation, userId, e.getErrorCode());
                throw e; // 예외를 숨기지 않고 GlobalExceptionHandler로 전파
            } catch (Exception e) {
                log.error("[{}] 시스템 오류 userId={}", operation, userId, e);
                throw e; // GlobalExceptionHandler로 전파되어 INTERNAL_SERVER_ERROR 처리됨
            }
        };

        WebAsyncTask<T> task = new WebAsyncTask<>(timeoutMs, wrappedCallable);

        task.onTimeout(() -> {
            log.warn("[{}] 타임아웃 userId={}, timeout={}ms", operation, userId, timeoutMs);
            // 글로벌 핸들러에서 일관된 타임아웃 예외로 처리
            throw new AsyncRequestTimeoutException();
        });

        return task;
    }
}

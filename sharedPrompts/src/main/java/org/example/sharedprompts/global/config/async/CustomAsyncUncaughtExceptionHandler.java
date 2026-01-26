package org.example.sharedprompts.global.config.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.config.async.metrics.AsyncMetricsService;
import org.example.sharedprompts.global.config.async.util.AsyncParamFormatter;
import org.example.sharedprompts.global.config.async.util.CriticalMethodChecker;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

/**
 * 비동기 메서드에서 발생한 예외를 처리하는 핸들러
 * 
 * <p>단일 책임: 예외 처리 오케스트레이션만 담당
 * 
 * <p>주요 기능:
 * <ul>
 *   <li>에러 로깅 (상세 정보 포함)</li>
 *   <li>메트릭 기록 (AsyncMetricsService 위임)</li>
 *   <li>중요 비즈니스 로직 실패 시 알람 발송 (CriticalMethodChecker + AsyncExceptionNotifier)</li>
 *   <li>민감 정보 마스킹 처리 (AsyncParamFormatter 위임)</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class CustomAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {
    
    private final AsyncMetricsService asyncMetricsService;
    private final AsyncExceptionNotifier asyncExceptionNotifier;
    private final CriticalMethodChecker criticalMethodChecker;
    
    /**
     * Handles uncaught exceptions thrown by asynchronous methods by logging the failure,
     * recording a failure metric, and sending a notification if the method is considered critical.
     *
     * If recording metrics or sending notifications fails, those failures are logged and not propagated.
     *
     * @param ex the uncaught exception thrown by the asynchronous method
     * @param method the reflected Method instance where the exception originated
     * @param params the arguments that were passed to the asynchronous method
     */
    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        String methodName = AsyncParamFormatter.formatMethodName(method);
        String formattedParams = AsyncParamFormatter.formatParams(params);
        
        // 1. 에러 로깅 (상세 정보 포함)
        log.error("Async method execution failed: method={}, params={}", 
            methodName, formattedParams, ex);
        
        // 2. 메트릭 기록 (모니터링) - 위임
        try {
            asyncMetricsService.recordFailure(method, ex);
        }catch (Exception metricEx) {
            log.warn("Async metrics recording failed: method={}", methodName, metricEx);
        }

        // 3. 중요 비즈니스 로직 실패 시 알람 발송 - 위임
        if (criticalMethodChecker.isCritical(method)) {
            try {
                asyncExceptionNotifier.notify(ex, method, formattedParams);
            }catch (Exception notifyEx) {
                log.warn("Async alert notification failed: method={}", methodName, notifyEx);
            }
        }
        
        // 4. Dead Letter Queue 또는 재시도 큐에 추가 (선택적)
        // asyncExceptionQueue.enqueue(methodName, params, ex);
    }
}

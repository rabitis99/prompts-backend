package org.example.sharedprompts.global.config.async.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

/**
 * 비동기 메서드 실행 관련 메트릭을 기록하는 서비스
 * 
 * <p>단일 책임: 비동기 실행 메트릭 기록만 담당
 */
@Service
@RequiredArgsConstructor
public class AsyncMetricsService {
    
    private static final String METRIC_NAME_FAILURE = "async.execution.failure";
    private static final String TAG_METHOD = "method";
    private static final String TAG_EXCEPTION = "exception";
    
    private final MeterRegistry meterRegistry;
    
    /**
     * 비동기 메서드 실행 실패 메트릭 기록
     * 
     * @param method 실행된 메서드
     * @param exception 발생한 예외
     */
    public void recordFailure(Method method, Throwable exception) {
        String methodName = formatMethodName(method);
        String exceptionName = exception.getClass().getSimpleName();
        
        meterRegistry.counter(METRIC_NAME_FAILURE,
            TAG_METHOD, methodName,
            TAG_EXCEPTION, exceptionName
        ).increment();
    }
    
    /**
     * 메서드 이름을 포맷팅
     * 클래스명.메서드명 형태로 반환
     */
    private String formatMethodName(Method method) {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }
}


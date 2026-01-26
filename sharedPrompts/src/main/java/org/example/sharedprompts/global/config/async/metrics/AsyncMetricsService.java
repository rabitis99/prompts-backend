package org.example.sharedprompts.global.config.async.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.config.async.util.AsyncParamFormatter;
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
     * Record a metric counter for a failed asynchronous method execution.
     *
     * If `method` or `exception` is null, the corresponding tag value "unknown" is used.
     *
     * @param method    the executed Method, or null to record the method tag as "unknown"
     * @param exception the Throwable that occurred, or null to record the exception tag as "unknown"
     */
    public void recordFailure(Method method, Throwable exception) {
        String methodName = method != null ? AsyncParamFormatter.formatMethodName(method) : "unknown";
        String exceptionName = exception != null ? exception.getClass().getSimpleName() : "unknown";

        meterRegistry.counter(METRIC_NAME_FAILURE,
            TAG_METHOD, methodName,
            TAG_EXCEPTION, exceptionName
        ).increment();
    }
}

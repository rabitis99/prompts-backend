package org.example.sharedprompts.global.config.async;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.config.async.util.AsyncParamFormatter;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 비동기 예외 알람 발송 구현체
 * 
 * <p>현재는 로그만 기록하며, 향후 Slack, PagerDuty 등 외부 알람 시스템과 연동 가능
 * 
 * <p>실제 알람 시스템 연동 시:
 * <ul>
 *   <li>Slack Webhook 연동</li>
 *   <li>PagerDuty API 연동</li>
 *   <li>이메일 발송</li>
 *   <li>SNS/SQS를 통한 알람 큐 발행</li>
 * </ul>
 */
@Slf4j
@Component
public class AsyncExceptionNotifierImpl implements AsyncExceptionNotifier {
    
    /**
     * Notify about an exception thrown by an asynchronous method.
     *
     * Logs a critical alert containing the target method name, exception type, and a masked exception message.
     *
     * @param ex the exception that was thrown
     * @param method the reflected method where the exception occurred
     * @param params the arguments that were passed to the method invocation
     */
    @Override
    public void notify(Throwable ex, Method method, Object... params) {
        String methodName = AsyncParamFormatter.formatMethodName(method);
        String safeMessage = SensitiveDataMasker.mask(ex.getMessage());
        // 현재는 로그만 기록
        // TODO: 실제 알람 시스템 연동 (Slack, PagerDuty 등)
        log.error("🚨 CRITICAL: Async method failure detected - method={}, exception={}, message={}", 
            methodName,
            ex.getClass().getSimpleName(),
                safeMessage);
        
        // 예시: Slack Webhook 연동
        // slackNotifier.sendAlert("Async method failure", methodName, ex);
        
        // 예시: PagerDuty 연동
        // pagerDutyClient.triggerIncident("Async method failure", methodName, ex);
    }
}

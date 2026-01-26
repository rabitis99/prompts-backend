package org.example.sharedprompts.global.config.async;

import lombok.extern.slf4j.Slf4j;
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
    
    @Override
    public void notify(Throwable ex, Method method, Object... params) {
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        
        // 현재는 로그만 기록
        // TODO: 실제 알람 시스템 연동 (Slack, PagerDuty 등)
        log.error("🚨 CRITICAL: Async method failure detected - method={}, exception={}, message={}", 
            methodName, 
            ex.getClass().getSimpleName(),
            ex.getMessage());
        
        // 예시: Slack Webhook 연동
        // slackNotifier.sendAlert("Async method failure", methodName, ex);
        
        // 예시: PagerDuty 연동
        // pagerDutyClient.triggerIncident("Async method failure", methodName, ex);
    }
}


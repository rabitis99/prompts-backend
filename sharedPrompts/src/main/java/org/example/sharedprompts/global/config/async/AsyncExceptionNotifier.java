package org.example.sharedprompts.global.config.async;

import java.lang.reflect.Method;

/**
 * 비동기 예외 알람 발송 인터페이스
 * 
 * <p>중요한 비즈니스 로직의 비동기 실행 실패 시 알람을 발송합니다.
 * 구현체에서 Slack, PagerDuty, 이메일 등 다양한 알람 채널을 지원할 수 있습니다.
 */
public interface AsyncExceptionNotifier {
    
    /**
 * Notify about a failure that occurred during asynchronous method execution.
 *
 * Sends an alert containing the thrown exception, the reflected method that failed,
 * and the invocation arguments so implementations can route the notification to appropriate channels.
 *
 * @param ex the exception that was thrown during execution
 * @param method the reflective Method that was invoked when the exception occurred
 * @param params the arguments passed to the method invocation
 */
    void notify(Throwable ex, Method method, Object... params);
}

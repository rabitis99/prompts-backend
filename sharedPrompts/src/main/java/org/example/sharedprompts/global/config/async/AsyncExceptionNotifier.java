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
     * 비동기 메서드 실행 실패 알람 발송
     * 
     * @param ex 발생한 예외
     * @param method 실행된 메서드
     * @param params 메서드 파라미터
     */
    void notify(Throwable ex, Method method, Object... params);
}


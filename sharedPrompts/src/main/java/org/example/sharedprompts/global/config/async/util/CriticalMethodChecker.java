package org.example.sharedprompts.global.config.async.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 중요 비즈니스 로직 메서드 판단
 *
 * <p>설정 기반으로 중요 메서드를 판단하여 알람 발송 여부를 결정합니다.
 * application.yml에서 critical-method-patterns로 설정 가능합니다.
 */
@Component
@ConfigurationProperties(prefix = "async.exception")
@Getter
@Setter
public class CriticalMethodChecker {

    /**
     * 중요 메서드 패턴 목록
     * 메서드 이름(클래스명.메서드명)에 이 패턴이 포함되면 중요 메서드로 판단
     */
    private List<String> criticalMethodPatterns = List.of(
            "AuthEventListener",
            "Payment",
            "Audit",
            "RateLimitLogBatchService"
    );

    /**
     * Determines whether the given reflected method matches any configured critical method pattern.
     *
     * A method is considered critical when its declaring class simple name and method name
     * concatenated as "ClassName.methodName" exactly equals any entry in {@code criticalMethodPatterns}.
     *
     * @param method the reflected method to check
     * @return {@code true} if the method is considered critical, {@code false} otherwise
     */
    public boolean isCritical(Method method) {
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        return criticalMethodPatterns.stream()
                .anyMatch(pattern -> methodName.equals(pattern));
    }
}
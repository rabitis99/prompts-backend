package org.example.sharedprompts.global.config.async;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
    private List<String> criticalMethodPatterns = new ArrayList<>();
    
    @PostConstruct
    public void init() {
        // 설정이 없으면 기본 패턴 사용
        if (criticalMethodPatterns.isEmpty()) {
            criticalMethodPatterns = List.of(
                "AuthEventListener",
                "Payment",
                "Audit",
                "RateLimitLogBatchService"
            );
        }
    }
    
    /**
     * 주어진 메서드가 중요 비즈니스 로직인지 판단
     * 
     * @param method 판단할 메서드
     * @return 중요 메서드이면 true
     */
    public boolean isCritical(Method method) {
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        return criticalMethodPatterns.stream()
            .anyMatch(pattern -> methodName.contains(pattern));
    }
}


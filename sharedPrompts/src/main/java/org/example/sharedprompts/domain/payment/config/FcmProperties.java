package org.example.sharedprompts.domain.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * FCM 설정 Properties (Immutable)
 *
 * <p>payment.enabled=true일 때만 빈이 로드됩니다.
 * 결제 기능이 비활성화된 환경에서는 FCM 관련 환경변수 없이도 부팅 가능.
 */
@Getter
@Component
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class FcmProperties {

    private final String projectId;
    private final String credentialsPath;
    private final String classpathResource;
    private final boolean enabled;

    public FcmProperties(
            @Value("${payment.fcm.project-id:}") String projectId,
            @Value("${payment.fcm.credentials-path:}") String credentialsPath,
            @Value("${payment.fcm.classpath-resource:firebase/firebase-adminsdk.json}") String classpathResource,
            @Value("${payment.fcm.enabled:true}") boolean enabled) {
        this.projectId = projectId;
        this.credentialsPath = credentialsPath;
        this.classpathResource = classpathResource;
        this.enabled = enabled;
    }
}






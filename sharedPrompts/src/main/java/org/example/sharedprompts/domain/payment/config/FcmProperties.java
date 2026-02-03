package org.example.sharedprompts.domain.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * FCM 설정 Properties (Immutable)
 */
@Getter
@Component
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





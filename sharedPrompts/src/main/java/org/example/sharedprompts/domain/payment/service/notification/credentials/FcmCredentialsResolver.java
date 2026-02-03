package org.example.sharedprompts.domain.payment.service.notification.credentials;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.FcmProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.WillNotClose;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
@Slf4j
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class FcmCredentialsResolver {

    @WillNotClose
    public InputStream resolve(FcmProperties props) throws IOException {

        // 1. 환경 변수
        String envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (envPath != null && !envPath.isBlank()) {
            log.debug("GOOGLE_APPLICATION_CREDENTIALS 사용: {}", envPath);
            return new FileInputStream(envPath);
        }

        // 2. application.yml 설정
        if (props.getCredentialsPath() != null &&
                !props.getCredentialsPath().isBlank()) {

            log.debug("payment.fcm.credentials-path 사용: {}", props.getCredentialsPath());
            return new FileInputStream(props.getCredentialsPath());
        }

        // 3. classpath (설정된 경로 또는 기본값)
        String classpathResource = props.getClasspathResource();
        if (classpathResource == null || classpathResource.isBlank()) {
            classpathResource = "firebase/firebase-adminsdk.json";
        }
        
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(classpathResource);

        if (stream != null) {
            log.debug("classpath firebase credentials 사용: {}", classpathResource);
            return stream;
        }

        // 4. fallback
        throw new IllegalStateException("FCM credentials를 찾을 수 없습니다.");
    }
}


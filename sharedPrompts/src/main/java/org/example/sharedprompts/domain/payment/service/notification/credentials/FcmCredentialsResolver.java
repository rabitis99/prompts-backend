package org.example.sharedprompts.domain.payment.service.notification.credentials;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
@Slf4j
public class FcmCredentialsResolver {

    public InputStream resolve(PaymentProperties props) throws IOException {

        // 1. 환경 변수
        String envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (envPath != null && !envPath.isBlank()) {
            log.debug("GOOGLE_APPLICATION_CREDENTIALS 사용: {}", envPath);
            return new FileInputStream(envPath);
        }

        // 2. application.yml 설정
        if (props.getFcmCredentialsPath() != null &&
                !props.getFcmCredentialsPath().isBlank()) {

            log.debug("payment.fcm.credentials-path 사용: {}", props.getFcmCredentialsPath());
            return new FileInputStream(props.getFcmCredentialsPath());
        }

        // 3. classpath (firebase/)
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("firebase/sharedprompt-8ed9d-firebase-adminsdk-fbsvc-601af9062b.json");

        if (stream != null) {
            log.debug("classpath firebase credentials 사용");
            return stream;
        }

        // 4. fallback
        throw new IllegalStateException("FCM credentials를 찾을 수 없습니다.");
    }
}


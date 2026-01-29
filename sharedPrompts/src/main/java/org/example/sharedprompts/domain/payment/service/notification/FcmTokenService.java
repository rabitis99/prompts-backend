package org.example.sharedprompts.domain.payment.service.notification;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
import org.example.sharedprompts.domain.payment.service.notification.credentials.FcmCredentialsResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/**
 * FCM HTTP v1 API용 OAuth2 Access Token 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final PaymentProperties paymentProperties;
    private final FcmCredentialsResolver credentialsResolver;

    /** 캐시된 Google OAuth2 Credentials */
    private GoogleCredentials credentials;

    /** 토큰 만료 시각 (여유 포함) */
    private long tokenExpirationTime = 0;

    /**
     * 유효한 FCM Access Token 반환
     */
    public String getAccessToken() {
        try {
            if (credentials == null || isTokenExpired()) {
                synchronized (this) {
                    if (credentials == null || isTokenExpired()) {
                        refreshCredentials();
                    }
                }
            }
            return credentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            throw new RuntimeException("FCM 액세스 토큰 발급 실패", e);
        }
    }

    /**
     * Credentials 로딩 및 토큰 갱신
     */
    private void refreshCredentials() throws IOException {
        if (!paymentProperties.isFcmEnabled()) {
            throw new IllegalStateException("FCM이 비활성화되어 있음");
        }

        try (InputStream credentialsStream =
                     credentialsResolver.resolve(paymentProperties)) {

            credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(
                            Collections.singletonList(
                                    "https://www.googleapis.com/auth/firebase.messaging"
                            )
                    );

            tokenExpirationTime = System.currentTimeMillis() + (55 * 60 * 1000);
        }
    }

    /** 토큰 만료 여부 */
    private boolean isTokenExpired() {
        return System.currentTimeMillis() >= tokenExpirationTime;
    }
}

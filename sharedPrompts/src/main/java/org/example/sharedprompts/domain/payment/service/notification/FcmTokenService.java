package org.example.sharedprompts.domain.payment.service.notification;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.FcmProperties;
import org.example.sharedprompts.domain.payment.service.notification.credentials.FcmCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * FCM HTTP v1 API용 OAuth2 Access Token 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmProperties fcmProperties;
    private final FcmCredentialsProvider credentialsProvider;

    /** 캐시된 Google OAuth2 Credentials */
    private volatile GoogleCredentials credentials;

    /** 토큰 만료 시각 (여유 포함) */
    private volatile long tokenExpirationTime = 0;

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
        if (!fcmProperties.isEnabled()) {
            throw new IllegalStateException("FCM이 비활성화되어 있음");
        }

        // FcmCredentialsProvider가 이미 스코핑된 GoogleCredentials를 반환
        credentials = credentialsProvider.load();
        
        // 실제 토큰을 가져오기 위해 refresh() 호출 필요
        // GoogleCredentials.fromStream().createScoped()는 credentials 객체만 생성하고
        // 실제 토큰을 가져오지 않으므로 refresh()를 호출해야 함
        credentials.refresh();

        tokenExpirationTime = System.currentTimeMillis() + (55 * 60 * 1000);
    }

    /** 토큰 만료 여부 */
    private boolean isTokenExpired() {
        return System.currentTimeMillis() >= tokenExpirationTime;
    }
}

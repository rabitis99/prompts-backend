package org.example.sharedprompts.domain.payment.service.notification;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/**
 * FCM HTTP v1 API용 OAuth2 토큰 생성 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final PaymentProperties paymentProperties;
    private GoogleCredentials credentials;
    private long tokenExpirationTime = 0;

    /**
     * 유효한 액세스 토큰을 반환합니다.
     * 토큰이 만료되었거나 없으면 새로 발급받습니다.
     */
    public String getAccessToken() {
        try {
            if (credentials == null || isTokenExpired()) {
                refreshCredentials();
            }
            
            // 토큰 새로고침 (필요한 경우)
            credentials.refreshIfExpired();
            
            return credentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            log.error("FCM 액세스 토큰 발급 실패: {}", e.getMessage(), e);
            throw new RuntimeException("FCM 액세스 토큰 발급 실패", e);
        }
    }

    /**
     * Google Credentials를 초기화하거나 새로고침합니다.
     * 우선순위:
     * 1. 환경 변수 GOOGLE_APPLICATION_CREDENTIALS
     * 2. application.yml의 payment.fcm.credentials-path
     * 3. 클래스 경로의 credentials 파일
     */
    private void refreshCredentials() throws IOException {
        if (!paymentProperties.isFcmEnabled()) {
            throw new IllegalStateException("FCM이 비활성화되어 있음");
        }

        InputStream credentialsStream = null;
        try {
            // 1. 환경 변수 GOOGLE_APPLICATION_CREDENTIALS 확인
            String envCredentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            if (envCredentialsPath != null && !envCredentialsPath.isEmpty()) {
                log.debug("환경 변수 GOOGLE_APPLICATION_CREDENTIALS 사용: {}", envCredentialsPath);
                credentialsStream = new FileInputStream(envCredentialsPath);
            }
            // 2. application.yml의 credentials-path 확인
            else if (paymentProperties.getFcmCredentialsPath() != null && 
                     !paymentProperties.getFcmCredentialsPath().isEmpty()) {
                log.debug("설정 파일의 credentials-path 사용: {}", paymentProperties.getFcmCredentialsPath());
                credentialsStream = new FileInputStream(paymentProperties.getFcmCredentialsPath());
            }
            // 3. 클래스 경로에서 찾기
            else {
                // 먼저 firebase 폴더에서 찾기
                credentialsStream = getClass().getClassLoader().getResourceAsStream("firebase/sharedprompt-8ed9d-firebase-adminsdk-fbsvc-601af9062b.json");
                if (credentialsStream != null) {
                    log.debug("클래스 경로의 firebase/sharedprompt-8ed9d-firebase-adminsdk-fbsvc-601af9062b.json 사용");
                } else {
                    // 대체로 fcm-credentials.json 찾기
                    credentialsStream = getClass().getClassLoader().getResourceAsStream("fcm-credentials.json");
                    if (credentialsStream != null) {
                        log.debug("클래스 경로의 fcm-credentials.json 사용");
                    }
                }
                
                if (credentialsStream == null) {
                    // 4. GoogleCredentials.getApplicationDefault() 시도 (GCP 환경에서 자동 감지)
                    try {
                        credentials = GoogleCredentials.getApplicationDefault()
                                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"));
                        tokenExpirationTime = System.currentTimeMillis() + (55 * 60 * 1000);
                        log.debug("GoogleCredentials.getApplicationDefault() 사용");
                        return;
                    } catch (IOException e) {
                        throw new IllegalStateException(
                            "FCM credentials를 찾을 수 없습니다. " +
                            "환경 변수 GOOGLE_APPLICATION_CREDENTIALS, " +
                            "application.yml의 payment.fcm.credentials-path, " +
                            "또는 클래스 경로의 firebase/sharedprompt-8ed9d-firebase-adminsdk-fbsvc-601af9062b.json 중 하나를 설정해주세요.", e);
                    }
                }
            }
            
            // GoogleCredentials 생성 (FCM 스코프 포함)
            credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"));
            
            // 토큰 만료 시간 설정 (일반적으로 1시간)
            tokenExpirationTime = System.currentTimeMillis() + (55 * 60 * 1000); // 55분 후 만료로 설정
            
            log.debug("FCM Credentials 초기화 완료");
        } finally {
            if (credentialsStream != null) {
                try {
                    credentialsStream.close();
                } catch (IOException e) {
                    log.warn("Credentials 스트림 닫기 실패: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 토큰이 만료되었는지 확인합니다.
     */
    private boolean isTokenExpired() {
        return System.currentTimeMillis() >= tokenExpirationTime;
    }
}


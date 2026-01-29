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
     * Provide a valid access token for the FCM HTTP v1 API.
     *
     * Ensures credentials are available and refreshed if necessary, then returns the token value.
     *
     * @return the access token string used to authenticate FCM HTTP v1 requests
     * @throws RuntimeException if a token cannot be obtained or refreshed
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
     * Initialize or refresh the GoogleCredentials used to obtain FCM access tokens.
     *
     * Attempts to load credentials in the following order: (1) the GOOGLE_APPLICATION_CREDENTIALS
     * environment variable, (2) the configured payment.fcm.credentials-path, (3) classpath credential
     * files (firebase/sharedprompt-8ed9d-firebase-adminsdk-fbsvc-601af9062b.json or fcm-credentials.json),
     * and finally (4) Application Default Credentials when running in a GCP environment.
     *
     * @throws IllegalStateException if FCM is disabled or no credentials can be located from any source
     * @throws IOException if an I/O error occurs while reading credential files or streams
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
     * Check whether the cached access token is expired.
     *
     * @return `true` if the current system time is greater than or equal to the token expiration timestamp, `false` otherwise.
     */
    private boolean isTokenExpired() {
        return System.currentTimeMillis() >= tokenExpirationTime;
    }
}

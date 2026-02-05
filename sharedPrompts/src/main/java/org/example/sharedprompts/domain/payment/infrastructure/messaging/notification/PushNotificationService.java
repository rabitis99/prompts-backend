package org.example.sharedprompts.domain.payment.infrastructure.messaging.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.properties.FcmProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.example.sharedprompts.global.util.SensitiveDataMasker.maskToken;

/**
 * 푸시 알림 서비스
 * FCM (Firebase Cloud Messaging) HTTP v1 API를 사용하여 푸시 알림 발송
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class PushNotificationService {

    private final RestTemplate restTemplate;
    private final FcmProperties fcmProperties;
    private final FcmTokenService fcmTokenService;

    /**
     * 결제 실패 푸시 알림 발송
     */
    public void sendPaymentFailurePush(String deviceToken, Long paymentId, String reason) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", "결제 실패");
            notification.put("body", String.format("결제 ID: %d, 사유: %s", paymentId, reason));

            Map<String, Object> data = new HashMap<>();
            data.put("paymentId", String.valueOf(paymentId));
            data.put("type", "PAYMENT_FAILED");
            data.put("reason", reason);

            sendPushNotification(deviceToken, notification, data);

            log.info("결제 실패 푸시 알림 발송 완료: deviceToken={}, paymentId={}", maskToken(deviceToken), paymentId);
        } catch (Exception e) {
            log.error("결제 실패 푸시 알림 발송 실패: deviceToken={}, paymentId={}, error={}", maskToken(deviceToken), paymentId, e.getMessage(), e);
        }
    }

    /**
     * 결제 성공 푸시 알림 발송
     */
    public void sendPaymentSuccessPush(String deviceToken, Long paymentId, String amount) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", "결제 완료");
            notification.put("body", String.format("결제 ID: %d, 금액: %s원", paymentId, amount));

            Map<String, Object> data = new HashMap<>();
            data.put("paymentId", String.valueOf(paymentId));
            data.put("type", "PAYMENT_SUCCESS");
            data.put("amount", amount);

            sendPushNotification(deviceToken, notification, data);
            
            log.info("결제 성공 푸시 알림 발송 완료: deviceToken={}, paymentId={}", maskToken(deviceToken), paymentId);
        } catch (Exception e) {
            log.error("결제 성공 푸시 알림 발송 실패: deviceToken={}, paymentId={}, error={}", maskToken(deviceToken), paymentId, e.getMessage(), e);
        }
    }

    private void sendPushNotification(String deviceToken, Map<String, Object> notification, Map<String, Object> data) {
        // FCM이 비활성화되어 있거나 프로젝트 ID가 없으면 로그만 남기고 종료
        if (!fcmProperties.isEnabled() || 
            fcmProperties.getProjectId() == null || 
            fcmProperties.getProjectId().isEmpty()) {
            log.debug("FCM이 비활성화되어 있거나 프로젝트 ID가 설정되지 않음. 푸시 알림 발송 건너뜀: deviceToken={}", maskToken(deviceToken));
            return;
        }

        try {
            // FCM HTTP v1 API 요청 본문 구성
            Map<String, Object> message = new HashMap<>();
            message.put("token", deviceToken);
            
            // notification 필드 구성
            Map<String, String> notificationPayload = new HashMap<>();
            notificationPayload.put("title", String.valueOf(notification.get("title")));
            notificationPayload.put("body", String.valueOf(notification.get("body")));
            message.put("notification", notificationPayload);
            
            // data 필드 구성 (모든 값은 문자열이어야 함)
            Map<String, String> stringData = new HashMap<>();
            data.forEach((key, value) -> stringData.put(key, String.valueOf(value)));
            message.put("data", stringData);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", message);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // OAuth2 액세스 토큰 사용
            String accessToken = fcmTokenService.getAccessToken();
            headers.setBearerAuth(accessToken);
            
            // FCM HTTP v1 API 엔드포인트
            String projectId = fcmProperties.getProjectId();
            String url = String.format("https://fcm.googleapis.com/v1/projects/%s/messages:send", projectId);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            
            if (response != null && response.containsKey("name")) {
                log.debug("푸시 알림 발송 성공: deviceToken={}, messageId={}", maskToken(deviceToken), response.get("name"));
            } else {
                log.warn("푸시 알림 발송 응답 이상: deviceToken={}, response={}", maskToken(deviceToken), response);
            }
        } catch (Exception e) {
            log.error("푸시 알림 발송 실패: deviceToken={}, error={}", maskToken(deviceToken), e.getMessage(), e);
            // 푸시 알림 실패는 결제 프로세스를 중단시키지 않도록 예외를 다시 던지지 않음
        }
    }
}


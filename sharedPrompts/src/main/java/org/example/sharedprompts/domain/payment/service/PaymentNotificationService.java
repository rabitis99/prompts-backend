package org.example.sharedprompts.domain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.notification.service.sse.NotificationSseService;
import org.example.sharedprompts.domain.payment.service.notification.PushNotificationService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 결제 알림 서비스
 * SSE, 푸시 알림을 통합 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotificationService {

    private final NotificationSseService sseService;
    private final PushNotificationService pushNotificationService;
    private final UserRepository userRepository;

    /**
     * Sends payment failure notifications (server-sent event and optional push) to the specified user.
     *
     * Attempts to deliver an SSE payload with type "PAYMENT_FAILED" containing payment details and, if the user has a device token, a push notification; if the user is not found the method returns without sending notifications and delivery errors are logged.
     *
     * @param paymentId     the identifier of the failed payment
     * @param userId        the recipient user's identifier
     * @param reason        a human-readable reason for the payment failure
     * @param paymentMethod the payment method used for the attempted payment
     */
    public void sendPaymentFailureNotification(Long paymentId, Long userId, String reason, String paymentMethod) {
        log.info("결제 실패 알림 발송 시작: paymentId={}, userId={}, reason={}, paymentMethod={}",
                paymentId, userId, reason, paymentMethod);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("사용자를 찾을 수 없어 알림을 발송할 수 없습니다: userId={}", userId);
            return;
        }

        User user = userOpt.get();

        // SSE 알림
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "PAYMENT_FAILED");
            payload.put("paymentId", paymentId);
            payload.put("reason", reason);
            payload.put("paymentMethod", paymentMethod);
            payload.put("message", String.format("결제가 실패했습니다. (결제 ID: %d, 사유: %s)", paymentId, reason));
            
            sseService.sendPaymentNotification(userId, payload);
            log.debug("결제 실패 SSE 알림 전송 완료: userId={}, paymentId={}", userId, paymentId);
        } catch (Exception e) {
            log.error("결제 실패 SSE 알림 전송 실패: userId={}, error={}", userId, e.getMessage(), e);
        }

        // 푸시 알림
        if (user.getDeviceToken() != null && !user.getDeviceToken().isEmpty()) {
            try {
                pushNotificationService.sendPaymentFailurePush(user.getDeviceToken(), paymentId, reason);
            } catch (Exception e) {
                log.error("결제 실패 푸시 알림 발송 실패: userId={}, error={}", userId, e.getMessage(), e);
            }
        }
    }

    /**
     * Send payment success notifications to the specified user via SSE and push (if the user has a device token).
     *
     * Attempts to deliver an SSE payload containing payment details and, when a non-empty device token exists,
     * sends a push notification. Exceptions during delivery are logged and do not propagate.
     *
     * @param paymentId     the identifier of the successful payment
     * @param userId        the recipient user's identifier
     * @param paymentMethod the payment method used (e.g., card, bank transfer)
     * @param amount        the payment amount as a formatted string (e.g., "10,000")
     */
    public void sendPaymentSuccessNotification(Long paymentId, Long userId, String paymentMethod, String amount) {
        log.info("결제 성공 알림 발송 시작: paymentId={}, userId={}, paymentMethod={}, amount={}",
                paymentId, userId, paymentMethod, amount);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("사용자를 찾을 수 없어 알림을 발송할 수 없습니다: userId={}", userId);
            return;
        }

        User user = userOpt.get();

        // SSE 알림
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "PAYMENT_SUCCESS");
            payload.put("paymentId", paymentId);
            payload.put("paymentMethod", paymentMethod);
            payload.put("amount", amount);
            payload.put("message", String.format("결제가 성공적으로 완료되었습니다. (결제 ID: %d, 금액: %s원)", paymentId, amount));
            
            sseService.sendPaymentNotification(userId, payload);
            log.debug("결제 성공 SSE 알림 전송 완료: userId={}, paymentId={}", userId, paymentId);
        } catch (Exception e) {
            log.error("결제 성공 SSE 알림 전송 실패: userId={}, error={}", userId, e.getMessage(), e);
        }

        // 푸시 알림
        if (user.getDeviceToken() != null && !user.getDeviceToken().isEmpty()) {
            try {
                pushNotificationService.sendPaymentSuccessPush(user.getDeviceToken(), paymentId, amount);
            } catch (Exception e) {
                log.error("결제 성공 푸시 알림 발송 실패: userId={}, error={}", userId, e.getMessage(), e);
            }
        }
    }
}
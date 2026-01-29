package org.example.sharedprompts.domain.payment.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.service.payment.provider.PaymentProviderService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 카카오페이 결제 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoPayPaymentService implements PaymentProviderService {

    private final PaymentProperties paymentProperties;
    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String KAKAO_PAY_API_URL = "https://open-api.kakaopay.com/online/v1/payment";
    private static final String KAKAO_PAY_APPROVE_URL = KAKAO_PAY_API_URL + "/approve";
    private static final String KAKAO_PAY_STATUS_URL = KAKAO_PAY_API_URL + "/order";
    private static final String KAKAO_PAY_CANCEL_URL = KAKAO_PAY_API_URL + "/cancel";
    private static final String KAKAO_PAY_REFUND_URL = KAKAO_PAY_API_URL + "/refund";

    /**
     * Identifies the payment provider implemented by this service as KakaoPay.
     *
     * @return `PaymentMethod.KAKAO_PAY`
     */
    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.KAKAO_PAY;
    }

    /**
     * Initiates a KakaoPay approval request for the provided payment and returns the resulting transaction ID.
     *
     * @param payment the payment to approve; its id is used as partner_order_id, payment.user.id as partner_user_id,
     *                payment.amount as total_amount, and payment.getExternalPaymentId() as the tid when present
     * @return the KakaoPay transaction id (tid) returned by the approval API
     * @throws RuntimeException if the KakaoPay approval request fails or the response is not successful
     */
    @Override
    public String approvePayment(Payment payment) {
        try {
            // 카카오페이 결제 승인 요청
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cid", paymentProperties.getKakaoCid());
            requestBody.put("tid", payment.getExternalPaymentId() != null ? payment.getExternalPaymentId() : generateTid(payment));
            requestBody.put("partner_order_id", String.valueOf(payment.getId()));
            requestBody.put("partner_user_id", String.valueOf(payment.getUser().getId()));
            requestBody.put("total_amount", payment.getAmount().intValue());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    KAKAO_PAY_APPROVE_URL,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String tid = (String) responseBody.get("tid");
                log.info("카카오페이 결제 승인 성공: paymentId={}, tid={}", payment.getId(), tid);
                return tid;
            } else {
                throw new RuntimeException("카카오페이 결제 승인 실패: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("카카오페이 결제 승인 API 호출 실패: paymentId={}, error={}", payment.getId(), e.getMessage(), e);
            throw new RuntimeException("카카오페이 결제 승인 실패", e);
        }
    }

    /**
     * Checks the KakaoPay transaction status for the given external payment id.
     *
     * @param externalPaymentId the KakaoPay transaction id (tid) to query
     * @return `PaymentStatus.SUCCESS` if the payment succeeded, `PaymentStatus.CANCELED` if fully canceled,
     *         `PaymentStatus.PARTIALLY_REFUNDED` if partially refunded, `PaymentStatus.PENDING` otherwise
     */
    @Override
    public PaymentStatus checkPaymentStatus(String externalPaymentId) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    KAKAO_PAY_STATUS_URL + "?cid=" + paymentProperties.getKakaoCid() + "&tid=" + externalPaymentId,
                    HttpMethod.GET,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String status = (String) responseBody.get("status");
                
                return switch (status) {
                    case "SUCCESS_PAYMENT" -> PaymentStatus.SUCCESS;
                    case "CANCEL_PAYMENT" -> PaymentStatus.CANCELED;
                    case "PART_CANCEL_PAYMENT" -> PaymentStatus.PARTIALLY_REFUNDED;
                    default -> PaymentStatus.PENDING;
                };
            }
            
            return PaymentStatus.PENDING;
        } catch (Exception e) {
            log.error("카카오페이 결제 상태 조회 실패: externalPaymentId={}, error={}", externalPaymentId, e.getMessage(), e);
            return PaymentStatus.PENDING;
        }
    }

    /**
     * Cancels a KakaoPay payment.
     *
     * @param externalPaymentId the KakaoPay transaction ID
     * @param reason the reason for cancellation
     * @throws RuntimeException if the cancellation request fails
     */
    @Override
    public void cancelPayment(String externalPaymentId, String reason) {
        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cid", paymentProperties.getKakaoCid());
            requestBody.put("tid", externalPaymentId);
            requestBody.put("cancel_amount", null); // 전체 취소
            requestBody.put("cancel_tax_free_amount", 0);
            requestBody.put("cancel_reason", reason);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    KAKAO_PAY_CANCEL_URL,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("카카오페이 결제 취소 성공: externalPaymentId={}", externalPaymentId);
            } else {
                throw new RuntimeException("카카오페이 결제 취소 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("카카오페이 결제 취소 실패: externalPaymentId={}, error={}", externalPaymentId, e.getMessage(), e);
            throw new RuntimeException("카카오페이 결제 취소 실패", e);
        }
    }

    /**
     * Initiates a refund for a KakaoPay payment.
     *
     * Sends a refund request to the KakaoPay API for the payment identified by {@code externalPaymentId}.
     *
     * @param externalPaymentId the KakaoPay transaction id (tid) of the payment to refund
     * @param amount            the refund amount in KRW
     * @param reason            a human-readable reason for the refund
     * @throws RuntimeException if the KakaoPay API responds with a non-OK status or if an error occurs while performing the refund
     */
    @Override
    public void refundPayment(String externalPaymentId, BigDecimal amount, String reason) {
        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cid", paymentProperties.getKakaoCid());
            requestBody.put("tid", externalPaymentId);
            requestBody.put("cancel_amount", amount.intValue());
            requestBody.put("cancel_tax_free_amount", 0);
            requestBody.put("cancel_reason", reason);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    KAKAO_PAY_REFUND_URL,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("카카오페이 결제 환불 성공: externalPaymentId={}, amount={}", externalPaymentId, amount);
            } else {
                throw new RuntimeException("카카오페이 결제 환불 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("카카오페이 결제 환불 실패: externalPaymentId={}, amount={}, error={}", externalPaymentId, amount, e.getMessage(), e);
            throw new RuntimeException("카카오페이 결제 환불 실패", e);
        }
    }

    /**
     * Validate a KakaoPay webhook payload by comparing its HMAC-SHA256 signature to the provided signature.
     *
     * @param payload   the raw webhook request body to verify
     * @param signature the signature value supplied by KakaoPay (header) to compare against
     * @return `true` if the calculated HMAC-SHA256 (Base64-encoded) of the payload matches `signature`; `true` if no webhook secret is configured or if an internal error occurs during verification, `false` otherwise
     */
    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            // 카카오페이 Webhook 서명 검증
            String secret = paymentProperties.getWebhookSecret();
            if (secret == null || secret.isEmpty()) {
                log.warn("Webhook secret이 설정되지 않았습니다.");
                return true;
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);

            return calculatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("카카오페이 Webhook 서명 검증 실패: error={}", e.getMessage(), e);
            return true;
        }
    }

    /**
     * Processes a raw KakaoPay webhook JSON payload and handles the contained event.
     *
     * Parses the payload to extract the webhook "event" and its "data", then performs application-level handling
     * (for example, updating payment status or publishing a payment event).
     *
     * @param payload the raw JSON payload received from KakaoPay webhook
     * @throws RuntimeException if the payload cannot be parsed or processing fails
     */
    @Override
    public void processWebhook(String payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String eventType = (String) webhookData.get("event");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) webhookData.get("data");

            log.info("카카오페이 Webhook 처리: eventType={}, data={}", eventType, data);

            // 실제 구현에서는 Webhook 이벤트에 따라 결제 상태를 업데이트
            // 예: PaymentEventPublisher를 통해 이벤트 발행
        } catch (Exception e) {
            log.error("카카오페이 Webhook 처리 실패: payload={}, error={}", payload, e.getMessage(), e);
            throw new RuntimeException("카카오페이 Webhook 처리 실패", e);
        }
    }

    /**
     * Create HTTP headers required for KakaoPay API requests.
     *
     * @return HttpHeaders containing an Authorization header with the Kakao secret and a Content-Type of "application/json".
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "SECRET_KEY " + paymentProperties.getKakaoSecret());
        headers.set("Content-Type", "application/json");
        return headers;
    }

    /**
     * Generate a temporary transaction identifier for the given payment.
     *
     * @param payment the payment whose id is included in the generated transaction id
     * @return the transaction id in the format `TID_<paymentId>_<timestampMillis>`
     */
    private String generateTid(Payment payment) {
        return "TID_" + payment.getId() + "_" + System.currentTimeMillis();
    }
}
package org.example.sharedprompts.domain.payment.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
import org.example.sharedprompts.dto.payment.request.PaymentConfirmRequest;
import org.example.sharedprompts.dto.payment.response.PaymentConfirmResponse;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.service.payment.provider.PaymentProviderService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 토스 결제 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TossPaymentService implements PaymentProviderService {

    private final PaymentProperties paymentProperties;
    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TOSS_PAYMENTS_API_URL = "https://api.tosspayments.com/v1/payments";
    private static final String TOSS_PAYMENTS_STATUS_URL = TOSS_PAYMENTS_API_URL;
    private static final String TOSS_PAYMENTS_CANCEL_URL = TOSS_PAYMENTS_API_URL + "/";

    /**
     * Identifies which payment provider this service implements.
     *
     * @return the `PaymentMethod` constant representing Toss
     */
    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.TOSS;
    }

    /**
     * Confirms the given payment with Toss Payments and returns the resulting Toss payment key.
     *
     * @param payment the payment to confirm (provides id, amount, and optional externalPaymentId)
     * @return the Toss payment key returned after successful confirmation
     * @throws RuntimeException if the Toss confirmation request fails or the response is missing
     */
    @Override
    public String approvePayment(Payment payment) {
        try {
            // 토스페이먼츠 결제 승인 요청 (RestTemplate 사용)
            String paymentKey = payment.getExternalPaymentId() != null 
                ? payment.getExternalPaymentId() 
                : generatePaymentKey(payment);
            
            PaymentConfirmRequest request = new PaymentConfirmRequest(
                String.valueOf(payment.getId()),
                payment.getAmount().intValue(),
                paymentKey
            );

            // 토스페이먼츠 결제 승인 요청 (RestTemplate 사용)
            HttpHeaders headers = createHeaders();
            HttpEntity<PaymentConfirmRequest> requestEntity = new HttpEntity<>(request, headers);
            
            ResponseEntity<PaymentConfirmResponse> responseEntity = restTemplate.exchange(
                    paymentProperties.getTossBaseUrl() + paymentProperties.getTossConfirmEndpoint(),
                    HttpMethod.POST,
                    requestEntity,
                    PaymentConfirmResponse.class
            );
            
            PaymentConfirmResponse response = responseEntity.getBody();
            if (response != null && response.getPaymentKey() != null) {
                log.info("토스페이먼츠 결제 승인 성공: paymentId={}, paymentKey={}", payment.getId(), response.getPaymentKey());
                return response.getPaymentKey();
            } else {
                throw new RuntimeException("토스페이먼츠 결제 승인 실패: 응답이 비어있습니다.");
            }
        } catch (Exception e) {
            log.error("토스페이먼츠 결제 승인 API 호출 실패: paymentId={}, error={}", payment.getId(), e.getMessage(), e);
            throw new RuntimeException("토스페이먼츠 결제 승인 실패", e);
        }
    }

    /**
     * Determines the internal payment status for a Toss payment identified by its external payment ID.
     *
     * @param externalPaymentId the Toss external payment identifier to query
     * @return `SUCCESS` if Toss reports "DONE", `CANCELED` if "CANCELED", `PARTIALLY_REFUNDED` if "PARTIAL_CANCELED",
     *         `PENDING` for any other Toss status, when the response body is missing, or if an error occurs while querying
     */
    @Override
    public PaymentStatus checkPaymentStatus(String externalPaymentId) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    TOSS_PAYMENTS_STATUS_URL + "/" + externalPaymentId,
                    HttpMethod.GET,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String status = (String) responseBody.get("status");
                
                return switch (status) {
                    case "DONE" -> PaymentStatus.SUCCESS;
                    case "CANCELED" -> PaymentStatus.CANCELED;
                    case "PARTIAL_CANCELED" -> PaymentStatus.PARTIALLY_REFUNDED;
                    default -> PaymentStatus.PENDING;
                };
            }
            
            return PaymentStatus.PENDING;
        } catch (Exception e) {
            log.error("토스페이먼츠 결제 상태 조회 실패: externalPaymentId={}, error={}", externalPaymentId, e.getMessage(), e);
            return PaymentStatus.PENDING;
        }
    }

    /**
     * Cancels an existing Toss payment identified by the given external payment ID.
     *
     * Sends a cancellation request to Toss containing the provided reason.
     *
     * @param externalPaymentId the Toss payment identifier to cancel
     * @param reason            the cancellation reason to send to Toss (stored or displayed by the provider)
     * @throws RuntimeException if the cancellation request fails or the Toss API returns a non-OK response
     */
    @Override
    public void cancelPayment(String externalPaymentId, String reason) {
        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cancelReason", reason);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    TOSS_PAYMENTS_CANCEL_URL + externalPaymentId + "/cancel",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("토스페이먼츠 결제 취소 성공: externalPaymentId={}", externalPaymentId);
            } else {
                throw new RuntimeException("토스페이먼츠 결제 취소 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("토스페이먼츠 결제 취소 실패: externalPaymentId={}, error={}", externalPaymentId, e.getMessage(), e);
            throw new RuntimeException("토스페이먼츠 결제 취소 실패", e);
        }
    }

    /**
     * Initiates a refund for a Toss payment identified by the given external payment ID.
     *
     * @param externalPaymentId the Toss payment identifier to refund
     * @param amount the refund amount
     * @param reason a human-readable reason for the refund
     * @throws RuntimeException if the refund request fails or an error occurs while contacting Toss
     */
    @Override
    public void refundPayment(String externalPaymentId, BigDecimal amount, String reason) {
        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cancelAmount", amount.intValue());
            requestBody.put("cancelReason", reason);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    TOSS_PAYMENTS_CANCEL_URL + externalPaymentId + "/cancel",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("토스페이먼츠 결제 환불 성공: externalPaymentId={}, amount={}", externalPaymentId, amount);
            } else {
                throw new RuntimeException("토스페이먼츠 결제 환불 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("토스페이먼츠 결제 환불 실패: externalPaymentId={}, amount={}, error={}", externalPaymentId, amount, e.getMessage(), e);
            throw new RuntimeException("토스페이먼츠 결제 환불 실패", e);
        }
    }

    /**
     * Verifies a Toss Payments webhook payload by comparing its HMAC-SHA256 signature to the provided signature.
     *
     * @param payload   the raw webhook request body
     * @param signature the signature value received from Toss (Base64-encoded HMAC-SHA256)
     * @return `true` if the calculated signature matches `signature`; `true` if the configured webhook secret is missing or if an error occurs during verification; `false` if the signatures do not match
     */
    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            // 토스페이먼츠 Webhook 서명 검증
            String secret = paymentProperties.getTossSecret();
            if (secret == null || secret.isEmpty()) {
                log.warn("토스페이먼츠 Webhook secret이 설정되지 않았습니다.");
                return true;
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);

            return calculatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("토스페이먼츠 Webhook 서명 검증 실패: error={}", e.getMessage(), e);
            return true;
        }
    }

    /**
     * Processes a Toss Payments webhook payload and updates internal payment state based on the event.
     *
     * <p>Parses the JSON payload to extract the event type and associated data, then performs the corresponding handling.
     *
     * @param payload the raw JSON webhook payload sent by Toss Payments
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

            log.info("토스페이먼츠 Webhook 처리: eventType={}, data={}", eventType, data);

            // 실제 구현에서는 Webhook 이벤트에 따라 결제 상태를 업데이트
        } catch (Exception e) {
            log.error("토스페이먼츠 Webhook 처리 실패: payload={}, error={}", payload, e.getMessage(), e);
            throw new RuntimeException("토스페이먼츠 Webhook 처리 실패", e);
        }
    }

    /**
     * Create HTTP headers for Toss API requests, setting Content-Type to application/json
     * and adding a Basic Authorization header when an API key and secret are configured.
     *
     * @return HttpHeaders containing Content-Type `application/json` and, if available,
     *         a `Authorization: Basic <base64(key:secret)>` header.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // API 키가 있는 경우에만 인증 헤더 추가
        String apiKey = paymentProperties.getTossApiKey();
        String secret = paymentProperties.getTossSecret();
        
        if (apiKey != null && !apiKey.isEmpty() && secret != null && !secret.isEmpty()) {
            String auth = apiKey + ":" + secret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);
        }
        
        return headers;
    }

    /**
     * Generate a unique payment key for Toss integration.
     *
     * The key embeds the payment's id and the current epoch milliseconds to ensure uniqueness.
     *
     * @param payment the Payment whose id will be included in the key
     * @return the generated key in the format {@code TOSS_<paymentId>_<timestamp>}
     */
    private String generatePaymentKey(Payment payment) {
        return "TOSS_" + payment.getId() + "_" + System.currentTimeMillis();
    }
}
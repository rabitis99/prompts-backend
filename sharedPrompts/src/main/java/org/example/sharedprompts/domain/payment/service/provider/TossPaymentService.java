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
import java.security.MessageDigest;
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

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.TOSS;
    }

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

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            // 토스페이먼츠 Webhook 서명 검증
            String secret = paymentProperties.getTossSecret();
            if (secret == null || secret.isEmpty()) {
                log.warn("토스페이먼츠 Webhook secret이 설정되지 않았습니다.");
                return false;
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] expectedSignature = Base64.getDecoder().decode(signature);

            return MessageDigest.isEqual(hash, expectedSignature);
        } catch (Exception e) {
            log.error("토스페이먼츠 Webhook 서명 검증 실패: error={}", e.getMessage(), e);
            return false;
        }
    }

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

    private String generatePaymentKey(Payment payment) {
        return "TOSS_" + payment.getId() + "_" + System.currentTimeMillis();
    }
}

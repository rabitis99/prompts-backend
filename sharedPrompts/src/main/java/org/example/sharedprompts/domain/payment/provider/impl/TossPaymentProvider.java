package org.example.sharedprompts.domain.payment.provider.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.TossPayProperties;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.model.CancelResult;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.model.RefundResult;
import org.example.sharedprompts.domain.payment.provider.PaymentProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Toss Payments Provider 구현체
 * 
 * <p>공식 권장 흐름:
 * - POST /v1/payments/confirm 호출로 결제 승인
 * - paymentKey, orderId, amount 필요
 * - 서버에서 금액/주문번호 검증 후 최종 승인 호출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentProvider implements PaymentProvider {
    
    private static final String TOSS_PAYMENTS_API_URL = "https://api.tosspayments.com/v1/payments";
    private static final String TOSS_CONFIRM_ENDPOINT = "/confirm";
    
    private final TossPayProperties tossPayProperties;
    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.TOSS;
    }
    
    @Override
    public PaymentResult confirmPayment(
            String paymentKey,
            String orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey
    ) {
        try {
            // Toss Payments 공식 Confirm API 호출
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 멱등성 키 헤더 추가 (Toss Payments가 지원하는 경우)
            if (idempotencyKey != null) {
                headers.set("Idempotency-Key", idempotencyKey);
            }
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("paymentKey", paymentKey);
            requestBody.put("orderId", orderId);
            requestBody.put("amount", amount.longValueExact());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    TOSS_PAYMENTS_API_URL + TOSS_CONFIRM_ENDPOINT,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // PaymentResult로 변환
                return PaymentResult.builder()
                        .externalPaymentId((String) responseBody.get("paymentKey"))
                        .status(parseStatus((String) responseBody.get("status")))
                        .amount(new BigDecimal(responseBody.get("totalAmount").toString()))
                        .currency((String) responseBody.get("currency"))
                        .orderId((String) responseBody.get("orderId"))
                        .approvedAt(parseApprovedAt(responseBody))
                        .metadata(objectMapper.writeValueAsString(responseBody))
                        .build();
            } else {
                throw new RuntimeException("Toss Payments 결제 승인 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Toss Payments 결제 승인 API 호출 실패: paymentKey={}, orderId={}, error={}", 
                    paymentKey, orderId, e.getMessage(), e);
            return PaymentResult.builder()
                    .externalPaymentId(paymentKey)
                    .status(PaymentStatus.FAILED)
                    .amount(amount)
                    .currency(currency)
                    .orderId(orderId)
                    .failureReason("Toss Payments API 호출 실패: " + e.getMessage())
                    .build();
        }
    }
    
    @Override
    public PaymentResult getPaymentStatus(String externalPaymentId) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    TOSS_PAYMENTS_API_URL + "/" + externalPaymentId,
                    HttpMethod.GET,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                return PaymentResult.builder()
                        .externalPaymentId(externalPaymentId)
                        .status(parseStatus((String) responseBody.get("status")))
                        .amount(new BigDecimal(responseBody.get("totalAmount").toString()))
                        .currency((String) responseBody.get("currency"))
                        .orderId((String) responseBody.get("orderId"))
                        .approvedAt(parseApprovedAt(responseBody))
                        .metadata(objectMapper.writeValueAsString(responseBody))
                        .build();
            }
            
            return PaymentResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(PaymentStatus.PENDING)
                    .build();
        } catch (Exception e) {
            log.error("Toss Payments 결제 상태 조회 실패: externalPaymentId={}, error={}", 
                    externalPaymentId, e.getMessage(), e);
            return PaymentResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(PaymentStatus.PENDING)
                    .build();
        }
    }
    
    @Override
    public CancelResult cancelPayment(String externalPaymentId, String reason, String idempotencyKey) {
        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (idempotencyKey != null) {
                headers.set("Idempotency-Key", idempotencyKey);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cancelReason", reason);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    TOSS_PAYMENTS_API_URL + "/" + externalPaymentId + "/cancel",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("Toss Payments 결제 취소 성공: externalPaymentId={}", externalPaymentId);

                return CancelResult.builder()
                        .externalPaymentId(externalPaymentId)
                        .status(PaymentStatus.CANCELED)
                        .canceledAt(parseCanceledAt(responseBody))
                        .reason(reason)
                        .metadata(objectMapper.writeValueAsString(responseBody))
                        .build();
            } else {
                throw new RuntimeException("Toss Payments 결제 취소 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Toss Payments 결제 취소 실패: externalPaymentId={}, error={}",
                    externalPaymentId, e.getMessage(), e);
            throw new RuntimeException("Toss Payments 결제 취소 실패", e);
        }
    }

    @Override
    public RefundResult refundPayment(String externalPaymentId, BigDecimal amount, String reason, String idempotencyKey) {
        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (idempotencyKey != null) {
                headers.set("Idempotency-Key", idempotencyKey);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cancelReason", reason);
            requestBody.put("cancelAmount", amount.longValueExact());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    TOSS_PAYMENTS_API_URL + "/" + externalPaymentId + "/cancel",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("Toss Payments 결제 환불 성공: externalPaymentId={}, amount={}", externalPaymentId, amount);

                return RefundResult.builder()
                        .externalPaymentId(externalPaymentId)
                        .status(parseStatus((String) responseBody.get("status")))
                        .refundedAmount(amount)
                        .refundedAt(parseCanceledAt(responseBody))
                        .reason(reason)
                        .metadata(objectMapper.writeValueAsString(responseBody))
                        .build();
            } else {
                throw new RuntimeException("Toss Payments 결제 환불 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Toss Payments 결제 환불 실패: externalPaymentId={}, amount={}, error={}",
                    externalPaymentId, amount, e.getMessage(), e);
            throw new RuntimeException("Toss Payments 결제 환불 실패", e);
        }
    }
    
    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            String secret = tossPayProperties.getSecret();
            if (secret == null || secret.isEmpty()) {
                log.warn("Toss Payments Webhook secret이 설정되지 않았습니다.");
                return false;
            }
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] expectedSignature = Base64.getDecoder().decode(signature);
            
            return MessageDigest.isEqual(hash, expectedSignature);
        } catch (Exception e) {
            log.error("Toss Payments Webhook 서명 검증 실패: error={}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public PaymentProvider.WebhookEvent parseWebhook(String payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String eventType = (String) webhookData.get("event");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) webhookData.get("data");
            
            String paymentKey = data != null ? (String) data.get("paymentKey") : null;
            String orderId = data != null ? (String) data.get("orderId") : null;
            
            PaymentResult paymentResult = null;
            if (data != null) {
                paymentResult = PaymentResult.builder()
                        .externalPaymentId(paymentKey)
                        .status(parseStatus((String) data.get("status")))
                        .orderId(orderId)
                        .metadata(objectMapper.writeValueAsString(data))
                        .build();
            }
            
            return new PaymentProvider.WebhookEvent(eventType, paymentKey, orderId, paymentResult);
        } catch (Exception e) {
            log.error("Toss Payments Webhook 파싱 실패: payloadSize={}, error={}",
                    payload != null ? payload.length() : 0, e.getMessage(), e);
            throw new RuntimeException("Toss Payments Webhook 파싱 실패", e);
        }
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String secret = tossPayProperties.getSecret();
        if (secret != null && !secret.isEmpty()) {
            String auth = secret + ":";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);
        } else {
            log.warn("Toss Payments API secret이 설정되지 않았습니다.");
        }
        
        return headers;
    }
    
    private PaymentStatus parseStatus(String status) {
        if (status == null) {
            return PaymentStatus.PENDING;
        }
        
        return switch (status) {
            case "DONE" -> PaymentStatus.SUCCESS;
            case "CANCELED" -> PaymentStatus.CANCELED;
            case "PARTIAL_CANCELED" -> PaymentStatus.PARTIALLY_REFUNDED;
            case "ABORTED", "EXPIRED" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }
    
    private LocalDateTime parseApprovedAt(Map<String, Object> responseBody) {
        try {
            Object approvedAtObj = responseBody.get("approvedAt");
            if (approvedAtObj != null) {
                String approvedAtStr = approvedAtObj.toString();
                // ISO 8601 형식 파싱
                return OffsetDateTime.parse(approvedAtStr, DateTimeFormatter.ISO_DATE_TIME)
                        .toLocalDateTime();
            }
        } catch (Exception e) {
            log.warn("승인 시간 파싱 실패: {}", e.getMessage());
        }
        return null;
    }

    private LocalDateTime parseCanceledAt(Map<String, Object> responseBody) {
        try {
            Object canceledAtObj = responseBody.get("canceledAt");
            if (canceledAtObj != null) {
                String canceledAtStr = canceledAtObj.toString();
                return OffsetDateTime.parse(canceledAtStr, DateTimeFormatter.ISO_DATE_TIME)
                        .toLocalDateTime();
            }
        } catch (Exception e) {
            log.warn("취소 시간 파싱 실패: {}", e.getMessage());
        }
        return null; // 파싱 실패 시 null 반환
    }
}


package org.example.sharedprompts.domain.payment.provider.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
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
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * KakaoPay Provider 구현체
 * 
 * <p>공식 권장 흐름:
 * 1) 결제 준비: POST /online/v1/payment/ready → tid, 리다이렉션 URL 획득
 * 2) 사용자가 인증/결제 진행 후 서버에서 받은 pg_token과 함께
 *    POST /online/v1/payment/approve → approve 호출 완료
 * 
 * <p>ready 단계에서 받은 tid 저장 후 approve 호출
 * 상태/멱등성은 DB + Webhook/approve 결과 반영
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoPayPaymentProvider implements PaymentProvider {
    
    private static final String KAKAO_PAY_API_URL = "https://open-api.kakaopay.com/online/v1/payment";
    private static final String KAKAO_PAY_APPROVE_ENDPOINT = "/approve";
    
    private final PaymentProperties paymentProperties;
    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.KAKAO_PAY;
    }
    
    @Override
    public PaymentResult confirmPayment(
            String paymentKey, // KakaoPay에서는 tid 또는 pg_token
            String orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey
    ) {
        try {
            // KakaoPay는 approve API 호출
            // paymentKey는 tid, 추가로 pg_token이 필요할 수 있음
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cid", paymentProperties.getKakaoCid());
            requestBody.put("tid", paymentKey); // tid는 ready 단계에서 받은 값
            requestBody.put("partner_order_id", orderId);
            requestBody.put("partner_user_id", orderId); // 사용자 ID는 orderId로 대체 가능
            requestBody.put("total_amount", amount.setScale(0, RoundingMode.HALF_UP).longValue());
            
            // pg_token이 있으면 추가 (사용자 인증 후 받은 토큰)
            if (paymentKey.contains("pg_token=")) {
                String pgToken = paymentKey.substring(paymentKey.indexOf("pg_token=") + 9);
                requestBody.put("pg_token", pgToken);
            }
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    KAKAO_PAY_API_URL + KAKAO_PAY_APPROVE_ENDPOINT,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String tid = (String) responseBody.get("tid");
                
                return PaymentResult.builder()
                        .externalPaymentId(tid)
                        .status(parseStatus((String) responseBody.get("status")))
                        .amount(new BigDecimal(responseBody.get("amount").toString()))
                        .currency("KRW") // KakaoPay는 기본적으로 KRW
                        .orderId((String) responseBody.get("partner_order_id"))
                        .approvedAt(LocalDateTime.now())
                        .metadata(objectMapper.writeValueAsString(responseBody))
                        .build();
            } else {
                throw new RuntimeException("KakaoPay 결제 승인 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("KakaoPay 결제 승인 API 호출 실패: paymentKey={}, orderId={}, error={}", 
                    paymentKey, orderId, e.getMessage(), e);
            return PaymentResult.builder()
                    .externalPaymentId(paymentKey)
                    .status(PaymentStatus.FAILED)
                    .amount(amount)
                    .currency(currency)
                    .orderId(orderId)
                    .failureReason("KakaoPay API 호출 실패: " + e.getMessage())
                    .build();
        }
    }
    
    @Override
    public PaymentResult getPaymentStatus(String externalPaymentId) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    KAKAO_PAY_API_URL + "/order?cid=" + paymentProperties.getKakaoCid() + "&tid=" + externalPaymentId,
                    HttpMethod.GET,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                return PaymentResult.builder()
                        .externalPaymentId(externalPaymentId)
                        .status(parseStatus((String) responseBody.get("status")))
                        .amount(new BigDecimal(responseBody.get("amount").toString()))
                        .currency("KRW")
                        .orderId((String) responseBody.get("partner_order_id"))
                        .metadata(objectMapper.writeValueAsString(responseBody))
                        .build();
            }
            
            return PaymentResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(PaymentStatus.PENDING)
                    .build();
        } catch (Exception e) {
            log.error("KakaoPay 결제 상태 조회 실패: externalPaymentId={}, error={}", 
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

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cid", paymentProperties.getKakaoCid());
            requestBody.put("tid", externalPaymentId);
            requestBody.put("cancel_amount", null); // 전체 취소
            requestBody.put("cancel_tax_free_amount", 0);
            requestBody.put("cancel_reason", reason);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    KAKAO_PAY_API_URL + "/cancel",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("KakaoPay 결제 취소 성공: externalPaymentId={}", externalPaymentId);

                return CancelResult.builder()
                        .externalPaymentId(externalPaymentId)
                        .status(PaymentStatus.CANCELED)
                        .canceledAt(LocalDateTime.now())
                        .reason(reason)
                        .metadata(objectMapper.writeValueAsString(responseBody))
                        .build();
            } else {
                throw new RuntimeException("KakaoPay 결제 취소 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("KakaoPay 결제 취소 실패: externalPaymentId={}, error={}",
                    externalPaymentId, e.getMessage(), e);
            throw new RuntimeException("KakaoPay 결제 취소 실패", e);
        }
    }
    
    @Override
    public RefundResult refundPayment(String externalPaymentId, BigDecimal amount, String reason, String idempotencyKey) {
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
                    KAKAO_PAY_API_URL + "/cancel",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("KakaoPay 결제 환불 성공: externalPaymentId={}, amount={}", externalPaymentId, amount);

                // 응답에서 실제 환불 금액 추출 (canceled_amount 필드)
                BigDecimal refundedAmount = amount;
                if (responseBody.get("canceled_amount") != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> canceledAmount = (Map<String, Object>) responseBody.get("canceled_amount");
                    if (canceledAmount.get("total") != null) {
                        refundedAmount = new BigDecimal(canceledAmount.get("total").toString());
                    }
                }

                return RefundResult.builder()
                        .externalPaymentId(externalPaymentId)
                        .status(PaymentStatus.PARTIALLY_REFUNDED)
                        .refundedAmount(refundedAmount)
                        .refundedAt(LocalDateTime.now())
                        .reason(reason)
                        .metadata(objectMapper.writeValueAsString(responseBody))
                        .build();
            } else {
                throw new RuntimeException("KakaoPay 결제 환불 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("KakaoPay 결제 환불 실패: externalPaymentId={}, amount={}, error={}",
                    externalPaymentId, amount, e.getMessage(), e);
            throw new RuntimeException("KakaoPay 결제 환불 실패", e);
        }
    }
    
    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            String secret = paymentProperties.getWebhookSecret();
            if (secret == null || secret.isEmpty()) {
                log.warn("KakaoPay Webhook secret이 설정되지 않았습니다.");
                return false;
            }
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);
            
            return calculatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("KakaoPay Webhook 서명 검증 실패: error={}", e.getMessage(), e);
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
            
            String tid = data != null ? (String) data.get("tid") : null;
            String orderId = data != null ? (String) data.get("partner_order_id") : null;
            
            PaymentResult paymentResult = null;
            if (data != null) {
                paymentResult = PaymentResult.builder()
                        .externalPaymentId(tid)
                        .status(parseStatus((String) data.get("status")))
                        .orderId(orderId)
                        .metadata(objectMapper.writeValueAsString(data))
                        .build();
            }
            
            return new PaymentProvider.WebhookEvent(eventType, tid, orderId, paymentResult);
        } catch (Exception e) {
            log.error("KakaoPay Webhook 파싱 실패: payload={}, error={}", payload, e.getMessage(), e);
            throw new RuntimeException("KakaoPay Webhook 파싱 실패", e);
        }
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "SECRET_KEY " + paymentProperties.getKakaoSecret());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
    
    private PaymentStatus parseStatus(String status) {
        if (status == null) {
            return PaymentStatus.PENDING;
        }
        
        return switch (status) {
            case "SUCCESS_PAYMENT" -> PaymentStatus.SUCCESS;
            case "CANCEL_PAYMENT" -> PaymentStatus.CANCELED;
            case "PART_CANCEL_PAYMENT" -> PaymentStatus.PARTIALLY_REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }
}


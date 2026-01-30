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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 페이팔 결제 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalPaymentService implements PaymentProviderService {

    private final PaymentProperties paymentProperties;
    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PAYPAL_API_URL = "https://api-m.paypal.com";
    private static final String PAYPAL_OAUTH_URL = PAYPAL_API_URL + "/v1/oauth2/token";
    private static final String PAYPAL_ORDERS_URL = PAYPAL_API_URL + "/v2/checkout/orders";

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.PAYPAL;
    }

    @Override
    public String approvePayment(Payment payment) {
        try {
            // 페이팔 액세스 토큰 획득
            String accessToken = getAccessToken();

            // 페이팔 주문 생성 및 승인
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("intent", "CAPTURE");
            
            Map<String, Object> purchaseUnits = new HashMap<>();
            Map<String, Object> amount = new HashMap<>();
            amount.put("currency_code", payment.getCurrency());
            amount.put("value", payment.getAmount().toString());
            purchaseUnits.put("amount", amount);
            
            requestBody.put("purchase_units", new Object[]{purchaseUnits});

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_ORDERS_URL,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<>() {
                    }
            );

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String orderId = (String) responseBody.get("id");
                
                // 주문 캡처 (결제 승인)
                captureOrder(orderId, accessToken);
                
                log.info("페이팔 결제 승인 성공: paymentId={}, orderId={}", payment.getId(), orderId);
                return orderId;
            } else {
                throw new RuntimeException("페이팔 결제 승인 실패: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("페이팔 결제 승인 API 호출 실패: paymentId={}, error={}", payment.getId(), e.getMessage(), e);
            throw new RuntimeException("페이팔 결제 승인 실패", e);
        }
    }

    @Override
    public PaymentStatus checkPaymentStatus(String externalPaymentId) {
        try {
            String accessToken = getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_ORDERS_URL + "/" + externalPaymentId,
                    HttpMethod.GET,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<>() {
                    }
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String status = (String) responseBody.get("status");
                
                return switch (status) {
                    case "COMPLETED" -> PaymentStatus.SUCCESS;
                    case "CANCELLED" -> PaymentStatus.CANCELED;
                    case "PARTIALLY_REFUNDED" -> PaymentStatus.PARTIALLY_REFUNDED;
                    case "REFUNDED" -> PaymentStatus.REFUNDED;
                    default -> PaymentStatus.PENDING;
                };
            }
            
            return PaymentStatus.PENDING;
        } catch (Exception e) {
            log.error("페이팔 결제 상태 조회 실패: externalPaymentId={}, error={}", externalPaymentId, e.getMessage(), e);
            return PaymentStatus.PENDING;
        }
    }

    @Override
    public void cancelPayment(String externalPaymentId, String reason) {
        try {
            String accessToken = getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("reason", reason);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_ORDERS_URL + "/" + externalPaymentId + "/cancel",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<>() {
                    }
            );

            if (response.getStatusCode() == HttpStatus.NO_CONTENT || response.getStatusCode() == HttpStatus.OK) {
                log.info("페이팔 결제 취소 성공: externalPaymentId={}", externalPaymentId);
            } else {
                throw new RuntimeException("페이팔 결제 취소 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("페이팔 결제 취소 실패: externalPaymentId={}, error={}", externalPaymentId, e.getMessage(), e);
            throw new RuntimeException("페이팔 결제 취소 실패", e);
        }
    }

    @Override
    public void refundPayment(String externalPaymentId, BigDecimal amount, String reason) {
        try {
            String accessToken = getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 주문에서 캡처 ID와 통화 코드 조회
            CaptureInfo captureInfo = getCaptureIdAndCurrency(externalPaymentId, accessToken);
            String captureId = captureInfo.captureId();
            String currency = captureInfo.currency();

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> amountMap = new HashMap<>();
            amountMap.put("currency_code", currency);
            amountMap.put("value", amount.toString());
            requestBody.put("amount", amountMap);
            requestBody.put("note_to_payer", reason);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_API_URL + "/v2/payments/captures/" + captureId + "/refund",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<>() {
                    }
            );

            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                log.info("페이팔 결제 환불 성공: externalPaymentId={}, amount={}", externalPaymentId, amount);
            } else {
                throw new RuntimeException("페이팔 결제 환불 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("페이팔 결제 환불 실패: externalPaymentId={}, amount={}, error={}", externalPaymentId, amount, e.getMessage(), e);
            throw new RuntimeException("페이팔 결제 환불 실패", e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            // 페이팔 Webhook 서명 검증
            String secret = paymentProperties.getWebhookSecret();
            if (secret == null || secret.isEmpty()) {
                log.warn("페이팔 Webhook secret이 설정되지 않았습니다.");
                return true;
            }

            // 페이팔은 특정 알고리즘으로 서명 검증
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);

            return calculatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("페이팔 Webhook 서명 검증 실패: error={}", e.getMessage(), e);
            return true;
        }
    }

    @Override
    public void processWebhook(String payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String eventType = (String) webhookData.get("event_type");
            @SuppressWarnings("unchecked")
            Map<String, Object> resource = (Map<String, Object>) webhookData.get("resource");

            log.info("페이팔 Webhook 처리: eventType={}, resource={}", eventType, resource);

            // 실제 구현에서는 Webhook 이벤트에 따라 결제 상태를 업데이트
        } catch (Exception e) {
            log.error("페이팔 Webhook 처리 실패: payload={}, error={}", payload, e.getMessage(), e);
            throw new RuntimeException("페이팔 Webhook 처리 실패", e);
        }
    }

    private String getAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(paymentProperties.getPaypalClientId(), paymentProperties.getPaypalClientSecret());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                PAYPAL_OAUTH_URL,
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<>() {
                }
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        }

        throw new RuntimeException("페이팔 액세스 토큰 획득 실패");
    }

    private void captureOrder(String orderId, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                PAYPAL_ORDERS_URL + "/" + orderId + "/capture",
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<>() {
                }
        );
        if (response.getStatusCode() != HttpStatus.CREATED && response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("페이팔 주문 캡처 실패: " + response.getStatusCode());
        }
    }

    /**
     * PayPal 주문에서 캡처 ID와 통화 코드를 조회
     */
    private CaptureInfo getCaptureIdAndCurrency(String orderId, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                PAYPAL_ORDERS_URL + "/" + orderId,
                HttpMethod.GET,
                request,
                new org.springframework.core.ParameterizedTypeReference<>() {
                }
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> purchaseUnits = ((List<Map<String, Object>>) response.getBody().get("purchase_units")).get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> payments = (Map<String, Object>) purchaseUnits.get("payments");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> captures = (List<Map<String, Object>>) payments.get("captures");
            if (!captures.isEmpty()) {
                Map<String, Object> capture = captures.get(0);
                String captureId = (String) capture.get("id");
                
                // 캡처 객체에서 통화 코드 추출
                @SuppressWarnings("unchecked")
                Map<String, Object> amount = (Map<String, Object>) capture.get("amount");
                String currency = (String) amount.get("currency_code");
                
                if (currency == null || currency.isEmpty()) {
                    throw new RuntimeException("페이팔 캡처에서 통화 코드를 찾을 수 없습니다");
                }
                
                return new CaptureInfo(captureId, currency);
            }
        }

        throw new RuntimeException("페이팔 캡처 ID 조회 실패");
    }

    /**
     * 캡처 ID와 통화 코드를 담는 레코드
     */
    private record CaptureInfo(String captureId, String currency) {}
}

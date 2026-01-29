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

    /**
     * Identify the payment method supported by this service.
     *
     * @return the supported PaymentMethod PAYPAL
     */
    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.PAYPAL;
    }

    /**
     * Creates a PayPal order for the given payment, captures the order, and returns the PayPal order ID.
     *
     * @param payment the internal Payment containing the amount and currency to be charged
     * @return the PayPal order ID for the captured order
     * @throws RuntimeException if order creation, capture, or the PayPal API call fails
     */
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
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
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

    /**
     * Maps a PayPal order's remote status to the application's PaymentStatus.
     *
     * @param externalPaymentId the PayPal order ID to query
     * @return {@code PaymentStatus.SUCCESS} for PayPal "COMPLETED", {@code PaymentStatus.CANCELED} for "CANCELLED",
     *         {@code PaymentStatus.PARTIALLY_REFUNDED} for "PARTIALLY_REFUNDED", {@code PaymentStatus.REFUNDED} for "REFUNDED",
     *         and {@code PaymentStatus.PENDING} for any other status or when the status cannot be determined
     */
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
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
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

    /**
     * Cancels the PayPal order identified by the given external payment ID using the provided reason.
     *
     * @param externalPaymentId the PayPal order ID to cancel
     * @param reason             a textual reason for the cancellation (may be null or empty)
     * @throws RuntimeException if the cancellation request fails or an error occurs while calling PayPal
     */
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
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
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

    /**
     * Issue a refund for a captured PayPal payment identified by an external payment ID.
     *
     * @param externalPaymentId the PayPal order ID used to locate the capture to refund
     * @param amount            the refund amount in KRW
     * @param reason            a note explaining the reason for the refund to the payer
     * @throws RuntimeException if the capture ID cannot be retrieved, the refund request fails, or any other error occurs during the refund process
     */
    @Override
    public void refundPayment(String externalPaymentId, BigDecimal amount, String reason) {
        try {
            String accessToken = getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 주문에서 캡처 ID 조회
            String captureId = getCaptureId(externalPaymentId, accessToken);

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> amountMap = new HashMap<>();
            amountMap.put("currency_code", "KRW");
            amountMap.put("value", amount.toString());
            requestBody.put("amount", amountMap);
            requestBody.put("note_to_payer", reason);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_API_URL + "/v2/payments/captures/" + captureId + "/refund",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
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

    /**
     * Validates a PayPal webhook payload against the configured webhook secret.
     *
     * @param payload   the raw webhook request body to verify
     * @param signature the Base64-encoded signature provided with the webhook
     * @return {@code true} if the webhook secret is not configured or if the signature matches; {@code false} if the signature does not match
     */
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

    /**
     * Process a PayPal webhook JSON payload and act on the event.
     *
     * Parses the raw JSON payload, extracts the `event_type` and `resource`, and performs processing (currently logs the event).
     *
     * @param payload the raw JSON webhook payload received from PayPal
     * @throws RuntimeException if the payload cannot be parsed or processing fails
     */
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

    /**
     * Retrieves an OAuth 2.0 access token from PayPal using the configured client credentials.
     *
     * @return the PayPal access token string
     * @throws RuntimeException if the token cannot be retrieved or the response is invalid
     */
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
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        }

        throw new RuntimeException("페이팔 액세스 토큰 획득 실패");
    }

    /**
     * Captures a PayPal order identified by the given orderId using the provided OAuth access token.
     *
     * @param orderId the PayPal order ID to capture
     * @param accessToken OAuth bearer token with permission to capture the order
     */
    private void captureOrder(String orderId, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        restTemplate.exchange(
                PAYPAL_ORDERS_URL + "/" + orderId + "/capture",
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
        );
    }

    /**
     * Retrieve the PayPal capture ID for the given order.
     *
     * @param orderId     the PayPal order ID to query
     * @param accessToken the OAuth bearer token used for the request
     * @return the capture ID associated with the order
     * @throws RuntimeException if the capture ID cannot be retrieved (missing response body, purchase units, or captures)
     */
    private String getCaptureId(String orderId, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                PAYPAL_ORDERS_URL + "/" + orderId,
                HttpMethod.GET,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> purchaseUnits = ((List<Map<String, Object>>) response.getBody().get("purchase_units")).get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> payments = (Map<String, Object>) purchaseUnits.get("payments");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> captures = (List<Map<String, Object>>) payments.get("captures");
            if (!captures.isEmpty()) {
                return (String) captures.get(0).get("id");
            }
        }

        throw new RuntimeException("페이팔 캡처 ID 조회 실패");
    }
}
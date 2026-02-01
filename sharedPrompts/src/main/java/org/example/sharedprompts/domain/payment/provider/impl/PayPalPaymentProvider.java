package org.example.sharedprompts.domain.payment.provider.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.PaypalProperties;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.model.CancelResult;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.model.RefundResult;
import org.example.sharedprompts.domain.payment.provider.PaymentProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PayPal Provider 구현체
 * 
 * <p>공식 권장 흐름 (Orders API 기반):
 * 1) 주문 생성: POST /v2/checkout/orders
 * 2) 사용자가 PayPal Checkout에서 승인
 * 3) 서버에서 POST /v2/checkout/orders/{orderId}/capture 호출로 결제 완료
 * 
 * <p>Webhook은 PAYMENT.CAPTURE.COMPLETED 등 이벤트로 확정 통지
 */
@Slf4j
@Component
public class PayPalPaymentProvider implements PaymentProvider {
    
    private static final String PAYPAL_API_URL = "https://api-m.paypal.com";
    private static final String PAYPAL_OAUTH_URL = PAYPAL_API_URL + "/v1/oauth2/token";
    private static final String PAYPAL_ORDERS_URL = PAYPAL_API_URL + "/v2/checkout/orders";
    
    private final PaypalProperties paypalProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Access Token 캐싱을 위한 필드
    private String cachedAccessToken;
    private long tokenExpiresAt;
    
    public PayPalPaymentProvider(
            PaypalProperties paypalProperties,
            @Qualifier("paymentRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper
    ) {
        this.paypalProperties = paypalProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.PAYPAL;
    }
    
    @Override
    public PaymentResult confirmPayment(
            String paymentKey, // PayPal에서는 orderId
            String orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey
    ) {
        try {
            // PayPal 액세스 토큰 획득
            String accessToken = getAccessToken();
            
            // PayPal Orders API - Capture 호출
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 멱등성 키 헤더 추가
            if (idempotencyKey != null) {
                headers.set("PayPal-Request-Id", idempotencyKey);
            }
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_ORDERS_URL + "/" + paymentKey + "/capture",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if ((response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) 
                    && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String orderIdFromResponse = (String) responseBody.get("id");
                
                // purchase_units에서 금액 추출
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> purchaseUnits = (List<Map<String, Object>>) responseBody.get("purchase_units");
                Map<String, Object> amountMap = null;
                if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payments = (Map<String, Object>) purchaseUnits.get(0).get("payments");
                    if (payments != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> captures = (List<Map<String, Object>>) payments.get("captures");
                        if (captures != null && !captures.isEmpty()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> amountObj = (Map<String, Object>) captures.get(0).get("amount");
                            amountMap = amountObj;
                        }
                    }
                }
                
                String currencyCode = currency;
                BigDecimal capturedAmount = amount;
                if (amountMap != null) {
                    currencyCode = (String) amountMap.get("currency_code");
                    capturedAmount = new BigDecimal(amountMap.get("value").toString());
                }
                
                return PaymentResult.builder()
                        .externalPaymentId(orderIdFromResponse)
                        .status(parseStatus((String) responseBody.get("status")))
                        .amount(capturedAmount)
                        .currency(currencyCode)
                        .orderId(orderId)
                        .approvedAt(parseApprovedAt(responseBody))
                        .metadata(objectMapper.writeValueAsString(responseBody))
                        .build();
            } else {
                throw new RuntimeException("PayPal 결제 승인 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("PayPal 결제 승인 API 호출 실패: paymentKey={}, orderId={}, error={}", 
                    paymentKey, orderId, e.getMessage(), e);
            return PaymentResult.builder()
                    .externalPaymentId(paymentKey)
                    .status(PaymentStatus.FAILED)
                    .amount(amount)
                    .currency(currency)
                    .orderId(orderId)
                    .failureReason("PayPal API 호출 실패: " + e.getMessage())
                    .build();
        }
    }
    
    @Override
    public PaymentResult getPaymentStatus(String externalPaymentId) {
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
                
                // purchase_units에서 금액 추출
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> purchaseUnits = (List<Map<String, Object>>) responseBody.get("purchase_units");
                BigDecimal orderAmount = BigDecimal.ZERO;
                String currencyCode = "USD";
                if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> amount = (Map<String, Object>) purchaseUnits.get(0).get("amount");
                    if (amount != null) {
                        currencyCode = (String) amount.get("currency_code");
                        orderAmount = new BigDecimal(amount.get("value").toString());
                    }
                }
                
                return PaymentResult.builder()
                        .externalPaymentId(externalPaymentId)
                        .status(parseStatus((String) responseBody.get("status")))
                        .amount(orderAmount)
                        .currency(currencyCode)
                        .orderId(externalPaymentId)
                        .approvedAt(parseApprovedAt(responseBody))
                        .metadata(objectMapper.writeValueAsString(responseBody))
                        .build();
            }
            
            return PaymentResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(PaymentStatus.PENDING)
                    .build();
        } catch (Exception e) {
            log.error("PayPal 결제 상태 조회 실패: externalPaymentId={}, error={}", 
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
            String accessToken = getAccessToken();
            
            // 먼저 주문 상태를 조회하여 취소 방법 결정
            OrderDetails orderDetails = getOrderDetails(externalPaymentId, accessToken);
            String orderStatus = orderDetails.status();
            
            // CREATED 또는 APPROVED 상태 (미인증/미캡처): API 호출 없이 시스템에서 취소 처리
            if ("CREATED".equals(orderStatus) || "APPROVED".equals(orderStatus)) {
                log.info("PayPal 주문 취소 (미캡처 상태): externalPaymentId={}, status={}", 
                        externalPaymentId, orderStatus);
                
                return CancelResult.builder()
                        .externalPaymentId(externalPaymentId)
                        .status(PaymentStatus.CANCELED)
                        .canceledAt(LocalDateTime.now())
                        .reason(reason)
                        .metadata(orderDetails.metadata())
                        .build();
            }
            
            // Authorization이 존재하는 경우: void 처리
            if (orderDetails.authorizationId() != null) {
                return voidAuthorization(orderDetails.authorizationId(), externalPaymentId, reason, accessToken, idempotencyKey);
            }
            
            // Capture가 존재하는 경우: 이미 캡처된 결제는 취소가 아닌 환불 처리 필요
            // 하지만 cancelPayment는 취소만 담당하므로 예외 발생
            if (orderDetails.captureId() != null) {
                throw new RuntimeException(
                        "PayPal 결제 취소 실패: 이미 캡처된 결제는 취소할 수 없습니다. 환불(refund)을 사용해주세요. " +
                        "externalPaymentId=" + externalPaymentId + ", captureId=" + orderDetails.captureId());
            }
            
            // 그 외의 경우: 주문 상태만 반환
            log.warn("PayPal 주문 취소 (상태 확인): externalPaymentId={}, status={}, " +
                    "authorizationId={}, captureId={}", 
                    externalPaymentId, orderStatus, orderDetails.authorizationId(), orderDetails.captureId());
            
            return CancelResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(PaymentStatus.CANCELED)
                    .canceledAt(LocalDateTime.now())
                    .reason(reason)
                    .metadata(orderDetails.metadata())
                    .build();
                    
        } catch (Exception e) {
            log.error("PayPal 결제 취소 실패: externalPaymentId={}, error={}",
                    externalPaymentId, e.getMessage(), e);
            throw new RuntimeException("PayPal 결제 취소 실패", e);
        }
    }
    
    /**
     * PayPal Authorization을 void 처리
     */
    private CancelResult voidAuthorization(String authorizationId, String externalPaymentId, 
                                           String reason, String accessToken, String idempotencyKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            if (idempotencyKey != null) {
                headers.set("PayPal-Request-Id", idempotencyKey);
            }

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_API_URL + "/v2/payments/authorizations/" + authorizationId + "/void",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.NO_CONTENT || response.getStatusCode() == HttpStatus.OK) {
                log.info("PayPal Authorization void 성공: externalPaymentId={}, authorizationId={}", 
                        externalPaymentId, authorizationId);

                String metadata = response.getBody() != null ? 
                        objectMapper.writeValueAsString(response.getBody()) : null;
                return CancelResult.builder()
                        .externalPaymentId(externalPaymentId)
                        .status(PaymentStatus.CANCELED)
                        .canceledAt(LocalDateTime.now())
                        .reason(reason)
                        .metadata(metadata)
                        .build();
            } else {
                throw new RuntimeException("PayPal Authorization void 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("PayPal Authorization void 실패: authorizationId={}, error={}", 
                    authorizationId, e.getMessage(), e);
            throw new RuntimeException("PayPal Authorization void 실패", e);
        }
    }
    
    /**
     * PayPal 주문 상세 정보 조회 및 Authorization/Capture ID 추출
     */
    private OrderDetails getOrderDetails(String orderId, String accessToken) {
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
            Map<String, Object> responseBody = response.getBody();
            String status = (String) responseBody.get("status");
            
            String authorizationId = null;
            String captureId = null;
            
            // purchase_units에서 payments 정보 추출
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> purchaseUnits = (List<Map<String, Object>>) responseBody.get("purchase_units");
            if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
                Map<String, Object> purchaseUnit = purchaseUnits.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> payments = (Map<String, Object>) purchaseUnit.get("payments");
                
                if (payments != null) {
                    // Authorization 추출
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> authorizations = (List<Map<String, Object>>) payments.get("authorizations");
                    if (authorizations != null && !authorizations.isEmpty()) {
                        Map<String, Object> authorization = authorizations.get(0);
                        authorizationId = (String) authorization.get("id");
                    }
                    
                    // Capture 추출
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> captures = (List<Map<String, Object>>) payments.get("captures");
                    if (captures != null && !captures.isEmpty()) {
                        Map<String, Object> capture = captures.get(0);
                        captureId = (String) capture.get("id");
                    }
                }
            }
            
            String metadata;
            try {
                metadata = objectMapper.writeValueAsString(responseBody);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.warn("PayPal 주문 메타데이터 직렬화 실패: orderId={}, error={}", orderId, e.getMessage());
                metadata = null;
            }
            return new OrderDetails(status, authorizationId, captureId, metadata);
        }
        
        throw new RuntimeException("PayPal 주문 정보 조회 실패: orderId=" + orderId);
    }
    
    @Override
    public RefundResult refundPayment(String externalPaymentId, BigDecimal amount, String reason, String idempotencyKey) {
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

            // 멱등성 키
            if (idempotencyKey != null) {
                headers.set("PayPal-Request-Id", idempotencyKey);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_API_URL + "/v2/payments/captures/" + captureId + "/refund",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if ((response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK)
                    && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("PayPal 결제 환불 성공: externalPaymentId={}, amount={}", externalPaymentId, amount);

                // 응답에서 실제 환불 금액 추출
                BigDecimal refundedAmount = amount;
                if (responseBody.get("amount") != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> refundAmountMap = (Map<String, Object>) responseBody.get("amount");
                    if (refundAmountMap.get("value") != null) {
                        refundedAmount = new BigDecimal(refundAmountMap.get("value").toString());
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
                throw new RuntimeException("PayPal 결제 환불 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("PayPal 결제 환불 실패: externalPaymentId={}, amount={}, error={}",
                    externalPaymentId, amount, e.getMessage(), e);
            throw new RuntimeException("PayPal 결제 환불 실패", e);
        }
    }
    
    /**
     * PayPal Webhook 서명 검증
     *
     * PayPal은 POST /v1/notifications/verify-webhook-signature 엔드포인트를 사용합니다.
     * signature 파라미터는 JSON 형태로 다음 헤더 정보를 포함해야 합니다:
     * - transmissionId: PAYPAL-TRANSMISSION-ID 헤더
     * - transmissionTime: PAYPAL-TRANSMISSION-TIME 헤더
     * - certUrl: PAYPAL-CERT-URL 헤더
     * - authAlgo: PAYPAL-AUTH-ALGO 헤더
     * - transmissionSig: PAYPAL-TRANSMISSION-SIG 헤더
     */
    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            String webhookId = paypalProperties.getWebhookId();
            if (webhookId == null || webhookId.isEmpty()) {
                log.warn("PayPal Webhook ID가 설정되지 않았습니다.");
                return false;
            }

            // signature 파라미터에서 PayPal 헤더 정보 파싱
            @SuppressWarnings("unchecked")
            Map<String, String> signatureData = objectMapper.readValue(signature, Map.class);

            String transmissionId = signatureData.get("transmissionId");
            String transmissionTime = signatureData.get("transmissionTime");
            String certUrl = signatureData.get("certUrl");
            String authAlgo = signatureData.get("authAlgo");
            String transmissionSig = signatureData.get("transmissionSig");

            if (transmissionId == null || transmissionSig == null) {
                log.warn("PayPal Webhook 서명 검증에 필요한 헤더가 누락되었습니다.");
                return false;
            }

            // PayPal verify-webhook-signature API 호출
            String accessToken = getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("auth_algo", authAlgo);
            requestBody.put("cert_url", certUrl);
            requestBody.put("transmission_id", transmissionId);
            requestBody.put("transmission_sig", transmissionSig);
            requestBody.put("transmission_time", transmissionTime);
            requestBody.put("webhook_id", webhookId);
            requestBody.put("webhook_event", objectMapper.readValue(payload, Map.class));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    PAYPAL_API_URL + "/v1/notifications/verify-webhook-signature",
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String verificationStatus = (String) response.getBody().get("verification_status");
                boolean verified = "SUCCESS".equals(verificationStatus);
                if (!verified) {
                    log.warn("PayPal Webhook 서명 검증 실패: status={}", verificationStatus);
                }
                return verified;
            }

            return false;
        } catch (Exception e) {
            log.error("PayPal Webhook 서명 검증 실패: error={}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public PaymentProvider.WebhookEvent parseWebhook(String payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String eventType = (String) webhookData.get("event_type");
            @SuppressWarnings("unchecked")
            Map<String, Object> resource = (Map<String, Object>) webhookData.get("resource");
            
            String orderId = null;
            String captureId = null;
            PaymentResult paymentResult = null;
            
            if (resource != null) {
                // PAYMENT.CAPTURE.COMPLETED 이벤트의 경우
                if (resource.containsKey("id")) {
                    captureId = (String) resource.get("id");
                }
                
                // purchase_units에서 orderId 추출
                @SuppressWarnings("unchecked")
                Map<String, Object> supplementaryData = (Map<String, Object>) resource.get("supplementary_data");
                if (supplementaryData != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> relatedIds = (Map<String, Object>) supplementaryData.get("related_ids");
                    if (relatedIds != null) {
                        orderId = (String) relatedIds.get("order_id");
                    }
                }
                
                paymentResult = PaymentResult.builder()
                        .externalPaymentId(orderId != null ? orderId : captureId)
                        .status(parseStatus((String) resource.get("status")))
                        .metadata(objectMapper.writeValueAsString(resource))
                        .build();
            }
            
            // orderId가 null이면 captureId를 대체값으로 사용
            String externalPaymentId = orderId != null ? orderId : captureId;
            return new PaymentProvider.WebhookEvent(eventType, externalPaymentId, externalPaymentId, paymentResult);
        } catch (Exception e) {
            log.error("PayPal Webhook 파싱 실패: payload={}, error={}", payload, e.getMessage(), e);
            throw new RuntimeException("PayPal Webhook 파싱 실패", e);
        }
    }
    
    private String getAccessToken() {
        // 캐시된 토큰이 유효하면 재사용
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return cachedAccessToken;
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(paypalProperties.getClientId(), paypalProperties.getClientSecret());
        
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
            cachedAccessToken = (String) response.getBody().get("access_token");
            Object expiresIn = response.getBody().get("expires_in");
            if (expiresIn != null) {
                // 만료 5분 전에 갱신하도록 설정
                tokenExpiresAt = System.currentTimeMillis() + 
                        (((Number) expiresIn).longValue() - 300) * 1000;
            }
            return cachedAccessToken;
        }
        
        throw new RuntimeException("PayPal 액세스 토큰 획득 실패");
    }
    
    private PaymentStatus parseStatus(String status) {
        if (status == null) {
            return PaymentStatus.PENDING;
        }
        
        return switch (status) {
            case "COMPLETED" -> PaymentStatus.SUCCESS;
            case "CANCELLED" -> PaymentStatus.CANCELED;
            case "PARTIALLY_REFUNDED" -> PaymentStatus.PARTIALLY_REFUNDED;
            case "REFUNDED" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }
    
    private LocalDateTime parseApprovedAt(Map<String, Object> responseBody) {
        try {
            Object createTimeObj = responseBody.get("create_time");
            if (createTimeObj != null) {
                String createTimeStr = createTimeObj.toString();
                // PayPal은 ISO 8601 형식으로 시간대 정보(Z)를 포함하여 반환함
                // 예: 2025-03-07T11:00:00Z
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(createTimeStr, DateTimeFormatter.ISO_DATE_TIME);
                return offsetDateTime.toLocalDateTime();
            }
        } catch (Exception e) {
            log.warn("승인 시간 파싱 실패: {}", e.getMessage());
        }
        return null;
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
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
        );
        
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> purchaseUnitsList = (List<Map<String, Object>>) response.getBody().get("purchase_units");
            if (purchaseUnitsList == null || purchaseUnitsList.isEmpty()) {
                throw new RuntimeException("PayPal 응답에서 purchase_units를 찾을 수 없습니다");
            }

            Map<String, Object> purchaseUnit = purchaseUnitsList.get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> payments = (Map<String, Object>) purchaseUnit.get("payments");
            if (payments == null) {
                throw new RuntimeException("PayPal 응답에서 payments를 찾을 수 없습니다");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> captures = (List<Map<String, Object>>) payments.get("captures");
            if (captures == null || captures.isEmpty()) {
                throw new RuntimeException("PayPal 응답에서 captures를 찾을 수 없습니다");
            }

            Map<String, Object> capture = captures.get(0);
            String captureId = (String) capture.get("id");

            @SuppressWarnings("unchecked")
            Map<String, Object> amount = (Map<String, Object>) capture.get("amount");
            if (amount == null) {
                throw new RuntimeException("PayPal 캡처에서 금액 정보를 찾을 수 없습니다");
            }

            String currency = (String) amount.get("currency_code");
            if (currency == null || currency.isEmpty()) {
                throw new RuntimeException("PayPal 캡처에서 통화 코드를 찾을 수 없습니다");
            }

            return new CaptureInfo(captureId, currency);
        }

        throw new RuntimeException("PayPal 캡처 ID 조회 실패");
    }
    
    /**
     * 캡처 ID와 통화 코드를 담는 레코드
     */
    private record CaptureInfo(String captureId, String currency) {}
    
    /**
     * 주문 상세 정보를 담는 레코드
     */
    private record OrderDetails(String status, String authorizationId, String captureId, String metadata) {}
}


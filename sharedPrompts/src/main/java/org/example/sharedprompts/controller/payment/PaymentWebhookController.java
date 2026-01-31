package org.example.sharedprompts.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.service.payment.provider.PaymentProviderServiceFactory;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 결제 Webhook 컨트롤러 (리팩토링)
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentProviderServiceFactory providerServiceFactory;
    private final PaymentLoggingService loggingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/kakao")
    public ResponseEntity<Map<String, String>> kakaoWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Kakao-Signature", required = false) String signature
    ) {
        return handleWebhook(payload, signature, PaymentMethod.KAKAO_PAY, "KAKAO_PAY");
    }

    @PostMapping("/toss")
    public ResponseEntity<Map<String, String>> tossWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Toss-Signature", required = false) String signature
    ) {
        return handleWebhook(payload, signature, PaymentMethod.TOSS, "TOSS");
    }

    @PostMapping("/paypal")
    public ResponseEntity<Map<String, String>> paypalWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "PayPal-Signature", required = false) String signature
    ) {
        return handleWebhook(payload, signature, PaymentMethod.PAYPAL, "PAYPAL");
    }

    /**
     * Webhook 공통 처리
     */
    private ResponseEntity<Map<String, String>> handleWebhook(
            String payload,
            String signature,
            PaymentMethod method,
            String providerName
    ) {
        String eventType = extractEventType(payload);
        loggingService.logWebhookReceived(providerName, eventType, payload);

        try {
            var service = providerServiceFactory.getService(method);

            // true → 정상, false → 실패 로직으로 직관적으로 변경
            if (!service.verifyWebhookSignature(payload, signature)) {
                log.warn("{} Webhook 서명 검증 실패", providerName);
                throw new ApiException(ErrorCode.PAYMENT_WEBHOOK_VERIFICATION_FAILED);
            }

            service.processWebhook(payload);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            loggingService.logWebhookProcessingFailure(providerName, eventType, e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }
    }

    /**
     * 이벤트 타입 추출
     */
    private String extractEventType(String payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String eventType = (String) webhookData.get("event");
            if (eventType == null) eventType = (String) webhookData.get("event_type");
            if (eventType == null) eventType = (String) webhookData.get("type");
            return eventType != null ? eventType : "UNKNOWN";
        } catch (Exception e) {
            return "PARSE_ERROR";
        }
    }
}

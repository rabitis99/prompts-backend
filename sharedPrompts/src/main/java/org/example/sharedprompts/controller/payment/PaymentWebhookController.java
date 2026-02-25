package org.example.sharedprompts.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.port.in.command.PaymentWebhookCommand;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.PaymentWebhookUseCase;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.PaymentLoggingService;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 결제 Webhook 컨트롤러
 * PaymentWebhookHandlingUseCase를 통해 웹훅을 처리합니다.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookUseCase paymentWebhookUseCase;
    private final PaymentLoggingService loggingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/kakao")
    public ResponseEntity<Map<String, String>> kakaoWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Kakao-Signature", required = false) String signature,
            @RequestHeader Map<String, String> headers
    ) {
        return handleWebhook(payload, signature, headers, PaymentMethod.KAKAO_PAY, "KAKAO_PAY");
    }

    @PostMapping("/toss")
    public ResponseEntity<Map<String, String>> tossWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Toss-Signature", required = false) String signature,
            @RequestHeader Map<String, String> headers
    ) {
        return handleWebhook(payload, signature, headers, PaymentMethod.TOSS, "TOSS");
    }

    @PostMapping("/paypal")
    public ResponseEntity<Map<String, String>> paypalWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "PayPal-Signature", required = false) String signature,
            @RequestHeader Map<String, String> headers
    ) {
        return handleWebhook(payload, signature, headers, PaymentMethod.PAYPAL, "PAYPAL");
    }

    /**
     * Webhook 공통 처리
     */
    private ResponseEntity<Map<String, String>> handleWebhook(
            String payload,
            String signature,
            Map<String, String> headers,
            PaymentMethod method,
            String providerName
    ) {
        String eventType = extractEventType(payload);
        loggingService.logWebhookReceived(providerName, eventType, payload);

        try {
            var command = PaymentWebhookCommand.builder()
                    .paymentMethod(method)
                    .payload(payload)
                    .signature(signature)
                    .headers(headers)
                    .build();
            paymentWebhookUseCase.handleWebhook(command);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (ApiException e) {
            loggingService.logWebhookProcessingFailure(providerName, eventType, e);
            throw e;
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

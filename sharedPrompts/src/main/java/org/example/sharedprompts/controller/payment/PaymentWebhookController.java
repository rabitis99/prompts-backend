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

    /**
     * Handle incoming Kakao Pay webhook requests.
     *
     * @param payload  the raw request body (typically a JSON string) received from Kakao Pay
     * @param signature the value of the `X-Kakao-Signature` header provided by Kakao Pay; may be null if absent
     * @return a ResponseEntity whose body is a map containing `"status": "success"` when the webhook is processed successfully
     */
    @PostMapping("/kakao")
    public ResponseEntity<Map<String, String>> kakaoWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Kakao-Signature", required = false) String signature
    ) {
        return handleWebhook(payload, signature, PaymentMethod.KAKAO_PAY, "KAKAO_PAY");
    }

    /**
     * Handles incoming Toss payment webhook requests.
     *
     * Verifies the optional signature, processes the webhook payload, and responds with a standard success body on completion.
     *
     * @param payload   the raw webhook request body (typically JSON)
     * @param signature the optional `X-Toss-Signature` header used to verify the webhook; may be null
     * @return          a 200 OK response containing a map with `status` set to `"success"` on successful processing
     */
    @PostMapping("/toss")
    public ResponseEntity<Map<String, String>> tossWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Toss-Signature", required = false) String signature
    ) {
        return handleWebhook(payload, signature, PaymentMethod.TOSS, "TOSS");
    }

    /**
     * Handle incoming PayPal webhook callbacks.
     *
     * @param payload   the raw request body sent by PayPal
     * @param signature the value of the `PayPal-Signature` header, or `null` if not provided
     * @return a ResponseEntity with a JSON body containing `"status": "success"` when processing completes successfully
     */
    @PostMapping("/paypal")
    public ResponseEntity<Map<String, String>> paypalWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "PayPal-Signature", required = false) String signature
    ) {
        return handleWebhook(payload, signature, PaymentMethod.PAYPAL, "PAYPAL");
    }

    /**
         * Handle an incoming payment provider webhook: verify its signature, delegate processing to the provider service, and return a standard success response.
         *
         * @param payload     the raw webhook request body
         * @param signature   the provider's webhook signature header (may be null)
         * @param method      the payment method used to select the provider service
         * @param providerName a human-readable provider identifier for logging
         * @return a ResponseEntity containing a map with "status" set to "success" when processing completes
         * @throws ApiException if signature verification fails or any processing error occurs
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
     * Extracts the event type from a webhook JSON payload.
     *
     * Attempts to read the JSON and return the first present of the keys "event", "event_type", and "type".
     *
     * @param payload the raw JSON payload received from the webhook
     * @return the extracted event type string; `"UNKNOWN"` if none of the keys are present; `"PARSE_ERROR"` if the payload cannot be parsed as JSON
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
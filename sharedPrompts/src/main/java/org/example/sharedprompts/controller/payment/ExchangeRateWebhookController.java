package org.example.sharedprompts.controller.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.logging.PaymentLoggingService;
import org.example.sharedprompts.global.annotation.AdminOnly;
import org.example.sharedprompts.scheduler.payment.ExchangeRateScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 환율 웹훅 컨트롤러
 * 환율 API에서 환율 업데이트를 알리는 웹훅을 수신
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateWebhookController {

    private final ExchangeRateScheduler exchangeRateScheduler;
    private final PaymentLoggingService loggingService;

    /**
     * 환율 업데이트 웹훅 수신
     * POST /webhooks/exchange-rates
     */
    @AdminOnly
    @PostMapping
    public ResponseEntity<Map<String, String>> receiveExchangeRateWebhook(
            @RequestBody(required = false) String payload
    ) {
        log.info("환율 웹훅 수신: payload={}", payload);
        loggingService.logWebhookReceived("EXCHANGE_RATE", "UPDATE", payload);

        try {
            // 웹훅 수신 시 환율 업데이트 실행
            exchangeRateScheduler.updateExchangeRates();
            return ResponseEntity.ok(Map.of("status", "success", "message", "환율 업데이트 완료"));
        } catch (Exception e) {
            log.error("환율 웹훅 처리 실패: error={}", e.getMessage(), e);
            loggingService.logWebhookProcessingFailure("EXCHANGE_RATE", "UPDATE", e);
            return ResponseEntity.ok(Map.of("status", "error", "message", "환율 업데이트 실패"));
        }
    }
}


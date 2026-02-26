package org.example.sharedprompts.domain.payment.application.port.in.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * 결제 웹훅 처리 명령
 * 외부 결제 제공자의 웹훅을 처리할 때 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookCommand {

    private PaymentMethod paymentMethod;
    private String payload;
    private String signature;
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    /**
     * 기본 정보로만 커맨드 생성
     */
    public static PaymentWebhookCommand of(
            PaymentMethod paymentMethod,
            String payload,
            String signature
    ) {
        return of(paymentMethod, payload, signature, null);
    }

    /**
     * HTTP 헤더를 포함한 커맨드 생성 (웹훅 서명 검증 등에 사용)
     */
    public static PaymentWebhookCommand of(
            PaymentMethod paymentMethod,
            String payload,
            String signature,
            Map<String, String> headers
    ) {
        return PaymentWebhookCommand.builder()
                .paymentMethod(paymentMethod)
                .payload(payload)
                .signature(signature)
                .headers(headers != null ? new HashMap<>(headers) : new HashMap<>())
                .build();
    }
}

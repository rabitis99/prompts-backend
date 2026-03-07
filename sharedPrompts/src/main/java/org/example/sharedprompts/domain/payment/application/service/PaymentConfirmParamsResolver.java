package org.example.sharedprompts.domain.payment.application.service;

import org.example.sharedprompts.domain.payment.application.port.in.command.ConfirmPaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.out.paymentgateway.PaymentConfirmParams;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.application.command.service.metadata.PaymentMetadataKeys;
import org.example.sharedprompts.domain.payment.application.command.service.metadata.PaymentMetadataParser;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 결제 확인 시 포트에 넘길 파라미터를 해석한다.
 * 애플리케이션 계층에 두어, PG별 차이(tid vs paymentKey, pgToken 등)를 포트/어댑터 밖에서 처리한다.
 */
@Component
public class PaymentConfirmParamsResolver {

    private final PaymentMetadataParser metadataParser;

    public PaymentConfirmParamsResolver(PaymentMetadataParser metadataParser) {
        this.metadataParser = metadataParser;
    }

    /**
     * Payment와 Command로부터 포트에 넘길 PaymentConfirmParams를 만든다.
     * - 카카오페이: paymentKey = tid(엔티티 또는 메타데이터), additionalParams에 pgToken
     * - 그 외: paymentKey = providerToken, additionalParams = command의 additionalParams
     */
    public PaymentConfirmParams resolve(Payment payment, ConfirmPaymentCommand command) {
        String paymentKey = resolvePaymentKey(payment, command.getProviderToken());
        Map<String, String> additionalParams = resolveAdditionalParams(payment.getPaymentMethod(), command);
        return PaymentConfirmParams.of(paymentKey, additionalParams);
    }

    private String resolvePaymentKey(Payment payment, String providerToken) {
        if (payment.getPaymentMethod() == PaymentMethod.KAKAO_PAY) {
            String tid = payment.getExternalPaymentId();
            if (tid != null && !tid.isBlank()) {
                return tid;
            }
            String metadata = payment.getMetadata();
            if (metadata != null && !metadata.isBlank()) {
                Optional<String> tidFromMeta = metadataParser.parseMetadata(metadata)
                        .map(m -> m.get(PaymentMetadataKeys.TID))
                        .filter(v -> v != null && !v.toString().isBlank())
                        .map(v -> v.toString());
                if (tidFromMeta.isPresent()) {
                    return tidFromMeta.get();
                }
            }
        }
        return providerToken;
    }

    private Map<String, String> resolveAdditionalParams(PaymentMethod method, ConfirmPaymentCommand command) {
        Map<String, String> fromCommand = command.getAdditionalParams() != null
                ? command.getAdditionalParams()
                : Collections.emptyMap();
        if (method == PaymentMethod.KAKAO_PAY) {
            String pgToken = fromCommand.get("pgToken");
            if ((pgToken == null || pgToken.isBlank()) && command.getProviderToken() != null && !command.getProviderToken().isBlank()) {
                Map<String, String> merged = new HashMap<>(fromCommand);
                merged.put("pgToken", command.getProviderToken());
                return merged;
            }
        }
        return fromCommand;
    }
}

package org.example.sharedprompts.domain.payment.application.command.service.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.command.service.PaymentMetadataService;
import org.example.sharedprompts.domain.payment.application.command.service.PaymentProviderIntegrationService;
import org.example.sharedprompts.domain.payment.application.command.service.provider.PreparePaymentResult;
import org.example.sharedprompts.domain.payment.application.dto.AmountProcessingResult;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentPreparationHandler {

    private final PaymentProviderIntegrationService providerIntegrationService;
    private final PaymentMetadataService metadataService;

    public Payment processPreparationIfNeeded(Payment payment, AmountProcessingResult amountResult,
                                              Long userId, PaymentRequestDto request) {
        try {
            PreparePaymentResult prepareResult = providerIntegrationService.preparePayment(
                    payment,
                    amountResult.actualPaymentAmount(),
                    metadataService.extractProductName(request.getMetadata()),
                    userId
            );

            if (prepareResult.required() && prepareResult.redirectUrl() != null) {
                String updatedMetadata = metadataService.addRedirectUrlToMetadata(
                        request.getMetadata(),
                        prepareResult.redirectUrl(),
                        prepareResult.tid()
                );
                payment.updateMetadata(updatedMetadata);
                log.info("결제 준비 완료: paymentId={}, paymentMethod={}, tid={}",
                        payment.getId(), payment.getPaymentMethod(), prepareResult.tid());
            } else {
                log.debug("결제 준비 불필요: paymentId={}, paymentMethod={}",
                        payment.getId(), payment.getPaymentMethod());
            }
        } catch (Exception e) {
            log.error("결제 준비 실패: paymentId={}, paymentMethod={}, error={}",
                    payment.getId(), payment.getPaymentMethod(), e.getMessage(), e);
            // 결제 준비 실패해도 결제 요청은 계속 진행 (이미 payment는 생성됨)
        }

        return payment;
    }
}


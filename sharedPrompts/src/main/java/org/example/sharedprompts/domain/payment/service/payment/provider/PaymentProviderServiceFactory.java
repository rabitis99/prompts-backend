package org.example.sharedprompts.domain.payment.service.payment.provider;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 결제사별 서비스 팩토리
 * Strategy 패턴 적용
 */
@Component
@RequiredArgsConstructor
public class PaymentProviderServiceFactory {

    private final List<PaymentProviderService> providerServices;
    private Map<PaymentMethod, PaymentProviderService> serviceMap;

    /**
     * 결제 수단에 맞는 서비스 조회
     */
    public PaymentProviderService getService(PaymentMethod paymentMethod) {
        if (serviceMap == null) {
            serviceMap = providerServices.stream()
                    .collect(Collectors.toMap(
                            PaymentProviderService::getPaymentMethod,
                            Function.identity()
                    ));
        }

        PaymentProviderService service = serviceMap.get(paymentMethod);
        if (service == null) {
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "지원하지 않는 결제 수단입니다.");
        }

        return service;
    }
}


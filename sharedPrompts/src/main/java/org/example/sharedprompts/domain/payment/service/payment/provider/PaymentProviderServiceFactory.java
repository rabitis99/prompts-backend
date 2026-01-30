package org.example.sharedprompts.domain.payment.service.payment.provider;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
     * 빈 초기화 시점에 서비스 맵 구성
     * EnumMap 사용으로 성능 최적화 및 스레드 안전
     */
    @PostConstruct
    public void init() {
        this.serviceMap = providerServices.stream()
                .collect(Collectors.toMap(
                        PaymentProviderService::getPaymentMethod,
                        Function.identity(),
                        (a, b) -> b, // 중복 키 발생 시 b 사용
                        () -> new EnumMap<>(PaymentMethod.class) // EnumMap 사용
                ));
    }

    /**
     * 결제 수단에 맞는 서비스 조회
     */
    public PaymentProviderService getService(PaymentMethod paymentMethod) {
        return Optional.ofNullable(serviceMap.get(paymentMethod))
                .orElseThrow(() ->
                        new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR,
                                "지원하지 않는 결제 수단입니다."));
    }
}

package org.example.sharedprompts.domain.payment.application.command;

import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.domain.payment.domain.service.PaymentDomainService;
import org.example.sharedprompts.domain.payment.infrastructure.rule.PaymentLimitRule;
import org.example.sharedprompts.domain.payment.service.user.tier.UserTierService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentValidationService 테스트")
class PaymentValidationServiceTest {

    @Mock
    private PaymentLimitRule paymentLimitRule;

    @Mock
    private UserTierService userTierService;

    @Mock
    private PaymentDomainService domainService;

    @InjectMocks
    private PaymentValidationService paymentValidationService;

    @Test
    @DisplayName("validateDailyLimit은 결제 횟수만 사용하고 모듈 사용량은 반영하지 않음")
    void validateDailyLimit_paymentOnly() {
        long userId = 1L;
        when(userTierService.getTodayPaymentCount(userId)).thenReturn(5L);

        paymentValidationService.validateDailyLimit(userId, UserTier.FREE);

        verify(userTierService).getTodayPaymentCount(userId);
        verify(userTierService, never()).getTodayUsedCount(anyLong());
        verify(paymentLimitRule).validateDailyLimit(userId, UserTier.FREE, 5L);
    }
}

package org.example.sharedprompts.domain.payment.service;

import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.*;
import org.example.sharedprompts.domain.payment.repository.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.cashback.CashbackService;
import org.example.sharedprompts.domain.payment.service.core.PaymentServiceImpl;
import org.example.sharedprompts.domain.payment.service.event.PaymentEventPublisher;
import org.example.sharedprompts.domain.payment.service.exchange.ExchangeRateService;
import org.example.sharedprompts.domain.payment.service.payment.provider.PaymentProviderService;
import org.example.sharedprompts.domain.payment.service.payment.provider.PaymentProviderServiceFactory;
import org.example.sharedprompts.domain.payment.service.point.PointService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 결제 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("결제 서비스 테스트")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentProviderServiceFactory providerServiceFactory;

    @Mock
    private PaymentProviderService providerService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private PointService pointService;

    @Mock
    private CashbackService cashbackService;

    @Mock
    private PaymentEventPublisher eventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User testUser;
    private PaymentRequestDto paymentRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .provider(Provider.LOCAL)
                .providerId("test-provider-id")
                .nickname("testuser")
                .role(Role.ROLE_USER)
                .tier(UserTier.FREE)
                .signupCompleted(true)
                .blocked(false)
                .build();

        paymentRequest = PaymentRequestDto.builder()
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .userType(PaymentUserType.PERSONAL)
                .build();
    }

    @Test
    @DisplayName("결제 요청 성공")
    void requestPayment_Success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.countTodaySuccessfulPayments(1L)).thenReturn(0L);
        when(providerServiceFactory.getService(PaymentMethod.KAKAO_PAY)).thenReturn(providerService);
        when(providerService.approvePayment(any(Payment.class))).thenReturn("EXTERNAL_PAYMENT_ID");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var response = paymentService.requestPayment(1L, paymentRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(pointService).accumulatePoints(anyLong(), anyLong(), any(BigDecimal.class));
        verify(cashbackService).accumulateCashback(anyLong(), anyLong(), any(BigDecimal.class));
        verify(eventPublisher).publishPaymentSucceeded(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("일일 결제 제한 초과 시 예외 발생")
    void requestPayment_DailyLimitExceeded() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.countTodaySuccessfulPayments(1L)).thenReturn(10L); // FREE 티어 제한 초과

        // when & then
        assertThatThrownBy(() -> paymentService.requestPayment(1L, paymentRequest))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_DAILY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("사용자 없음 시 예외 발생")
    void requestPayment_UserNotFound() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.requestPayment(1L, paymentRequest))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("결제 승인 실패 시 실패 상태로 저장")
    void requestPayment_ApprovalFailed() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.countTodaySuccessfulPayments(1L)).thenReturn(0L);
        when(providerServiceFactory.getService(PaymentMethod.KAKAO_PAY)).thenReturn(providerService);
        when(providerService.approvePayment(any(Payment.class))).thenThrow(new RuntimeException("API 호출 실패"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var response = paymentService.requestPayment(1L, paymentRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(eventPublisher).publishPaymentFailed(anyLong(), anyLong(), anyString(), anyString());
    }
}


package org.example.sharedprompts.domain.payment.service;

import org.example.sharedprompts.domain.payment.application.command.PaymentValidationService;
import org.example.sharedprompts.domain.payment.application.command.service.amount.PaymentAmountProcessingService;
import org.example.sharedprompts.domain.payment.application.command.service.request.PaymentCreator;
import org.example.sharedprompts.domain.payment.application.command.service.request.PaymentPreparationHandler;
import org.example.sharedprompts.domain.payment.application.dto.AmountProcessingResult;
import org.example.sharedprompts.domain.payment.application.port.in.command.ApprovePaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentApprovalResult;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentCommandRepositoryPort;
import org.example.sharedprompts.domain.payment.application.service.impl.DefaultPaymentApprovalService;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentUserType;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.PaymentLoggingService;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 결제 승인 유스케이스 단위 테스트
 * DefaultPaymentApprovalService(결제 요청/승인) 동작을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("결제 승인 서비스 테스트")
class PaymentServiceTest {

    @Mock
    private PaymentCommandRepositoryPort paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentValidationService validationService;

    @Mock
    private PaymentAmountProcessingService amountProcessingService;

    @Mock
    private PaymentCreator paymentCreator;

    @Mock
    private PaymentPreparationHandler preparationHandler;

    @Mock
    private PaymentLoggingService loggingService;

    private DefaultPaymentApprovalService paymentApprovalService;

    private User testUser;
    private ApprovePaymentCommand approveCommand;
    private Payment mockPayment;

    @BeforeEach
    void setUp() {
        paymentApprovalService = new DefaultPaymentApprovalService(
                paymentRepository,
                validationService,
                amountProcessingService,
                paymentCreator,
                preparationHandler,
                loggingService,
                userRepository
        );

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

        approveCommand = ApprovePaymentCommand.builder()
                .userId(1L)
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .usePointAmount(BigDecimal.ZERO)
                .userType(PaymentUserType.PERSONAL)
                .build();

        mockPayment = Payment.builder()
                .id(1L)
                .user(testUser)
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .userType(PaymentUserType.PERSONAL)
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("결제 요청 성공")
    void requestPayment_Success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(validationService).validateDailyLimit(1L, UserTier.FREE);
        when(amountProcessingService.processPaymentAmount(anyLong(), any(BigDecimal.class), anyString(), any()))
                .thenReturn(new AmountProcessingResult(
                        new BigDecimal("10000"),
                        new BigDecimal("10000"),
                        BigDecimal.ZERO,
                        new BigDecimal("10000")
                ));
        when(paymentCreator.createPayment(any(User.class), any(PaymentRequestDto.class), any(AmountProcessingResult.class)))
                .thenReturn(mockPayment);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(preparationHandler.processPreparationIfNeeded(any(Payment.class), any(AmountProcessingResult.class), anyLong(), any(PaymentRequestDto.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // when
        PaymentApprovalResult result = paymentApprovalService.approve(approveCommand);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(loggingService).logPaymentRequest(any(Payment.class));
    }

    @Test
    @DisplayName("일일 결제 제한 초과 시 예외 발생")
    void requestPayment_DailyLimitExceeded() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doThrow(new ApiException(ErrorCode.PAYMENT_DAILY_LIMIT_EXCEEDED))
                .when(validationService).validateDailyLimit(1L, UserTier.FREE);

        // when & then
        assertThatThrownBy(() -> paymentApprovalService.approve(approveCommand))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_DAILY_LIMIT_EXCEEDED);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("사용자 없음 시 예외 발생")
    void requestPayment_UserNotFound() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentApprovalService.approve(approveCommand))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 준비 처리 실패 시 예외 전파")
    void requestPayment_PreparationFails() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(validationService).validateDailyLimit(1L, UserTier.FREE);
        when(amountProcessingService.processPaymentAmount(anyLong(), any(BigDecimal.class), anyString(), any()))
                .thenReturn(new AmountProcessingResult(
                        new BigDecimal("10000"),
                        new BigDecimal("10000"),
                        BigDecimal.ZERO,
                        new BigDecimal("10000")
                ));
        when(paymentCreator.createPayment(any(User.class), any(PaymentRequestDto.class), any(AmountProcessingResult.class)))
                .thenReturn(mockPayment);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(preparationHandler.processPreparationIfNeeded(any(Payment.class), any(AmountProcessingResult.class), anyLong(), any(PaymentRequestDto.class)))
                .thenThrow(new RuntimeException("PG API 호출 실패"));

        // when & then
        assertThatThrownBy(() -> paymentApprovalService.approve(approveCommand))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("PG API 호출 실패");
    }
}

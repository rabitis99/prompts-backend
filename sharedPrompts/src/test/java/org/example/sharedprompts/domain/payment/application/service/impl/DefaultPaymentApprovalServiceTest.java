package org.example.sharedprompts.domain.payment.application.service.impl;

import org.example.sharedprompts.domain.payment.application.command.PaymentValidationService;
import org.example.sharedprompts.domain.payment.application.command.service.amount.PaymentAmountProcessingService;
import org.example.sharedprompts.domain.payment.application.command.service.request.PaymentCreator;
import org.example.sharedprompts.domain.payment.application.command.service.request.PaymentPreparationHandler;
import org.example.sharedprompts.domain.payment.application.dto.AmountProcessingResult;
import org.example.sharedprompts.domain.payment.application.port.in.command.ApprovePaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentApprovalResult;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentCommandRepositoryPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentUserType;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.PaymentLoggingService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultPaymentApprovalService 테스트")
class DefaultPaymentApprovalServiceTest {

    @Mock
    private PaymentCommandRepositoryPort paymentRepository;

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

    @Mock
    private UserRepository userRepository;

    private DefaultPaymentApprovalService service;

    @BeforeEach
    void setUp() {
        service = new DefaultPaymentApprovalService(
                paymentRepository,
                validationService,
                amountProcessingService,
                paymentCreator,
                preparationHandler,
                loggingService,
                userRepository
        );
    }

    @Test
    @DisplayName("결제 승인 성공")
    void testApproveSuccess() {
        // Given
        Long userId = 1L;
        ApprovePaymentCommand command = ApprovePaymentCommand.builder()
                .userId(userId)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .usePointAmount(BigDecimal.ZERO)
                .userType(PaymentUserType.PERSONAL)
                .metadata("{}")
                .build();

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);
        when(mockUser.getTier()).thenReturn(UserTier.FREE);

        Payment mockPayment = Payment.builder()
                .id(1L)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .status(PaymentStatus.PENDING)
                .userType(PaymentUserType.PERSONAL)
                .build();

        AmountProcessingResult amountResult = new AmountProcessingResult(
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(10000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(10000)
        );

        // When
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(amountProcessingService.processPaymentAmount(
                userId,
                command.getAmount(),
                command.getCurrency(),
                command.getUsePointAmount()
        )).thenReturn(amountResult);
        when(paymentCreator.createPayment(mockUser, any(), any())).thenReturn(mockPayment);
        when(paymentRepository.save(mockPayment)).thenReturn(mockPayment);
        when(preparationHandler.processPreparationIfNeeded(mockPayment, amountResult, userId, any()))
                .thenReturn(mockPayment);

        PaymentApprovalResult result = service.approve(command);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getPaymentId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        verify(validationService).validateDailyLimit(userId, mockUser.getTier());
        verify(paymentCreator).createPayment(eq(mockUser), any(), any());
        verify(paymentRepository, times(2)).save(mockPayment);
        verify(loggingService).logPaymentRequest(mockPayment);
    }

    @Test
    @DisplayName("사용자를 찾을 수 없으면 실패")
    void testApproveUserNotFound() {
        // Given
        Long userId = 999L;
        ApprovePaymentCommand command = ApprovePaymentCommand.builder()
                .userId(userId)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .usePointAmount(BigDecimal.ZERO)
                .userType(PaymentUserType.PERSONAL)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> service.approve(command));
        verify(userRepository).findById(userId);
        verifyNoInteractions(paymentCreator, paymentRepository);
    }

    @Test
    @DisplayName("결제 승인 happy-path: save 2회(의도적—생성 후 업데이트), 로깅 호출 검증 (UseCase → Port 흐름)")
    void approveHappyPath_verifiesSaveAndLoggingPortCalls() {
        // 의도: save 1회=결제 엔티티 생성 저장, save 2회=준비 처리 후 상태 반영 저장.
        Long userId = 1L;
        ApprovePaymentCommand command = ApprovePaymentCommand.builder()
                .userId(userId)
                .amount(BigDecimal.valueOf(5000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.TOSS)
                .usePointAmount(BigDecimal.ZERO)
                .userType(PaymentUserType.PERSONAL)
                .metadata(null)
                .build();

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);
        when(mockUser.getTier()).thenReturn(UserTier.FREE);
        AmountProcessingResult amountResult = new AmountProcessingResult(
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(5000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(5000)
        );
        Payment mockPayment = Payment.builder()
                .id(2L)
                .user(mockUser)
                .amount(BigDecimal.valueOf(5000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.TOSS)
                .status(PaymentStatus.PENDING)
                .userType(PaymentUserType.PERSONAL)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(amountProcessingService.processPaymentAmount(any(), any(), any(), any())).thenReturn(amountResult);
        when(paymentCreator.createPayment(eq(mockUser), any(), any())).thenReturn(mockPayment);
        when(paymentRepository.save(any())).thenReturn(mockPayment);
        when(preparationHandler.processPreparationIfNeeded(any(), eq(amountResult), eq(userId), any())).thenReturn(mockPayment);

        PaymentApprovalResult result = service.approve(command);

        assertNotNull(result);
        assertEquals(2L, result.getPaymentId());
        verify(paymentRepository, times(2)).save(any());
        verify(loggingService).logPaymentRequest(mockPayment);
    }
}

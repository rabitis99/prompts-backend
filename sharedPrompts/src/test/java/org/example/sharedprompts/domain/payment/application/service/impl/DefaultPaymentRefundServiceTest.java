package org.example.sharedprompts.domain.payment.application.service.impl;

import org.example.sharedprompts.domain.payment.application.port.in.command.RefundPaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentRefundResult;
import org.example.sharedprompts.domain.payment.application.port.out.event.PaymentEventPublisherPort;
import org.example.sharedprompts.domain.payment.application.port.out.paymentgateway.PaymentGatewayPort;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentCommandRepositoryPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentUserType;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentNotFoundException;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentValidationException;
import org.example.sharedprompts.domain.payment.infrastructure.transaction.PaymentTransactionManager;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultPaymentRefundService 테스트")
class DefaultPaymentRefundServiceTest {

    @Mock
    private PaymentCommandRepositoryPort paymentRepository;

    @Mock
    private PaymentGatewayPort paymentGateway;

    @Mock
    private PaymentEventPublisherPort eventPublisher;

    @Mock
    private PaymentTransactionManager transactionManager;

    private DefaultPaymentRefundService service;

    @BeforeEach
    void setUp() {
        service = new DefaultPaymentRefundService(
                paymentRepository,
                paymentGateway,
                eventPublisher,
                transactionManager
        );
    }

    @Test
    @DisplayName("전체 환불 성공")
    void testFullRefundSuccess() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;
        BigDecimal refundAmount = BigDecimal.valueOf(10000);

        RefundPaymentCommand command = RefundPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(userId)
                .refundAmount(refundAmount)
                .reason("Full refund")
                .build();

        User mockUser = userWithId(userId);

        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .userType(PaymentUserType.PERSONAL)
                .status(PaymentStatus.SUCCESS)
                .refundedAmount(BigDecimal.ZERO)
                .build();

        PaymentGatewayPort.PaymentGatewayResult refundResult =
                PaymentGatewayPort.PaymentGatewayResult.success("EXT_PAY_ID", null);

        // When
        when(transactionManager.executeInTransaction(any())).thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(paymentRepository.findByIdForUpdate(paymentId))
                .thenReturn(Optional.of(mockPayment))
                .thenReturn(Optional.of(mockPayment));
        when(paymentGateway.refundPayment(mockPayment, refundAmount)).thenReturn(refundResult);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentRefundResult result = service.refund(command);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getPaymentId());
        assertEquals(refundAmount, result.getRefundAmount());
        assertEquals(PaymentStatus.REFUNDED, result.getStatus());
        verify(paymentGateway).refundPayment(mockPayment, refundAmount);
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(eventPublisher).publishPaymentRefunded(any());
    }

    @Test
    @DisplayName("부분 환불 성공")
    void testPartialRefundSuccess() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;
        BigDecimal refundAmount = BigDecimal.valueOf(5000);

        RefundPaymentCommand command = RefundPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(userId)
                .refundAmount(refundAmount)
                .reason("Partial refund")
                .build();

        User mockUser = userWithId(userId);

        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .userType(PaymentUserType.PERSONAL)
                .status(PaymentStatus.SUCCESS)
                .refundedAmount(BigDecimal.ZERO)
                .build();

        PaymentGatewayPort.PaymentGatewayResult refundResult =
                PaymentGatewayPort.PaymentGatewayResult.success("EXT_PAY_ID", null);

        // When
        when(transactionManager.executeInTransaction(any())).thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(paymentRepository.findByIdForUpdate(paymentId))
                .thenReturn(Optional.of(mockPayment))
                .thenReturn(Optional.of(mockPayment));
        when(paymentGateway.refundPayment(mockPayment, refundAmount)).thenReturn(refundResult);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentRefundResult result = service.refund(command);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, result.getStatus());
        verify(paymentGateway).refundPayment(mockPayment, refundAmount);
        verify(eventPublisher).publishPaymentRefunded(any());
    }

    @Test
    @DisplayName("결제를 찾을 수 없으면 실패")
    void testRefundPaymentNotFound() {
        // Given
        Long paymentId = 999L;
        Long userId = 1L;

        RefundPaymentCommand command = RefundPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(userId)
                .refundAmount(BigDecimal.valueOf(1000))
                .reason("reason")
                .build();

        // When & Then
        when(transactionManager.executeInTransaction(any())).thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> service.refund(command));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("권한이 없으면 실패")
    void testRefundUnauthorized() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;
        Long differentUserId = 999L;

        RefundPaymentCommand command = RefundPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(differentUserId)
                .refundAmount(BigDecimal.valueOf(1000))
                .reason("reason")
                .build();

        User mockUser = userWithId(userId);

        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .userType(PaymentUserType.PERSONAL)
                .status(PaymentStatus.SUCCESS)
                .build();

        // When & Then
        when(transactionManager.executeInTransaction(any())).thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(mockPayment));

        assertThrows(PaymentValidationException.class, () -> service.refund(command));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("환불 불가능한 상태면 실패")
    void testRefundInvalidStatus() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;

        RefundPaymentCommand command = RefundPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(userId)
                .refundAmount(BigDecimal.valueOf(1000))
                .reason("reason")
                .build();

        User mockUser = userWithId(userId);

        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .userType(PaymentUserType.PERSONAL)
                .status(PaymentStatus.CANCELED)  // Cannot refund cancelled payment
                .build();

        // When & Then
        when(transactionManager.executeInTransaction(any())).thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(mockPayment));

        assertThrows(PaymentValidationException.class, () -> service.refund(command));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("환불 금액이 초과하면 실패")
    void testRefundAmountExceeds() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;
        BigDecimal refundAmount = BigDecimal.valueOf(15000);  // More than original amount

        RefundPaymentCommand command = RefundPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(userId)
                .refundAmount(refundAmount)
                .reason("reason")
                .build();

        User mockUser = userWithId(userId);

        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .userType(PaymentUserType.PERSONAL)
                .status(PaymentStatus.SUCCESS)
                .refundedAmount(BigDecimal.ZERO)
                .build();

        // When & Then
        when(transactionManager.executeInTransaction(any())).thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(mockPayment));

        assertThrows(PaymentValidationException.class, () -> service.refund(command));
        verifyNoInteractions(paymentGateway);
    }

    private static User userWithId(Long id) {
        return User.builder()
                .id(id)
                .email("test@example.com")
                .provider(Provider.LOCAL)
                .providerId("provider-id")
                .nickname("testuser")
                .role(Role.ROLE_USER)
                .tier(UserTier.FREE)
                .signupCompleted(true)
                .blocked(false)
                .build();
    }
}


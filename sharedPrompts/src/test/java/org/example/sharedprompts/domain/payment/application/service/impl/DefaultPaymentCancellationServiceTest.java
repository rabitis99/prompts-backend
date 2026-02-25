package org.example.sharedprompts.domain.payment.application.service.impl;

import org.example.sharedprompts.domain.payment.application.port.in.command.CancelPaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentCancellationResult;
import org.example.sharedprompts.domain.payment.application.port.out.event.PaymentEventPublisherPort;
import org.example.sharedprompts.domain.payment.application.port.out.paymentgateway.PaymentGatewayPort;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentCommandRepositoryPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentUserType;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentNotFoundException;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentValidationException;
import org.example.sharedprompts.domain.user.User;
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
@DisplayName("DefaultPaymentCancellationService 테스트")
class DefaultPaymentCancellationServiceTest {

    @Mock
    private PaymentCommandRepositoryPort paymentRepository;

    @Mock
    private PaymentGatewayPort paymentGateway;

    @Mock
    private PaymentEventPublisherPort eventPublisher;

    private DefaultPaymentCancellationService service;

    @BeforeEach
    void setUp() {
        service = new DefaultPaymentCancellationService(
                paymentRepository,
                paymentGateway,
                eventPublisher
        );
    }

    @Test
    @DisplayName("결제 취소 성공 - SUCCESS 상태")
    void testCancelSuccessPayment() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;
        String reason = "User requested cancellation";

        CancelPaymentCommand command = CancelPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(userId)
                .reason(reason)
                .build();

        User mockUser = new User();
        mockUser.setId(userId);

        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.SUCCESS)
                .userType(PaymentUserType.FREE)
                .refundedAmount(BigDecimal.ZERO)
                .build();

        PaymentGatewayPort.PaymentGatewayResult cancelResult =
                PaymentGatewayPort.PaymentGatewayResult.success("EXT_PAY_ID", null);

        // When
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(mockPayment));
        when(paymentGateway.cancelPayment(mockPayment)).thenReturn(cancelResult);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        PaymentCancellationResult result = service.cancel(command);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getPaymentId());
        assertEquals(PaymentStatus.CANCELED, result.getStatus());
        verify(paymentGateway).cancelPayment(mockPayment);
        verify(paymentRepository).save(mockPayment);
        verify(eventPublisher).publishPaymentCanceled(any());
    }

    @Test
    @DisplayName("결제 취소 성공 - PENDING 상태")
    void testCancelPendingPayment() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;

        CancelPaymentCommand command = CancelPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(userId)
                .reason("Cancel pending payment")
                .build();

        User mockUser = new User();
        mockUser.setId(userId);

        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .status(PaymentStatus.PENDING)
                .refundedAmount(BigDecimal.ZERO)
                .build();

        // When
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        PaymentCancellationResult result = service.cancel(command);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.CANCELED, result.getStatus());
        verify(paymentGateway, never()).cancelPayment(any());
        verify(paymentRepository).save(mockPayment);
    }

    @Test
    @DisplayName("결제를 찾을 수 없으면 실패")
    void testCancelPaymentNotFound() {
        // Given
        Long paymentId = 999L;
        Long userId = 1L;

        CancelPaymentCommand command = CancelPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(userId)
                .reason("reason")
                .build();

        // When & Then
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> service.cancel(command));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("권한이 없으면 실패")
    void testCancelUnauthorized() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;
        Long differentUserId = 999L;

        CancelPaymentCommand command = CancelPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(differentUserId)
                .reason("reason")
                .build();

        User mockUser = new User();
        mockUser.setId(userId);

        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .status(PaymentStatus.SUCCESS)
                .build();

        // When & Then
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(mockPayment));

        assertThrows(PaymentValidationException.class, () -> service.cancel(command));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("취소할 수 없는 상태면 실패")
    void testCancelInvalidStatus() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;

        CancelPaymentCommand command = CancelPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(userId)
                .reason("reason")
                .build();

        User mockUser = new User();
        mockUser.setId(userId);

        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .status(PaymentStatus.REFUNDED)  // Cannot cancel refunded payment
                .build();

        // When & Then
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(mockPayment));

        assertThrows(PaymentValidationException.class, () -> service.cancel(command));
        verifyNoInteractions(paymentGateway);
    }
}

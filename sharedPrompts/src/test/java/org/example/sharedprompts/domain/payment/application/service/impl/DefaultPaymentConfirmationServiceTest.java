package org.example.sharedprompts.domain.payment.application.service.impl;

import org.example.sharedprompts.domain.payment.application.port.in.command.ConfirmPaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentConfirmationResult;
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
@DisplayName("DefaultPaymentConfirmationService 테스트")
class DefaultPaymentConfirmationServiceTest {

    @Mock
    private PaymentCommandRepositoryPort paymentRepository;

    @Mock
    private PaymentGatewayPort paymentGateway;

    @Mock
    private PaymentEventPublisherPort eventPublisher;

    private DefaultPaymentConfirmationService service;

    @BeforeEach
    void setUp() {
        service = new DefaultPaymentConfirmationService(
                paymentRepository,
                paymentGateway,
                eventPublisher
        );
    }

    @Test
    @DisplayName("결제 확인 성공")
    void testConfirmSuccess() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;
        String providerToken = "test_token_123";

        ConfirmPaymentCommand command = ConfirmPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(userId)
                .providerToken(providerToken)
                .rawPayload("{}")
                .build();

        User mockUser = new User();
        mockUser.setId(userId);

        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .userType(PaymentUserType.FREE)
                .build();

        PaymentGatewayPort.PaymentGatewayResult successResult =
                PaymentGatewayPort.PaymentGatewayResult.success("EXT_PAY_ID_123", "APPROVAL_CODE");

        // When
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(mockPayment));
        when(paymentGateway.confirmPayment(mockPayment, providerToken)).thenReturn(successResult);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        PaymentConfirmationResult result = service.confirm(command);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getPaymentId());
        assertEquals("EXT_PAY_ID_123", result.getExternalPaymentId());
        verify(paymentRepository).findByIdForUpdate(paymentId);
        verify(paymentGateway).confirmPayment(mockPayment, providerToken);
        verify(paymentRepository).save(mockPayment);
        verify(eventPublisher).publishPaymentConfirmed(any());
    }

    @Test
    @DisplayName("결제를 찾을 수 없으면 실패")
    void testConfirmPaymentNotFound() {
        // Given
        Long paymentId = 999L;
        Long userId = 1L;

        ConfirmPaymentCommand command = ConfirmPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(userId)
                .providerToken("token")
                .build();

        // When & Then
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> service.confirm(command));
        verify(paymentRepository).findByIdForUpdate(paymentId);
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("권한이 없으면 실패")
    void testConfirmUnauthorized() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;
        Long differentUserId = 999L;

        ConfirmPaymentCommand command = ConfirmPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(differentUserId)
                .providerToken("token")
                .build();

        User mockUser = new User();
        mockUser.setId(userId);

        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .status(PaymentStatus.PENDING)
                .build();

        // When & Then
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(mockPayment));

        assertThrows(PaymentValidationException.class, () -> service.confirm(command));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("이미 처리된 결제면 실패")
    void testConfirmAlreadyProcessed() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;

        ConfirmPaymentCommand command = ConfirmPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(userId)
                .providerToken("token")
                .build();

        User mockUser = new User();
        mockUser.setId(userId);

        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .status(PaymentStatus.SUCCESS)  // Already processed
                .build();

        // When & Then
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(mockPayment));

        assertThrows(PaymentValidationException.class, () -> service.confirm(command));
        verifyNoInteractions(paymentGateway);
    }
}

package org.example.sharedprompts.domain.payment.application.service.impl;

import org.example.sharedprompts.domain.payment.application.port.in.command.ConfirmPaymentCommand;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentConfirmationResult;
import org.example.sharedprompts.domain.payment.application.port.out.event.PaymentEventPublisherPort;
import org.example.sharedprompts.domain.payment.application.port.out.paymentgateway.PaymentConfirmParams;
import org.example.sharedprompts.domain.payment.application.port.out.paymentgateway.PaymentGatewayPort;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentCommandRepositoryPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.transaction.PaymentTransactionManager;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentUserType;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentNotFoundException;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentValidationException;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.domain.payment.application.service.PaymentConfirmParamsResolver;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultPaymentConfirmationService 테스트")
class DefaultPaymentConfirmationServiceTest {

    @Mock
    private PaymentCommandRepositoryPort paymentRepository;

    @Mock
    private PaymentGatewayPort paymentGateway;

    @Mock
    private PaymentConfirmParamsResolver confirmParamsResolver;

    @Mock
    private PaymentEventPublisherPort eventPublisher;

    @Mock
    private PaymentTransactionManager transactionManager;

    private DefaultPaymentConfirmationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new DefaultPaymentConfirmationService(
                paymentRepository,
                paymentGateway,
                confirmParamsResolver,
                eventPublisher,
                transactionManager
        );
        when(transactionManager.executeInTransaction(any(Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(0, Supplier.class).get());
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

        User mockUser = userWithId(userId);

        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .status(PaymentStatus.PENDING)
                .userType(PaymentUserType.PERSONAL)
                .build();

        PaymentGatewayPort.PaymentGatewayResult successResult =
                PaymentGatewayPort.PaymentGatewayResult.success("EXT_PAY_ID_123", "APPROVAL_CODE");

        // When
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(mockPayment));
        when(confirmParamsResolver.resolve(eq(mockPayment), eq(command)))
                .thenReturn(PaymentConfirmParams.of("RESOLVED_KEY", Collections.emptyMap()));
        when(paymentGateway.confirmPayment(eq(mockPayment), any(PaymentConfirmParams.class))).thenReturn(successResult);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        PaymentConfirmationResult result = service.confirm(command);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getPaymentId());
        assertEquals("EXT_PAY_ID_123", result.getExternalPaymentId());
        verify(paymentRepository, times(2)).findByIdForUpdate(paymentId);
        verify(confirmParamsResolver).resolve(mockPayment, command);
        verify(paymentGateway).confirmPayment(eq(mockPayment), any(PaymentConfirmParams.class));
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

        User mockUser = userWithId(userId);

        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .userType(PaymentUserType.PERSONAL)
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

        User mockUser = userWithId(userId);

        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .userType(PaymentUserType.PERSONAL)
                .status(PaymentStatus.SUCCESS)  // Already processed
                .build();

        // When & Then
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(mockPayment));

        assertThrows(PaymentValidationException.class, () -> service.confirm(command));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("게이트웨이 승인 실패 시 FAILED 저장 후 결과 반환(이벤트는 커밋 후 발행)")
    void testConfirmGatewayFailureReturnsResult() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;
        String providerToken = "token";

        ConfirmPaymentCommand command = ConfirmPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(userId)
                .providerToken(providerToken)
                .build();

        User mockUser = userWithId(userId);
        Payment mockPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .status(PaymentStatus.PENDING)
                .userType(PaymentUserType.PERSONAL)
                .build();

        PaymentGatewayPort.PaymentGatewayResult failResult =
                PaymentGatewayPort.PaymentGatewayResult.failure("Gateway error", null);

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(mockPayment));
        when(confirmParamsResolver.resolve(eq(mockPayment), eq(command)))
                .thenReturn(PaymentConfirmParams.of("KEY", Collections.emptyMap()));
        when(paymentGateway.confirmPayment(eq(mockPayment), any(PaymentConfirmParams.class))).thenReturn(failResult);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        PaymentConfirmationResult result = service.confirm(command);

        // Then: 예외 대신 결과 반환, FAILED 저장 및 실패 이벤트 등록(커밋 후 발행)
        assertNotNull(result);
        assertEquals(paymentId, result.getPaymentId());
        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertNull(result.getExternalPaymentId());
        verify(paymentRepository, times(2)).findByIdForUpdate(paymentId);
        verify(paymentRepository).save(any(Payment.class));
        verify(eventPublisher).publishPaymentFailed(any());
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

package org.example.sharedprompts.domain.payment.application.service.impl;

import org.example.sharedprompts.domain.payment.application.port.in.command.PaymentStatusCheckQuery;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentStatusResult;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentQueryRepositoryPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentUserType;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentNotFoundException;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultPaymentStatusCheckService 테스트")
class DefaultPaymentStatusCheckServiceTest {

    @Mock
    private PaymentQueryRepositoryPort paymentRepository;

    private DefaultPaymentStatusCheckService service;

    @BeforeEach
    void setUp() {
        service = new DefaultPaymentStatusCheckService(paymentRepository);
    }

    @Test
    @DisplayName("결제 상태 조회 성공")
    void testCheckStatusSuccess() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;

        PaymentStatusCheckQuery query = PaymentStatusCheckQuery.builder()
                .paymentId(paymentId)
                .userId(userId)
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
                .approvedAt(LocalDateTime.now())
                .build();

        // When
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(mockPayment));

        PaymentStatusResult result = service.checkStatus(query);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals(BigDecimal.valueOf(10000), result.getAmount());
        assertEquals("KRW", result.getCurrency());
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    @DisplayName("결제를 찾을 수 없으면 실패")
    void testCheckStatusPaymentNotFound() {
        // Given
        Long paymentId = 999L;
        Long userId = 1L;

        PaymentStatusCheckQuery query = PaymentStatusCheckQuery.builder()
                .paymentId(paymentId)
                .userId(userId)
                .build();

        // When & Then
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> service.checkStatus(query));
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    @DisplayName("권한이 없으면 실패")
    void testCheckStatusUnauthorized() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;
        Long differentUserId = 999L;

        PaymentStatusCheckQuery query = PaymentStatusCheckQuery.builder()
                .paymentId(paymentId)
                .userId(differentUserId)
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
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(mockPayment));

        assertThrows(PaymentNotFoundException.class, () -> service.checkStatus(query));
        verify(paymentRepository).findById(paymentId);
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

    @Test
    @DisplayName("다양한 결제 상태 조회 성공")
    void testCheckStatusVariousStates() {
        // Given
        Long paymentId = 1L;
        Long userId = 1L;

        PaymentStatusCheckQuery query = PaymentStatusCheckQuery.builder()
                .paymentId(paymentId)
                .userId(userId)
                .build();

        User mockUser = userWithId(userId);

        // Test for PENDING status
        Payment pendingPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .userType(PaymentUserType.PERSONAL)
                .status(PaymentStatus.PENDING)
                .canceledAt(LocalDateTime.now())
                .build();

        // When
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(pendingPayment));

        PaymentStatusResult result = service.checkStatus(query);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getStatus());

        // Test for REFUNDED status
        Payment refundedPayment = Payment.builder()
                .id(paymentId)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .userType(PaymentUserType.PERSONAL)
                .status(PaymentStatus.REFUNDED)
                .canceledAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(refundedPayment));

        result = service.checkStatus(query);

        assertNotNull(result);
        assertEquals(PaymentStatus.REFUNDED, result.getStatus());
    }
}

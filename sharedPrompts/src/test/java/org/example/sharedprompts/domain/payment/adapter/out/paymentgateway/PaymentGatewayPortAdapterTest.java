package org.example.sharedprompts.domain.payment.adapter.out.paymentgateway;

import org.example.sharedprompts.domain.payment.application.dto.response.CancelResult;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.domain.payment.application.dto.response.RefundResult;
import org.example.sharedprompts.domain.payment.application.port.out.paymentgateway.PaymentGatewayPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentUserType;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentGatewayPortAdapter 테스트")
class PaymentGatewayPortAdapterTest {

    @Mock
    private PaymentProviderFactory providerFactory;

    @Mock
    private PaymentProvider paymentProvider;

    private PaymentGatewayPortAdapter adapter;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        adapter = new PaymentGatewayPortAdapter(providerFactory);

        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .provider(Provider.LOCAL)
                .providerId("provider-1")
                .nickname("testuser")
                .role(Role.ROLE_USER)
                .signupCompleted(true)
                .blocked(false)
                .build();

        testPayment = Payment.builder()
                .id(1L)
                .user(user)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.TOSS)
                .userType(PaymentUserType.PERSONAL)
                .idempotencyKey("KEY_123")
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("결제 준비 성공")
    void testPreparePaymentSuccess() {
        // Given
        PaymentProvider.PrepareResult prepareResult = PaymentProvider.PrepareResult.success("TID_123", null, null);

        when(providerFactory.getProvider(PaymentMethod.TOSS)).thenReturn(paymentProvider);
        when(paymentProvider.preparePayment(
                testPayment.getId().toString(),
                testPayment.getAmount(),
                testPayment.getCurrency(),
                "Payment Item",
                testPayment.getUser().getId().toString(),
                testPayment.getIdempotencyKey()
        )).thenReturn(prepareResult);

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.preparePayment(testPayment);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("TID_123", result.getExternalPaymentId());
        assertNull(result.getErrorMessage());
        verify(providerFactory).getProvider(PaymentMethod.TOSS);
        verify(paymentProvider).preparePayment(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("결제 준비 실패")
    void testPreparePaymentFailure() {
        // Given
        when(providerFactory.getProvider(PaymentMethod.TOSS))
                .thenThrow(new RuntimeException("Provider error"));

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.preparePayment(testPayment);

        // Then
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("오류"));
        verify(providerFactory).getProvider(PaymentMethod.TOSS);
    }

    @Test
    @DisplayName("결제 승인 성공")
    void testConfirmPaymentSuccess() {
        // Given
        String approvalToken = "TOKEN_123";
        PaymentResult paymentResult = PaymentResult.builder()
                .externalPaymentId("EXT_PAY_123")
                .status(PaymentStatus.SUCCESS)
                .build();

        when(providerFactory.getProvider(PaymentMethod.TOSS)).thenReturn(paymentProvider);
        when(paymentProvider.confirmPayment(
                eq(approvalToken),
                eq(testPayment.getId().toString()),
                eq(testPayment.getAmount()),
                eq(testPayment.getCurrency()),
                eq(testPayment.getIdempotencyKey()),
                eq(testPayment.getUser().getId().toString()),
                any()
        )).thenReturn(paymentResult);

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.confirmPayment(testPayment, approvalToken);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("EXT_PAY_123", result.getExternalPaymentId());
        verify(providerFactory).getProvider(PaymentMethod.TOSS);
        verify(paymentProvider).confirmPayment(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("결제 승인 실패")
    void testConfirmPaymentFailure() {
        // Given
        String approvalToken = "INVALID_TOKEN";

        when(providerFactory.getProvider(PaymentMethod.TOSS))
                .thenThrow(new RuntimeException("Invalid token"));

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.confirmPayment(testPayment, approvalToken);

        // Then
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("결제 상태 조회 - 지원하지 않음")
    void testQueryPaymentStatusUnsupported() {
        // Given
        String externalPaymentId = "EXT_123";

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.queryPaymentStatus(externalPaymentId);

        // Then
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("제공자를 특정할 수 없습니다"));
    }

    @Test
    @DisplayName("결제 취소 성공")
    void testCancelPaymentSuccess() {
        // Given
        testPayment.updateExternalPaymentId("EXT_PAY_123");
        CancelResult cancelResult = CancelResult.builder()
                .externalPaymentId("EXT_PAY_123")
                .status(PaymentStatus.CANCELED)
                .canceledAt(LocalDateTime.now())
                .reason(null)
                .metadata(null)
                .originalAmount(null)
                .taxFreeAmount(null)
                .build();

        when(providerFactory.getProvider(PaymentMethod.TOSS)).thenReturn(paymentProvider);
        when(paymentProvider.cancelPayment(
                "EXT_PAY_123",
                "User requested cancel",
                testPayment.getIdempotencyKey()
        )).thenReturn(cancelResult);

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.cancelPayment(testPayment);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("EXT_PAY_123", result.getExternalPaymentId());
        verify(providerFactory).getProvider(PaymentMethod.TOSS);
        verify(paymentProvider).cancelPayment(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("결제 취소 실패")
    void testCancelPaymentFailure() {
        // Given
        testPayment.updateExternalPaymentId("INVALID_EXT_ID");

        when(providerFactory.getProvider(PaymentMethod.TOSS))
                .thenThrow(new RuntimeException("Payment not found"));

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.cancelPayment(testPayment);

        // Then
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("환불 성공")
    void testRefundPaymentSuccess() {
        // Given
        testPayment.updateExternalPaymentId("EXT_PAY_123");
        BigDecimal refundAmount = BigDecimal.valueOf(5000);
        RefundResult refundResult = RefundResult.builder()
                .externalPaymentId("EXT_PAY_123")
                .status(PaymentStatus.REFUNDED)
                .refundedAmount(refundAmount)
                .refundedAt(LocalDateTime.now())
                .reason(null)
                .metadata(null)
                .build();

        when(providerFactory.getProvider(PaymentMethod.TOSS)).thenReturn(paymentProvider);
        when(paymentProvider.refundPayment(
                testPayment.getExternalPaymentId(),
                refundAmount,
                "User requested refund",
                testPayment.getIdempotencyKey()
        )).thenReturn(refundResult);

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.refundPayment(testPayment, refundAmount);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("EXT_PAY_123", result.getExternalPaymentId());
        verify(providerFactory).getProvider(PaymentMethod.TOSS);
        verify(paymentProvider).refundPayment(anyString(), any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    @DisplayName("환불 실패 - 금액 초과")
    void testRefundPaymentFailureAmountExceeds() {
        // Given
        testPayment.updateExternalPaymentId("EXT_PAY_123");
        BigDecimal refundAmount = BigDecimal.valueOf(20000); // 원본 금액이 10000

        when(providerFactory.getProvider(PaymentMethod.TOSS))
                .thenThrow(new RuntimeException("Refund amount exceeds payment amount"));

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.refundPayment(testPayment, refundAmount);

        // Then
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("거래 상세 조회")
    void testQueryTransactionDetails() {
        // Given
        String externalPaymentId = "EXT_123";

        // When - 실제 구현에서는 UnsupportedOperationException을 반환
        PaymentGatewayPort.PaymentGatewayResult result = adapter.queryTransactionDetails(externalPaymentId);

        // Then
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("다양한 결제 수단 - TOSS")
    void testDifferentPaymentMethodToss() {
        // Given - already TOSS in setUp
        PaymentProvider.PrepareResult prepareResult = PaymentProvider.PrepareResult.success("TID_TOSS", null, null);

        when(providerFactory.getProvider(PaymentMethod.TOSS)).thenReturn(paymentProvider);
        when(paymentProvider.preparePayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(prepareResult);

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.preparePayment(testPayment);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("TID_TOSS", result.getExternalPaymentId());
    }

    @Test
    @DisplayName("다양한 결제 수단 - KAKAO_PAY")
    void testDifferentPaymentMethodTransfer() {
        // Given - build payment with KAKAO_PAY
        User user = User.builder()
                .id(2L)
                .email("u2@test.com")
                .provider(Provider.LOCAL)
                .providerId("provider-2")
                .nickname("user2")
                .role(Role.ROLE_USER)
                .signupCompleted(true)
                .blocked(false)
                .build();
        Payment kakaoPayment = Payment.builder()
                .id(2L)
                .user(user)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .userType(PaymentUserType.PERSONAL)
                .idempotencyKey("KEY_KAKAO")
                .status(PaymentStatus.PENDING)
                .build();
        PaymentProvider.PrepareResult prepareResult = PaymentProvider.PrepareResult.success("TID_KAKAO", null, null);

        when(providerFactory.getProvider(PaymentMethod.KAKAO_PAY)).thenReturn(paymentProvider);
        when(paymentProvider.preparePayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(prepareResult);

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.preparePayment(kakaoPayment);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("TID_KAKAO", result.getExternalPaymentId());
    }

    @Test
    @DisplayName("부분 환불")
    void testPartialRefund() {
        // Given
        testPayment.updateExternalPaymentId("EXT_PAY_123");
        BigDecimal fullAmount = testPayment.getAmount();
        BigDecimal partialRefundAmount = fullAmount.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);

        RefundResult refundResult = RefundResult.builder()
                .externalPaymentId("EXT_PAY_123")
                .status(PaymentStatus.PARTIALLY_REFUNDED)
                .refundedAmount(partialRefundAmount)
                .refundedAt(LocalDateTime.now())
                .reason(null)
                .metadata(null)
                .build();

        when(providerFactory.getProvider(PaymentMethod.TOSS)).thenReturn(paymentProvider);
        when(paymentProvider.refundPayment(
                testPayment.getExternalPaymentId(),
                partialRefundAmount,
                "User requested refund",
                testPayment.getIdempotencyKey()
        )).thenReturn(refundResult);

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.refundPayment(testPayment, partialRefundAmount);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("EXT_PAY_123", result.getExternalPaymentId());
    }
}

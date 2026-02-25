package org.example.sharedprompts.domain.payment.adapter.out.paymentgateway;

import org.example.sharedprompts.domain.payment.application.dto.response.CancelResult;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.domain.payment.application.dto.response.RefundResult;
import org.example.sharedprompts.domain.payment.application.port.out.paymentgateway.PaymentGatewayPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

        User user = new User();
        user.setId(1L);

        testPayment = Payment.builder()
                .id(1L)
                .user(user)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .paymentMethod(PaymentMethod.CARD)
                .idempotencyKey("KEY_123")
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("결제 준비 성공")
    void testPreparePaymentSuccess() {
        // Given
        PaymentProvider.PrepareResult prepareResult = new PaymentProvider.PrepareResult("TID_123");

        when(providerFactory.getProvider(PaymentMethod.CARD)).thenReturn(paymentProvider);
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
        verify(providerFactory).getProvider(PaymentMethod.CARD);
        verify(paymentProvider).preparePayment(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("결제 준비 실패")
    void testPreparePaymentFailure() {
        // Given
        when(providerFactory.getProvider(PaymentMethod.CARD))
                .thenThrow(new RuntimeException("Provider error"));

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.preparePayment(testPayment);

        // Then
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("오류"));
        verify(providerFactory).getProvider(PaymentMethod.CARD);
    }

    @Test
    @DisplayName("결제 승인 성공")
    void testConfirmPaymentSuccess() {
        // Given
        String approvalToken = "TOKEN_123";
        PaymentResult paymentResult = new PaymentResult();
        paymentResult.setExternalPaymentId("EXT_PAY_123");

        when(providerFactory.getProvider(PaymentMethod.CARD)).thenReturn(paymentProvider);
        when(paymentProvider.confirmPayment(
                approvalToken,
                testPayment.getId().toString(),
                testPayment.getAmount(),
                testPayment.getCurrency(),
                testPayment.getIdempotencyKey(),
                testPayment.getUser().getId().toString(),
                any()
        )).thenReturn(paymentResult);

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.confirmPayment(testPayment, approvalToken);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("EXT_PAY_123", result.getExternalPaymentId());
        verify(providerFactory).getProvider(PaymentMethod.CARD);
        verify(paymentProvider).confirmPayment(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("결제 승인 실패")
    void testConfirmPaymentFailure() {
        // Given
        String approvalToken = "INVALID_TOKEN";

        when(providerFactory.getProvider(PaymentMethod.CARD))
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
        testPayment.setExternalPaymentId("EXT_PAY_123");
        CancelResult cancelResult = new CancelResult();
        cancelResult.setCancelledAmount(testPayment.getAmount());

        when(providerFactory.getProvider(PaymentMethod.CARD)).thenReturn(paymentProvider);
        when(paymentProvider.cancelPayment(
                testPayment.getExternalPaymentId(),
                "User requested cancel",
                testPayment.getIdempotencyKey()
        )).thenReturn(cancelResult);

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.cancelPayment(testPayment);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("EXT_PAY_123", result.getExternalPaymentId());
        verify(providerFactory).getProvider(PaymentMethod.CARD);
        verify(paymentProvider).cancelPayment(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("결제 취소 실패")
    void testCancelPaymentFailure() {
        // Given
        testPayment.setExternalPaymentId("INVALID_EXT_ID");

        when(providerFactory.getProvider(PaymentMethod.CARD))
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
        testPayment.setExternalPaymentId("EXT_PAY_123");
        BigDecimal refundAmount = BigDecimal.valueOf(5000);
        RefundResult refundResult = new RefundResult();
        refundResult.setRefundedAmount(refundAmount);

        when(providerFactory.getProvider(PaymentMethod.CARD)).thenReturn(paymentProvider);
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
        verify(providerFactory).getProvider(PaymentMethod.CARD);
        verify(paymentProvider).refundPayment(anyString(), any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    @DisplayName("환불 실패 - 금액 초과")
    void testRefundPaymentFailureAmountExceeds() {
        // Given
        testPayment.setExternalPaymentId("EXT_PAY_123");
        BigDecimal refundAmount = BigDecimal.valueOf(20000); // 원본 금액이 10000

        when(providerFactory.getProvider(PaymentMethod.CARD))
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
    @DisplayName("다양한 결제 수단 - CARD")
    void testDifferentPaymentMethodCard() {
        // Given
        testPayment.setPaymentMethod(PaymentMethod.CARD);
        PaymentProvider.PrepareResult prepareResult = new PaymentProvider.PrepareResult("TID_CARD");

        when(providerFactory.getProvider(PaymentMethod.CARD)).thenReturn(paymentProvider);
        when(paymentProvider.preparePayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(prepareResult);

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.preparePayment(testPayment);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("TID_CARD", result.getExternalPaymentId());
    }

    @Test
    @DisplayName("다양한 결제 수단 - TRANSFER")
    void testDifferentPaymentMethodTransfer() {
        // Given
        testPayment.setPaymentMethod(PaymentMethod.TRANSFER);
        PaymentProvider.PrepareResult prepareResult = new PaymentProvider.PrepareResult("TID_TRANSFER");

        when(providerFactory.getProvider(PaymentMethod.TRANSFER)).thenReturn(paymentProvider);
        when(paymentProvider.preparePayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(prepareResult);

        // When
        PaymentGatewayPort.PaymentGatewayResult result = adapter.preparePayment(testPayment);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("TID_TRANSFER", result.getExternalPaymentId());
    }

    @Test
    @DisplayName("부분 환불")
    void testPartialRefund() {
        // Given
        testPayment.setExternalPaymentId("EXT_PAY_123");
        BigDecimal fullAmount = testPayment.getAmount();
        BigDecimal partialRefundAmount = fullAmount.divide(BigDecimal.TWO);

        RefundResult refundResult = new RefundResult();
        refundResult.setRefundedAmount(partialRefundAmount);

        when(providerFactory.getProvider(PaymentMethod.CARD)).thenReturn(paymentProvider);
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

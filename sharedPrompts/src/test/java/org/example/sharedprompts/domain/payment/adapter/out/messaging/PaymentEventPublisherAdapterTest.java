package org.example.sharedprompts.domain.payment.adapter.out.messaging;

import org.example.sharedprompts.domain.payment.domain.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventPublisherAdapter 테스트")
class PaymentEventPublisherAdapterTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private PaymentEventPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PaymentEventPublisherAdapter(applicationEventPublisher);
    }

    @Test
    @DisplayName("결제 승인 이벤트 발행")
    void testPublishPaymentApproved() {
        // Given
        PaymentApprovedEvent event = PaymentApprovedEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        adapter.publishPaymentApproved(event);

        // Then
        ArgumentCaptor<PaymentApprovedEvent> captor = ArgumentCaptor.forClass(PaymentApprovedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        PaymentApprovedEvent capturedEvent = captor.getValue();
        assertNotNull(capturedEvent);
        assertEquals(1L, capturedEvent.getPaymentId());
        assertEquals(1L, capturedEvent.getUserId());
        assertEquals(BigDecimal.valueOf(10000), capturedEvent.getAmount());
    }

    @Test
    @DisplayName("결제 확인 이벤트 발행")
    void testPublishPaymentConfirmed() {
        // Given
        PaymentConfirmedEvent event = PaymentConfirmedEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .externalPaymentId("EXT_123")
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        adapter.publishPaymentConfirmed(event);

        // Then
        ArgumentCaptor<PaymentConfirmedEvent> captor = ArgumentCaptor.forClass(PaymentConfirmedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        PaymentConfirmedEvent capturedEvent = captor.getValue();
        assertNotNull(capturedEvent);
        assertEquals(1L, capturedEvent.getPaymentId());
        assertEquals("EXT_123", capturedEvent.getExternalPaymentId());
    }

    @Test
    @DisplayName("결제 취소 이벤트 발행")
    void testPublishPaymentCanceled() {
        // Given
        PaymentCanceledEvent event = PaymentCanceledEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .reason("User requested")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        adapter.publishPaymentCanceled(event);

        // Then
        ArgumentCaptor<PaymentCanceledEvent> captor = ArgumentCaptor.forClass(PaymentCanceledEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        PaymentCanceledEvent capturedEvent = captor.getValue();
        assertNotNull(capturedEvent);
        assertEquals(1L, capturedEvent.getPaymentId());
        assertEquals("User requested", capturedEvent.getReason());
    }

    @Test
    @DisplayName("결제 환불 이벤트 발행")
    void testPublishPaymentRefunded() {
        // Given
        PaymentRefundedEvent event = PaymentRefundedEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .refundAmount(BigDecimal.valueOf(5000))
                .reason("Partial refund")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        adapter.publishPaymentRefunded(event);

        // Then
        ArgumentCaptor<PaymentRefundedEvent> captor = ArgumentCaptor.forClass(PaymentRefundedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        PaymentRefundedEvent capturedEvent = captor.getValue();
        assertNotNull(capturedEvent);
        assertEquals(1L, capturedEvent.getPaymentId());
        assertEquals(BigDecimal.valueOf(5000), capturedEvent.getRefundAmount());
        assertEquals("Partial refund", capturedEvent.getReason());
    }

    @Test
    @DisplayName("결제 실패 이벤트 발행")
    void testPublishPaymentFailed() {
        // Given
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .errorCode("PAYMENT_FAILED")
                .errorMessage("Payment processing failed")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        adapter.publishPaymentFailed(event);

        // Then
        ArgumentCaptor<PaymentFailedEvent> captor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        PaymentFailedEvent capturedEvent = captor.getValue();
        assertNotNull(capturedEvent);
        assertEquals(1L, capturedEvent.getPaymentId());
        assertEquals("PAYMENT_FAILED", capturedEvent.getErrorCode());
        assertEquals("Payment processing failed", capturedEvent.getErrorMessage());
    }

    @Test
    @DisplayName("결제 만료 이벤트 발행")
    void testPublishPaymentExpired() {
        // Given
        PaymentExpiredEvent event = PaymentExpiredEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .timestamp(LocalDateTime.now())
                .build();

        // When
        adapter.publishPaymentExpired(event);

        // Then
        ArgumentCaptor<PaymentExpiredEvent> captor = ArgumentCaptor.forClass(PaymentExpiredEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        PaymentExpiredEvent capturedEvent = captor.getValue();
        assertNotNull(capturedEvent);
        assertEquals(1L, capturedEvent.getPaymentId());
        assertNotNull(capturedEvent.getTimestamp());
    }

    @Test
    @DisplayName("모든 이벤트 발행 검증")
    void testAllEventsPublished() {
        // Given
        PaymentApprovedEvent approvedEvent = PaymentApprovedEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .build();

        PaymentConfirmedEvent confirmedEvent = PaymentConfirmedEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .externalPaymentId("EXT_123")
                .build();

        PaymentCanceledEvent canceledEvent = PaymentCanceledEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .reason("test")
                .build();

        PaymentRefundedEvent refundedEvent = PaymentRefundedEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .refundAmount(BigDecimal.valueOf(5000))
                .reason("test")
                .timestamp(LocalDateTime.now())
                .build();

        PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .errorCode("TEST_ERROR")
                .errorMessage("test error")
                .build();

        PaymentExpiredEvent expiredEvent = PaymentExpiredEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .build();

        // When
        adapter.publishPaymentApproved(approvedEvent);
        adapter.publishPaymentConfirmed(confirmedEvent);
        adapter.publishPaymentCanceled(canceledEvent);
        adapter.publishPaymentRefunded(refundedEvent);
        adapter.publishPaymentFailed(failedEvent);
        adapter.publishPaymentExpired(expiredEvent);

        // Then
        verify(applicationEventPublisher, times(6)).publishEvent(any());
    }

    @Test
    @DisplayName("이벤트 발행 예외 처리")
    void testPublishEventWithException() {
        // Given
        PaymentApprovedEvent event = PaymentApprovedEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .build();

        doThrow(new RuntimeException("Event publisher error"))
                .when(applicationEventPublisher).publishEvent(any());

        // When & Then
        assertThrows(RuntimeException.class, () -> adapter.publishPaymentApproved(event));
        verify(applicationEventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("여러 사용자 이벤트 동시 발행")
    void testPublishMultipleUserEventsSequentially() {
        // Given
        PaymentApprovedEvent event1 = PaymentApprovedEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .build();

        PaymentApprovedEvent event2 = PaymentApprovedEvent.builder()
                .paymentId(2L)
                .userId(2L)
                .amount(BigDecimal.valueOf(20000))
                .currency("KRW")
                .build();

        // When
        adapter.publishPaymentApproved(event1);
        adapter.publishPaymentApproved(event2);

        // Then
        verify(applicationEventPublisher, times(2)).publishEvent(any());
    }

    @Test
    @DisplayName("이벤트 필드 검증 - 필수 필드")
    void testEventFieldsValidation() {
        // Given
        PaymentApprovedEvent event = PaymentApprovedEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        adapter.publishPaymentApproved(event);

        // Then
        ArgumentCaptor<PaymentApprovedEvent> captor = ArgumentCaptor.forClass(PaymentApprovedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        PaymentApprovedEvent capturedEvent = captor.getValue();
        assertNotNull(capturedEvent.getPaymentId());
        assertNotNull(capturedEvent.getUserId());
        assertNotNull(capturedEvent.getAmount());
        assertNotNull(capturedEvent.getCurrency());
        assertNotNull(capturedEvent.getTimestamp());
    }

    @Test
    @DisplayName("발행된 이벤트 타입 검증")
    void testPublishedEventTypes() {
        // Given
        PaymentApprovedEvent approvedEvent = PaymentApprovedEvent.builder()
                .paymentId(1L)
                .userId(1L)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .build();

        // When
        adapter.publishPaymentApproved(approvedEvent);

        // Then
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        Object publishedEvent = captor.getValue();
        assertInstanceOf(PaymentApprovedEvent.class, publishedEvent);
    }
}

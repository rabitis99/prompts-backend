package org.example.sharedprompts.domain.payment.application.service.impl;

import org.example.sharedprompts.domain.payment.application.port.in.command.PaymentWebhookCommand;
import org.example.sharedprompts.domain.payment.application.port.out.webhook.PaymentWebhookProcessingPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
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
@DisplayName("DefaultPaymentWebhookHandlingService 테스트")
class DefaultPaymentWebhookHandlingServiceTest {

    @Mock
    private PaymentWebhookProcessingPort webhookProcessingPort;

    private DefaultPaymentWebhookHandlingService service;

    @BeforeEach
    void setUp() {
        service = new DefaultPaymentWebhookHandlingService(webhookProcessingPort);
    }

    @Test
    @DisplayName("웹훅 처리 - 포트에 위임")
    void testHandleWebhookDelegatesToPort() {
        PaymentWebhookCommand command = PaymentWebhookCommand.builder()
                .paymentMethod(PaymentMethod.TOSS)
                .payload("{\"externalPaymentId\": \"EXT_12345\"}")
                .build();

        when(webhookProcessingPort.processWebhook(any(), any(), any(), any())).thenReturn(Optional.empty());

        Optional<Payment> result = service.handleWebhook(command);

        assertTrue(result.isEmpty());
        verify(webhookProcessingPort).processWebhook(eq(PaymentMethod.TOSS), eq(command.getPayload()), eq(command.getSignature()), any());
    }

    @Test
    @DisplayName("웹훅 처리 - 포트가 결제 반환 시 해당 결제 반환")
    void testHandleWebhookReturnsPaymentFromPort() {
        PaymentWebhookCommand command = PaymentWebhookCommand.builder()
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .payload("{}")
                .build();

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        Payment mockPayment = Payment.builder()
                .id(1L)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .status(PaymentStatus.PENDING)
                .build();

        when(webhookProcessingPort.processWebhook(any(), any(), any(), any())).thenReturn(Optional.of(mockPayment));

        Optional<Payment> result = service.handleWebhook(command);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals(PaymentStatus.PENDING, result.get().getStatus());
    }

    @Test
    @DisplayName("웹훅 처리 - null payload")
    void testHandleWebhookNullPayload() {
        PaymentWebhookCommand command = PaymentWebhookCommand.builder()
                .paymentMethod(PaymentMethod.PAYPAL)
                .payload(null)
                .build();

        when(webhookProcessingPort.processWebhook(any(), any(), any(), any())).thenReturn(Optional.empty());

        Optional<Payment> result = service.handleWebhook(command);

        assertTrue(result.isEmpty());
    }
}

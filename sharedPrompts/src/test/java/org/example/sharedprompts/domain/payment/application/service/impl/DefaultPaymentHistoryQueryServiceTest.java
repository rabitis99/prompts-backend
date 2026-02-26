package org.example.sharedprompts.domain.payment.application.service.impl;

import org.example.sharedprompts.domain.payment.application.port.in.command.PaymentHistoryQuery;
import org.example.sharedprompts.domain.payment.application.port.in.result.PaymentHistoryResult;
import org.example.sharedprompts.domain.payment.application.port.out.repository.PaymentQueryRepositoryPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultPaymentHistoryQueryService 테스트")
class DefaultPaymentHistoryQueryServiceTest {

    @Mock
    private PaymentQueryRepositoryPort paymentRepository;

    private DefaultPaymentHistoryQueryService service;

    @BeforeEach
    void setUp() {
        service = new DefaultPaymentHistoryQueryService(paymentRepository);
    }

    @Test
    @DisplayName("결제 내역 조회 happy-path: repository stub, DTO 매핑 검증 (Query UseCase → Port)")
    void testGetHistorySuccess() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        PaymentHistoryQuery query = PaymentHistoryQuery.builder()
                .userId(userId)
                .pageable(pageable)
                .build();

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);

        Payment payment1 = Payment.builder()
                .id(1L)
                .user(mockUser)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .status(PaymentStatus.SUCCESS)
                .approvedAt(LocalDateTime.now())
                .build();

        Payment payment2 = Payment.builder()
                .id(2L)
                .user(mockUser)
                .amount(BigDecimal.valueOf(50000))
                .currency("KRW")
                .status(PaymentStatus.SUCCESS)
                .approvedAt(LocalDateTime.now().minusHours(1))
                .build();

        Payment payment3 = Payment.builder()
                .id(3L)
                .user(mockUser)
                .amount(BigDecimal.valueOf(30000))
                .currency("KRW")
                .status(PaymentStatus.REFUNDED)
                .approvedAt(LocalDateTime.now().minusDays(1))
                .build();

        List<Payment> payments = Arrays.asList(payment1, payment2, payment3);
        Page<Payment> paymentPage = new PageImpl<>(payments, pageable, 3);

        // When
        when(paymentRepository.findByUserId(userId, pageable)).thenReturn(paymentPage);

        Page<PaymentHistoryResult> result = service.getHistory(query);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(3, result.getContent().size());

        PaymentHistoryResult firstResult = result.getContent().get(0);
        assertEquals(1L, firstResult.getId());
        assertEquals(BigDecimal.valueOf(10000), firstResult.getAmount());
        assertEquals("KRW", firstResult.getCurrency());
        assertEquals(PaymentStatus.SUCCESS, firstResult.getStatus());

        verify(paymentRepository).findByUserId(userId, pageable);
    }

    @Test
    @DisplayName("결제 내역 조회 - 빈 페이지")
    void testGetHistoryEmptyPage() {
        // Given
        Long userId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        PaymentHistoryQuery query = PaymentHistoryQuery.builder()
                .userId(userId)
                .pageable(pageable)
                .build();

        Page<Payment> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        // When
        when(paymentRepository.findByUserId(userId, pageable)).thenReturn(emptyPage);

        Page<PaymentHistoryResult> result = service.getHistory(query);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());
        verify(paymentRepository).findByUserId(userId, pageable);
    }

    @Test
    @DisplayName("결제 내역 조회 - 페이지네이션")
    void testGetHistoryPagination() {
        // Given
        Long userId = 1L;
        Pageable firstPageable = PageRequest.of(0, 2);

        PaymentHistoryQuery query = PaymentHistoryQuery.builder()
                .userId(userId)
                .pageable(firstPageable)
                .build();

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);

        List<Payment> firstPagePayments = Arrays.asList(
                Payment.builder()
                        .id(1L)
                        .user(mockUser)
                        .amount(BigDecimal.valueOf(10000))
                        .currency("KRW")
                        .status(PaymentStatus.SUCCESS)
                        .approvedAt(LocalDateTime.now())
                        .build(),
                Payment.builder()
                        .id(2L)
                        .user(mockUser)
                        .amount(BigDecimal.valueOf(20000))
                        .currency("KRW")
                        .status(PaymentStatus.SUCCESS)
                        .approvedAt(LocalDateTime.now())
                        .build()
        );

        Page<Payment> firstPageResult = new PageImpl<>(firstPagePayments, firstPageable, 5);

        // When
        when(paymentRepository.findByUserId(userId, firstPageable)).thenReturn(firstPageResult);

        Page<PaymentHistoryResult> result = service.getHistory(query);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(2, result.getContent().size());
        assertEquals(0, result.getNumber()); // First page
        assertTrue(result.hasNext());
        assertFalse(result.hasPrevious());

        verify(paymentRepository).findByUserId(userId, firstPageable);
    }

    @Test
    @DisplayName("결제 내역 조회 - 다양한 상태")
    void testGetHistoryVariousStatuses() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        PaymentHistoryQuery query = PaymentHistoryQuery.builder()
                .userId(userId)
                .pageable(pageable)
                .build();

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);

        List<Payment> payments = Arrays.asList(
                Payment.builder()
                        .id(1L)
                        .user(mockUser)
                        .amount(BigDecimal.valueOf(10000))
                        .currency("KRW")
                        .status(PaymentStatus.SUCCESS)
                        .approvedAt(LocalDateTime.now())
                        .build(),
                Payment.builder()
                        .id(2L)
                        .user(mockUser)
                        .amount(BigDecimal.valueOf(20000))
                        .currency("KRW")
                        .status(PaymentStatus.REFUNDED)
                        .approvedAt(LocalDateTime.now())
                        .build(),
                Payment.builder()
                        .id(3L)
                        .user(mockUser)
                        .amount(BigDecimal.valueOf(30000))
                        .currency("KRW")
                        .status(PaymentStatus.PARTIALLY_REFUNDED)
                        .approvedAt(LocalDateTime.now())
                        .build(),
                Payment.builder()
                        .id(4L)
                        .user(mockUser)
                        .amount(BigDecimal.valueOf(40000))
                        .currency("KRW")
                        .status(PaymentStatus.CANCELED)
                        .approvedAt(LocalDateTime.now())
                        .build()
        );

        Page<Payment> paymentPage = new PageImpl<>(payments, pageable, 4);

        // When
        when(paymentRepository.findByUserId(userId, pageable)).thenReturn(paymentPage);

        Page<PaymentHistoryResult> result = service.getHistory(query);

        // Then
        assertNotNull(result);
        assertEquals(4, result.getContent().size());
        assertEquals(PaymentStatus.SUCCESS, result.getContent().get(0).getStatus());
        assertEquals(PaymentStatus.REFUNDED, result.getContent().get(1).getStatus());
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, result.getContent().get(2).getStatus());
        assertEquals(PaymentStatus.CANCELED, result.getContent().get(3).getStatus());

        verify(paymentRepository).findByUserId(userId, pageable);
    }
}

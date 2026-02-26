package org.example.sharedprompts.domain.payment.adapter.out.persistence;

import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRepositoryAdapter 테스트")
class PaymentRepositoryAdapterTest {

    @Mock
    private PaymentJpaAdapter paymentJpaAdapter;

    private PaymentRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PaymentRepositoryAdapter(paymentJpaAdapter);
    }

    @Test
    @DisplayName("결제 저장 성공")
    void testSaveSuccess() {
        // Given
        User user = new User();
        user.setId(1L);

        Payment payment = Payment.builder()
                .id(1L)
                .user(user)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        // When
        when(paymentJpaAdapter.save(payment)).thenReturn(payment);
        Payment result = adapter.save(payment);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        verify(paymentJpaAdapter).save(payment);
    }

    @Test
    @DisplayName("ID로 결제 조회 성공")
    void testFindByIdSuccess() {
        // Given
        Long paymentId = 1L;
        User user = new User();
        user.setId(1L);

        Payment payment = Payment.builder()
                .id(paymentId)
                .user(user)
                .amount(BigDecimal.valueOf(10000))
                .currency("KRW")
                .status(PaymentStatus.SUCCESS)
                .build();

        // When
        when(paymentJpaAdapter.findById(paymentId)).thenReturn(Optional.of(payment));
        Optional<Payment> result = adapter.findById(paymentId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(paymentId, result.get().getId());
        verify(paymentJpaAdapter).findById(paymentId);
    }

    @Test
    @DisplayName("ID로 결제 조회 - 없음")
    void testFindByIdNotFound() {
        // Given
        Long paymentId = 999L;

        // When
        when(paymentJpaAdapter.findById(paymentId)).thenReturn(Optional.empty());
        Optional<Payment> result = adapter.findById(paymentId);

        // Then
        assertTrue(result.isEmpty());
        verify(paymentJpaAdapter).findById(paymentId);
    }

    @Test
    @DisplayName("ID로 결제 조회 (잠금) 성공")
    void testFindByIdForUpdateSuccess() {
        // Given
        Long paymentId = 1L;
        User user = new User();
        user.setId(1L);

        Payment payment = Payment.builder()
                .id(paymentId)
                .user(user)
                .amount(BigDecimal.valueOf(10000))
                .status(PaymentStatus.SUCCESS)
                .build();

        // When
        when(paymentJpaAdapter.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        Optional<Payment> result = adapter.findByIdForUpdate(paymentId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(paymentId, result.get().getId());
        verify(paymentJpaAdapter).findByIdForUpdate(paymentId);
    }

    @Test
    @DisplayName("외부 결제 ID로 결제 조회 성공")
    void testFindByExternalPaymentIdSuccess() {
        // Given
        String externalPaymentId = "EXT_123";
        User user = new User();
        user.setId(1L);

        Payment payment = Payment.builder()
                .id(1L)
                .user(user)
                .externalPaymentId(externalPaymentId)
                .amount(BigDecimal.valueOf(10000))
                .status(PaymentStatus.SUCCESS)
                .build();

        // When
        when(paymentJpaAdapter.findByExternalPaymentId(externalPaymentId)).thenReturn(Optional.of(payment));
        Optional<Payment> result = adapter.findByExternalPaymentId(externalPaymentId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(externalPaymentId, result.get().getExternalPaymentId());
        verify(paymentJpaAdapter).findByExternalPaymentId(externalPaymentId);
    }

    @Test
    @DisplayName("사용자별 결제 조회 성공")
    void testFindByUserIdSuccess() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        User user = new User();
        user.setId(userId);

        List<Payment> payments = Arrays.asList(
                Payment.builder()
                        .id(1L)
                        .user(user)
                        .amount(BigDecimal.valueOf(10000))
                        .status(PaymentStatus.SUCCESS)
                        .build(),
                Payment.builder()
                        .id(2L)
                        .user(user)
                        .amount(BigDecimal.valueOf(20000))
                        .status(PaymentStatus.SUCCESS)
                        .build()
        );

        Page<Payment> paymentPage = new PageImpl<>(payments, pageable, 2);

        // When
        when(paymentJpaAdapter.findByUserIdWithFetchJoin(userId, pageable)).thenReturn(paymentPage);
        Page<Payment> result = adapter.findByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        verify(paymentJpaAdapter).findByUserIdWithFetchJoin(userId, pageable);
    }

    @Test
    @DisplayName("멱등성 키로 결제 조회")
    void testFindByIdempotencyKey() {
        // Given
        String idempotencyKey = "KEY_123";
        User user = new User();
        user.setId(1L);

        Payment payment = Payment.builder()
                .id(1L)
                .user(user)
                .idempotencyKey(idempotencyKey)
                .status(PaymentStatus.PENDING)
                .build();

        // When
        when(paymentJpaAdapter.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(payment));
        Optional<Payment> result = adapter.findByIdempotencyKey(idempotencyKey);

        // Then
        assertTrue(result.isPresent());
        assertEquals(idempotencyKey, result.get().getIdempotencyKey());
        verify(paymentJpaAdapter).findByIdempotencyKey(idempotencyKey);
    }

    @Test
    @DisplayName("상태와 날짜로 결제 개수 조회")
    void testCountByStatusAndCreatedAtAfter() {
        // Given
        PaymentStatus status = PaymentStatus.SUCCESS;
        LocalDateTime dateTime = LocalDateTime.now().minusDays(1);
        long expectedCount = 5L;

        // When
        when(paymentJpaAdapter.countByDateAndStatus(dateTime, status)).thenReturn(expectedCount);
        long result = adapter.countByStatusAndCreatedAtAfter(status, dateTime);

        // Then
        assertEquals(expectedCount, result);
        verify(paymentJpaAdapter).countByDateAndStatus(dateTime, status);
    }

    @Test
    @DisplayName("오늘의 상태별 결제 개수 조회")
    void testCountTodayPaymentsByStatus() {
        // Given
        Long userId = 1L;
        PaymentStatus status = PaymentStatus.SUCCESS;
        long expectedCount = 3L;

        // When
        when(paymentJpaAdapter.countTodaySuccessfulPayments(userId, status)).thenReturn(expectedCount);
        long result = adapter.countTodayPaymentsByStatus(userId, status);

        // Then
        assertEquals(expectedCount, result);
        verify(paymentJpaAdapter).countTodaySuccessfulPayments(userId, status);
    }

    @Test
    @DisplayName("재시도 가능한 결제 조회")
    void testFindRetryablePayments() {
        // Given
        User user = new User();
        user.setId(1L);
        PaymentStatus status = PaymentStatus.PENDING;
        int maxRetry = 5;
        LocalDateTime now = LocalDateTime.now();

        List<Payment> retryablePayments = Arrays.asList(
                Payment.builder()
                        .id(1L)
                        .user(user)
                        .status(PaymentStatus.PENDING)
                        .build()
        );

        // When
        when(paymentJpaAdapter.findRetryablePayments(status, maxRetry, now))
                .thenReturn(retryablePayments);
        List<Payment> result = adapter.findRetryablePayments(status, maxRetry, now);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.stream().allMatch(p -> p.getStatus() == PaymentStatus.PENDING));
        verify(paymentJpaAdapter).findRetryablePayments(status, maxRetry, now);
    }

    @Test
    @DisplayName("만료된 대기 중인 결제 조회")
    void testFindExpiredPendingPayments() {
        // Given
        LocalDateTime expirationTime = LocalDateTime.now().minusHours(1);
        User user = new User();
        user.setId(1L);

        List<Payment> expiredPayments = Arrays.asList(
                Payment.builder()
                        .id(1L)
                        .user(user)
                        .status(PaymentStatus.PENDING)
                        .createdAt(expirationTime.minusHours(1))
                        .build()
        );

        // When
        when(paymentJpaAdapter.findExpiredPendingPayments(PaymentStatus.PENDING, expirationTime))
                .thenReturn(expiredPayments);
        List<Payment> result = adapter.findExpiredPendingPayments(expirationTime);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(paymentJpaAdapter).findExpiredPendingPayments(PaymentStatus.PENDING, expirationTime);
    }

    @Test
    @DisplayName("외부 결제 ID 존재 여부 확인")
    void testExistsByExternalPaymentId() {
        // Given
        String externalPaymentId = "EXT_123";

        // When
        when(paymentJpaAdapter.existsByExternalPaymentId(externalPaymentId)).thenReturn(true);
        boolean result = adapter.existsByExternalPaymentId(externalPaymentId);

        // Then
        assertTrue(result);
        verify(paymentJpaAdapter).existsByExternalPaymentId(externalPaymentId);
    }

    @Test
    @DisplayName("ID 존재 여부 확인")
    void testExistsById() {
        // Given
        Long paymentId = 1L;

        // When
        when(paymentJpaAdapter.existsById(paymentId)).thenReturn(true);
        boolean result = adapter.existsById(paymentId);

        // Then
        assertTrue(result);
        verify(paymentJpaAdapter).existsById(paymentId);
    }

    @Test
    @DisplayName("결제 삭제")
    void testDelete() {
        // Given
        User user = new User();
        user.setId(1L);

        Payment payment = Payment.builder()
                .id(1L)
                .user(user)
                .build();

        // When
        adapter.delete(payment);

        // Then
        verify(paymentJpaAdapter).delete(payment);
    }
}

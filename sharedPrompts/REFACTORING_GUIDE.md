# Payment Domain 헥사고날 리팩토링 - 통합 가이드

## 1. 레거시 코드와의 통합 전략

### 1.1 PaymentFacade의 역할 (임시 어댑터)

기존 PaymentFacade는 임시 호환성 레이어로 유지:

```java
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentApprovalUseCase paymentApprovalUseCase;
    private final PaymentConfirmationUseCase paymentConfirmationUseCase;
    private final PaymentCancellationUseCase paymentCancellationUseCase;
    private final PaymentRefundUseCase paymentRefundUseCase;
    private final PaymentStatusCheckUseCase paymentStatusCheckUseCase;
    private final PaymentHistoryQueryUseCase paymentHistoryQueryUseCase;

    @Transactional
    public PaymentResponseDto requestPayment(Long userId, PaymentRequestDto request) {
        var command = new ApprovePaymentCommand(...);
        var result = paymentApprovalUseCase.approve(command);
        return mapToResponse(result);
    }
    // ... 다른 메서드들도 유사하게 매핑
}
```

### 1.2 레거시 서비스와의 조화

| 레거시 서비스 | 새 UseCase에서의 활용 |
|------------|------------------|
| PaymentRequestService | DefaultPaymentApprovalService에서 호출 |
| PaymentConfirmService | DefaultPaymentConfirmationService에서 호출 |
| PaymentValidationService | 모든 서비스에서 호출 |
| PaymentAmountProcessingService | 금액 관련 UseCase에서 호출 |
| PaymentCreator | 결제 생성 시 호출 |
| PaymentPreparationHandler | 준비 단계에서 호출 |

---

## 2. TODO 해결 현황

### 2.1 이벤트 발행 (@TransactionalEventListener)

**문제**: 트랜잭션 완료 후 이벤트 발행 필요

**해결 방안**:

```java
@Transactional
public PaymentApprovalResult approve(ApprovePaymentCommand command) {
    // 비즈니스 로직
    Payment payment = ... // 저장됨
    return result;
}

// 트랜잭션 완료 후 발행
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onPaymentApproved(PaymentApprovedEvent event) {
    log.info("결제 승인됨: {}", event.getPaymentId());
    // Credit 도메인 등이 구독할 수 있음
}
```

**구현 위치**: `PaymentEventPublisherAdapter`에서 이미 `ApplicationEventPublisher` 사용 중

### 2.2 Payment 엔티티 메서드 호출

**문제**: `payment.approve()` 메서드 호출 형식 불일치

**기존 구현**:
```java
public void approve(String externalPaymentId) {
    markSuccess(externalPaymentId);
}

public void markSuccess(String externalPaymentId) {
    this.status = PaymentStatus.SUCCESS;
    if (externalPaymentId != null) {
        this.externalPaymentId = externalPaymentId;
    }
    this.approvedAt = LocalDateTime.now();
}
```

**사용 코드 (DefaultPaymentConfirmationService)**:
```java
// ✅ 올바른 호출
payment.approve();  // 외부 ID는 이미 설정됨
payment.markSuccess(confirmResult.externalPaymentId);  // 또는 이 방식

// JPA 자동 감지로 저장됨
paymentRepository.save(payment);  // 또는 flush
```

### 2.3 웹훅 처리 세부 구현

**레거시 코드 활용**:

```java
@Override
public Optional<Payment> handleWebhook(PaymentWebhookCommand command) {
    try {
        // 1. 웹훅 검증
        WebhookVerifier.verify(command.getSignature(), command.getPayload());

        // 2. 멱등성 확인
        WebhookIdempotencyService.checkIdempotency(command.getPayload());

        // 3. 페이로드 파싱 (제공자별)
        PaymentProvider provider = providerFactory.getProvider(command.getPaymentMethod());
        WebhookPayload payload = provider.parseWebhookPayload(command.getPayload());

        // 4. 결제 조회 및 업데이트
        Payment payment = paymentRepository.findByExternalPaymentId(payload.externalPaymentId)
                .orElseThrow(...);

        if (payload.isSuccessful()) {
            payment.markSuccess(payload.externalPaymentId);
        } else if (payload.isFailed()) {
            payment.markFailed(payload.failureReason);
        }

        return Optional.of(paymentRepository.save(payment));

    } catch (Exception e) {
        log.error("Webhook 처리 실패", e);
        return Optional.empty();
    }
}
```

---

## 3. 컴파일 오류 해결

### 3.1 UserRepository 의존성

**추가 필요**:
```java
// PaymentBeanConfiguration에 추가
private final UserRepository userRepository;

@Bean
public DefaultPaymentApprovalService defaultPaymentApprovalService(...) {
    return new DefaultPaymentApprovalService(
        paymentRepositoryPort,
        paymentGatewayPort,
        eventPublisherPort,
        paymentValidationService,
        amountProcessingService,
        paymentCreator,
        preparationHandler,
        paymentJpaAdapter,
        loggingService,
        userRepository  // 추가
    );
}
```

### 3.2 refund() 메서드 호출

**Payment 엔티티의 refund() 메서드**:
```java
public void refund(BigDecimal refundAmount) {
    BigDecimal newRefundedAmount = this.refundedAmount.add(refundAmount);

    if (newRefundedAmount.compareTo(this.amount) >= 0) {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAmount = this.amount;
    } else {
        this.status = PaymentStatus.PARTIALLY_REFUNDED;
        this.refundedAmount = newRefundedAmount;
    }
}
```

**사용 코드 (DefaultPaymentRefundService)**:
```java
payment.refund(refundAmount);  // 메서드 호출
payment = paymentRepository.save(payment);  // 자동 감지로 저장
```

---

## 4. 레거시 코드 제거 로드맵

### Phase A: 호환성 유지 (현재)
- ✅ 기존 PaymentFacade 유지
- ✅ 기존 서비스들 유지
- ✅ 새 UseCase에서 레거시 서비스 호출

### Phase B: 점진적 마이그레이션 (향후)
- TestPaymentFacade가 새 UseCase를 사용하도록 변경
- 기존 클라이언트가 새 구조로 이동
- 중복 로직 통합

### Phase C: 완전 제거 (최종)
- PaymentFacade 완전 제거
- 레거시 서비스 분해
- 모든 코드가 UseCase를 직접 호출

---

## 5. 구현 체크리스트

### DefaultPaymentApprovalService
- [x] 레거시 서비스 주입
- [x] 방향성 명확화
- [ ] 포인트 회복 처리 (실패 시) 추가 필요

### DefaultPaymentConfirmationService
- [ ] payment.approve() 메서드 호출 방식 검증
- [ ] 외부 ID 업데이트 로직 확인
- [ ] 후처리 로직 (점수, 캐시백) 통합 필요

### DefaultPaymentCancellationService
- [ ] payment.cancel() / markCanceled() 메서드 호출
- [ ] 포인트 회복 로직 필요

### DefaultPaymentRefundService
- [ ] payment.refund() 호출 확인
- [ ] 부분 환불 상태 관리 검증

### PaymentRepositoryAdapter
- [ ] idempotencyKey 조회 메서드 구현 필요
- [ ] 재시도 가능 결제 조회 구현

### PaymentGatewayPortAdapter
- [ ] 제공자별 결과 파싱 로직 추가
- [ ] 외부 ID 추출 로직 구현

### PaymentEventPublisherAdapter
- [ ] ApplicationEventPublisher와 @TransactionalEventListener 검증

---

## 6. 동시성 관리

### 낙관적 락 (Optimistic Lock)
```java
// Payment.java의 @Version 필드 활용
@Version
private Long version;

// 동시 업데이트 감지 시 OptimisticLockingFailureException 발생
// DefaultPaymentConfirmationService에서 처리
try {
    payment = paymentRepository.findByIdForUpdate(paymentId);
    // 업데이트
    paymentRepository.save(payment);
} catch (ObjectOptimisticLockingFailureException e) {
    // 재시도 또는 에러 반환
}
```

### 비관적 락 (Pessimistic Lock)
```java
// PaymentRepositoryPort의 findByIdForUpdate()
Optional<Payment> payment = paymentRepository.findByIdForUpdate(paymentId);
// SELECT FOR UPDATE 사용 - 동시 수정 방지
```

---

## 7. 이벤트 구독 예시 (Credit 도메인)

```java
@Component
public class PaymentEventListener {

    private final ChargeCreditUseCase chargeCreditUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentApproved(PaymentApprovedEvent event) {
        // Payment 성공 → Credit 적립
        chargeCreditUseCase.chargeCredit(
            event.getUserId(),
            event.getAmount(),
            "PAYMENT",
            event.getMetadata()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        // Payment 환불 → Credit 회수
        refundCreditUseCase.refundCredit(
            event.getUserId(),
            event.getRefundAmount(),
            "PAYMENT_REFUND"
        );
    }
}
```

---

## 8. 다음 단계

1. **컴파일 오류 최종 수정**
   - UserRepository 의존성 추가
   - Payment 메서드 호출 검증
   - 모든 서비스 파일 검토

2. **통합 테스트 작성**
   - UseCase 단위 테스트
   - 레거시 서비스 통합 테스트
   - E2E 테스트

3. **배포 준비**
   - API 호환성 검증
   - 성능 테스트
   - 점진적 롤아웃 계획


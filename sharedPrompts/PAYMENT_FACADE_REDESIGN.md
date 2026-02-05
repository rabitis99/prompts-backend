# 결제 도메인 Facade 패턴 기반 재설계

## 1. 클래스 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PaymentFacade (Facade)                          │
│  - requestPayment()                                                     │
│  - confirmPayment()                                                     │
│  - cancelPayment()                                                      │
│  - refundPayment()                                                      │
│  - checkPaymentStatus()                                                 │
│  - getPaymentHistory()                                                  │
└─────────────────────────────────────────────────────────────────────────┘
                              │
                              │ orchestrates
                              ▼
        ┌─────────────────────────────────────────────────────┐
        │         PaymentTransactionOrchestrator                │
        │  - executeWithLockAndTransaction()                    │
        │  - executeWithIdempotency()                           │
        │  - executeWithCompensation()                          │
        └─────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ PaymentRequest│   │PaymentConfirm │   │PaymentCancel  │
│   Service     │   │   Service     │   │   Service     │
└───────────────┘   └───────────────┘   └───────────────┘
        │                     │                     │
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│PaymentRefund  │   │PaymentAmount  │   │PaymentProvider│
│   Service     │   │  Processing   │   │ Integration   │
└───────────────┘   │   Service     │   │   Service     │
                    └───────────────┘   └───────────────┘
                              │
                              ▼
                    ┌───────────────┐
                    │PaymentMetadata│
                    │   Service     │
                    └───────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│PaymentPost    │   │Compensation   │   │PaymentLogging │
│ProcessService │   │   Handler     │   │   Service     │
└───────────────┘   └───────────────┘   └───────────────┘
```

## 2. 책임 분리 상세

### 2.1 PaymentFacade (Facade)
- **책임**: 클라이언트에게 단일 진입점 제공, 전체 결제 프로세스 조율
- **의존성**: 모든 하위 서비스들

### 2.2 PaymentRequestService
- **책임**: 결제 요청 처리
  - 일일 제한 검증
  - 금액 처리 (환율, 포인트)
  - Payment 엔티티 생성
  - PG사 준비 (카카오페이 등)
  - 로깅

### 2.3 PaymentConfirmService
- **책임**: 결제 승인 처리
  - 소유권 검증
  - 상태 검증
  - PG사 승인 호출
  - 성공/실패 후처리
  - 로깅

### 2.4 PaymentCancelService
- **책임**: 결제 취소 처리
  - 소유권/상태 검증
  - PG사 취소 호출
  - 취소 후처리 (포인트 복구)
  - 로깅

### 2.5 PaymentRefundService
- **책임**: 결제 환불 처리
  - 소유권/상태/금액 검증
  - PG사 환불 호출
  - 환불 후처리 (포인트 복구)
  - 로깅

### 2.6 PaymentAmountProcessingService
- **책임**: 금액 관련 처리
  - 환율 변환
  - 포인트 사용/복구
  - 실제 결제 금액 계산
  - 환불 포인트 계산

### 2.7 PaymentProviderIntegrationService
- **책임**: PG사 연동
  - 결제 준비 (카카오페이)
  - 결제 승인
  - 결제 취소
  - 결제 환불
  - Idempotency 키 생성

### 2.8 PaymentMetadataService
- **책임**: 메타데이터 처리
  - 상품명 추출
  - Redirect URL 추가
  - 메타데이터 파싱/생성

### 2.9 PaymentTransactionOrchestrator
- **책임**: 트랜잭션, 락 통합 처리
  - 분산 락 + 트랜잭션 실행
  - 락 키 생성

### 2.10 CompensationHandler
- **책임**: 보상 큐 처리 통합
  - 후처리 실패 시 보상 큐 등록
  - 보상 태스크 생성

## 3. 호출 흐름

### 3.1 requestPayment 흐름
```
PaymentFacade.requestPayment()
  → PaymentRequestService.processRequest()
    → PaymentValidationService.validateDailyLimit()
    → PaymentAmountProcessingService.processAmount()
      → ExchangeRateService.getExchangeRate()
      → PointService.usePoints()
    → PaymentMetadataService.extractProductName()
    → PaymentProviderIntegrationService.preparePayment() [if KAKAO_PAY]
    → PaymentJpaAdapter.save()
    → PaymentLoggingService.logPaymentRequest()
```

### 3.2 confirmPayment 흐름
```
PaymentFacade.confirmPayment()
  → PaymentTransactionOrchestrator.executeWithLockAndTransaction()
    → PaymentConfirmService.confirm()
      → PaymentValidationService.validatePaymentOwnership()
      → PaymentValidator.validatePaymentKey()
      → PaymentAmountProcessingService.calculateActualAmount()
      → PaymentProviderIntegrationService.approvePayment()
      → PaymentExecutionService.executePayment()
      → PaymentPostProcessService.processPaymentSuccessAfterCommit()
        → PointService.accumulatePoints()
        → CashbackService.accumulateCashback()
      → CompensationHandler.handlePostProcessFailure() [if failed]
```

### 3.3 cancelPayment 흐름
```
PaymentFacade.cancelPayment()
  → PaymentCancelService.cancel()
    → PaymentValidationService.validatePaymentOwnership()
    → PaymentValidationService.validateCancelableStatus()
    → PaymentTransactionOrchestrator.executeWithLockAndTransaction()
      → PaymentExecutionService.executeCancel()
        → PaymentProviderIntegrationService.cancelPayment()
      → PaymentPostProcessService.processPaymentCancelAfterCommit()
        → PointService.addPointsDirectly() [포인트 복구]
      → CompensationHandler.handlePostProcessFailure() [if failed]
```

### 3.4 refundPayment 흐름
```
PaymentFacade.refundPayment()
  → PaymentRefundService.refund()
    → PaymentTransactionOrchestrator.executeWithLockAndTransaction()
      → PaymentValidationService.validatePaymentOwnership()
      → PaymentValidationService.validateRefundableStatus()
      → PaymentValidationService.validateRefundAmount()
      → PaymentAmountProcessingService.calculateRefundPointAmount()
      → PaymentExecutionService.executeRefund()
        → PaymentProviderIntegrationService.refundPayment()
      → PaymentPostProcessService.processPaymentRefundAfterCommit()
        → PointService.addPointsDirectly() [포인트 복구]
      → CompensationHandler.handlePostProcessFailure() [if failed]
```

## 4. 개선 사항

### 4.1 단일 책임 원칙 (SRP)
- 각 서비스가 하나의 명확한 책임만 가짐
- 결제 요청/승인/취소/환불이 각각 독립적인 서비스로 분리

### 4.2 코드 간결화
- 중복 로직 제거 (트랜잭션, 락, 보상 큐 처리)
- 공통 유틸리티화 (메타데이터, 금액 계산)

### 4.3 재사용성 향상
- PaymentTransactionOrchestrator로 트랜잭션/락/idempotency 패턴 재사용
- CompensationHandler로 보상 큐 처리 패턴 재사용

### 4.4 테스트 용이성
- 각 서비스를 독립적으로 테스트 가능
- Mock 객체 주입이 용이

## 5. 주요 메서드 예시 코드

### 5.1 PaymentFacade.requestPayment()

```java
@Override
@Transactional
public PaymentResponseDto requestPayment(Long userId, PaymentRequestDto request) {
    String traceId = null;
    try {
        traceId = loggingService.startTrace(null, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        
        Payment payment = requestService.processRequest(userId, request, user);
        
        if (traceId != null && payment.getId() != null) {
            loggingService.startTrace(payment.getId(), userId);
        }
        return PaymentResponseDto.from(payment);
    } finally {
        loggingService.endTrace();
    }
}
```

### 5.2 PaymentFacade.confirmPayment()

```java
@Override
@Transactional
public PaymentConfirmResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
    return confirmService.confirm(userId, request);
}
```

### 5.3 PaymentConfirmService.confirm()

```java
public PaymentConfirmResponse confirm(Long userId, PaymentConfirmRequest request) {
    Long paymentId = request.getOrderIdAsLong();
    String lockKey = orchestrator.createLockKey("payment", paymentId) + ":state";
    
    long[] processingTimeHolder = new long[1];
    boolean[] paymentSucceededHolder = new boolean[1];
    
    try {
        PaymentConfirmResponse response = orchestrator.executeWithLockAndTransaction(lockKey, () -> {
            Payment payment = paymentJpaAdapter.findByIdForUpdate(paymentId)
                    .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
            
            validationService.validatePaymentOwnership(payment, userId);
            
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                return toConfirmResponse(payment, request);
            }
            
            paymentValidator.validatePaymentKey(request.getPaymentKey(), payment.getPaymentMethod());
            BigDecimal calculatedActualAmount = amountProcessingService.calculateActualAmount(
                    payment.getAmount(), payment.getUsedPointAmount());
            
            Map<String, String> additionalParams = buildAdditionalParams(payment, request);
            payment = executionService.executePayment(payment, calculatedActualAmount, additionalParams);
            
            processingTimeHolder[0] = System.currentTimeMillis() - startTime;
            paymentSucceededHolder[0] = true;
            
            return toConfirmResponse(payment, request);
        });
        
        if (paymentSucceededHolder[0]) {
            processSuccessPostCommit(paymentId, userId, processingTimeHolder[0]);
        }
        
        return response;
    } catch (ObjectOptimisticLockingFailureException e) {
        // 낙관적 락 충돌 처리
        Payment fresh = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        if (fresh.getStatus() == PaymentStatus.SUCCESS) {
            return toConfirmResponse(fresh, request);
        }
        throw e;
    }
}
```

### 5.4 PaymentFacade.cancelPayment()

```java
@Override
@Transactional
public PaymentResponseDto cancelPayment(Long userId, PaymentCancelRequestDto request) {
    Payment canceledPayment = cancelService.cancel(userId, request);
    return PaymentResponseDto.from(canceledPayment);
}
```

### 5.5 PaymentCancelService.cancel()

```java
@Transactional
public Payment cancel(Long userId, PaymentCancelRequestDto request) {
    Payment payment = paymentJpaAdapter.findById(request.getPaymentIdAsLong())
            .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
    
    validationService.validatePaymentOwnership(payment, userId);
    validationService.validateCancelableStatus(payment);
    
    Payment canceledPayment = executionService.executeCancel(payment, request.getReasonOrDefault());
    
    try {
        postProcessService.processPaymentCancelAfterCommit(
                canceledPayment.getId(), userId, request.getReasonOrDefault());
    } catch (Exception e) {
        compensationHandler.handlePostProcessFailure(
                CompensationTaskType.POINT_RECOVERY_CANCEL,
                canceledPayment.getId(), userId,
                canceledPayment.getUsedPointAmount(),
                e.getMessage());
    }
    
    return canceledPayment;
}
```

### 5.6 PaymentFacade.refundPayment()

```java
@Override
@Transactional
public PaymentResponseDto refundPayment(Long userId, PaymentRefundRequestDto request) {
    Payment refundedPayment = refundService.refund(userId, request);
    return PaymentResponseDto.from(refundedPayment);
}
```

### 5.7 PaymentRefundService.refund()

```java
public Payment refund(Long userId, PaymentRefundRequestDto request) {
    Long paymentId = request.getPaymentIdAsLong();
    String lockKey = orchestrator.createLockKey("payment", paymentId) + ":state";
    
    Payment refundedPayment = orchestrator.executeWithLockAndTransaction(lockKey, () -> {
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        validationService.validatePaymentOwnership(payment, userId);
        validationService.validateRefundableStatus(payment);
        BigDecimal refundAmount = validationService.validateRefundAmount(request.getAmount(), payment);
        
        return executionService.executeRefund(payment, refundAmount, request.getReasonOrDefault());
    });
    
    BigDecimal refundAmount = validationService.validateRefundAmount(request.getAmount(), refundedPayment);
    BigDecimal refundPointAmount = amountProcessingService.calculateRefundPointAmount(
            refundedPayment.getUsedPointAmount(),
            refundedPayment.getAmount(),
            refundAmount);
    
    try {
        postProcessService.processPaymentRefundAfterCommit(
                refundedPayment.getId(), userId, refundAmount, refundPointAmount, request.getReasonOrDefault());
    } catch (Exception e) {
        compensationHandler.handlePostProcessFailure(
                CompensationTaskType.POINT_RECOVERY_REFUND,
                refundedPayment.getId(), userId, refundPointAmount, e.getMessage());
    }
    
    return refundedPayment;
}
```

### 5.8 PaymentTransactionOrchestrator

```java
@Component
@RequiredArgsConstructor
public class PaymentTransactionOrchestrator {
    private final PaymentTransactionManager transactionManager;
    private final DistributedLockService distributedLockService;
    
    public <T> T executeWithLockAndTransaction(String lockKey, Supplier<T> task) {
        return transactionManager.executeWithLockAndTransaction(lockKey, task);
    }
    
    public String createLockKey(String prefix, Long id) {
        return distributedLockService.createLockKey(prefix, id);
    }
}
```

### 5.9 CompensationHandler

```java
@Component
@RequiredArgsConstructor
public class CompensationHandler {
    private final CompensationQueue compensationQueue;
    
    public void handlePostProcessFailure(CompensationTaskType taskType, Long paymentId, Long userId,
                                        BigDecimal amount, String errorMessage) {
        CompensationTask task = new CompensationTask(
                taskType, paymentId, userId, amount, null, errorMessage, null);
        compensationQueue.enqueue(task);
    }
}
```


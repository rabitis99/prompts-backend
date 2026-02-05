# Payment 도메인 아키텍처 분석

## 1. 책임 분류 (기능적 / 도메인적 / 기술적 관점)

### 1.1 기능적 책임 (Use Case / Application Layer)

**결제 생명주기 관리**
- `PaymentService` / `PaymentServiceImpl`: 결제 요청, 승인, 취소, 환불, 상태 조회
- `PaymentFacade`: 클라이언트 진입점, 여러 서비스 조율
- `PaymentRetryFacade`: 재시도 로직 조율
- `PaymentWebhookFacade`: Webhook 처리 조율

**금액 처리**
- `PaymentAmountFacade`: 환율 변환, 포인트 사용, 실제 결제 금액 계산

**후처리**
- `PaymentPostProcessService`: 포인트/캐시백 적립, 이벤트 발행, 메트릭 수집

**상태 동기화**
- `PaymentStatusSyncService`: 외부 결제사 API와 DB 상태 동기화

### 1.2 도메인적 책임 (Domain Layer)

**핵심 엔티티**
- `Payment`: 결제 상태, 금액, 멱등성 키 등 핵심 도메인 모델
  - 상태 전이 메서드: `markSuccess()`, `markFailed()`, `cancel()`, `refund()`
  - 비즈니스 규칙: `isRetryable()`, `isExpired()`, `getRefundableAmount()`
- `Point`, `Cashback`: 포인트/캐시백 도메인 모델
- `ExchangeRate`: 환율 도메인 모델

**도메인 서비스**
- `PaymentValidationService`: 결제 관련 비즈니스 규칙 검증
  - 일일 결제 제한, 소유권 검증, 상태 전이 검증
- `PaymentValidator`: 결제사별 paymentKey 형식 검증

**도메인 이벤트**
- `PaymentEvent`: 결제 성공/실패/취소/환불 이벤트
- `PaymentEventPublisher`: 이벤트 발행

### 1.3 기술적 책임 (Infrastructure Layer)

**외부 API 통신**
- `PaymentProvider` 인터페이스: 결제사 API 추상화
- `TossPaymentProvider`, `KakaoPayPaymentProvider`, `PayPalPaymentProvider`: 결제사별 구현
- `PaymentProviderFactory`: Provider 선택 전략

**인프라스트럭처**
- `DistributedLockService`: 분산 락 관리 (ShedLock 기반)
- `PaymentRepository`: JPA Repository
- `PaymentLoggingService`: 결제 로깅
- `PaymentMetrics`: 메트릭 수집

**트랜잭션 관리**
- `TransactionTemplate`: 프로그래밍 방식 트랜잭션 제어
- `@Transactional` 어노테이션: 선언적 트랜잭션 관리

---

## 2. SRP와 변경 가능성 기준 분석

### 2.1 반드시 분리해야 하는 지점

#### ✅ **외부 결제사 API 의존성과 내부 비즈니스 로직 분리**

**현재 상태:**
- `PaymentProvider` 인터페이스로 잘 추상화됨
- 각 Provider 구현체가 결제사별 특화 로직 캡슐화

**분리 이유:**
- 결제사 추가/변경 시 Provider 구현체만 수정
- 내부 비즈니스 규칙(일일 제한, 포인트 적립률 등)과 독립적으로 변경 가능
- 테스트 시 Mock Provider 주입 용이

**현재 구조 평가: ✅ 잘 분리됨**

#### ✅ **멱등성 키 생성과 외부 API 호출 분리**

**현재 상태:**
- `PaymentExecutionService.executePayment()`에서 멱등성 키 생성 후 Provider 호출
- 멱등성 키 생성 로직이 ExecutionService에 포함됨

**분리 필요성:**
- 멱등성 전략 변경 시(예: UUID 기반 → 해시 기반) ExecutionService 수정 불필요
- 멱등성 키 생성은 **도메인 책임** (결제 식별), Provider 호출은 **인프라 책임**

**개선 제안:**
```java
// 멱등성 키 생성 전략을 별도 서비스로 분리
public interface IdempotencyKeyGenerator {
    String generateForPayment(Payment payment);
    String generateForRefund(Payment payment, BigDecimal refundedAmount);
}
```

#### ✅ **트랜잭션 경계와 비즈니스 로직 분리**

**현재 상태:**
- `PaymentServiceImpl.confirmPayment()`에서 분산 락 → 트랜잭션 → 비즈니스 로직이 혼재
- `TransactionTemplate`을 사용하여 락 획득 후 트랜잭션 시작 (✅ 올바른 순서)

**분리 필요성:**
- 트랜잭션 전파 전략 변경 시 비즈니스 로직 수정 불필요
- 테스트 시 트랜잭션 없이 비즈니스 로직만 테스트 가능

**개선 제안:**
```java
// 트랜잭션 경계를 명확히 분리
public class PaymentTransactionManager {
    public <T> T executeInTransaction(Supplier<T> task) {
        return transactionTemplate.execute(status -> task.get());
    }
}
```

### 2.2 분리하면 오히려 복잡도가 증가하는 지점

#### ❌ **Payment 엔티티의 상태 전이 메서드 분리**

**현재 상태:**
- `Payment.markSuccess()`, `markFailed()`, `cancel()`, `refund()` 등이 엔티티 내부에 존재
- 상태 전이 검증은 `PaymentValidationService`에서 수행

**분리하지 않는 이유:**
- 상태 전이는 Payment 엔티티의 **핵심 책임**
- 상태와 상태 전이 로직을 분리하면 데이터와 행동의 불일치 발생
- DDD 원칙: 엔티티는 자신의 상태를 관리하는 책임을 가짐

**현재 구조 평가: ✅ 적절함**

#### ❌ **PaymentExecutionService의 즉시 재시도 로직 분리**

**현재 상태:**
- `executePaymentWithImmediateRetry()`가 ExecutionService 내부에 존재
- 즉시 재시도와 지연 재시도(스케줄러)가 분리되어 있음

**분리하지 않는 이유:**
- 즉시 재시도는 **결제 실행의 일부**로 보는 것이 자연스러움
- 별도 서비스로 분리하면 ExecutionService → RetryService 의존성 추가
- 재시도 정책 변경 시 두 곳을 수정해야 하는 복잡도 증가

**현재 구조 평가: ✅ 적절함**

#### ❌ **PaymentPostProcessService의 세부 정책 분리**

**현재 상태:**
- `PointAccrualPolicy`, `CashbackAccrualPolicy`로 정책이 이미 분리됨
- PostProcessService는 정책을 조율하는 역할만 수행

**추가 분리 불필요:**
- 정책 변경 시 Policy 클래스만 수정하면 됨
- PostProcessService를 더 세분화하면 오히려 조율 복잡도 증가

**현재 구조 평가: ✅ 적절함**

---

## 3. 레이어 관점에서 역할 침범 평가

### 3.1 Controller 레이어

**현재 상태:**
- `PaymentController`: Facade만 호출, 비즈니스 로직 없음 ✅
- `PaymentWebhookController`: Facade 호출, 서명 검증은 Provider에 위임 ✅

**평가: ✅ 역할 침범 없음**

### 3.2 Service 레이어

#### ⚠️ **Infrastructure 책임 침범**

**문제점:**
1. **트랜잭션 관리 책임**
   - `PaymentServiceImpl`이 `TransactionTemplate`을 직접 생성/관리
   - 트랜잭션 전파 전략이 Service 레이어에 하드코딩됨

2. **분산 락 관리 책임**
   - `PaymentServiceImpl.confirmPayment()`에서 분산 락 키 생성 및 획득
   - 락 획득 순서와 트랜잭션 순서를 Service가 직접 관리

**개선 제안:**
```java
// 트랜잭션 경계를 별도 매니저로 분리
@Component
public class PaymentTransactionBoundary {
    private final TransactionTemplate transactionTemplate;
    private final DistributedLockService lockService;
    
    public <T> T executeWithLockAndTransaction(String lockKey, Supplier<T> task) {
        return lockService.executeWithLock(lockKey, () -> 
            transactionTemplate.execute(status -> task.get())
        );
    }
}
```

#### ✅ **도메인 책임은 적절히 수행**

- `PaymentValidationService`: 비즈니스 규칙 검증 ✅
- `PaymentExecutionService`: 결제 실행 조율 ✅
- `PaymentPostProcessService`: 후처리 조율 ✅

### 3.3 Domain 레이어

**현재 상태:**
- `Payment` 엔티티: 상태 전이 메서드, 비즈니스 규칙 메서드 포함 ✅
- `PaymentValidationService`: 상태 전이 검증, 비즈니스 규칙 검증 ✅

**평가: ✅ 역할 침범 없음**

### 3.4 Infrastructure 레이어

**현재 상태:**
- `PaymentProvider`: 외부 API 추상화 ✅
- `DistributedLockService`: 분산 락 추상화 ✅
- `PaymentRepository`: JPA Repository ✅

**평가: ✅ 역할 침범 없음**

---

## 4. 결제 도메인 특성 고려한 구조 개선안

### 4.1 외부 결제사 API 의존성 독립성

**현재 구조: ✅ 잘 분리됨**
- `PaymentProvider` 인터페이스로 추상화
- Factory 패턴으로 Provider 선택

**추가 개선안:**

#### 4.1.1 Provider별 정책 분리 강화

**현재:**
- 각 Provider 구현체 내부에 정책 로직 포함 (예: `TossPayAmountPolicy`, `KakaoPayRefundPolicy`)

**개선:**
```java
// Provider별 정책을 별도 패키지로 명확히 분리
provider/
  ├── PaymentProvider.java (인터페이스)
  ├── impl/
  │   ├── TossPaymentProvider.java (조율만)
  │   └── ...
  └── policy/
      ├── AmountPolicy.java (인터페이스)
      ├── RefundPolicy.java (인터페이스)
      └── toss/
          ├── TossPayAmountPolicy.java
          └── TossPayRefundPolicy.java
```

#### 4.1.2 Provider별 예외 처리 전략 분리

**현재:**
- `DuplicateOrderIdException` 등 Provider별 예외가 도메인 레이어에 노출

**개선:**
```java
// Provider별 예외를 Provider 패키지 내부로 캡슐화
provider/
  └── toss/
      └── exception/
          └── DuplicateOrderIdException.java (package-private 또는 Provider 내부 처리)
```

### 4.2 내부 비즈니스 규칙 독립성

**현재 구조: ✅ 잘 분리됨**
- `PaymentValidationService`: 비즈니스 규칙 검증
- `PointAccrualPolicy`, `CashbackAccrualPolicy`: 적립 정책 분리

**추가 개선안:**

#### 4.2.1 비즈니스 규칙을 도메인 서비스로 명확히 분리

**현재:**
- 일일 결제 제한, 포인트 적립률 등이 여러 서비스에 산재

**개선:**
```java
// 비즈니스 규칙을 명확히 분리
domain/
  └── payment/
      └── service/
          └── rule/
              ├── PaymentLimitRule.java (일일 제한)
              ├── PointAccrualRule.java (포인트 적립률)
              └── CashbackAccrualRule.java (캐시백 적립률)
```

### 4.3 멱등성 / 재시도 / 트랜잭션 경계 독립성

#### 4.3.1 멱등성 전략 분리

**현재:**
- 멱등성 키 생성이 `PaymentExecutionService`에 포함

**개선:**
```java
// 멱등성 전략을 별도 서비스로 분리
public interface IdempotencyService {
    String generateKey(Payment payment, String action);
    boolean isProcessed(String key);
    void markAsProcessed(String key);
}
```

#### 4.3.2 재시도 전략 분리

**현재:**
- 즉시 재시도: `PaymentExecutionService` 내부
- 지연 재시도: `PaymentRetryService` (스케줄러)

**개선:**
```java
// 재시도 전략을 통합 인터페이스로 추상화
public interface RetryStrategy {
    boolean shouldRetry(Exception e, int attemptCount);
    long calculateDelay(int attemptCount);
    int getMaxAttempts();
}

// 즉시 재시도 전략
@Component
public class ImmediateRetryStrategy implements RetryStrategy { ... }

// 지수 백오프 재시도 전략
@Component
public class ExponentialBackoffRetryStrategy implements RetryStrategy { ... }
```

#### 4.3.3 트랜잭션 경계 명확화

**현재:**
- `PaymentServiceImpl`에서 `TransactionTemplate` 직접 관리
- 락 획득과 트랜잭션 순서를 Service가 직접 제어

**개선:**
```java
// 트랜잭션 경계를 별도 매니저로 분리
@Component
public class PaymentTransactionBoundary {
    private final TransactionTemplate transactionTemplate;
    private final DistributedLockService lockService;
    
    /**
     * 락 획득 → 트랜잭션 시작 → 작업 수행 → 커밋 → 락 해제
     */
    public <T> T executeWithLockAndTransaction(
        String lockKey, 
        Supplier<T> task
    ) {
        return lockService.executeWithLock(lockKey, () -> 
            transactionTemplate.execute(status -> task.get())
        );
    }
    
    /**
     * 트랜잭션만 실행 (락 없이)
     */
    public <T> T executeInTransaction(Supplier<T> task) {
        return transactionTemplate.execute(status -> task.get());
    }
}
```

---

## 5. 트랜잭션 전파 검토

### 5.1 REQUIRES_NEW 사용 현황

#### ✅ **의미적으로 올바른 사용**

**1. `PaymentPostProcessService.processPaymentSuccessAfterCommit()`**
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void processPaymentSuccessAfterCommit(...)
```

**의미:**
- 결제 승인 트랜잭션이 커밋된 후 별도 트랜잭션에서 포인트/캐시백 적립
- 락 해제 후 후처리 수행하여 교착 상태 방지

**평가: ✅ 올바름**

**2. `PaymentRetryService`의 재시도 메서드들**
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void retryPayment(...)
```

**의미:**
- 스케줄러에서 호출되는 재시도는 독립 트랜잭션으로 실행
- 상위 트랜잭션과 독립적으로 실패/성공 처리

**평가: ✅ 올바름**

#### ⚠️ **의미적으로 개선 필요**

**1. `PaymentExecutionService.saveIdempotencyKeyInNewTransaction()` (Deprecated)**
```java
@Deprecated
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveIdempotencyKeyInNewTransaction(...)
```

**문제점:**
- Deprecated 처리됨 (✅ 올바른 판단)
- Pessimistic lock이 걸린 트랜잭션 내에서 REQUIRES_NEW 사용 시 lock timeout 발생 가능

**평가: ✅ 이미 개선됨 (같은 트랜잭션에서 처리)**

### 5.2 비즈니스 트랜잭션과 기술적 트랜잭션 분리 필요 지점

#### ✅ **현재 잘 분리된 지점**

**1. 결제 승인과 후처리 분리**
```java
// PaymentServiceImpl.confirmPayment()
// 1. 결제 승인 트랜잭션 (락 + 트랜잭션)
PaymentConfirmResponse response = distributedLockService.executeWithLock(
    lockKey, 
    () -> transactionTemplate.execute(txStatus -> {
        // 결제 승인 로직
    })
);

// 2. 후처리 트랜잭션 (별도 트랜잭션)
if (paymentSucceededHolder[0]) {
    postProcessService.processPaymentSuccessAfterCommit(...);
}
```

**평가: ✅ 비즈니스 트랜잭션(결제 승인)과 기술적 트랜잭션(후처리)이 분리됨**

#### ⚠️ **개선 필요 지점**

**1. 환불 처리의 트랜잭션 경계**

**현재:**
```java
// PaymentServiceImpl.refundPayment()
return distributedLockService.executeWithLock(lockKey, () -> {
    return transactionTemplate.execute(status -> {
        // 환불 실행 + 후처리 모두 같은 트랜잭션
        payment = executionService.executeRefund(...);
        postProcessService.processPaymentRefund(...);
    });
});
```

**문제점:**
- 환불 실행(외부 API 호출)과 후처리(포인트 복구)가 같은 트랜잭션
- 외부 API 호출 실패 시 전체 롤백 → 후처리도 롤백됨
- 하지만 후처리 실패는 이미 로깅만 하고 예외를 던지지 않음 (일관성 부족)

**개선안:**
```java
// 환불 실행 트랜잭션
Payment refundedPayment = distributedLockService.executeWithLock(lockKey, () -> 
    transactionTemplate.execute(status -> {
        return executionService.executeRefund(...);
    })
);

// 후처리 트랜잭션 (별도)
try {
    postProcessService.processPaymentRefundAfterCommit(
        refundedPayment.getId(), 
        userId, 
        refundAmount, 
        ...
    );
} catch (Exception e) {
    // 후처리 실패는 보상 트랜잭션 큐에 추가
    compensationQueue.enqueue(...);
}
```

**2. 취소 처리의 트랜잭션 경계**

**현재:**
```java
// PaymentServiceImpl.cancelPayment()
@Transactional
public PaymentResponseDto cancelPayment(...) {
    payment = executionService.executeCancel(...);
    postProcessService.processPaymentCancel(...);
}
```

**문제점:**
- 취소 실행과 후처리가 같은 트랜잭션
- 후처리 실패 시 전체 롤백 (하지만 예외를 던지지 않음 - 일관성 부족)

**개선안:**
- 환불과 동일하게 분리 고려

### 5.3 트랜잭션 경계 분리 원칙

**비즈니스 트랜잭션 (Business Transaction):**
- 결제 승인, 취소, 환불 등 **핵심 비즈니스 로직**
- 외부 API 호출 포함
- 실패 시 전체 롤백 필요

**기술적 트랜잭션 (Technical Transaction):**
- 포인트/캐시백 적립, 이벤트 발행, 메트릭 수집 등 **부가 기능**
- 실패해도 핵심 비즈니스에는 영향 없음
- 보상 트랜잭션으로 처리 가능

**분리 기준:**
1. **외부 API 호출이 포함된 작업**은 별도 트랜잭션으로 분리
2. **후처리 작업**은 핵심 비즈니스 트랜잭션 커밋 후 별도 트랜잭션으로 실행
3. **후처리 실패**는 보상 트랜잭션 큐에 추가하여 나중에 재시도

---

## 6. 최종 추천 결론

### 6.1 현재 구조를 유지하는 선택의 장단점

#### ✅ **장점**

1. **실용적인 복잡도 관리**
   - 현재 구조는 **충분히 모듈화**되어 있음
   - 각 서비스의 책임이 명확히 분리됨
   - 새로운 결제사 추가 시 Provider만 구현하면 됨

2. **검증된 동시성 제어**
   - 분산 락 + Pessimistic lock 조합으로 동시성 문제 해결
   - 락 획득 → 트랜잭션 시작 순서 보장
   - 멱등성 키를 통한 중복 호출 방지

3. **점진적 개선 가능**
   - 현재 구조에서도 필요한 부분만 개선 가능
   - 큰 리팩토링 없이 점진적으로 개선 가능

4. **팀 학습 곡선이 낮음**
   - 현재 구조를 이해하기 쉬움
   - 과도한 추상화로 인한 복잡도 증가 없음

#### ⚠️ **단점**

1. **Service 레이어의 Infrastructure 책임**
   - `PaymentServiceImpl`이 트랜잭션/락 관리 직접 수행
   - 트랜잭션 전파 전략 변경 시 Service 수정 필요

2. **트랜잭션 경계가 명확하지 않은 부분**
   - 환불/취소의 후처리가 핵심 트랜잭션과 같은 경계
   - 후처리 실패 시 보상 처리 전략이 일관되지 않음

3. **멱등성 전략이 하드코딩됨**
   - 멱등성 키 생성 로직이 ExecutionService에 포함
   - 전략 변경 시 ExecutionService 수정 필요

### 6.2 분리했을 때 얻는 이점과 감수해야 할 비용

#### ✅ **이점**

1. **변경 가능성 향상**
   - 멱등성 전략 변경 시 ExecutionService 수정 불필요
   - 트랜잭션 전파 전략 변경 시 비즈니스 로직 수정 불필요
   - 재시도 전략 변경 시 ExecutionService 수정 불필요

2. **테스트 용이성 향상**
   - 트랜잭션 없이 비즈니스 로직만 테스트 가능
   - 멱등성 전략을 Mock으로 교체 가능
   - 재시도 전략을 Mock으로 교체 가능

3. **책임 명확화**
   - Service 레이어는 비즈니스 로직만 담당
   - Infrastructure 레이어는 기술적 관심사만 담당

#### ⚠️ **비용**

1. **추상화 레이어 증가**
   - `IdempotencyService`, `RetryStrategy`, `PaymentTransactionBoundary` 등 추가
   - 코드 이해를 위한 학습 곡선 증가
   - 디버깅 시 추적해야 할 레이어 증가

2. **과도한 엔지니어링 위험**
   - 현재 요구사항에서는 과도한 추상화일 수 있음
   - YAGNI 원칙 위반 가능성

3. **리팩토링 비용**
   - 기존 코드 수정 범위가 큼
   - 테스트 코드도 함께 수정 필요

### 6.3 추천 결론

#### 🎯 **현재 구조를 유지하되, 점진적으로 개선**

**이유:**

1. **현재 구조가 이미 충분히 모듈화되어 있음**
   - Provider 패턴으로 결제사 의존성 분리 ✅
   - Facade 패턴으로 복잡한 로직 분리 ✅
   - Policy 패턴으로 비즈니스 규칙 분리 ✅
   - 도메인 엔티티가 상태 전이 책임 보유 ✅

2. **실용적인 복잡도 관리**
   - 과도한 추상화는 오히려 복잡도 증가
   - 현재 구조는 **"적절한 복잡도"**를 유지하고 있음

3. **점진적 개선이 가능**
   - 필요한 부분만 선택적으로 개선 가능
   - 큰 리팩토링 없이도 개선 가능

#### 📋 **우선순위별 개선 제안**

**High Priority (즉시 개선 권장):**

1. **트랜잭션 경계 명확화**
   ```java
   // PaymentTransactionBoundary 도입
   // 환불/취소 후처리를 별도 트랜잭션으로 분리
   ```

2. **후처리 실패 시 보상 트랜잭션 큐 도입**
   ```java
   // 후처리 실패 시 큐에 추가하여 나중에 재시도
   // 일관된 보상 처리 전략 수립
   ```

**Medium Priority (점진적 개선):**

3. **멱등성 전략 분리**
   ```java
   // IdempotencyService 도입
   // 전략 변경 시 ExecutionService 수정 불필요
   ```

4. **재시도 전략 통합 인터페이스**
   ```java
   // RetryStrategy 인터페이스 도입
   // 즉시 재시도와 지연 재시도 통합 관리
   ```

**Low Priority (필요 시 개선):**

5. **Provider별 정책 패키지 구조 개선**
   - 현재 구조도 충분히 명확함
   - 필요 시에만 개선

### 6.4 최종 결론

**"현재 구조는 합리적이며, 점진적 개선을 통해 더욱 견고하게 만들 수 있다"**

**현재 구조가 합리적인 이유:**
1. ✅ **결제사 의존성이 잘 분리됨** - Provider 패턴으로 독립적 변경 가능
2. ✅ **비즈니스 규칙이 도메인 레이어에 위치** - ValidationService, Policy 패턴
3. ✅ **동시성 제어가 잘 구현됨** - 분산 락 + Pessimistic lock
4. ✅ **멱등성이 보장됨** - 멱등성 키 생성 및 검증
5. ✅ **트랜잭션 경계가 대부분 올바름** - 결제 승인과 후처리 분리

**점진적 개선이 필요한 이유:**
1. ⚠️ **트랜잭션 경계 일부 개선 필요** - 환불/취소 후처리 분리
2. ⚠️ **보상 트랜잭션 전략 수립 필요** - 후처리 실패 시 일관된 처리
3. ⚠️ **Service 레이어의 Infrastructure 책임 일부 제거** - 트랜잭션/락 관리 분리

**결론:**
현재 구조는 **실용적이고 합리적**이며, 큰 리팩토링 없이도 점진적으로 개선 가능합니다. 
과도한 추상화보다는 **필요한 부분만 선택적으로 개선**하는 것이 더 나은 접근입니다.


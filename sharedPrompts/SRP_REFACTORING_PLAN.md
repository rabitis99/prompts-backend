# PaymentServiceImpl & CashbackServiceImpl SRP 리팩토링 계획

## 📋 목표
- **SRP(Single Responsibility Principle) 철저히 준수**
- 각 서비스가 단일 책임만 수행하도록 분리
- 도메인 메서드를 통한 상태 변경 강제
- 클라이언트는 Facade만 사용

---

## 🏗️ 1. PaymentServiceImpl 분리 계획

### 📦 패키지 구조
```
domain/payment/
├── facade/
│   └── PaymentFacade.java                    # 클라이언트 단일 진입점 (기존)
├── service/
│   ├── core/
│   │   ├── PaymentService.java                # 인터페이스 (기존)
│   │   └── PaymentServiceImpl.java            # 조율만 담당 (리팩토링)
│   │
│   ├── validation/
│   │   └── PaymentValidationService.java      # ✨ 신규: 검증만 담당
│   │
│   ├── execution/
│   │   └── PaymentExecutionService.java       # ✨ 신규: 외부 API 호출만 담당
│   │
│   ├── postprocess/
│   │   └── PaymentPostProcessService.java     # ✨ 신규: 후처리만 담당
│   │
│   ├── sync/
│   │   └── PaymentStatusSyncService.java      # ✨ 신규: 상태 동기화만 담당
│   │
│   └── logging/
│       └── PaymentLoggingService.java         # 기존 (유지)
```

### 🔧 각 서비스 역할 정의

#### 1.1 PaymentValidationService
**책임**: 결제 관련 모든 검증만 담당

```java
package org.example.sharedprompts.domain.payment.service.validation;

public interface PaymentValidationService {
    // 일일 결제 제한 검증
    void validateDailyLimit(Long userId, UserTier tier);
    
    // 결제 소유권 검증
    void validatePaymentOwnership(Payment payment, Long userId);
    
    // 결제 상태 검증 (취소 가능 여부)
    void validateCancelableStatus(Payment payment);
    
    // 결제 상태 검증 (환불 가능 여부)
    void validateRefundableStatus(Payment payment);
    
    // 환불 금액 검증
    BigDecimal validateRefundAmount(BigDecimal requestedAmount, Payment payment);
    
    // 결제 금액 검증 (PaymentValidator와 협력)
    void validatePaymentAmount(Payment payment, PaymentResult result);
}
```

**구현 위치**: `service/validation/PaymentValidationServiceImpl.java`

**기존 코드에서 분리**:
- `PaymentValidationFacade`의 로직을 Service로 이동
- `PaymentValidator`와 협력하여 검증 수행

---

#### 1.2 PaymentExecutionService
**책임**: 외부 Provider 호출 및 결제 실행만 담당

```java
package org.example.sharedprompts.domain.payment.service.execution;

public interface PaymentExecutionService {
    // 결제 실행 (승인)
    PaymentResult executePayment(Payment payment, BigDecimal actualAmount);
    
    // 결제 취소 실행
    void executeCancel(Payment payment, String reason);
    
    // 결제 환불 실행
    void executeRefund(Payment payment, BigDecimal refundAmount, String reason);
    
    // 외부 결제사 상태 조회
    PaymentResult getPaymentStatus(String externalPaymentId, PaymentMethod method);
}
```

**구현 위치**: `service/execution/PaymentExecutionServiceImpl.java`

**기존 코드에서 분리**:
- `PaymentExecutionFacade`의 로직을 Service로 이동
- `PaymentProvider`와 직접 협력
- 상태 변경은 **도메인 메서드**로만 수행 (`payment.markSuccess()`, `payment.markFailed()` 등)

---

#### 1.3 PaymentPostProcessService
**책임**: 결제 후처리만 담당 (포인트, 캐시백, 이벤트, 메트릭)

```java
package org.example.sharedprompts.domain.payment.service.postprocess;

public interface PaymentPostProcessService {
    // 결제 성공 후처리
    void processPaymentSuccess(
        Payment payment, 
        Long userId, 
        BigDecimal actualPaymentAmount,
        BigDecimal originalAmount, 
        long processingTime
    );
    
    // 결제 실패 후처리
    void processPaymentFailure(
        Payment payment, 
        Long userId, 
        String errorMessage, 
        Exception exception, 
        long processingTime
    );
    
    // 결제 취소 후처리
    void processPaymentCancel(
        Payment payment, 
        Long userId, 
        String reason, 
        PaymentStatus oldStatus
    );
    
    // 결제 환불 후처리
    void processPaymentRefund(
        Payment payment, 
        Long userId, 
        BigDecimal refundAmount, 
        BigDecimal refundPointAmount, 
        String reason, 
        PaymentStatus oldStatus
    );
}
```

**구현 위치**: `service/postprocess/PaymentPostProcessServiceImpl.java`

**기존 코드에서 분리**:
- `PaymentPostProcessFacade`의 로직을 Service로 이동
- `PointService`, `CashbackService`, `PaymentEventPublisher`, `PaymentMetrics`와 협력

---

#### 1.4 PaymentStatusSyncService
**책임**: 외부 API 상태 동기화 및 Payment 엔티티 상태 업데이트만 담당

```java
package org.example.sharedprompts.domain.payment.service.sync;

public interface PaymentStatusSyncService {
    // 외부 결제사 상태와 DB 상태 동기화
    void syncPaymentStatus(Payment payment, PaymentResult result);
    
    // 외부 결제사 상태 조회 및 동기화
    Payment syncPaymentStatusFromProvider(Payment payment);
    
    // Webhook 결과를 Payment에 적용
    void applyWebhookResult(Payment payment, PaymentResult result);
}
```

**구현 위치**: `service/sync/PaymentStatusSyncServiceImpl.java`

**기존 코드에서 분리**:
- `PaymentServiceImpl.syncPaymentStatusFromResult()` 메서드 이동
- `PaymentProvider.getPaymentStatus()` 호출
- 상태 변경은 **도메인 메서드**로만 수행 (`payment.approve()`, `payment.fail()`, `payment.cancel()` 등)

---

#### 1.5 PaymentServiceImpl (리팩토링 후)
**책임**: 서비스 조율만 담당 (Orchestration)

```java
package org.example.sharedprompts.domain.payment.service.core;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PaymentValidationService validationService;      // ✨ 신규
    private final PaymentAmountFacade amountFacade;                // 기존 유지
    private final PaymentExecutionService executionService;        // ✨ 신규
    private final PaymentPostProcessService postProcessService;    // ✨ 신규
    private final PaymentStatusSyncService statusSyncService;      // ✨ 신규
    private final PaymentLoggingService loggingService;            // 기존 유지
    
    // 각 메서드는 위 서비스들을 조율만 함
    // 예: requestPayment()는 validation → execution → postProcess 순서로 호출
}
```

**변경 사항**:
- `syncPaymentStatusFromResult()` → `statusSyncService.syncPaymentStatus()` 호출
- `generateIdempotencyKey()` → `executionService`로 이동
- 검증 로직 → `validationService` 호출
- 후처리 로직 → `postProcessService` 호출

---

## 🏗️ 2. CashbackServiceImpl 분리 계획

### 📦 패키지 구조
```
domain/payment/
├── service/
│   └── cashback/
│       ├── CashbackService.java              # 인터페이스 (기존)
│       ├── CashbackServiceImpl.java           # 조율만 담당 (리팩토링)
│       │
│       ├── validation/
│       │   └── CashbackValidationService.java # ✨ 신규: 검증만 담당
│       │
│       ├── amount/
│       │   └── CashbackAmountService.java     # ✨ 신규: 금액 계산만 담당
│       │
│       ├── execution/
│       │   └── CashbackExecutionService.java  # ✨ 신규: 적립/지급 실행만 담당
│       │
│       └── lock/
│           └── CashbackLockService.java        # ✨ 신규: 분산 락 관리만 담당
│
└── facade/
    └── CashbackFacade.java                     # ✨ 신규: 클라이언트 단일 진입점
```

### 🔧 각 서비스 역할 정의

#### 2.1 CashbackValidationService
**책임**: 캐시백 관련 모든 검증만 담당

```java
package org.example.sharedprompts.domain.payment.service.cashback.validation;

public interface CashbackValidationService {
    // 캐시백 적립 가능 여부 검증 (중복 적립 방지)
    void validateAccumulation(Long paymentId);
    
    // 캐시백 지급 권한 검증
    void validatePaymentOwnership(Cashback cashback, Long userId);
    
    // 캐시백 지급 가능 상태 검증
    void validatePayableStatus(Cashback cashback);
    
    // 관리자용 지급 가능 상태 검증 (소유권 검증 없음)
    void validatePayableStatusForAdmin(Cashback cashback);
}
```

**구현 위치**: `service/cashback/validation/CashbackValidationServiceImpl.java`

**기존 코드에서 분리**:
- `CashbackServiceImpl.doPayCashback()`의 검증 로직 이동
- `CashbackRepository.findByPaymentId()` 호출하여 중복 검증

---

#### 2.2 CashbackAmountService
**책임**: 캐시백 금액 계산만 담당

```java
package org.example.sharedprompts.domain.payment.service.cashback.amount;

public interface CashbackAmountService {
    // 캐시백 적립 금액 계산
    BigDecimal calculateCashbackAmount(BigDecimal paymentAmount, BigDecimal cashbackRate);
    
    // 캐시백 적립률 조회
    BigDecimal getCashbackRate();
}
```

**구현 위치**: `service/cashback/amount/CashbackAmountServiceImpl.java`

**기존 코드에서 분리**:
- `CashbackServiceImpl.accumulateCashback()`의 금액 계산 로직 이동
- `PaymentProperties.getCashbackRate()` 사용

---

#### 2.3 CashbackExecutionService
**책임**: 실제 캐시백 적립/지급 실행만 담당

```java
package org.example.sharedprompts.domain.payment.service.cashback.execution;

public interface CashbackExecutionService {
    // 캐시백 적립 실행
    Cashback executeAccumulation(Long userId, Long paymentId, BigDecimal cashbackAmount, BigDecimal paymentAmount);
    
    // 캐시백 지급 실행 (사용자용)
    void executePayment(Long userId, Long cashbackId);
    
    // 캐시백 지급 실행 (관리자용)
    void executePaymentForAdmin(Long cashbackId);
}
```

**구현 위치**: `service/cashback/execution/CashbackExecutionServiceImpl.java`

**기존 코드에서 분리**:
- `CashbackServiceImpl.doPayCashback()` 로직 이동
- `CashbackServiceImpl.doPayCashbackForAdmin()` 로직 이동
- `CashbackServiceImpl.accumulateCashback()`의 저장 로직 이동
- `PointService.addPointsDirectly()` 호출
- 상태 변경은 **도메인 메서드**로만 수행 (`cashback.markAsPaid()`)

---

#### 2.4 CashbackLockService
**책임**: 분산 락 관리만 담당

```java
package org.example.sharedprompts.domain.payment.service.cashback.lock;

public interface CashbackLockService {
    // 락을 획득한 후 작업 실행
    <T> T executeWithLock(Long cashbackId, Supplier<T> task);
    
    // 락 키 생성
    String getLockKey(Long cashbackId);
}
```

**구현 위치**: `service/cashback/lock/CashbackLockServiceImpl.java`

**기존 코드에서 분리**:
- `CashbackServiceImpl.executeWithLock()` 메서드 이동
- `CashbackServiceImpl.getLockKey()` 메서드 이동
- `LockProvider`와 직접 협력

---

#### 2.5 CashbackFacade
**책임**: 클라이언트 단일 진입점, 내부 서비스 조율

```java
package org.example.sharedprompts.domain.payment.facade;

@Component
@RequiredArgsConstructor
public class CashbackFacade {
    private final CashbackRepository cashbackRepository;
    private final CashbackValidationService validationService;
    private final CashbackAmountService amountService;
    private final CashbackExecutionService executionService;
    private final CashbackLockService lockService;
    
    // 클라이언트 메서드: 내부 서비스들을 조율
    public void accumulateCashback(Long userId, Long paymentId, BigDecimal paymentAmount);
    public void payCashback(Long userId, Long cashbackId);
    public Page<CashbackResponseDto> getCashbackHistory(Long customerId, Pageable pageable);
    // ... 기타 조회 메서드
}
```

**구현 위치**: `facade/CashbackFacade.java`

---

#### 2.6 CashbackServiceImpl (리팩토링 후)
**책임**: 서비스 조율만 담당 (Orchestration)

```java
package org.example.sharedprompts.domain.payment.service.cashback;

@Service
@RequiredArgsConstructor
public class CashbackServiceImpl implements CashbackService {
    private final CashbackRepository cashbackRepository;
    private final CashbackValidationService validationService;     // ✨ 신규
    private final CashbackAmountService amountService;             // ✨ 신규
    private final CashbackExecutionService executionService;      // ✨ 신규
    private final CashbackLockService lockService;                 // ✨ 신규
    
    // 각 메서드는 위 서비스들을 조율만 함
    // 예: payCashback()는 lockService.executeWithLock() 내부에서 
    //     validationService → executionService 순서로 호출
}
```

**변경 사항**:
- `doPayCashback()` → `executionService.executePayment()` 호출
- `doPayCashbackForAdmin()` → `executionService.executePaymentForAdmin()` 호출
- `accumulateCashback()` → `amountService.calculateCashbackAmount()` → `executionService.executeAccumulation()` 호출
- `executeWithLock()` → `lockService.executeWithLock()` 호출
- 검증 로직 → `validationService` 호출

---

## 🔄 3. 상태 변경 원칙

### ✅ 도메인 메서드 사용 (필수)
모든 상태 변경은 도메인 엔티티의 메서드를 통해서만 수행:

**Payment 엔티티**:
- `payment.markInProgress()`
- `payment.markSuccess(externalPaymentId)`
- `payment.markFailed(reason)`
- `payment.markCanceled()`
- `payment.refund(amount)`
- `payment.applyWebhookResult(result)`

**Cashback 엔티티**:
- `cashback.markAsPaid()`

### ❌ Setter 사용 금지
- 모든 `@Setter` 제거
- 필드 직접 변경 금지

---

## 📝 4. 구현 순서

### Phase 1: Payment 서비스 분리
1. ✅ `PaymentValidationService` 생성
2. ✅ `PaymentExecutionService` 생성 (기존 Facade 로직 이동)
3. ✅ `PaymentPostProcessService` 생성 (기존 Facade 로직 이동)
4. ✅ `PaymentStatusSyncService` 생성
5. ✅ `PaymentServiceImpl` 리팩토링 (조율만 담당)

### Phase 2: Cashback 서비스 분리
1. ✅ `CashbackValidationService` 생성
2. ✅ `CashbackAmountService` 생성
3. ✅ `CashbackExecutionService` 생성
4. ✅ `CashbackLockService` 생성
5. ✅ `CashbackFacade` 생성
6. ✅ `CashbackServiceImpl` 리팩토링 (조율만 담당)

### Phase 3: 통합 및 테스트
1. ✅ Controller에서 Facade 사용 확인
2. ✅ 단위 테스트 작성
3. ✅ 통합 테스트 작성

---

## 🎯 5. 최종 구조 요약

### Payment 도메인
```
PaymentFacade (클라이언트 진입점)
  └── PaymentServiceImpl (조율)
      ├── PaymentValidationService (검증)
      ├── PaymentExecutionService (외부 API 호출)
      ├── PaymentPostProcessService (후처리)
      ├── PaymentStatusSyncService (상태 동기화)
      └── PaymentLoggingService (로깅)
```

### Cashback 도메인
```
CashbackFacade (클라이언트 진입점)
  └── CashbackServiceImpl (조율)
      ├── CashbackValidationService (검증)
      ├── CashbackAmountService (금액 계산)
      ├── CashbackExecutionService (적립/지급 실행)
      └── CashbackLockService (분산 락)
```

---

## ✅ 6. 체크리스트

### Payment
- [ ] PaymentValidationService 생성 및 테스트
- [ ] PaymentExecutionService 생성 및 테스트
- [ ] PaymentPostProcessService 생성 및 테스트
- [ ] PaymentStatusSyncService 생성 및 테스트
- [ ] PaymentServiceImpl 리팩토링
- [ ] PaymentFacade 업데이트
- [ ] Controller에서 PaymentFacade 사용 확인

### Cashback
- [ ] CashbackValidationService 생성 및 테스트
- [ ] CashbackAmountService 생성 및 테스트
- [ ] CashbackExecutionService 생성 및 테스트
- [ ] CashbackLockService 생성 및 테스트
- [ ] CashbackFacade 생성
- [ ] CashbackServiceImpl 리팩토링
- [ ] Controller에서 CashbackFacade 사용 확인

### 공통
- [ ] 모든 Setter 제거 확인
- [ ] 도메인 메서드로 상태 변경 확인
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성


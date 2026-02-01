# PaymentServiceImpl & CashbackServiceImpl SRP 분리 계획

## 📋 목표
- 각 서비스가 단일 책임만 갖도록 리팩토링
- 클라이언트는 PaymentFacade와 CashbackFacade만 사용
- 유지보수 용이, 테스트 용이, SRP 준수

---

## 1️⃣ PaymentServiceImpl 분리

### 현재 구조 분석
**PaymentServiceImpl이 담당하는 책임:**
1. ✅ 검증: 일일 제한, 소유권, 상태, 금액 검증
2. ✅ 외부 API 호출: Provider 호출, 결제 승인/취소/환불
3. ✅ 상태 동기화: 외부 API 상태와 DB 상태 동기화
4. ✅ 후처리: 포인트/캐시백 적립, 이벤트 발행, 메트릭
5. ✅ 로깅: 트레이싱, 결제 로그
6. ✅ 멱등성: idempotencyKey 생성 (이미 ExecutionFacade로 이동)
7. ✅ 금액 계산: 환불 포인트 계산

### 분리 계획

#### 1. PaymentValidationService (신규)
**패키지:** `domain/payment/service/validation/`
**책임:** 결제 관련 모든 검증만 담당

**메서드:**
- `validateDailyLimit(Long userId, UserTier tier)` - 일일 결제 제한 검증
- `validatePaymentOwnership(Payment payment, Long userId)` - 소유권 검증
- `validateCancelableStatus(Payment payment)` - 취소 가능 상태 검증
- `validateRefundableStatus(Payment payment)` - 환불 가능 상태 검증
- `validateRefundAmount(BigDecimal requestedAmount, Payment payment)` - 환불 금액 검증

**이동 대상:**
- `PaymentValidationFacade`의 모든 메서드 → `PaymentValidationService`로 이동

---

#### 2. PaymentExecutionService (신규)
**패키지:** `domain/payment/service/execution/`
**책임:** 외부 Provider 호출 및 결제 실행만 담당

**메서드:**
- `PaymentResult executePayment(Payment payment, BigDecimal actualAmount)` - 결제 실행
- `void executeCancel(Payment payment, String reason)` - 취소 실행
- `void executeRefund(Payment payment, BigDecimal refundAmount, String reason)` - 환불 실행

**이동 대상:**
- `PaymentExecutionFacade`의 모든 메서드 → `PaymentExecutionService`로 이동

---

#### 3. PaymentPostProcessService (신규)
**패키지:** `domain/payment/service/postprocess/`
**책임:** 결제 후처리만 담당 (포인트/캐시백 적립, 이벤트, 메트릭)

**메서드:**
- `void processPaymentSuccess(Payment payment, Long userId, BigDecimal actualPaymentAmount, BigDecimal originalAmount, long processingTime)` - 성공 후처리
- `void processPaymentFailure(Payment payment, Long userId, String errorMessage, Exception exception, long processingTime)` - 실패 후처리
- `void processPaymentCancel(Payment payment, Long userId, String reason, PaymentStatus oldStatus)` - 취소 후처리
- `void processPaymentRefund(Payment payment, Long userId, BigDecimal refundAmount, BigDecimal refundPointAmount, String reason, PaymentStatus oldStatus)` - 환불 후처리

**이동 대상:**
- `PaymentPostProcessFacade`의 모든 메서드 → `PaymentPostProcessService`로 이동

---

#### 4. PaymentLoggingService (이미 존재)
**패키지:** `domain/payment/logging/`
**책임:** 결제 관련 로깅만 담당
**상태:** 이미 분리되어 있음, 그대로 사용

---

#### 5. PaymentStatusSyncService (신규)
**패키지:** `domain/payment/service/sync/`
**책임:** 외부 API 상태 조회 및 DB 상태 동기화만 담당

**메서드:**
- `PaymentStatusResponseDto syncPaymentStatus(Long paymentId, Long userId)` - 사용자용 상태 동기화
- `PaymentStatusResponseDto syncPaymentStatusForAdmin(Long paymentId)` - 관리자용 상태 동기화
- `void syncPaymentStatusFromResult(Payment payment, PaymentResult result)` - PaymentResult 기반 상태 동기화

**이동 대상:**
- `PaymentServiceImpl.checkPaymentStatus()` → `syncPaymentStatus()`
- `PaymentServiceImpl.checkPaymentStatusForAdmin()` → `syncPaymentStatusForAdmin()`
- `PaymentServiceImpl.syncPaymentStatusFromResult()` → `syncPaymentStatusFromResult()`

---

#### 6. PaymentFacade (이미 존재, 수정 필요)
**패키지:** `domain/payment/facade/`
**책임:** 클라이언트 단일 진입점, 내부 서비스 조율

**수정 사항:**
- `PaymentValidationService` 주입
- `PaymentExecutionService` 주입
- `PaymentPostProcessService` 주입
- `PaymentStatusSyncService` 주입
- `PaymentLoggingService` 주입 (이미 존재)
- 기존 `PaymentValidationFacade`, `PaymentExecutionFacade`, `PaymentPostProcessFacade` 제거

---

#### 7. PaymentServiceImpl (수정)
**패키지:** `domain/payment/service/core/`
**책임:** 도메인 메서드 호출 및 서비스 조율만 담당

**수정 사항:**
- 모든 검증 → `PaymentValidationService` 호출
- 모든 외부 API 호출 → `PaymentExecutionService` 호출
- 모든 후처리 → `PaymentPostProcessService` 호출
- 모든 상태 동기화 → `PaymentStatusSyncService` 호출
- 모든 로깅 → `PaymentLoggingService` 호출
- `syncPaymentStatusFromResult()` 제거 (PaymentStatusSyncService로 이동)
- `generateIdempotencyKey()` 제거 (PaymentExecutionService로 이동)

---

## 2️⃣ CashbackServiceImpl 분리

### 현재 구조 분석
**CashbackServiceImpl이 담당하는 책임:**
1. ✅ 검증: 중복 적립 방지, 소유권, 이미 지급 여부
2. ✅ 금액 계산: `paymentAmount * cashbackRate`
3. ✅ 락 관리: 분산 락 획득/해제
4. ✅ 포인트 적립: `pointService.addPointsDirectly()`
5. ✅ 상태 변경: `cashback.markAsPaid()` (도메인 메서드 사용)

### 분리 계획

#### 1. CashbackValidationService (신규)
**패키지:** `domain/payment/service/cashback/validation/`
**책임:** 캐시백 관련 모든 검증만 담당

**메서드:**
- `void validateDuplicateAccumulation(Long paymentId)` - 중복 적립 방지 검증
- `void validateOwnership(Cashback cashback, Long userId)` - 소유권 검증
- `void validateNotPaid(Cashback cashback)` - 미지급 상태 검증

**이동 대상:**
- `CashbackServiceImpl.accumulateCashback()` 내부 중복 검증 로직
- `CashbackServiceImpl.doPayCashback()` 내부 소유권/미지급 검증 로직
- `CashbackServiceImpl.doPayCashbackForAdmin()` 내부 미지급 검증 로직

---

#### 2. CashbackAmountService (신규)
**패키지:** `domain/payment/service/cashback/amount/`
**책임:** 캐시백 금액 계산만 담당

**메서드:**
- `BigDecimal calculateCashbackAmount(BigDecimal paymentAmount, BigDecimal cashbackRate)` - 캐시백 금액 계산

**이동 대상:**
- `CashbackServiceImpl.accumulateCashback()` 내부 금액 계산 로직

---

#### 3. CashbackExecutionService (신규)
**패키지:** `domain/payment/service/cashback/execution/`
**책임:** 실제 캐시백 적립/지급 수행만 담당

**메서드:**
- `Cashback accumulateCashback(Long userId, Long paymentId, BigDecimal cashbackAmount, BigDecimal paymentAmount, BigDecimal rate)` - 캐시백 적립
- `void payCashback(Long userId, Long cashbackId)` - 캐시백 지급 (포인트 전환)
- `void payCashbackForAdmin(Long cashbackId)` - 관리자용 캐시백 지급

**이동 대상:**
- `CashbackServiceImpl.accumulateCashback()` 내부 적립 로직
- `CashbackServiceImpl.doPayCashback()` 내부 지급 로직
- `CashbackServiceImpl.doPayCashbackForAdmin()` 내부 지급 로직

---

#### 4. CashbackLockService (신규)
**패키지:** `domain/payment/service/cashback/lock/`
**책임:** 분산 락 관리만 담당

**메서드:**
- `<T> T executeWithLock(Long cashbackId, Supplier<T> task)` - 락 획득 후 작업 실행
- `String getLockKey(Long cashbackId)` - 락 키 생성

**이동 대상:**
- `CashbackServiceImpl.executeWithLock()` → `CashbackLockService.executeWithLock()`
- `CashbackServiceImpl.getLockKey()` → `CashbackLockService.getLockKey()`

---

#### 5. CashbackFacade (신규)
**패키지:** `domain/payment/service/cashback/facade/`
**책임:** 클라이언트 단일 진입점, 내부 서비스 조율

**메서드:**
- `Page<CashbackResponseDto> getCashbackHistory(Long customerId, Pageable pageable)` - 캐시백 이력 조회
- `void accumulateCashback(Long userId, Long paymentId, BigDecimal paymentAmount)` - 캐시백 적립
- `void payCashback(Long userId, Long cashbackId)` - 캐시백 지급
- `Page<CashbackResponseDto> getUnpaidCashbacks(Long customerId, Pageable pageable)` - 미지급 캐시백 조회
- `BigDecimal getUnpaidCashbackTotal(Long userId)` - 미지급 캐시백 총액
- `BigDecimal getAllUnpaidCashbackTotal()` - 전체 미지급 캐시백 총액 (관리자)
- `Page<CashbackResponseDto> getAllUnpaidCashbacks(Pageable pageable)` - 전체 미지급 캐시백 목록 (관리자)
- `void payCashbackForAdmin(Long cashbackId)` - 관리자용 캐시백 지급

**구성:**
- `CashbackValidationService` 주입
- `CashbackAmountService` 주입
- `CashbackExecutionService` 주입
- `CashbackLockService` 주입
- `CashbackRepository` 주입

---

#### 6. CashbackServiceImpl (수정)
**패키지:** `domain/payment/service/cashback/`
**책임:** 도메인 메서드 호출 및 서비스 조율만 담당

**수정 사항:**
- 모든 검증 → `CashbackValidationService` 호출
- 모든 금액 계산 → `CashbackAmountService` 호출
- 모든 적립/지급 → `CashbackExecutionService` 호출 (락 내부에서)
- 모든 락 관리 → `CashbackLockService` 호출
- `executeWithLock()` 제거 (CashbackLockService로 이동)
- `getLockKey()` 제거 (CashbackLockService로 이동)

**참고:** `CashbackServiceImpl`은 인터페이스 구현체로 유지하되, 실제 로직은 `CashbackFacade`로 위임하거나, `CashbackFacade`가 `CashbackService` 인터페이스를 구현하도록 변경 가능.

---

## 3️⃣ 최종 패키지 구조

```
domain/payment/
├── facade/
│   ├── PaymentFacade.java                    # 클라이언트 단일 진입점
│   ├── PaymentExecutionFacade.java           # (제거 예정)
│   ├── PaymentWebhookFacade.java
│   └── PaymentRetryFacade.java
│
├── service/
│   ├── core/
│   │   ├── PaymentService.java
│   │   └── PaymentServiceImpl.java           # 도메인 메서드 호출 및 조율만
│   │
│   ├── validation/
│   │   └── PaymentValidationService.java      # (신규) 검증만 담당
│   │
│   ├── execution/
│   │   └── PaymentExecutionService.java       # (신규) 외부 API 호출만 담당
│   │
│   ├── postprocess/
│   │   └── PaymentPostProcessService.java     # (신규) 후처리만 담당
│   │
│   ├── sync/
│   │   └── PaymentStatusSyncService.java      # (신규) 상태 동기화만 담당
│   │
│   ├── cashback/
│   │   ├── CashbackService.java
│   │   ├── CashbackServiceImpl.java          # 도메인 메서드 호출 및 조율만
│   │   │
│   │   ├── facade/
│   │   │   └── CashbackFacade.java            # (신규) 클라이언트 단일 진입점
│   │   │
│   │   ├── validation/
│   │   │   └── CashbackValidationService.java # (신규) 검증만 담당
│   │   │
│   │   ├── amount/
│   │   │   └── CashbackAmountService.java     # (신규) 금액 계산만 담당
│   │   │
│   │   ├── execution/
│   │   │   └── CashbackExecutionService.java  # (신규) 적립/지급만 담당
│   │   │
│   │   └── lock/
│   │       └── CashbackLockService.java       # (신규) 락 관리만 담당
│   │
│   ├── facade/                                # (제거 예정)
│   │   ├── PaymentValidationFacade.java       # → PaymentValidationService
│   │   ├── PaymentExecutionFacade.java       # → PaymentExecutionService
│   │   └── PaymentPostProcessFacade.java      # → PaymentPostProcessService
│   │
│   └── ... (기타 서비스)
│
├── logging/
│   └── PaymentLoggingService.java             # 이미 분리되어 있음
│
└── ... (기타 도메인)
```

---

## 4️⃣ 작업 순서

### Phase 1: Payment 서비스 분리
1. `PaymentValidationService` 생성 및 `PaymentValidationFacade` 로직 이동
2. `PaymentExecutionService` 생성 및 `PaymentExecutionFacade` 로직 이동
3. `PaymentPostProcessService` 생성 및 `PaymentPostProcessFacade` 로직 이동
4. `PaymentStatusSyncService` 생성 및 `PaymentServiceImpl` 상태 동기화 로직 이동
5. `PaymentServiceImpl` 수정 (위 서비스들 주입 및 호출)
6. `PaymentFacade` 수정 (위 서비스들 주입 및 호출)
7. 기존 Facade 제거 (`PaymentValidationFacade`, `PaymentExecutionFacade`, `PaymentPostProcessFacade`)

### Phase 2: Cashback 서비스 분리
1. `CashbackValidationService` 생성
2. `CashbackAmountService` 생성
3. `CashbackLockService` 생성
4. `CashbackExecutionService` 생성
5. `CashbackFacade` 생성
6. `CashbackServiceImpl` 수정 (위 서비스들 주입 및 호출)

### Phase 3: 테스트 및 검증
1. 단위 테스트 작성 (각 서비스별)
2. 통합 테스트 작성 (Facade 통합)
3. 기존 기능 동작 확인

---

## 5️⃣ 주의사항

1. **도메인 메서드 사용:** 모든 상태 변경은 `Payment.markSuccess()`, `Cashback.markAsPaid()` 등 도메인 메서드를 통해 수행
2. **Setter 제거:** 엔티티에서 Setter 제거, 도메인 메서드로만 변경
3. **트랜잭션 관리:** 각 서비스 메서드에 적절한 `@Transactional` 적용
4. **의존성 주입:** 순환 참조 방지
5. **기존 기능 유지:** 리팩토링 후에도 모든 기존 기능이 정상 동작해야 함

---

## 6️⃣ 예상 결과

### Before (PaymentServiceImpl)
- 545줄, 여러 책임 혼재
- 테스트 어려움
- 유지보수 어려움

### After (분리 후)
- `PaymentServiceImpl`: ~100줄 (조율만)
- `PaymentValidationService`: ~100줄 (검증만)
- `PaymentExecutionService`: ~150줄 (실행만)
- `PaymentPostProcessService`: ~120줄 (후처리만)
- `PaymentStatusSyncService`: ~100줄 (동기화만)
- 각 서비스가 단일 책임만 담당
- 테스트 용이
- 유지보수 용이

---

## 7️⃣ 다음 단계

이 계획서를 기반으로 실제 리팩토링을 진행합니다.


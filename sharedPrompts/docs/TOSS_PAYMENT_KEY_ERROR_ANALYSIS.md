# Toss Payments 결제 승인 에러 분석

## 1. Toss Payments의 paymentKey 정확한 의미

### 1.1 paymentKey란?

**paymentKey**는 Toss Payments에서 **결제 세션을 고유하게 식별하는 키**입니다.

- **생성 시점**: 클라이언트에서 Toss 결제 위젯을 통해 결제를 진행할 때 자동 생성
- **형식**: `tgen_` 또는 `t`로 시작하는 긴 문자열 (예: `tgen_2024010112345678901234567890`)
- **용도**: 
  - 결제 승인 API(`POST /v1/payments/confirm`) 호출 시 필수 파라미터
  - Toss 서버에서 해당 결제 세션을 찾기 위한 고유 식별자
- **생명주기**: 
  - 결제 위젯에서 결제 시도 시 생성
  - 결제 승인 완료 또는 세션 만료 시까지 유효
  - **일반적으로 10분 내외의 TTL을 가짐**

### 1.2 paymentKey와 PaymentMethod의 차이

| 구분 | paymentKey | PaymentMethod |
|------|-----------|---------------|
| **타입** | String (동적 생성) | Enum (고정값) |
| **값 예시** | `tgen_2024010112345678901234567890` | `TOSS` |
| **용도** | 결제 세션 식별 | 결제 수단 구분 |
| **생성 주체** | Toss Payments 서버 | 애플리케이션 코드 |
| **변경 가능성** | 결제마다 다름 | 고정값 |

---

## 2. 에러 발생 근본 원인 분석

### 2.1 로그 분석

```
paymentKey = "Toss" (문자열)
orderId = "2"
```

### 2.2 근본 원인

**`paymentKey`에 실제 결제 세션 키가 아닌 `PaymentMethod.TOSS.name()` 값(`"Toss"`)이 전달되고 있습니다.**

#### 왜 404 NOT_FOUND_PAYMENT_SESSION이 발생하는가?

1. **Toss 서버 관점**:
   - `POST /v1/payments/confirm` 호출 시 `paymentKey="Toss"`를 받음
   - Toss 서버는 내부에서 `"Toss"`라는 키로 결제 세션을 검색
   - **결제 세션은 `tgen_` 또는 `t`로 시작하는 실제 키로만 저장됨**
   - `"Toss"`라는 키는 존재하지 않으므로 → **404 NOT_FOUND_PAYMENT_SESSION**

2. **에러 메시지의 의미**:
   - "결제 시간이 만료되어 결제 진행 데이터가 존재하지 않습니다"
   - 이는 Toss가 해당 키로 세션을 찾지 못했을 때 반환하는 일반적인 에러 메시지
   - 실제로는 시간 만료가 아니라 **잘못된 키 형식** 때문

### 2.3 데이터 흐름 추적

```
프론트엔드 → 서버 → Toss API
   ↓           ↓         ↓
[문제 발생 지점 추정]
```

**예상되는 잘못된 흐름**:
1. 프론트엔드에서 결제 위젯 완료 후 실제 `paymentKey`를 받음
2. **어딘가에서 `paymentKey` 대신 `PaymentMethod.TOSS.name()`을 전달**
3. 서버의 `PaymentServiceImpl.confirmPayment()`에서 `request.getPaymentKey()` = `"Toss"` 수신
4. `payment.updateExternalPaymentId("Toss")` 실행
5. `PaymentExecutionService.executePayment()`에서 `payment.getExternalPaymentId()` = `"Toss"` 사용
6. `TossPaymentProvider.confirmPayment("Toss", ...)` 호출
7. `TossConfirmApiClient.confirm("Toss", ...)` → Toss API 호출
8. **Toss 서버에서 `"Toss"` 키로 세션 검색 실패 → 404 에러**

---

## 3. 코드 레벨 실수 추정

### 3.1 PaymentMethod와 paymentKey 혼용 가능성

#### 추정 시나리오 1: 프론트엔드에서 잘못된 값 전달

```javascript
// ❌ 잘못된 코드 (추정)
const confirmRequest = {
  orderId: "2",
  paymentKey: "Toss",  // PaymentMethod를 전달
  amount: 10000
};

// ✅ 올바른 코드
const confirmRequest = {
  orderId: "2",
  paymentKey: response.paymentKey,  // Toss 위젯에서 받은 실제 키
  amount: 10000
};
```

#### 추정 시나리오 2: 서버에서 PaymentMethod로 치환

현재 코드를 확인한 결과, **서버 코드에서는 직접적인 치환이 보이지 않습니다.**

하지만 다음 지점에서 확인이 필요합니다:

**`PaymentServiceImpl.confirmPayment()` (라인 201-207)**:
```java
if (request.getPaymentKey() != null && !request.getPaymentKey().isEmpty()) {
    if (payment.getPaymentMethod() != PaymentMethod.KAKAO_PAY) {
        payment.updateExternalPaymentId(request.getPaymentKey());
    }
}
```

이 코드는 `request.getPaymentKey()`를 그대로 사용하므로, **프론트엔드에서 잘못된 값이 오면 그대로 전달됩니다.**

#### 추정 시나리오 3: 프론트엔드에서 paymentKey 누락 시 기본값 사용

```javascript
// ❌ 잘못된 코드 (추정)
const confirmRequest = {
  orderId: "2",
  paymentKey: paymentMethod || "Toss",  // paymentKey가 없으면 PaymentMethod 사용
  amount: 10000
};
```

### 3.2 코드에서 확인해야 할 지점

#### 1. PaymentConfirmRequest 검증 부재

**현재 코드** (`PaymentConfirmRequest.java`):
```java
@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {
    private String orderId;
    private long amount;
    private String paymentKey;  // 검증 없음
    private String pgToken;
}
```

**문제점**:
- `paymentKey`에 대한 형식 검증이 없음
- `"Toss"` 같은 잘못된 값도 그대로 통과

#### 2. PaymentServiceImpl에서 paymentKey 검증 부재

**현재 코드** (`PaymentServiceImpl.java` 라인 201-207):
```java
if (request.getPaymentKey() != null && !request.getPaymentKey().isEmpty()) {
    if (payment.getPaymentMethod() != PaymentMethod.KAKAO_PAY) {
        payment.updateExternalPaymentId(request.getPaymentKey());
        // ❌ "Toss" 같은 잘못된 값도 그대로 저장됨
    }
}
```

**문제점**:
- `paymentKey`가 실제 Toss 형식인지 검증하지 않음
- `"Toss"` 같은 값도 `externalPaymentId`로 저장됨

#### 3. PaymentExecutionService에서 paymentKey 검증 부재

**현재 코드** (`PaymentExecutionService.java` 라인 89-98):
```java
if (payment.getExternalPaymentId() == null || payment.getExternalPaymentId().isEmpty()) {
    // null/empty 체크만 함
    throw new ApiException(...);
}
// ❌ "Toss" 같은 잘못된 형식은 통과됨
```

**문제점**:
- `paymentKey` 형식 검증이 없음
- `"Toss"` 같은 값도 그대로 Toss API로 전달됨

---

## 4. 올바른 결제 흐름

### 4.1 Toss Payments 결제 승인 플로우

```
[1] 프론트엔드: 결제 요청
    POST /api/payments
    → 서버에서 Payment 엔티티 생성 (PENDING 상태)
    → orderId 반환 (예: "2")

[2] 프론트엔드: Toss 결제 위젯 호출
    - Toss 결제 위젯 초기화
    - orderId, amount 등 전달
    - 사용자가 결제 진행

[3] 프론트엔드: Toss 위젯 완료 콜백
    - Toss가 paymentKey 생성 및 반환
    - 예: paymentKey = "tgen_2024010112345678901234567890"
    - ⚠️ 이 시점에서 실제 paymentKey를 받아야 함

[4] 프론트엔드 → 서버: 결제 승인 요청
    POST /api/payments/confirm
    {
      "orderId": "2",
      "paymentKey": "tgen_2024010112345678901234567890",  // ✅ 실제 키
      "amount": 10000
    }

[5] 서버: PaymentServiceImpl.confirmPayment()
    - Payment 조회 (orderId="2")
    - payment.updateExternalPaymentId("tgen_...")  // ✅ 실제 키 저장
    - PaymentExecutionService.executePayment() 호출

[6] 서버: PaymentExecutionService.executePayment()
    - payment.getExternalPaymentId() = "tgen_..." 사용
    - TossPaymentProvider.confirmPayment("tgen_...", ...) 호출

[7] 서버: TossPaymentProvider.confirmPayment()
    - TossConfirmApiClient.confirm("tgen_...", ...) 호출

[8] 서버 → Toss API: 결제 승인 요청
    POST https://api.tosspayments.com/v1/payments/confirm
    {
      "paymentKey": "tgen_2024010112345678901234567890",  // ✅ 실제 키
      "orderId": "2",
      "amount": 10000
    }

[9] Toss API: 결제 승인 처리
    - paymentKey로 결제 세션 찾기
    - 결제 승인 처리
    - 성공 응답 반환

[10] 서버: Payment 상태 업데이트
     - SUCCESS 상태로 변경
     - 후처리 (포인트 적립 등)
```

### 4.2 핵심 포인트

1. **paymentKey는 Toss 위젯에서만 생성됨**
   - 서버에서 생성할 수 없음
   - 프론트엔드에서 반드시 받아서 전달해야 함

2. **paymentKey는 결제 세션 식별자**
   - 각 결제 시도마다 고유하게 생성
   - `"Toss"` 같은 고정값이 아님

3. **paymentKey 형식**
   - `tgen_` 또는 `t`로 시작
   - 긴 문자열 (보통 30자 이상)

---

## 5. 서버 코드에서 보장해야 할 조건 (체크리스트)

### 5.1 PaymentConfirmRequest 검증

- [ ] **paymentKey 필수 검증**
  ```java
  @NotBlank(message = "paymentKey는 필수입니다")
  private String paymentKey;
  ```

- [ ] **paymentKey 형식 검증 (Toss의 경우)**
  ```java
  @Pattern(regexp = "^t(gen_|\\w+).*", message = "Toss paymentKey 형식이 올바르지 않습니다")
  private String paymentKey;
  ```

- [ ] **orderId 형식 검증**
  ```java
  @NotBlank(message = "orderId는 필수입니다")
  @Pattern(regexp = "^\\d+$", message = "orderId는 숫자여야 합니다")
  private String orderId;
  ```

### 5.2 PaymentServiceImpl.confirmPayment() 검증

- [ ] **paymentKey null/empty 체크**
  ```java
  if (request.getPaymentKey() == null || request.getPaymentKey().isEmpty()) {
      throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, 
          "paymentKey는 필수입니다");
  }
  ```

- [ ] **Toss의 경우 paymentKey 형식 검증**
  ```java
  if (payment.getPaymentMethod() == PaymentMethod.TOSS) {
      if (!request.getPaymentKey().matches("^t(gen_|\\w+).*")) {
          throw new ApiException(ErrorCode.INVALID_INPUT_VALUE,
              "Toss paymentKey 형식이 올바르지 않습니다: " + request.getPaymentKey());
      }
  }
  ```

- [ ] **PaymentMethod와 paymentKey 불일치 방지**
  ```java
  // PaymentMethod.TOSS인데 paymentKey가 "Toss"인 경우 차단
  if (payment.getPaymentMethod() == PaymentMethod.TOSS 
      && "Toss".equals(request.getPaymentKey())) {
      throw new ApiException(ErrorCode.INVALID_INPUT_VALUE,
          "paymentKey는 PaymentMethod가 아닌 실제 결제 세션 키여야 합니다");
  }
  ```

### 5.3 PaymentExecutionService.executePayment() 검증

- [ ] **externalPaymentId 형식 검증 (Provider별)**
  ```java
  if (payment.getPaymentMethod() == PaymentMethod.TOSS) {
      String externalId = payment.getExternalPaymentId();
      if (externalId == null || !externalId.matches("^t(gen_|\\w+).*")) {
          throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR,
              "Toss paymentKey 형식이 올바르지 않습니다: " + externalId);
      }
  }
  ```

- [ ] **로깅 강화**
  ```java
  log.info("결제 승인 시작: paymentId={}, paymentMethod={}, externalPaymentId={}, orderId={}",
      payment.getId(), payment.getPaymentMethod(), 
      payment.getExternalPaymentId(), payment.getId());
  ```

### 5.4 TossPaymentProvider.confirmPayment() 검증

- [ ] **paymentKey 형식 사전 검증**
  ```java
  private void validatePaymentKeyFormat(String paymentKey) {
      if (paymentKey == null || paymentKey.isEmpty()) {
          throw new IllegalArgumentException("paymentKey는 필수입니다");
      }
      if (!paymentKey.matches("^t(gen_|\\w+).*")) {
          throw new IllegalArgumentException(
              "Toss paymentKey 형식이 올바르지 않습니다: " + paymentKey);
      }
      // "Toss" 같은 PaymentMethod 값 차단
      if ("Toss".equals(paymentKey) || "TOSS".equals(paymentKey)) {
          throw new IllegalArgumentException(
              "paymentKey는 PaymentMethod가 아닌 실제 결제 세션 키여야 합니다: " + paymentKey);
      }
  }
  ```

---

## 6. confirmPayment 메서드 시그니처 및 호출부 예시 코드

### 6.1 PaymentConfirmRequest 개선안

```java
package org.example.sharedprompts.dto.payment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

/**
 * 결제 승인 요청 DTO
 * 
 * <p>결제사별 필드:
 * <ul>
 *   <li>토스페이먼츠: paymentKey (필수, tgen_ 또는 t로 시작)</li>
 *   <li>카카오페이: pgToken (필수), paymentKey는 tid (ready 시 받은 값)</li>
 * </ul>
 */
@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {
    
    @NotBlank(message = "orderId는 필수입니다")
    @Pattern(regexp = "^\\d+$", message = "orderId는 숫자여야 합니다")
    private String orderId;
    
    private long amount;
    
    /**
     * 결제 세션 키
     * - 토스페이먼츠: Toss 위젯에서 받은 paymentKey (tgen_ 또는 t로 시작)
     * - 카카오페이: ready API에서 받은 tid
     */
    private String paymentKey;
    
    /**
     * 카카오페이 결제 승인 토큰 (pg_token)
     */
    private String pgToken;

    public PaymentConfirmRequest(String orderId, long amount, String paymentKey) {
        this.orderId = orderId;
        this.amount = amount;
        this.paymentKey = paymentKey;
    }

    /**
     * 주문 ID를 Long으로 변환
     */
    public Long getOrderIdAsLong() {
        try {
            return Long.parseLong(this.orderId);
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "orderId", 
                    "주문 ID는 숫자여야 합니다: " + this.orderId);
        }
    }
    
    /**
     * paymentKey 형식 검증 (결제사별)
     * 
     * @param paymentMethod 결제 수단
     * @throws ApiException 형식이 올바르지 않은 경우
     */
    public void validatePaymentKeyFormat(org.example.sharedprompts.domain.payment.enums.PaymentMethod paymentMethod) {
        if (this.paymentKey == null || this.paymentKey.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                    "paymentKey는 필수입니다");
        }
        
        if (paymentMethod == org.example.sharedprompts.domain.payment.enums.PaymentMethod.TOSS) {
            // Toss paymentKey 형식 검증: tgen_ 또는 t로 시작
            if (!this.paymentKey.matches("^t(gen_|\\w+).*")) {
                throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                        "Toss paymentKey 형식이 올바르지 않습니다: " + this.paymentKey);
            }
            // PaymentMethod 값과 혼동 방지
            if ("Toss".equals(this.paymentKey) || "TOSS".equals(this.paymentKey)) {
                throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                        "paymentKey는 PaymentMethod가 아닌 실제 결제 세션 키여야 합니다");
            }
        }
    }
}
```

### 6.2 PaymentServiceImpl.confirmPayment() 개선안

```java
@Override
@Transactional
public PaymentConfirmResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
    long startTime = System.currentTimeMillis();

    Payment payment = paymentRepository.findById(request.getOrderIdAsLong())
            .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

    validationService.validatePaymentOwnership(payment, userId);

    // ✅ paymentKey 형식 검증 추가
    if (payment.getPaymentMethod() == PaymentMethod.TOSS) {
        request.validatePaymentKeyFormat(PaymentMethod.TOSS);
    }

    // 결제사별 paymentKey 처리
    if (request.getPaymentKey() != null && !request.getPaymentKey().isEmpty()) {
        if (payment.getPaymentMethod() != PaymentMethod.KAKAO_PAY) {
            // ✅ 로깅 추가: paymentKey 값 확인
            log.info("결제 승인 요청: paymentId={}, paymentMethod={}, paymentKey={}, orderId={}",
                    payment.getId(), payment.getPaymentMethod(), 
                    request.getPaymentKey(), request.getOrderId());
            
            payment.updateExternalPaymentId(request.getPaymentKey());
        }
    } else {
        // ✅ paymentKey 필수 검증
        if (payment.getPaymentMethod() == PaymentMethod.TOSS) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                    "Toss 결제 승인을 위해서는 paymentKey가 필수입니다");
        }
    }

    // 실제 결제 금액 계산 (포인트 사용 후 금액)
    BigDecimal actualAmount = payment.getAmount().subtract(
            payment.getUsedPointAmount() != null ? payment.getUsedPointAmount() : BigDecimal.ZERO
    );

    // 카카오페이의 경우 pgToken을 additionalParams로 전달
    Map<String, String> additionalParams = Collections.emptyMap();
    if (payment.getPaymentMethod() == PaymentMethod.KAKAO_PAY) {
        if (request.getPgToken() == null || request.getPgToken().isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "pgToken", 
                    "카카오페이 결제 승인을 위해서는 pgToken이 필수입니다");
        }
        additionalParams = Map.of("pgToken", request.getPgToken());
    }

    try {
        // ✅ 로깅: Toss API 호출 전 paymentKey 확인
        if (payment.getPaymentMethod() == PaymentMethod.TOSS) {
            log.info("Toss 결제 승인 API 호출: paymentId={}, paymentKey={}, orderId={}, amount={}",
                    payment.getId(), payment.getExternalPaymentId(), 
                    payment.getId(), actualAmount);
        }
        
        // PaymentExecutionService를 통한 결제 실행
        payment = executionService.executePayment(payment, actualAmount, additionalParams);

        long processingTime = System.currentTimeMillis() - startTime;

        try {
            postProcessService.processPaymentSuccess(
                    payment,
                    userId,
                    actualAmount,
                    payment.getAmount(),
                    processingTime
            );
        } catch (Exception postProcessException) {
            log.error("결제 승인 성공 후 후처리 실패: paymentId={}, userId={}, error={}",
                    payment.getId(), userId, postProcessException.getMessage(), postProcessException);
        }

    } catch (Exception e) {
        long processingTime = System.currentTimeMillis() - startTime;
        payment.fail("결제 승인 실패: " + e.getMessage());
        payment = paymentRepository.save(payment);

        try {
            postProcessService.processPaymentFailure(
                    payment,
                    userId,
                    e.getMessage(),
                    e,
                    processingTime
            );
        } catch (Exception postProcessException) {
            log.error("결제 실패 후처리 중 오류 발생: paymentId={}, userId={}, error={}",
                    payment.getId(), userId, postProcessException.getMessage(), postProcessException);
        }

        log.error("결제 승인 실패: paymentId={}, userId={}, paymentKey={}, error={}", 
                payment.getId(), userId, payment.getExternalPaymentId(), e.getMessage(), e);
        throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 승인 실패: " + e.getMessage());
    }

    PaymentConfirmResponse response = new PaymentConfirmResponse();
    response.setPaymentKey(payment.getExternalPaymentId());
    response.setOrderId(request.getOrderId());
    response.setStatus(payment.getStatus().name());
    response.setTotalAmount(payment.getAmount().intValue());
    if (payment.getApprovedAt() != null) {
        response.setApprovedAt(payment.getApprovedAt().atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime());
    }
    response.setMethod(payment.getPaymentMethod().name());

    return response;
}
```

### 6.3 TossPaymentProvider.confirmPayment() 개선안

```java
@Override
public PaymentResult confirmPayment(
        String paymentKey,
        String orderId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String userId,
        java.util.Map<String, String> additionalParams
) {
    // ✅ paymentKey 형식 검증 강화
    validatePaymentKeyFormat(paymentKey);
    validateRequired(orderId, "orderId");
    validateRequired(amount, "amount");
    validateRequired(currency, "currency");

    try {
        long tossAmount = amountPolicy.toTossAmount(amount);
        
        // ✅ 로깅: Toss API 호출 전 paymentKey 확인
        log.info("Toss 결제 승인 API 호출: paymentKey={}, orderId={}, amount={}", 
                paymentKey, orderId, tossAmount);
        
        var response = tossConfirmApiClient.confirm(paymentKey, orderId, tossAmount);
        PaymentStatus status = statusMapper.map(response.status());

        log.info("TossPay 결제 승인 성공: paymentKey={}, orderId={}, status={}", 
                paymentKey, orderId, status);
        return PaymentResult.builder()
                .externalPaymentId(response.paymentKey())
                .status(status)
                .amount(response.totalAmount())
                .currency(response.currency())
                .orderId(response.orderId())
                .approvedAt(response.approvedAt())
                .metadata(response.metadata())
                .build();
    } catch (Exception e) {
        log.error("TossPay 결제 승인 실패: paymentKey={}, orderId={}, error={}", 
                paymentKey, orderId, e.getMessage(), e);
        return PaymentResult.builder()
                .externalPaymentId(paymentKey)
                .status(PaymentStatus.FAILED)
                .amount(amount)
                .currency(currency)
                .orderId(orderId)
                .failureReason("TossPay API 호출 실패: " + e.getMessage())
                .build();
    }
}

/**
 * Toss paymentKey 형식 검증
 * 
 * @param paymentKey 검증할 paymentKey
 * @throws IllegalArgumentException 형식이 올바르지 않은 경우
 */
private void validatePaymentKeyFormat(String paymentKey) {
    if (paymentKey == null || paymentKey.isEmpty()) {
        throw new IllegalArgumentException("paymentKey는 필수입니다");
    }
    
    // Toss paymentKey 형식: tgen_ 또는 t로 시작
    if (!paymentKey.matches("^t(gen_|\\w+).*")) {
        throw new IllegalArgumentException(
                "Toss paymentKey 형식이 올바르지 않습니다: " + paymentKey);
    }
    
    // PaymentMethod 값과 혼동 방지
    if ("Toss".equals(paymentKey) || "TOSS".equals(paymentKey)) {
        throw new IllegalArgumentException(
                "paymentKey는 PaymentMethod가 아닌 실제 결제 세션 키여야 합니다: " + paymentKey);
    }
}
```

### 6.4 프론트엔드 예시 코드 (참고)

```javascript
// ✅ 올바른 Toss 결제 승인 요청
async function confirmTossPayment(orderId, amount) {
  // 1. Toss 결제 위젯 초기화 및 결제 진행
  const widget = window.TossPayments(widgetClientKey);
  
  await widget.requestPayment('카드', {
    amount: amount,
    orderId: orderId,
    orderName: '결제 테스트',
    successUrl: `${window.location.origin}/payment/success`,
    failUrl: `${window.location.origin}/payment/fail`,
  });
  
  // 2. 결제 위젯 완료 후 콜백에서 paymentKey 받기
  // (실제로는 successUrl로 리다이렉트되며 URL 파라미터로 전달됨)
  const urlParams = new URLSearchParams(window.location.search);
  const paymentKey = urlParams.get('paymentKey');  // ✅ 실제 paymentKey
  
  // 3. 서버로 결제 승인 요청
  const response = await fetch('/api/payments/confirm', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({
      orderId: orderId,
      paymentKey: paymentKey,  // ✅ 실제 paymentKey 전달
      amount: amount
    })
  });
  
  return response.json();
}

// ❌ 잘못된 예시 (절대 하지 말 것)
async function confirmTossPaymentWrong(orderId, amount) {
  const response = await fetch('/api/payments/confirm', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({
      orderId: orderId,
      paymentKey: "Toss",  // ❌ PaymentMethod를 전달
      amount: amount
    })
  });
  
  return response.json();
}
```

---

## 7. 즉시 수정 가능한 조치사항

### 7.1 우선순위 1: 즉시 적용

1. **PaymentConfirmRequest에 paymentKey 검증 추가**
   - `validatePaymentKeyFormat()` 메서드 추가
   - `PaymentServiceImpl.confirmPayment()`에서 호출

2. **로깅 강화**
   - `PaymentServiceImpl.confirmPayment()`에서 paymentKey 로깅
   - `TossPaymentProvider.confirmPayment()`에서 paymentKey 로깅

3. **에러 메시지 개선**
   - "paymentKey 형식이 올바르지 않습니다" 명확한 메시지

### 7.2 우선순위 2: 프론트엔드 확인

1. **프론트엔드 코드 확인**
   - Toss 위젯에서 받은 실제 `paymentKey`를 전달하는지 확인
   - `PaymentMethod.TOSS.name()`을 전달하지 않는지 확인

2. **프론트엔드 로깅 추가**
   - `confirmPayment` 호출 전 `paymentKey` 값 로깅

### 7.3 우선순위 3: 장기 개선

1. **통합 테스트 추가**
   - `paymentKey="Toss"` 전달 시 에러 발생하는지 테스트

2. **문서화**
   - 프론트엔드 개발자를 위한 `paymentKey` 전달 가이드 작성

---

## 8. 요약

### 8.1 문제의 핵심

- **paymentKey에 `"Toss"` (PaymentMethod)가 전달됨**
- **실제 Toss 결제 세션 키(`tgen_...`)가 전달되지 않음**
- **Toss 서버에서 해당 키로 세션을 찾을 수 없어 404 에러 발생**

### 8.2 해결 방향

1. **서버에서 paymentKey 형식 검증 추가**
2. **프론트엔드에서 실제 paymentKey 전달 확인**
3. **로깅 강화로 디버깅 용이성 향상**

### 8.3 예방 조치

- **PaymentMethod와 paymentKey 혼용 방지 검증**
- **명확한 에러 메시지로 빠른 문제 파악**
- **통합 테스트로 재발 방지**









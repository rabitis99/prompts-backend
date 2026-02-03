# 카카오페이 결제 흐름: pg_token 완전 정리

> **목표**: pg_token 때문에 다시 헷갈릴 일이 없게 만드는 기준 문서

---

## 1. pg_token의 정체와 역할

### 1.1 pg_token이란?

**pg_token은 카카오가 발급하는 1회성 결제 승인 토큰입니다.**

- ❌ **PG 식별값이 아님** (Payment Gateway 식별자가 아님)
- ❌ **보안 키나 인증 토큰이 아님**
- ✅ **카카오페이 결제 승인을 위한 일회용 토큰**
- ✅ **결제 페이지에서 사용자가 결제를 승인한 후 카카오가 발급**
- ✅ **승인 API 호출 시에만 사용되며, 사용 후 즉시 무효화됨**

### 1.2 pg_token의 생명주기

```
1. 사용자가 카카오페이 결제 페이지에서 결제 승인
   ↓
2. 카카오가 pg_token 생성 (1회성, 짧은 유효기간)
   ↓
3. 카카오가 redirect URL에 pg_token을 query parameter로 전달
   예: https://your-app.com/payment/callback?pg_token=ABC123XYZ
   ↓
4. 프론트엔드가 pg_token을 추출하여 서버로 전달
   ↓
5. 서버가 pg_token + tid로 카카오페이 승인 API 호출
   ↓
6. 승인 완료 후 pg_token은 더 이상 사용 불가 (무효화)
```

### 1.3 pg_token은 어디서 오는가? (중요!)

**pg_token은 카카오페이가 자동으로 생성해서 보내줍니다.**

#### 단계별 설명:

1. **결제 준비 단계 (`/v1/payment/ready`)**
   ```java
   // 서버에서 카카오페이 ready API 호출
   POST /v1/payment/ready
   
   // 카카오페이 응답
   {
     "tid": "T1234567890123456789",
     "next_redirect_pc_url": "https://kakaopay.com/payment/...",
     "approval_url": "https://your-app.com/payment/callback"  // ← 이 URL에 나중에 pg_token이 붙음
   }
   ```
   - 이때는 **아직 pg_token이 없음**
   - `approval_url`은 결제 승인 후 돌아올 URL (서버에서 ready 호출 시 지정)

2. **사용자가 카카오페이 결제 페이지에서 결제 승인**
   - 사용자가 카카오페이 결제 페이지에서 비밀번호 입력, 생체인증 등으로 결제 승인
   - **이 시점에 카카오가 pg_token을 생성**

3. **카카오가 redirect URL에 pg_token 추가해서 리다이렉트**
   ```
   원래 URL: https://your-app.com/payment/callback
   
   카카오가 리다이렉트: 
   https://your-app.com/payment/callback?pg_token=ABC123XYZ789
                                 ↑
                         카카오가 자동으로 추가!
   ```

4. **프론트엔드가 URL에서 pg_token 추출**
   ```javascript
   // 현재 페이지 URL: https://your-app.com/payment/callback?pg_token=ABC123XYZ789
   const urlParams = new URLSearchParams(window.location.search);
   const pgToken = urlParams.get('pg_token');  // "ABC123XYZ789"
   ```

**핵심 정리:**
- ✅ pg_token은 **카카오페이가 생성**해서 보내줌
- ✅ **프론트엔드가 생성하지 않음**
- ✅ **서버가 생성하지 않음**
- ✅ 결제 승인 후 카카오가 **redirect URL의 query parameter로 자동 추가**
- ✅ 프론트엔드는 단순히 **URL에서 추출**만 하면 됨

### 1.4 pg_token과 tid의 관계

| 항목 | tid | pg_token |
|------|-----|----------|
| **의미** | 결제 고유 ID (Transaction ID) | 결제 승인 토큰 |
| **발급 시점** | `/v1/payment/ready` 호출 시 | 결제 페이지에서 승인 시 |
| **저장 여부** | ✅ DB에 영구 저장 | ❌ DB에 저장하지 않음 |
| **재사용 가능** | ✅ 재사용 가능 (결제 조회 등) | ❌ 1회성, 사용 후 무효화 |
| **용도** | 결제 식별 및 조회 | 승인 API 호출 시에만 사용 |

---

## 2. 프론트엔드 책임

### 2.1 프론트엔드가 해야 할 일

1. **카카오페이 결제 페이지로 리다이렉트**
   - 서버에서 받은 `redirectUrl`로 이동

2. **redirect URL에서 pg_token 추출**
   ```javascript
   // 예시: https://your-app.com/payment/callback?pg_token=ABC123XYZ
   const urlParams = new URLSearchParams(window.location.search);
   const pgToken = urlParams.get('pg_token');
   ```

3. **서버로 pg_token 전달**
   - pg_token을 생성하거나 해석하지 않음
   - 단순히 추출하여 서버로 전달만 함

### 2.2 프론트엔드가 하면 안 되는 일

- ❌ pg_token을 DB에 저장
- ❌ pg_token을 해석하거나 검증
- ❌ pg_token으로 직접 카카오페이 승인 API 호출
- ❌ pg_token을 보안 키처럼 취급

### 2.3 프론트엔드 코드 예시

```typescript
// 결제 준비 후 리다이렉트
async function requestPayment() {
  const response = await fetch('/api/v1/payments', {
    method: 'POST',
    body: JSON.stringify({ amount: 10000, paymentMethod: 'KAKAO_PAY' })
  });
  
  const { redirectUrl, paymentId } = await response.json();
  
  // 카카오페이 결제 페이지로 리다이렉트
  window.location.href = redirectUrl;
}

// 결제 승인 후 콜백 처리
function handlePaymentCallback() {
  const urlParams = new URLSearchParams(window.location.search);
  const pgToken = urlParams.get('pg_token');
  
  if (!pgToken) {
    alert('결제 승인 토큰을 받지 못했습니다.');
    return;
  }
  
  // 서버로 pg_token 전달
  approvePayment(pgToken);
}

async function approvePayment(pgToken: string) {
  // paymentId는 이전 단계에서 저장해둔 값
  const paymentId = getStoredPaymentId();
  
  const response = await fetch(`/api/v1/payments/${paymentId}/approve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      pgToken: pgToken  // 단순히 전달만 함
    })
  });
  
  const result = await response.json();
  if (result.success) {
    // 결제 완료 처리
  }
}
```

---

## 3. 백엔드 책임

### 3.1 백엔드가 해야 할 일

1. **결제 준비 (`/v1/payment/ready`)**
   - Payment 엔티티 생성 (status = PENDING)
   - 카카오페이 `/v1/payment/ready` API 호출
   - 응답으로 받은 `tid`를 DB에 저장 (`externalPaymentId`)
   - `redirectUrl`을 프론트엔드에 반환

2. **결제 승인 (`/v1/payment/approve`)**
   - 프론트엔드로부터 `pg_token` 수신
   - Payment(id) 기준으로 저장된 `tid` 조회
   - `tid + pg_token + partner_order_id + partner_user_id`로 카카오페이 승인 API 호출
   - 승인 성공 시 Payment 상태를 COMPLETED로 변경

### 3.2 백엔드가 하면 안 되는 일

- ❌ pg_token을 DB에 영구 저장
- ❌ pg_token을 재사용하려고 시도
- ❌ pg_token 없이 승인 API 호출 (KakaoPay는 필수)

### 3.3 백엔드 코드 예시

#### 3.3.1 결제 준비 (Ready)

**현재 프로젝트 구조:**
- `POST /api/payments` → `PaymentController.requestPayment()` → `PaymentFacade.requestPayment()` → `PaymentService.requestPayment()`
- Payment 엔티티 생성 (status = PENDING)
- `PaymentExecutionService`를 통해 카카오페이 Provider의 `preparePayment()` 호출
- tid를 `payment.externalPaymentId`에 저장
- redirectUrl을 프론트엔드에 반환

**실제 코드 흐름:**
```java
// PaymentServiceImpl.requestPayment()
Payment payment = request.toPaymentBuilder(user, user.getTier(), ...).build();
payment = paymentRepository.save(payment);  // status = PENDING

// PaymentExecutionService.executePayment() 내부에서
// PaymentProvider.preparePayment() 호출하여 tid 받음
// tid는 payment.updateExternalPaymentId(tid)로 저장됨
```

#### 3.3.2 결제 승인 (Approve)

**현재 프로젝트 구조:**
- `POST /api/payments/confirm` → `PaymentController.confirmPayment()` → `PaymentFacade.confirmPayment()` → `PaymentService.confirmPayment()`
- Payment(id) 기준으로 저장된 tid 조회 (`payment.getExternalPaymentId()`)
- 카카오페이인 경우 pgToken을 `additionalParams`로 전달
- `PaymentExecutionService.executePayment(payment, actualAmount, additionalParams)` 호출
- 내부에서 `KakaoPayPaymentProvider.confirmPayment()` 호출
- 최종적으로 `KakakoApproveApiClient.approve(tid, orderId, userId, pgToken)` 호출

**실제 코드 (PaymentServiceImpl.confirmPayment()):**
```java
@PostMapping("/confirm")
public PaymentConfirmResponse confirmPayment(
        Long userId, 
        PaymentConfirmRequest request
) {
    // 1. Payment 조회 (tid가 externalPaymentId에 저장되어 있음)
    Payment payment = paymentRepository.findById(request.getOrderIdAsLong())
            .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
    
    // 2. 소유권 검증
    validationService.validatePaymentOwnership(payment, userId);
    
    // 3. 카카오페이의 경우 ready 시 이미 tid가 저장되어 있으므로 업데이트하지 않음
    // 토스페이먼츠의 경우 클라이언트에서 받은 paymentKey를 설정
    if (request.getPaymentKey() != null && !request.getPaymentKey().isEmpty()) {
        if (payment.getPaymentMethod() != PaymentMethod.KAKAO_PAY) {
            payment.updateExternalPaymentId(request.getPaymentKey());
        }
    }
    
    // 4. 카카오페이의 경우 pgToken을 additionalParams로 전달
    Map<String, String> additionalParams = Collections.emptyMap();
    if (payment.getPaymentMethod() == PaymentMethod.KAKAO_PAY) {
        if (request.getPgToken() == null || request.getPgToken().isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "pgToken", 
                    "카카오페이 결제 승인을 위해서는 pgToken이 필수입니다");
        }
        additionalParams = Map.of("pgToken", request.getPgToken());
    }
    
    // 5. PaymentExecutionService를 통한 결제 실행
    // 내부에서 KakaoPayPaymentProvider.confirmPayment() 호출
    // → KakakoApproveApiClient.approve(tid, orderId, userId, pgToken) 호출
    payment = executionService.executePayment(payment, actualAmount, additionalParams);
    
    // 6. 승인 성공 시 Payment 상태가 SUCCESS로 변경됨
    return PaymentConfirmResponse.from(payment);
}
```

**KakaoPayPaymentProvider.confirmPayment() 내부:**
```java
public PaymentResult confirmPayment(
        String paymentKey,  // tid
        String orderId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String userId,
        Map<String, String> additionalParams
) {
    // pgToken 추출
    String pgToken = additionalParams != null ? additionalParams.get("pgToken") : null;
    if (pgToken == null || pgToken.isEmpty()) {
        throw new IllegalArgumentException("pgToken은(는) 필수입니다");
    }
    
    // 카카오페이 승인 API 호출
    var response = kakakoApproveApiClient.approve(
            paymentKey,  // tid
            orderId,     // partner_order_id
            userId,      // partner_user_id
            pgToken      // pg_token
    );
    
    return PaymentResult.builder()
            .externalPaymentId(paymentKey)
            .status(statusMapper.map(response.status()))
            .build();
}
```

#### 3.3.3 DTO 예시

**프론트엔드 → 서버 승인 요청 DTO:**
```java
// PaymentConfirmRequest.java
@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {
    private String orderId;      // Payment ID (String으로 받아서 Long으로 변환)
    private long amount;
    private String paymentKey;   // 토스페이먼츠: paymentKey, 카카오페이: tid (이미 저장됨)
    
    /**
     * 카카오페이 결제 승인 토큰 (pg_token)
     * - 결제 페이지에서 사용자가 결제를 승인한 후 카카오가 redirect URL에 전달
     * - 프론트엔드에서 추출하여 서버로 전달
     * - 1회성 토큰이므로 승인 API 호출 후 즉시 무효화됨
     * - DB에 저장하지 않음
     */
    private String pgToken;
    
    public Long getOrderIdAsLong() {
        return Long.parseLong(this.orderId);
    }
}
```

**서버 → 프론트엔드 응답 DTO:**
```java
// PaymentConfirmResponse.java
@Getter
@Setter
public class PaymentConfirmResponse {
    private String paymentKey;  // externalPaymentId (tid)
    private String orderId;
    private String status;      // PaymentStatus.name()
    private int totalAmount;
    private OffsetDateTime approvedAt;
    private String method;      // PaymentMethod.name()
}
```

---

## 4. 전체 시퀀스 다이어그램

```
[프론트엔드]              [백엔드]                    [카카오페이]
     |                       |                            |
     |-- POST /api/payments -->|                            |
     |   {amount, method}    |                            |
     |                       |                            |
     |                       | [PaymentFacade.requestPayment()]
     |                       | [PaymentService.requestPayment()]
     |                       | [Payment 생성: status=PENDING]
     |                       |                            |
     |                       | [KakaoPayProvider.preparePayment()]
     |                       |-- POST /v1/payment/ready -->|
     |                       |   {cid, partner_order_id,  |
     |                       |    partner_user_id, ...}   |
     |                       |                            |
     |                       |<-- tid, next_redirect_pc_url --|
     |                       |                            |
     |                       | [payment.updateExternalPaymentId(tid)]
     |                       | [Payment 저장: externalPaymentId=tid]
     |                       |                            |
     |<-- {paymentId, redirectUrl} -----------------------|
     |                       |                            |
     |-- GET redirectUrl --------------------------------->|
     |   (카카오페이 결제 페이지)                           |
     |                       |                            |
     | [사용자 결제 승인]                                  |
     |                       |                            |
     |<-- redirect?pg_token=ABC123 -----------------------|
     |   https://your-app.com/payment/callback?pg_token=ABC123
     |                       |                            |
     | [pg_token 추출]                                     |
     | const pgToken = urlParams.get('pg_token');        |
     |                       |                            |
     |-- POST /api/payments/confirm --------------------->|
     |   {orderId, pgToken}  |                            |
     |                       |                            |
     |                       | [PaymentFacade.confirmPayment()]
     |                       | [PaymentService.confirmPayment()]
     |                       |                            |
     |                       | [Payment 조회: paymentId로 조회]
     |                       | [tid = payment.getExternalPaymentId()]
     |                       |                            |
     |                       | [PaymentExecutionService.executePayment()]
     |                       | [additionalParams = {pgToken: "ABC123"}]
     |                       |                            |
     |                       | [KakaoPayProvider.confirmPayment()]
     |                       | [KakakoApproveApiClient.approve()]
     |                       |-- POST /v1/payment/approve ->|
     |                       |   {cid, tid,                |
     |                       |    partner_order_id,        |
     |                       |    partner_user_id,         |
     |                       |    pg_token}                |
     |                       |                            |
     |                       |<-- 승인 완료 --------------|
     |                       |   {status, approved_at}    |
     |                       |                            |
     |                       | [payment.markSuccess(tid)]
     |                       | [Payment 상태 변경: SUCCESS]
     |                       |                            |
     |<-- {paymentId, status, approvedAt} ----------------|
     |                       |                            |
```

---

## 5. 절대 하면 안 되는 것 (금지 사항)

### 5.1 pg_token을 DB에 영구 저장 ❌

```java
// ❌ 잘못된 예시
payment.setPgToken(request.getPgToken());  // 절대 하지 마세요!
paymentRepository.save(payment);
```

**이유:**
- pg_token은 1회성 토큰이므로 저장해도 의미 없음
- 승인 후 즉시 무효화되므로 재사용 불가
- 보안상 불필요한 데이터 저장

### 5.2 프론트엔드에서 approve API 직접 호출 ❌

```javascript
// ❌ 잘못된 예시
async function approvePayment(pgToken) {
  // 프론트엔드에서 직접 카카오페이 API 호출
  await fetch('https://open-api.kakaopay.com/online/v1/payment/approve', {
    method: 'POST',
    body: JSON.stringify({
      tid: '...',  // tid는 서버에만 있음!
      pg_token: pgToken
    })
  });
}
```

**이유:**
- tid는 서버에만 저장되어 있음
- 보안상 민감한 정보 (cid, secret key 등)는 서버에만 있어야 함
- 결제 승인은 반드시 서버에서 처리해야 함

### 5.3 pg_token을 PG 식별자나 보안 키처럼 취급 ❌

```java
// ❌ 잘못된 예시
if (payment.getPgToken() != null) {
    // pg_token으로 결제 조회 시도
    paymentService.getPaymentByPgToken(payment.getPgToken());
}
```

**이유:**
- pg_token은 결제 식별자가 아님 (tid가 식별자)
- pg_token은 승인 API 호출에만 사용됨
- 승인 후에는 더 이상 사용할 수 없음

---

## 6. 핵심 정리

### 6.1 pg_token의 정체

- **카카오가 발급하는 1회성 결제 승인 토큰**
- **PG 식별값이 아님**
- **보안 키가 아님**
- **승인 API 호출 시에만 사용**

### 6.2 프론트엔드 책임

- ✅ redirect URL에서 pg_token 추출
- ✅ 서버로 pg_token 전달
- ❌ pg_token 생성/해석/검증
- ❌ approve API 직접 호출

### 6.3 백엔드 책임

- ✅ Payment(id) 기준으로 tid 조회
- ✅ tid + pg_token으로 승인 API 호출
- ✅ 승인 성공 시 Payment 상태 변경
- ❌ pg_token을 DB에 저장

### 6.4 데이터 흐름

```
1. 서버: POST /api/payments
   → Payment 생성 (status=PENDING)
   → KakaoPayProvider.preparePayment() 호출
   → /v1/payment/ready API 호출
   → tid 받음 → payment.updateExternalPaymentId(tid) → DB 저장
   → redirectUrl 반환

2. 프론트: redirectUrl로 카카오페이 결제 페이지 이동

3. 카카오: 사용자 결제 승인 후
   → redirect URL에 pg_token을 query parameter로 전달
   → 예: https://your-app.com/payment/callback?pg_token=ABC123

4. 프론트: redirect URL에서 pg_token 추출
   → POST /api/payments/confirm {orderId, pgToken} 전달

5. 서버: Payment(id) 기준으로 tid 조회
   → payment.getExternalPaymentId() = tid
   → additionalParams = {pgToken: "ABC123"}
   → KakaoPayProvider.confirmPayment() 호출
   → /v1/payment/approve API 호출 (tid + pg_token)

6. 서버: 승인 성공
   → payment.markSuccess(tid)
   → Payment 상태 변경: SUCCESS
```

---

## 7. FAQ

### Q1. pg_token이 만료되면 어떻게 되나요?

A: pg_token은 매우 짧은 유효기간을 가지며, 승인 API 호출 후 즉시 무효화됩니다. 만료된 pg_token으로 승인 API를 호출하면 카카오페이에서 오류를 반환합니다. 이 경우 사용자에게 다시 결제를 진행하도록 안내해야 합니다.

### Q2. pg_token을 여러 번 사용할 수 있나요?

A: 아니요. pg_token은 1회성 토큰입니다. 한 번 승인 API 호출에 사용되면 즉시 무효화되어 재사용할 수 없습니다.

### Q3. pg_token 없이 승인할 수 있나요?

A: 카카오페이 신규 API(`/v1/payment/approve`)는 pg_token이 필수입니다. pg_token 없이는 승인할 수 없습니다.

### Q4. tid와 pg_token의 차이는?

A: 
- **tid**: 결제 고유 ID, `/v1/payment/ready`에서 받음, DB에 저장, 재사용 가능
- **pg_token**: 결제 승인 토큰, 결제 페이지에서 받음, DB에 저장하지 않음, 1회성

### Q5. pg_token을 로그에 남겨도 되나요?

A: pg_token은 민감한 정보는 아니지만, 로그에 남기는 것은 권장하지 않습니다. 필요하다면 마스킹 처리하거나 로그 레벨을 조정하세요.

---

## 8. 참고 자료

- [카카오페이 개발자 문서](https://developers.kakao.com/docs/latest/ko/kakaopay/single-payment)
- 카카오페이 API 명세서

---

**작성일**: 2024년
**최종 수정일**: 2024년
**작성자**: 백엔드 아키텍트


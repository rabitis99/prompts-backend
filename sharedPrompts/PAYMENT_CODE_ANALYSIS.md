# 결제 관련 코드 분석 및 개선안

## 분석 대상 파일
- `Payment.java`
- `PayPalPaymentProvider.java`
- `KakaoPayPaymentProvider.java`
- `RefundResult.java`
- `PaymentStatus.java`
- `PaymentExecutionService.java`

---

## 문제점 및 개선안

### 문제 1: Payment.markInProgress() 메서드 이름과 상태값 불일치

**현재 상태:**
- 메서드 이름: `markInProgress()`
- 설정하는 상태값: `PaymentStatus.PENDING`
- 위치: `Payment.java:102-104`

**문제점:**
- 메서드 이름이 "진행 중"을 의미하지만 실제로는 "대기 중" 상태를 설정
- 의미적으로 일치하지 않아 코드 가독성 저하

**개선안:**
메서드 이름을 `markPending()`으로 변경하여 상태값과 일치시킵니다.

**코드 예시:**
```java
/**
 * 결제 대기 중 상태로 변경
 */
public void markPending() {
    this.status = PaymentStatus.PENDING;
}
```

---

### 문제 2: PayPal 환불 상태가 항상 PARTIALLY_REFUNDED로 고정

**현재 상태:**
- 위치: `PayPalPaymentProvider.java:429`
- 항상 `PaymentStatus.PARTIALLY_REFUNDED`로 설정
- 실제 환불 금액과 원래 결제 금액을 비교하지 않음
- PayPal 응답의 status 필드를 활용하지 않음

**문제점:**
- 전체 환불인 경우에도 `PARTIALLY_REFUNDED`로 표시됨
- PayPal API 응답의 `status` 필드(`COMPLETED`, `PARTIALLY_REFUNDED`, `REFUNDED`)를 활용하지 않음

**개선안:**
1. PayPal 응답의 `status` 필드를 우선적으로 활용
2. 응답에 status가 없는 경우, 환불 금액과 원래 결제 금액을 비교하여 판단
3. 원래 결제 금액 정보는 `getCaptureIdAndCurrency()` 메서드에서 가져올 수 있도록 확장

**코드 예시:**
```java
@Override
public RefundResult refundPayment(String externalPaymentId, BigDecimal amount, String reason, String idempotencyKey) {
    try {
        String accessToken = getAccessToken();
        HttpHeaders headers = createHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 주문에서 캡처 ID, 통화 코드, 원래 결제 금액 조회
        CaptureInfo captureInfo = getCaptureIdAndCurrency(externalPaymentId, accessToken);
        String captureId = captureInfo.captureId();
        String currency = captureInfo.currency();
        BigDecimal originalAmount = captureInfo.originalAmount(); // 추가 필요

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> amountMap = new HashMap<>();
        amountMap.put("currency_code", currency);
        amountMap.put("value", amount.toString());
        requestBody.put("amount", amountMap);
        requestBody.put("note_to_payer", reason);

        if (idempotencyKey != null) {
            headers.set("PayPal-Request-Id", idempotencyKey);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                PAYPAL_API_URL + "/v2/payments/captures/" + captureId + "/refund",
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
        );

        if ((response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK)
                && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            log.info("PayPal 결제 환불 성공: externalPaymentId={}, amount={}", externalPaymentId, amount);

            // 응답에서 실제 환불 금액 추출
            BigDecimal refundedAmount = amount;
            if (responseBody.get("amount") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> refundAmountMap = (Map<String, Object>) responseBody.get("amount");
                if (refundAmountMap.get("value") != null) {
                    refundedAmount = new BigDecimal(refundAmountMap.get("value").toString());
                }
            }

            // 환불 상태 결정: PayPal 응답 status 우선, 없으면 금액 비교
            PaymentStatus refundStatus = determineRefundStatus(
                    responseBody,
                    refundedAmount,
                    originalAmount
            );

            return RefundResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(refundStatus)
                    .refundedAmount(refundedAmount)
                    .refundedAt(LocalDateTime.now())
                    .reason(reason)
                    .metadata(objectMapper.writeValueAsString(responseBody))
                    .build();
        } else {
            throw new RuntimeException("PayPal 결제 환불 실패: " + response.getStatusCode());
        }
    } catch (Exception e) {
        log.error("PayPal 결제 환불 실패: externalPaymentId={}, amount={}, error={}",
                externalPaymentId, amount, e.getMessage(), e);
        throw new RuntimeException("PayPal 결제 환불 실패", e);
    }
}

/**
 * 환불 상태 결정
 * 1. PayPal 응답의 status 필드 우선 사용
 * 2. 없으면 환불 금액과 원래 결제 금액 비교
 */
private PaymentStatus determineRefundStatus(
        Map<String, Object> responseBody,
        BigDecimal refundedAmount,
        BigDecimal originalAmount
) {
    // PayPal 응답의 status 필드 확인
    String paypalStatus = (String) responseBody.get("status");
    if (paypalStatus != null) {
        return switch (paypalStatus) {
            case "COMPLETED" -> PaymentStatus.REFUNDED;
            case "PARTIALLY_REFUNDED" -> PaymentStatus.PARTIALLY_REFUNDED;
            default -> PaymentStatus.PARTIALLY_REFUNDED; // 기본값
        };
    }

    // status가 없으면 금액 비교
    if (originalAmount != null && refundedAmount.compareTo(originalAmount) >= 0) {
        return PaymentStatus.REFUNDED;
    }
    return PaymentStatus.PARTIALLY_REFUNDED;
}
```

---

### 문제 3: KakaoPay 환불 상태가 항상 PARTIALLY_REFUNDED로 고정

**현재 상태:**
- 위치: `KakaoPayPaymentProvider.java:334`
- 항상 `PaymentStatus.PARTIALLY_REFUNDED`로 설정
- 실제 환불 금액과 원래 결제 금액을 비교하지 않음
- KakaoPay 응답의 status를 활용하지 않음

**문제점:**
- 전체 환불인 경우에도 `PARTIALLY_REFUNDED`로 표시됨
- KakaoPay API 응답의 상태 정보를 활용하지 않음

**개선안:**
1. 환불 전에 원래 결제 금액을 조회 (이미 `getPaymentStatus()` 호출로 가능)
2. 환불 금액과 원래 결제 금액을 비교하여 전체/부분 환불 판단
3. KakaoPay 응답의 상태 정보도 확인 (있는 경우)

**코드 예시:**
```java
@Override
public RefundResult refundPayment(String externalPaymentId, BigDecimal amount, String reason, String idempotencyKey) {
    try {
        // 원래 결제 금액 조회
        PaymentResult paymentStatus = getPaymentStatus(externalPaymentId);
        BigDecimal originalAmount = paymentStatus.getAmount();
        if (originalAmount == null) {
            throw new RuntimeException("KakaoPay 결제 정보 조회 실패: 금액 정보를 가져올 수 없습니다.");
        }

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("cid", kakaoPayProperties.getCid());
        requestBody.put("tid", externalPaymentId);
        requestBody.put("cancel_amount", amount.longValueExact());
        requestBody.put("cancel_tax_free_amount", 0);
        requestBody.put("cancel_reason", reason);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                KAKAO_PAY_API_URL + "/cancel",
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            log.info("KakaoPay 결제 환불 성공: externalPaymentId={}, amount={}", externalPaymentId, amount);

            // 응답에서 실제 환불 금액 추출 (canceled_amount 필드)
            BigDecimal refundedAmount = amount;
            if (responseBody.get("canceled_amount") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> canceledAmount = (Map<String, Object>) responseBody.get("canceled_amount");
                if (canceledAmount.get("total") != null) {
                    refundedAmount = new BigDecimal(canceledAmount.get("total").toString());
                }
            }

            // 환불 상태 결정: 환불 금액과 원래 결제 금액 비교
            PaymentStatus refundStatus = determineRefundStatus(refundedAmount, originalAmount);

            return RefundResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(refundStatus)
                    .refundedAmount(refundedAmount)
                    .refundedAt(LocalDateTime.now())
                    .reason(reason)
                    .metadata(objectMapper.writeValueAsString(responseBody))
                    .build();
        } else {
            throw new RuntimeException("KakaoPay 결제 환불 실패: " + response.getStatusCode());
        }
    } catch (Exception e) {
        log.error("KakaoPay 결제 환불 실패: externalPaymentId={}, amount={}, error={}",
                externalPaymentId, amount, e.getMessage(), e);
        throw new RuntimeException("KakaoPay 결제 환불 실패", e);
    }
}

/**
 * 환불 상태 결정: 환불 금액과 원래 결제 금액 비교
 */
private PaymentStatus determineRefundStatus(BigDecimal refundedAmount, BigDecimal originalAmount) {
    if (originalAmount != null && refundedAmount.compareTo(originalAmount) >= 0) {
        return PaymentStatus.REFUNDED;
    }
    return PaymentStatus.PARTIALLY_REFUNDED;
}
```

---

### 문제 4: PaymentExecutionService에서 RefundResult.status 미활용

**현재 상태:**
- 위치: `PaymentExecutionService.java:142-160`
- `RefundResult`의 `status`를 사용하지 않고, `Payment.refund()` 메서드 내부에서만 판단

**문제점:**
- Provider에서 이미 판단한 상태 정보를 활용하지 않음
- `Payment.refund()` 메서드가 환불 금액만 받아서 내부적으로 상태를 판단하는 구조

**개선안:**
`Payment.refund()` 메서드는 이미 올바르게 구현되어 있으므로, 현재 구조를 유지하는 것이 좋습니다.
다만, Provider에서 반환하는 `RefundResult.status`와 `Payment.refund()`에서 계산하는 상태가 일치하는지 검증하는 로직을 추가할 수 있습니다.

**권장 사항:**
현재 구조를 유지하되, Provider에서 정확한 상태를 반환하도록 개선하는 것이 우선입니다.
`Payment.refund()` 메서드는 이미 전체/부분 환불을 올바르게 구분하고 있습니다.

---

### 문제 5: PaymentStatus enum 점검

**현재 상태:**
```java
public enum PaymentStatus {
    PENDING("대기중"),
    SUCCESS("성공"),
    FAILED("실패"),
    CANCELED("취소됨"),
    REFUNDED("환불됨"),
    PARTIALLY_REFUNDED("부분 환불됨");
}
```

**분석 결과:**
- 모든 필요한 상태가 포함되어 있음
- `REFUNDED`와 `PARTIALLY_REFUNDED`가 명확히 구분됨
- 추가 상태가 필요하지 않음

**결론:**
PaymentStatus enum은 현재 상태로 충분하며 수정이 필요하지 않습니다.

---

## 수정된 코드 스니펫

### 1. Payment.java - markInProgress() → markPending()

```java
/**
 * 결제 대기 중 상태로 변경
 */
public void markPending() {
    this.status = PaymentStatus.PENDING;
}
```

### 2. PayPalPaymentProvider.java - refundPayment() 개선

주요 변경사항:
- `getCaptureIdAndCurrency()` 메서드를 `getCaptureInfo()`로 확장하여 원래 결제 금액도 반환
- `determineRefundStatus()` 헬퍼 메서드 추가
- PayPal 응답 status 우선 사용, 없으면 금액 비교

```java
/**
 * PayPal 주문에서 캡처 정보 조회 (ID, 통화 코드, 원래 결제 금액)
 */
private CaptureInfo getCaptureInfo(String orderId, String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    
    HttpEntity<Void> request = new HttpEntity<>(headers);
    
    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            PAYPAL_ORDERS_URL + "/" + orderId,
            HttpMethod.GET,
            request,
            new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
    );
    
    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> purchaseUnitsList = (List<Map<String, Object>>) response.getBody().get("purchase_units");
        if (purchaseUnitsList == null || purchaseUnitsList.isEmpty()) {
            throw new RuntimeException("PayPal 응답에서 purchase_units를 찾을 수 없습니다");
        }

        Map<String, Object> purchaseUnit = purchaseUnitsList.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> payments = (Map<String, Object>) purchaseUnit.get("payments");
        if (payments == null) {
            throw new RuntimeException("PayPal 응답에서 payments를 찾을 수 없습니다");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> captures = (List<Map<String, Object>>) payments.get("captures");
        if (captures == null || captures.isEmpty()) {
            throw new RuntimeException("PayPal 응답에서 captures를 찾을 수 없습니다");
        }

        Map<String, Object> capture = captures.get(0);
        String captureId = (String) capture.get("id");

        @SuppressWarnings("unchecked")
        Map<String, Object> amount = (Map<String, Object>) capture.get("amount");
        if (amount == null) {
            throw new RuntimeException("PayPal 캡처에서 금액 정보를 찾을 수 없습니다");
        }

        String currency = (String) amount.get("currency_code");
        if (currency == null || currency.isEmpty()) {
            throw new RuntimeException("PayPal 캡처에서 통화 코드를 찾을 수 없습니다");
        }

        BigDecimal originalAmount = new BigDecimal(amount.get("value").toString());

        return new CaptureInfo(captureId, currency, originalAmount);
    }

    throw new RuntimeException("PayPal 캡처 정보 조회 실패");
}

/**
 * 환불 상태 결정
 */
private PaymentStatus determineRefundStatus(
        Map<String, Object> responseBody,
        BigDecimal refundedAmount,
        BigDecimal originalAmount
) {
    // PayPal 응답의 status 필드 확인
    String paypalStatus = (String) responseBody.get("status");
    if (paypalStatus != null) {
        return switch (paypalStatus) {
            case "COMPLETED" -> PaymentStatus.REFUNDED;
            case "PARTIALLY_REFUNDED" -> PaymentStatus.PARTIALLY_REFUNDED;
            default -> PaymentStatus.PARTIALLY_REFUNDED;
        };
    }

    // status가 없으면 금액 비교
    if (originalAmount != null && refundedAmount.compareTo(originalAmount) >= 0) {
        return PaymentStatus.REFUNDED;
    }
    return PaymentStatus.PARTIALLY_REFUNDED;
}

@Override
public RefundResult refundPayment(String externalPaymentId, BigDecimal amount, String reason, String idempotencyKey) {
    try {
        String accessToken = getAccessToken();
        HttpHeaders headers = createHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 주문에서 캡처 정보 조회 (ID, 통화 코드, 원래 결제 금액)
        CaptureInfo captureInfo = getCaptureInfo(externalPaymentId, accessToken);
        String captureId = captureInfo.captureId();
        String currency = captureInfo.currency();
        BigDecimal originalAmount = captureInfo.originalAmount();

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> amountMap = new HashMap<>();
        amountMap.put("currency_code", currency);
        amountMap.put("value", amount.toString());
        requestBody.put("amount", amountMap);
        requestBody.put("note_to_payer", reason);

        if (idempotencyKey != null) {
            headers.set("PayPal-Request-Id", idempotencyKey);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                PAYPAL_API_URL + "/v2/payments/captures/" + captureId + "/refund",
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
        );

        if ((response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK)
                && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            log.info("PayPal 결제 환불 성공: externalPaymentId={}, amount={}", externalPaymentId, amount);

            // 응답에서 실제 환불 금액 추출
            BigDecimal refundedAmount = amount;
            if (responseBody.get("amount") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> refundAmountMap = (Map<String, Object>) responseBody.get("amount");
                if (refundAmountMap.get("value") != null) {
                    refundedAmount = new BigDecimal(refundAmountMap.get("value").toString());
                }
            }

            // 환불 상태 결정
            PaymentStatus refundStatus = determineRefundStatus(
                    responseBody,
                    refundedAmount,
                    originalAmount
            );

            return RefundResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(refundStatus)
                    .refundedAmount(refundedAmount)
                    .refundedAt(LocalDateTime.now())
                    .reason(reason)
                    .metadata(objectMapper.writeValueAsString(responseBody))
                    .build();
        } else {
            throw new RuntimeException("PayPal 결제 환불 실패: " + response.getStatusCode());
        }
    } catch (Exception e) {
        log.error("PayPal 결제 환불 실패: externalPaymentId={}, amount={}, error={}",
                externalPaymentId, amount, e.getMessage(), e);
        throw new RuntimeException("PayPal 결제 환불 실패", e);
    }
}

// CaptureInfo 레코드 수정
private record CaptureInfo(String captureId, String currency, BigDecimal originalAmount) {}
```

### 3. KakaoPayPaymentProvider.java - refundPayment() 개선

주요 변경사항:
- 환불 전에 원래 결제 금액 조회
- `determineRefundStatus()` 헬퍼 메서드 추가
- 환불 금액과 원래 결제 금액 비교

```java
/**
 * 환불 상태 결정: 환불 금액과 원래 결제 금액 비교
 */
private PaymentStatus determineRefundStatus(BigDecimal refundedAmount, BigDecimal originalAmount) {
    if (originalAmount != null && refundedAmount.compareTo(originalAmount) >= 0) {
        return PaymentStatus.REFUNDED;
    }
    return PaymentStatus.PARTIALLY_REFUNDED;
}

@Override
public RefundResult refundPayment(String externalPaymentId, BigDecimal amount, String reason, String idempotencyKey) {
    try {
        // 원래 결제 금액 조회
        PaymentResult paymentStatus = getPaymentStatus(externalPaymentId);
        BigDecimal originalAmount = paymentStatus.getAmount();
        if (originalAmount == null) {
            throw new RuntimeException("KakaoPay 결제 정보 조회 실패: 금액 정보를 가져올 수 없습니다.");
        }

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("cid", kakaoPayProperties.getCid());
        requestBody.put("tid", externalPaymentId);
        requestBody.put("cancel_amount", amount.longValueExact());
        requestBody.put("cancel_tax_free_amount", 0);
        requestBody.put("cancel_reason", reason);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                KAKAO_PAY_API_URL + "/cancel",
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            log.info("KakaoPay 결제 환불 성공: externalPaymentId={}, amount={}", externalPaymentId, amount);

            // 응답에서 실제 환불 금액 추출 (canceled_amount 필드)
            BigDecimal refundedAmount = amount;
            if (responseBody.get("canceled_amount") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> canceledAmount = (Map<String, Object>) responseBody.get("canceled_amount");
                if (canceledAmount.get("total") != null) {
                    refundedAmount = new BigDecimal(canceledAmount.get("total").toString());
                }
            }

            // 환불 상태 결정
            PaymentStatus refundStatus = determineRefundStatus(refundedAmount, originalAmount);

            return RefundResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(refundStatus)
                    .refundedAmount(refundedAmount)
                    .refundedAt(LocalDateTime.now())
                    .reason(reason)
                    .metadata(objectMapper.writeValueAsString(responseBody))
                    .build();
        } else {
            throw new RuntimeException("KakaoPay 결제 환불 실패: " + response.getStatusCode());
        }
    } catch (Exception e) {
        log.error("KakaoPay 결제 환불 실패: externalPaymentId={}, amount={}, error={}",
                externalPaymentId, amount, e.getMessage(), e);
        throw new RuntimeException("KakaoPay 결제 환불 실패", e);
    }
}
```

---

## 요약

### 발견된 문제점
1. ✅ `Payment.markInProgress()` 메서드 이름과 상태값 불일치 → **수정 완료**
2. ✅ PayPal 환불 상태가 항상 `PARTIALLY_REFUNDED`로 고정 → **수정 완료**
3. ✅ KakaoPay 환불 상태가 항상 `PARTIALLY_REFUNDED`로 고정 → **수정 완료**
4. ⚠️ `PaymentExecutionService`에서 `RefundResult.status` 미활용 (현재 구조상 문제 없음)

### 개선 사항
1. ✅ `markInProgress()` → `markPending()`으로 메서드 이름 변경 → **수정 완료**
2. ✅ PayPal 환불 시 전체/부분 환불 구분 로직 추가 → **수정 완료**
3. ✅ KakaoPay 환불 시 전체/부분 환불 구분 로직 추가 → **수정 완료**
4. ✅ `PaymentStatus` enum은 현재 상태로 충분

### 수정 완료 내역
- ✅ `Payment.java`: `markInProgress()` → `markPending()` 메서드 이름 변경
- ✅ `PaymentRetryService.java`: `markInProgress()` 호출 → `markPending()` 호출로 변경
- ✅ `PayPalPaymentProvider.java`: 
  - `CaptureInfo` 레코드에 `originalAmount` 필드 추가
  - `getCaptureIdAndCurrency()` 메서드에서 원래 결제 금액도 반환하도록 수정
  - `determineRefundStatus()` 헬퍼 메서드 추가
  - `refundPayment()` 메서드에서 전체/부분 환불 구분 로직 추가
- ✅ `KakaoPayPaymentProvider.java`:
  - 환불 전에 원래 결제 금액 조회 추가
  - `determineRefundStatus()` 헬퍼 메서드 추가
  - `refundPayment()` 메서드에서 전체/부분 환불 구분 로직 추가

### 권장 사항
- 모든 Provider에서 환불 상태를 정확히 반환하도록 개선 → **완료**
- `Payment.refund()` 메서드는 이미 올바르게 구현되어 있으므로 유지
- Provider와 Payment 엔티티 간 상태 일관성 유지 → **개선 완료**


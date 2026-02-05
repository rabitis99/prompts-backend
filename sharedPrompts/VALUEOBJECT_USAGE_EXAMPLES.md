# ValueObject 사용 예시 가이드

## 📋 개요

Payment 도메인에서 ValueObject를 서비스 레이어에서 사용하는 방법을 설명합니다.

## ✅ 적용 완료

### 1. PaymentAmountFacade
- `processPaymentAmount()`: 결제 금액 처리 시 ValueObject 사용
- `calculateRefundPointAmount()`: 환불 포인트 계산 시 ValueObject 사용

### 2. CashbackAmountService
- `calculateCashbackAmount()`: 캐시백 금액 계산 시 ValueObject 사용
- 통화 정보가 있는 경우와 없는 경우 모두 지원

### 3. PaymentPostProcessService
- `executeSuccessPostProcessing()`: 포인트 사용 금액 계산 시 ValueObject 사용
- 원본 금액과 실제 결제 금액 간 계산의 타입 안정성 보장

## 📝 다른 서비스에 적용 예시

### 1. CashbackAmountService에서 사용 (적용 완료)

```java
@Service
public class CashbackAmountService {
    
    public BigDecimal calculateCashbackAmount(BigDecimal paymentAmount, BigDecimal cashbackRate) {
        // PaymentAmount로 변환 (캐시백은 KRW 기준)
        PaymentAmount payment = PaymentAmount.krw(paymentAmount);
        
        // 캐시백 금액 계산
        PaymentAmount cashbackAmount = payment.multiply(cashbackRate);
        
        // 소수점 둘째 자리까지 반올림하여 반환
        return cashbackAmount.toBigDecimal().setScale(2, RoundingMode.DOWN);
    }
    
    // 통화 정보가 있는 경우
    public BigDecimal calculateCashbackAmount(BigDecimal paymentAmount, String currency, BigDecimal cashbackRate) {
        PaymentAmount payment = PaymentAmount.of(paymentAmount, currency);
        PaymentAmount cashbackAmount = payment.multiply(cashbackRate);
        return cashbackAmount.toBigDecimal().setScale(2, RoundingMode.DOWN);
    }
}
```

### 2. PaymentPostProcessService에서 사용 (적용 완료)

```java
private void executeSuccessPostProcessing(Payment payment, Long userId, 
        BigDecimal actualPaymentAmount, BigDecimal originalAmount, long processingTime) {
    // PaymentAmount로 변환하여 타입 안정성 확보
    PaymentAmount originalPaymentAmount = PaymentAmount.of(
        originalAmount,
        payment.getCurrency()
    );
    PaymentAmount actualPaymentAmountVO = PaymentAmount.of(
        actualPaymentAmount,
        payment.getCurrency()
    );
    
    // 사용된 포인트 금액 계산 (같은 통화이므로 안전하게 계산 가능)
    PaymentAmount usedPointAmountVO = originalPaymentAmount.subtract(actualPaymentAmountVO);
    BigDecimal usedPointAmount = usedPointAmountVO.toBigDecimal();
    
    // 포인트/캐시백 적립 처리
    // ...
}
```

### 3. PaymentExecutionService에서 사용 (예시)

```java
@Service
public class PaymentExecutionService {
    
    public PaymentResult executePayment(Payment payment, PaymentRequestDto request) {
        // 원본 금액
        PaymentAmount originalAmount = PaymentAmount.of(
            request.getAmount(),
            request.getCurrency()
        );
        
        // 환율 변환
        if (!originalAmount.getCurrency().equals(Currency.KRW())) {
            BigDecimal rate = exchangeRateService.getExchangeRate(
                originalAmount.getCurrencyCode(),
                Currency.KRW().getCode()
            );
            
            ExchangeRate exchangeRate = ExchangeRate.of(
                originalAmount.getCurrency(),
                Currency.KRW(),
                rate
            );
            
            originalAmount = exchangeRate.convert(originalAmount);
        }
        
        // 결제 실행
        // ...
    }
}
```

### 3. 환불 금액 계산

```java
public BigDecimal calculateRefundAmount(Payment payment, BigDecimal refundRequestAmount) {
    PaymentAmount paymentAmount = PaymentAmount.of(
        payment.getAmount(),
        payment.getCurrency()
    );
    
    PaymentAmount refundRequest = PaymentAmount.of(
        refundRequestAmount,
        payment.getCurrency()
    );
    
    // 환불 가능 금액 확인
    PaymentAmount refundedAmount = PaymentAmount.of(
        payment.getRefundedAmount(),
        payment.getCurrency()
    );
    
    PaymentAmount refundableAmount = paymentAmount.subtract(refundedAmount);
    
    if (refundRequest.isGreaterThan(refundableAmount)) {
        throw new ApiException(ErrorCode.REFUND_AMOUNT_EXCEEDED);
    }
    
    return refundRequest.toBigDecimal();
}
```

### 4. 포인트 적립 계산

```java
public Point calculatePointAccrual(Payment payment) {
    PaymentAmount paymentAmount = PaymentAmount.of(
        payment.getAmount(),
        payment.getCurrency()
    );
    
    // 포인트 적립률 (예: 1%)
    BigDecimal accrualRate = BigDecimal.valueOf(0.01);
    PaymentAmount pointAmount = paymentAmount.multiply(accrualRate);
    
    return Point.builder()
        .amount(pointAmount.toBigDecimal())
        .type(PointType.PAYMENT)
        .build();
}
```

## 🔄 변환 패턴

### 엔티티 → ValueObject

```java
// Payment 엔티티에서 PaymentAmount 생성
PaymentAmount paymentAmount = PaymentAmount.of(
    payment.getAmount(),
    payment.getCurrency()
);
```

### ValueObject → 엔티티 저장

```java
// PaymentAmount를 엔티티 필드에 저장
payment.setAmount(paymentAmount.toBigDecimal());
payment.setCurrency(paymentAmount.getCurrencyCode());
```

### DTO → ValueObject

```java
// PaymentRequestDto에서 PaymentAmount 생성
PaymentAmount amount = PaymentAmount.of(
    request.getAmount(),
    request.getCurrency()
);
```

### ValueObject → DTO

```java
// PaymentAmount를 DTO에 매핑
PaymentResponseDto dto = PaymentResponseDto.builder()
    .amount(paymentAmount.toBigDecimal())
    .currency(paymentAmount.getCurrencyCode())
    .build();
```

## ⚠️ 주의사항

### 1. 통화 일치 검증

```java
// ❌ 잘못된 예: 다른 통화끼리 계산 시도
PaymentAmount krwAmount = PaymentAmount.krw(BigDecimal.valueOf(10000));
PaymentAmount usdAmount = PaymentAmount.usd(BigDecimal.valueOf(100));
PaymentAmount total = krwAmount.add(usdAmount); // IllegalArgumentException 발생!

// ✅ 올바른 예: 같은 통화끼리만 계산
PaymentAmount amount1 = PaymentAmount.krw(BigDecimal.valueOf(10000));
PaymentAmount amount2 = PaymentAmount.krw(BigDecimal.valueOf(5000));
PaymentAmount total = amount1.add(amount2); // OK
```

### 2. null 처리

```java
// ❌ 잘못된 예
PaymentAmount amount = PaymentAmount.of(null, "KRW"); // IllegalArgumentException

// ✅ 올바른 예
if (request.getAmount() != null) {
    PaymentAmount amount = PaymentAmount.of(request.getAmount(), request.getCurrency());
}
```

### 3. 엔티티 필드는 그대로 유지

```java
// 엔티티는 BigDecimal과 String을 그대로 사용
@Entity
public class Payment {
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // ValueObject로 변경하지 않음
    
    @Column(nullable = false, length = 3)
    private String currency; // ValueObject로 변경하지 않음
}

// 서비스 레이어에서만 ValueObject 사용
@Service
public class PaymentService {
    public void processPayment(Payment payment) {
        // 서비스에서 ValueObject로 변환하여 계산
        PaymentAmount amount = PaymentAmount.of(
            payment.getAmount(),
            payment.getCurrency()
        );
        
        // 계산 후 엔티티에 저장
        PaymentAmount result = amount.multiply(BigDecimal.valueOf(0.1));
        payment.setAmount(result.toBigDecimal());
    }
}
```

## 🎯 적용 우선순위

1. **높은 우선순위**: 금액 계산이 복잡한 서비스
   - `PaymentAmountFacade` ✅ (완료)
   - `CashbackService`
   - `PaymentExecutionService`

2. **중간 우선순위**: 간단한 계산이 있는 서비스
   - `PointService`
   - 환불 관련 서비스

3. **낮은 우선순위**: 단순 조회만 하는 서비스
   - 조회 전용 서비스는 필요 시에만 적용

## 📚 참고

- ValueObject는 불변 객체이므로, 계산 결과는 항상 새로운 객체를 반환합니다.
- 엔티티 필드는 JPA 매핑을 위해 기본 타입(BigDecimal, String)을 유지합니다.
- 서비스 레이어에서만 ValueObject를 사용하여 도메인 규칙을 보장합니다.


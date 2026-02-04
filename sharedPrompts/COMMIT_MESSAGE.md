refactor(payment): 코드 리뷰 피드백 반영 - 중복 제거 및 정밀도 개선

## 주요 변경사항

### 중복 코드 제거 및 단순화
- PaymentWebhookFacade: 중복 catch 블록 통합
  - IllegalArgumentException과 RuntimeException catch 블록이 동일한 로직 수행
  - RuntimeException 하나로 통합하여 코드 중복 제거
- PaymentServiceImpl: fallback 로직 단순화
  - objectMapper 실패 후 동일한 objectMapper로 재시도하는 불필요한 로직 제거
  - 최종 fallback은 하드코딩된 JSON 문자열 반환으로 변경
- ShedLockDistributedLockService: 불필요한 catch 블록 제거
  - LockAcquisitionException catch가 단순히 rethrow만 수행하여 제거
  - 일반 Exception catch와 동일하게 동작하므로 통합

### 정밀도 및 안정성 개선
- KakaoCancelApiClient: 금융 계산 정밀도 개선
  - double 타입 사용으로 인한 부동소수점 정밀도 손실 문제 해결
  - BigDecimal을 사용하여 정수 오버플로우 방지 및 정밀도 유지
  - 면세 금액 계산 시 HALF_UP 반올림 적용

### 문서화 개선
- DistributedLockService: tryLock/unlock 계약 문서화
  - Javadoc에 락 해제 필수 사항 및 executeWithLock 사용 권장 사항 추가
  - 락 누수 방지를 위한 사용 주의사항 명시
- KakaoPayProperties: 파일 끝 불필요한 공백 제거
  - 파일 끝의 여러 빈 줄을 하나의 newline으로 정리

### 아키텍처 개선
- Payment 엔티티: 검증 로직 제거
  - canCancel(), canRefund() 메서드 제거
  - 검증 로직은 이미 PaymentValidationService에 존재
  - 엔티티는 상태 보관에 집중, 검증/정책 판단은 서비스 계층으로 분리
  - 팀 가이드라인 준수: 엔티티에 비즈니스 규칙 포함 금지

## 수정된 파일

### Facade
- `domain/payment/facade/PaymentWebhookFacade.java`
  - 중복 catch 블록 통합 (IllegalArgumentException 제거)

### Service
- `domain/payment/service/core/PaymentServiceImpl.java`
  - fallback 로직 단순화 (불필요한 objectMapper 재시도 제거)
- `domain/payment/service/lock/DistributedLockService.java`
  - tryLock 메서드 Javadoc에 락 해제 필수 사항 추가
- `domain/payment/service/lock/ShedLockDistributedLockService.java`
  - 불필요한 LockAcquisitionException catch 블록 제거

### Entity
- `domain/payment/Payment.java`
  - canCancel(), canRefund() 검증 메서드 제거
  - 검증 로직은 PaymentValidationService에서 처리

### Provider
- `domain/payment/provider/kakao/client/KakaoCancelApiClient.java`
  - 면세 금액 계산에 BigDecimal 사용 (double → BigDecimal)
  - RoundingMode.HALF_UP 적용

### Config
- `domain/payment/config/KakaoPayProperties.java`
  - 파일 끝 불필요한 공백 제거

## 개선 효과

### 코드 품질
- 중복 코드 제거로 유지보수성 향상
- 불필요한 재시도 로직 제거로 성능 개선
- 명확한 문서화로 사용자 오류 방지

### 정밀도 및 안정성
- 금융 계산에서 BigDecimal 사용으로 정밀도 보장
- 정수 오버플로우 방지

### 아키텍처
- 엔티티와 서비스 계층 책임 분리 명확화
- 검증 로직 중앙화로 일관성 확보

## 관련 이슈

- 코드 리뷰 피드백 반영
- 엔티티 검증 로직 제거 (서비스 계층으로 이동)
- 금융 계산 정밀도 개선

---

refactor(payment): API 중복 호출 제거 및 트랜잭션 전파 순서 개선

## 주요 변경사항

### API 중복 호출 제거
- KakaoPayPaymentProvider: Status API 중복 호출 제거
  - cancelPayment()에서 이미 status() 호출 후 cancel() 내부에서도 status() 재호출
  - KakaoCancelApiClient.cancel()에 totalAmount, taxFreeAmount 파라미터 추가
  - Provider에서 조회한 금액 정보를 파라미터로 전달하여 내부 조회 생략
  - Status API 호출 횟수: 2회 → 1회로 감소

### 트랜잭션 전파 순서 개선
- CashbackServiceImpl: @Transactional 제거
  - accumulateCashback(), payCashback(), payCashbackForAdmin() 메서드의 @Transactional 제거
  - 트랜잭션은 CashbackFacade의 TransactionTemplate으로 관리
  - 락 획득 → 트랜잭션 시작 순서 보장
- CashbackFacade: TransactionTemplate에 REQUIRES_NEW 설정
  - 상위 트랜잭션과 독립적으로 실행
  - 락 획득 후 독립적인 트랜잭션 시작으로 DB 커넥션 고갈 방지
- PointServiceImpl: TransactionTemplate에 REQUIRES_NEW 설정
  - 상위 트랜잭션과 독립적으로 실행
  - 동일한 패턴 적용으로 일관성 확보

### Null 안전성 개선
- PaymentExecutionService: refundedAmount null 방어 로직 추가
  - generateRefundIdempotencyKey()에서 refundedAmount가 null일 때 NPE 방지
  - DB에서 로드 시 null일 수 있으므로 BigDecimal.ZERO 기본값 사용

### 락 관리 개선
- ShedLockDistributedLockService: 중복 락 획득 방지
  - tryLock()에서 동일 키로 중복 호출 시 락 누수 방지
  - activeLocks에 이미 존재하는 경우 false 반환
  - 이전 SimpleLock이 덮어쓰이지 않도록 보호

### 데이터 무결성 개선
- PointServiceImpl: paymentId 유효성 및 소유자 검증 추가
  - doAddPointsDirectly()에서 paymentId가 제공되면 반드시 존재하는지 확인
  - 결제 소유자와 사용자 정보 일치 여부 검증
  - 잘못된 paymentId나 다른 사용자 결제로 포인트 적립 방지

## 수정된 파일

### Provider
- `domain/payment/provider/impl/KakaoPayPaymentProvider.java`
  - cancelPayment()에서 조회한 금액 정보를 cancel()에 전달
- `domain/payment/provider/kakao/client/KakaoCancelApiClient.java`
  - cancel() 메서드에 totalAmount, taxFreeAmount 파라미터 추가
  - 내부 status() 호출 제거

### Service - Cashback
- `domain/payment/service/cashback/CashbackServiceImpl.java`
  - accumulateCashback(), payCashback(), payCashbackForAdmin()의 @Transactional 제거
- `domain/payment/service/cashback/facade/CashbackFacade.java`
  - TransactionTemplate에 REQUIRES_NEW 전파 설정
  - @RequiredArgsConstructor 제거, 수동 생성자로 변경

### Service - Point
- `domain/payment/service/point/PointServiceImpl.java`
  - TransactionTemplate에 REQUIRES_NEW 전파 설정
  - doAddPointsDirectly()에서 paymentId 유효성 및 소유자 검증 추가

### Service - Execution
- `domain/payment/service/execution/PaymentExecutionService.java`
  - generateRefundIdempotencyKey()에서 refundedAmount null 방어 로직 추가

### Service - Lock
- `domain/payment/service/lock/ShedLockDistributedLockService.java`
  - tryLock()에서 중복 락 획득 방지 로직 추가

## 개선 효과

### 성능
- Status API 호출 횟수 감소 (2회 → 1회)
- 불필요한 네트워크 요청 제거

### 안정성
- 트랜잭션 전파 순서 개선으로 DB 커넥션 고갈 방지
- 락 누수 방지로 리소스 관리 개선
- Null 안전성 향상으로 NPE 방지
- 데이터 무결성 보장으로 잘못된 포인트 적립 방지

### 아키텍처
- 락 획득 → 트랜잭션 시작 순서 보장
- 상위 트랜잭션과 독립적인 실행으로 데드락 위험 감소

## 관련 이슈

- 코드 리뷰 피드백 반영
- API 중복 호출 제거
- 트랜잭션 전파 순서 개선
- Null 안전성 및 데이터 무결성 개선
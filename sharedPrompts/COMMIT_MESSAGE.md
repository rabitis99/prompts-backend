fix(payment): 부분 환불 동시성 문제 해결 및 환율 스케줄러 문서 일관성 개선

## 주요 변경사항

### 부분 환불 멱등성 키 생성 동시성 문제 해결
- executeRefund()에 REQUIRES_NEW 기반 키 저장 추가
  - executePayment()와 동일한 패턴으로 외부 API 호출 전에 멱등성 키를 별도 트랜잭션으로 먼저 저장
  - 동시 요청 시 동일한 refundedAmount를 읽어 같은 키가 생성되는 경쟁 조건 방지
- 환불 처리에 분산 락 적용
  - refundPayment()와 refundPaymentForAdmin()에 DistributedLockService 적용
  - 락 키: "payment:{paymentId}:refund" 형식으로 동일 Payment에 대한 동시 환불 요청 직렬화
  - 이중 보호: REQUIRES_NEW 트랜잭션 + 분산 락으로 동시성 문제 완전 해결

### 환율 스케줄러 실행 주기 문서 일관성 개선
- application.yml 주석 명확화
  - 기본값: 매 시간 정각 (1시간 간격) 명시
  - 예시 정리: 다른 옵션(매일 새벽 2시, 30분 간격) 명시
- 환경 변수 문서 일관성 수정
  - TODO_PAYMENT_MODULE.md: 1시간 간격으로 변경
  - PAYMENT_MODULE_SETUP.md: 3곳 수정 (환경 변수 예시, 설명 텍스트)
  - ENV_VARIABLES.md: 기본값을 1시간 간격으로 명시
  - payment_analysis.md: "현재는 매일 새벽 2시" 문구 제거

## 수정된 파일

### 서비스
- `domain/payment/service/execution/PaymentExecutionService.java`
  - executeRefund()에 saveIdempotencyKeyInNewTransaction() 호출 추가
  - 동시성 보호 주석 추가
- `domain/payment/service/core/PaymentServiceImpl.java`
  - DistributedLockService 의존성 추가
  - refundPayment()에 분산 락 적용
  - refundPaymentForAdmin()에 분산 락 적용

### 설정
- `resources/application.yml` - 환율 스케줄러 주석 및 예시 수정

### 문서
- `TODO_PAYMENT_MODULE.md` - 환경 변수 예시 수정
- `PAYMENT_MODULE_SETUP.md` - 환경 변수 예시 및 설명 수정
- `ENV_VARIABLES.md` - 기본값 명시 수정
- `payment_analysis.md` - 불일치 내용 수정

## 해결된 문제

### 부분 환불 동시성 문제
- 문제: 동시 부분 환불 요청 시 동일한 멱등성 키 생성으로 결제사 중복 거부
- 해결: REQUIRES_NEW 트랜잭션 + 분산 락으로 이중 보호
- 효과: 동시 요청 시에도 각 요청이 고유한 멱등성 키로 처리됨

### 환율 스케줄러 문서 불일치
- 문제: 코드 기본값(1시간 간격)과 문서/환경 변수 예시(매일 새벽 2시) 불일치
- 해결: 모든 문서를 1시간 간격으로 통일
- 효과: 문서와 코드의 일관성 확보, 설정 오류 방지

## 관련 이슈

- 부분 환불 멱등성 키 생성의 동시성 문제
- 환율 스케줄러 실행 주기 불일치

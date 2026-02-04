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

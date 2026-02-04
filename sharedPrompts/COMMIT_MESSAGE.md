fix(payment): 결제 시스템 개선 - 환율 정책, Webhook 처리, 카카오페이 취소 로직

## 주요 변경사항

### 환율 관리 정책 개선 (#13, #23)
- 스케줄러 기반 환율 갱신으로 외부 API 의존도 감소
  - ExchangeRateScheduler 주기를 1시간 간격으로 변경 (기본값)
  - application.yml에 schedule 설정 추가
- ExchangeRateService에 캐싱 추가 (@Cacheable)
- Payment 엔티티에 환율 저장 필드 추가 (exchangeRate, originalCurrency)
- 장점: 외부 API 장애 시에도 최근 갱신된 환율로 결제 진행 가능

### Webhook 처리 개선 (#33)
- Webhook 파싱 실패 시 400 Bad Request 반환
- 결제사에 재시도 중단 요청하여 불필요한 재시도 방지
- 파싱 실패 원인 로깅 개선 (payload 크기 포함)

### 카카오페이 취소 로직 개선 (#31)
- Payment 엔티티에 원본 금액/면세 금액 저장 필드 추가
- CancelResult에 originalAmount, taxFreeAmount 필드 추가
- 취소 시 원본 금액 정보를 Payment에 저장하여 이후 환불 시 재사용

### 페이팔 취소 처리 문서화 (#32)
- PayPal 주문 생명주기에 따른 취소 처리 로직 문서화
- CREATED/APPROVED, Authorization, Capture 상태별 처리 방식 명시

## 수정된 파일

### 도메인 모델
- `domain/payment/Payment.java` - 환율 저장 필드, 원본 금액/면세 금액 필드 추가
- `domain/payment/model/CancelResult.java` - originalAmount, taxFreeAmount 필드 추가

### 서비스
- `domain/payment/service/exchange/ExchangeRateServiceImpl.java` - 캐싱 추가
- `domain/payment/service/execution/PaymentExecutionService.java` - 카카오페이 취소 시 원본 금액 저장
- `domain/payment/facade/PaymentWebhookFacade.java` - Webhook 파싱 실패 시 400 반환

### Provider
- `domain/payment/provider/impl/KakaoPayPaymentProvider.java` - 취소 시 원본 금액 조회 및 반환
- `domain/payment/provider/impl/PayPalPaymentProvider.java` - 취소 처리 로직 문서화, 린터 경고 수정

### 스케줄러
- `scheduler/payment/ExchangeRateScheduler.java` - 주기 1시간 간격으로 변경, 문서화 개선

### 설정
- `resources/application.yml` - 환율 스케줄러 설정 추가

## 완료 현황

- 완료: 29개 이슈 (이전: 25개)
- 미완료: 8개 이슈 (#3 부분, #9, #10, #12, #14, #19, #25, #36)

## 관련 이슈

- #13: 환율 변환 시점과 환율 변동 리스크
- #23: 환율 서비스의 캐싱 및 실패 처리 전략 부재
- #31: 카카오페이 취소 시 금액 정보 조회 방식의 문제
- #32: 페이팔 주문 상태별 취소 처리의 복잡성
- #33: Webhook 파싱 실패 시 RuntimeException throw


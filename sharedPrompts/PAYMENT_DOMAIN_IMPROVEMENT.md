# Payment Domain 구조 개선 제안서

## 📋 목차
1. [현재 구조 분석](#현재-구조-분석)
2. [발견된 문제점](#발견된-문제점)
3. [개선 제안](#개선-제안)
4. [제안된 새로운 구조](#제안된-새로운-구조)
5. [마이그레이션 전략](#마이그레이션-전략)
6. [구체적 개선 예시](#구체적-개선-예시)

---

## 현재 구조 분석

### 실제 디렉토리 구조 (2024 기준)

```
payment/
├── application/                        # ✅ Application 레이어 (부분적 구현됨)
│   ├── command/
│   │   ├── PaymentCommandService.java
│   │   ├── PaymentCommandServiceImpl.java
│   │   ├── CancelPaymentCommand.java
│   │   └── RefundPaymentCommand.java
│   ├── query/
│   │   ├── PaymentQueryService.java
│   │   ├── PaymentQueryServiceImpl.java
│   │   └── PaymentHistoryQuery.java
│   └── facade/
│       ├── PaymentFacade.java
│       ├── PaymentWebhookFacade.java
│       └── PaymentRetryFacade.java
│
├── domain/                            # ✅ Domain 레이어 (부분적 구현됨)
│   ├── entity/
│   │   ├── Payment.java
│   │   ├── Cashback.java
│   │   ├── Point.java
│   │   ├── ExchangeRate.java
│   │   └── UserTierHistory.java
│   ├── enums/
│   │   ├── PaymentMethod.java
│   │   ├── PaymentStatus.java
│   │   ├── PaymentUserType.java
│   │   ├── PointType.java
│   │   └── UserTier.java
│   ├── exception/
│   │   ├── PaymentDomainException.java
│   │   └── PaymentMethodException.java
│   ├── service/                       # ✅ 도메인 서비스 (일부 구현됨)
│   │   ├── PaymentDomainService.java
│   │   ├── PaymentAmountCalculator.java
│   │   └── PaymentValidator.java
│   └── valueobject/
│       ├── Currency.java
│       ├── ExchangeRate.java
│       └── PaymentAmount.java
│
├── infrastructure/                    # ✅ Infrastructure 레이어 (부분적 구현됨)
│   ├── persistence/
│   │   ├── adapter/
│   │   │   ├── PaymentJpaAdapter.java
│   │   │   ├── CashbackJpaAdapter.java
│   │   │   ├── PointJpaAdapter.java
│   │   │   ├── ExchangeRateJpaAdapter.java
│   │   │   └── UserTierHistoryJpaAdapter.java
│   │   └── repository/
│   │       ├── payment/
│   │       ├── cashback/
│   │       ├── point/
│   │       ├── exchange/
│   │       └── userTier/
│   └── external/
│       ├── exchange/
│       │   ├── ExchangeRateService.java
│       │   └── ExchangeRateServiceImpl.java
│       └── provider/
│           ├── PaymentProvider.java
│           ├── PaymentProviderFactory.java
│           ├── impl/
│           │   ├── KakaoPayPaymentProvider.java
│           │   ├── PayPalPaymentProvider.java
│           │   └── TossPaymentProvider.java
│           ├── kakao/                 # 카카오페이 (26개 파일)
│           │   ├── client/
│           │   │   ├── KakaoApproveApiClient.java
│           │   │   ├── KakaoCancelApiClient.java
│           │   │   ├── KakaoReadyApiClient.java
│           │   │   └── KakaoStatusApiClient.java
│           │   ├── dto/
│           │   │   ├── KakaoApproveResponse.java
│           │   │   ├── KakaoCancelResponse.java
│           │   │   ├── KakaoReadyResponse.java
│           │   │   ├── KakaoRefundResponse.java
│           │   │   └── KakaoStatusResponse.java
│           │   ├── mapper/
│           │   │   └── KakaoPayStatusMapper.java
│           │   ├── policy/
│           │   │   ├── KakaoPayAmountPolicy.java
│           │   │   └── KakaoPayRefundPolicy.java
│           │   ├── util/
│           │   │   ├── KakaoApproveErrorHandler.java
│           │   │   ├── KakaoApproveResponseParser.java
│           │   │   ├── KakaoCancelErrorHandler.java
│           │   │   ├── KakaoCancelResponseParser.java
│           │   │   ├── KakaoPayHeadersProvider.java
│           │   │   ├── KakaoPayJsonConverter.java
│           │   │   ├── KakaoPayResponseParser.java
│           │   │   ├── KakaoReadyErrorHandler.java
│           │   │   ├── KakaoReadyResponseParser.java
│           │   │   ├── KakaoStatusErrorHandler.java
│           │   │   └── KakaoStatusResponseParser.java
│           │   └── webhook/
│           │       ├── KakaoPayWebhookHandler.java
│           │       ├── KakaoPayWebhookParser.java
│           │       └── KakaoPayWebhookVerifier.java
│           ├── paypal/                # 페이팔 (20개 파일)
│           │   ├── client/
│           │   ├── dto/
│           │   ├── mapper/
│           │   ├── policy/
│           │   ├── util/
│           │   └── webhook/
│           ├── toss/                  # 토스페이먼츠 (23개 파일)
│           │   ├── client/
│           │   ├── dto/
│           │   ├── mapper/
│           │   ├── policy/
│           │   ├── util/
│           │   ├── exception/
│           │   └── webhook/
│           └── webhook/
│               ├── PaymentWebhookHandler.java
│               ├── PaymentWebhookHandlerFactory.java
│               └── WebhookEvent.java
│
├── service/                           # ⚠️ Service 레이어 (리팩토링 필요)
│   ├── integration/                   # ⚠️ 통합 서비스 (책임 과다)
│   │   ├── PaymentAmountFacade.java   # ⚠️ Facade가 아님, 계산 로직
│   │   ├── AmountProcessingResult.java
│   │   ├── execution/
│   │   │   └── PaymentExecutionService.java
│   │   ├── postprocess/
│   │   │   ├── PaymentPostProcessService.java
│   │   │   └── policy/
│   │   │       ├── CashbackAccrualPolicy.java
│   │   │       ├── PointAccrualPolicy.java
│   │   │       └── RewardBasisPolicy.java
│   │   ├── compensation/
│   │   │   ├── CompensationQueue.java
│   │   │   ├── CompensationTask.java
│   │   │   ├── CompensationTaskType.java
│   │   │   └── LoggingCompensationQueue.java
│   │   ├── idempotency/
│   │   │   ├── IdempotencyService.java
│   │   │   └── IdempotencyServiceImpl.java
│   │   ├── lock/
│   │   │   ├── DistributedLockService.java
│   │   │   └── ShedLockDistributedLockService.java
│   │   ├── retry/
│   │   │   ├── ImmediateRetryStrategy.java
│   │   │   └── RetryStrategy.java
│   │   ├── rule/
│   │   │   ├── PaymentLimitRule.java
│   │   │   └── PaymentLimitRuleImpl.java
│   │   ├── sync/
│   │   │   └── PaymentStatusSyncService.java
│   │   └── transaction/
│   │       └── PaymentTransactionBoundary.java  # ⚠️ Infrastructure 책임
│   ├── cashback/                      # ⚠️ 캐시백 서비스 (도메인 분리 고려)
│   │   ├── CashbackService.java
│   │   ├── CashbackServiceImpl.java
│   │   ├── amount/
│   │   │   └── CashbackAmountService.java
│   │   ├── execution/
│   │   │   └── CashbackExecutionService.java
│   │   ├── facade/
│   │   │   └── CashbackFacade.java
│   │   ├── lock/
│   │   │   └── CashbackLockService.java
│   │   └── validation/
│   │       └── CashbackValidationService.java
│   ├── point/                         # ⚠️ 포인트 서비스 (도메인 분리 고려)
│   │   ├── PointService.java
│   │   └── PointServiceImpl.java
│   ├── exchange/                      # ⚠️ 중복 (infrastructure/external/exchange와 중복)
│   │   └── ExchangeRateServiceImpl.java
│   ├── validation/                    # ⚠️ 검증 서비스 (validator/와 중복)
│   │   └── PaymentValidationService.java
│   ├── webhook/
│   │   └── PaymentWebhookTransactionService.java
│   ├── payment/
│   │   └── PaymentRetryService.java
│   └── user/
│       └── tier/
│           ├── UserTierService.java
│           └── UserTierServiceImpl.java
│
├── validator/                         # ⚠️ 검증 컴포넌트 (service/validation과 중복)
│   └── PaymentValidator.java
│
├── messaging/                         # ⚠️ 메시징 (일부는 infrastructure로 이동 필요)
│   ├── event/
│   │   ├── PaymentEvent.java
│   │   └── PaymentEventPublisher.java
│   ├── notification/                  # ⚠️ 알림 (별도 도메인으로 분리 권장)
│   │   ├── PaymentNotificationService.java
│   │   ├── PushNotificationService.java
│   │   ├── FcmTokenService.java
│   │   └── credentials/
│   │       ├── FcmCredentialsProvider.java
│   │       ├── DefaultFcmCredentialsProvider.java
│   │       └── FcmCredentialsResolver.java
│   └── webhook/                       # ⚠️ 빈 디렉토리
│
├── logging/                           # ⚠️ 로깅 (infrastructure/monitoring으로 이동)
│   └── PaymentLoggingService.java
│
├── metrics/                           # ⚠️ 메트릭 (infrastructure/monitoring으로 이동)
│   └── PaymentMetrics.java
│
├── statistics/                        # ⚠️ 통계 (infrastructure/monitoring으로 이동)
│   └── PaymentFailureStatistics.java
│
├── webhook/                           # ⚠️ 웹훅 (infrastructure/messaging/webhook으로 통합)
│   └── WebhookIdempotencyService.java
│
├── model/                             # ⚠️ 모델 (application/dto로 이동)
│   ├── PaymentResult.java
│   ├── CancelResult.java
│   └── RefundResult.java
│
├── config/                            # ✅ 설정
│   ├── PaymentClientConfig.java
│   ├── PaymentMetricsConfig.java
│   └── properties/
│       ├── ExchangeRateProperties.java
│       ├── FcmProperties.java
│       ├── KakaoPayProperties.java
│       ├── PaymentExpirationProperties.java
│       ├── PaypalProperties.java
│       ├── RetryProperties.java
│       ├── RewardProperties.java
│       ├── TossPayProperties.java
│       └── WebhookProperties.java
│
└── PaymentMonitoringService.java      # ⚠️ 루트에 위치 (infrastructure/monitoring으로 이동)
```

### 현재 구조의 장점
- ✅ **레이어 구조 시작**: `domain/`, `application/`, `infrastructure/` 구조가 이미 부분적으로 구현됨
- ✅ **Provider 패턴**: 결제사별 구현이 잘 분리됨 (KakaoPay, PayPal, Toss)
- ✅ **CQRS 패턴**: `application/command/`와 `application/query/`로 분리
- ✅ **Value Object 사용**: `PaymentAmount`, `Currency`, `ExchangeRate` 등 타입 안정성 확보
- ✅ **도메인 서비스**: `PaymentDomainService`, `PaymentAmountCalculator` 등 순수 비즈니스 로직 분리
- ✅ **Adapter 패턴**: `PaymentJpaAdapter` 등 영속성 어댑터 사용

---

## 발견된 문제점

### 1. **레이어 구조의 불완전성**
- ✅ `domain/`, `application/`, `infrastructure/` 구조는 있으나, `service/` 디렉토리에 많은 로직이 남아있음
- ⚠️ `service/integration/`에 너무 많은 책임이 집중됨:
  - 금액 처리 (`PaymentAmountFacade`)
  - 결제 실행 (`PaymentExecutionService`)
  - 후처리 (`PaymentPostProcessService`)
  - 보상 (`CompensationQueue`)
  - 트랜잭션 관리 (`PaymentTransactionBoundary`) - Infrastructure 책임
  - 분산 락 (`DistributedLockService`) - Infrastructure 책임
  - 재시도 (`RetryStrategy`) - Infrastructure 책임
  - 동기화 (`PaymentStatusSyncService`)
  - 규칙 (`PaymentLimitRule`)
- ⚠️ `service/cashback/`, `service/point/` 등이 별도 도메인일 수 있음

### 2. **책임 분산 및 중복 문제**

#### 검증 로직 중복
- `validator/PaymentValidator.java`: 금액/주문번호/통화 검증 (순수 검증 로직)
- `service/validation/PaymentValidationService.java`: 일일 제한, 소유권, 상태 검증 (Infrastructure 의존)
- `domain/service/PaymentValidator.java`: 도메인 검증 로직 (이미 존재)
- **문제**: 3곳에 검증 로직이 분산되어 있음

#### 웹훅 처리 분산
- `infrastructure/external/provider/*/webhook/`: Provider별 웹훅 핸들러
- `infrastructure/external/provider/webhook/`: 공통 웹훅 처리
- `service/webhook/PaymentWebhookTransactionService.java`: 웹훅 트랜잭션 처리
- `webhook/WebhookIdempotencyService.java`: 웹훅 멱등성 처리
- `messaging/webhook/`: 빈 디렉토리
- **문제**: 웹훅 관련 로직이 5곳에 분산

#### 로깅/모니터링 분산
- `logging/PaymentLoggingService.java`: 로깅 서비스
- `service/integration/compensation/LoggingCompensationQueue.java`: 로깅 보상 큐
- `metrics/PaymentMetrics.java`: 메트릭 수집
- `statistics/PaymentFailureStatistics.java`: 통계 수집
- `PaymentMonitoringService.java`: 모니터링 서비스 (루트에 위치)
- **문제**: 모니터링 관련 로직이 5곳에 분산

#### 환율 서비스 중복
- `infrastructure/external/exchange/ExchangeRateService.java`: 인터페이스
- `infrastructure/external/exchange/ExchangeRateServiceImpl.java`: 구현체
- `service/exchange/ExchangeRateServiceImpl.java`: 중복 구현체
- **문제**: 동일한 서비스가 2곳에 구현됨

### 3. **네이밍 일관성 부족**
- `PaymentAmountFacade`: Facade 패턴이 아님, 실제로는 금액 계산 로직 (`domain/service/PaymentAmountCalculator`와 중복 가능)
- `PaymentValidator` (3곳): 역할 구분이 불명확
- `PaymentMonitoringService` vs `PaymentNotificationService`: 차이가 모호
- `PaymentTransactionBoundary`: Infrastructure 책임인데 `service/integration/`에 위치

### 4. **의존성 방향 문제**
- `service/integration/transaction/PaymentTransactionBoundary`: Infrastructure 관심사인데 Service 레이어에 포함
- `service/integration/lock/DistributedLockService`: Infrastructure 관심사인데 Service 레이어에 포함
- `service/integration/retry/RetryStrategy`: Infrastructure 관심사인데 Service 레이어에 포함
- Provider 구현체들이 각각 `client`, `dto`, `mapper`, `policy`, `util`, `webhook`을 중복으로 가짐 (공통화 필요)

### 5. **확장성 문제**
- 새로운 결제사 추가 시 `provider/*/` 하위에 전체 구조(`client`, `dto`, `mapper`, `policy`, `util`, `webhook`)를 복제해야 함
- 각 provider의 구조가 동일하지만 공통화되지 않음
- Provider별로 동일한 패턴의 클래스들이 반복됨:
  - `*ErrorHandler`, `*ResponseParser`, `*HeadersProvider`, `*JsonConverter` 등

### 6. **도메인 경계 문제**
- `service/cashback/`: 캐시백은 별도 도메인일 수 있음
- `service/point/`: 포인트는 별도 도메인일 수 있음
- `messaging/notification/`: 알림은 별도 도메인일 수 있음
- Payment 도메인에서 이들을 직접 의존하는 것은 도메인 경계를 넘어섬

### 7. **테스트 어려움**
- `service/integration`에 너무 많은 의존성이 집중되어 단위 테스트가 어려움
- Provider별 구조가 복잡하여 Mock 생성이 번거로움
- Infrastructure 의존성이 Domain/Application 레이어에 섞여있어 테스트 격리가 어려움

---

## 개선 제안

### 원칙
1. **계층화된 아키텍처**: Presentation → Application → Domain → Infrastructure
2. **단일 책임 원칙**: 각 클래스/패키지는 하나의 책임만 가짐
3. **의존성 역전**: Domain이 Infrastructure에 의존하지 않도록
4. **공통화**: Provider별 공통 패턴 추출
5. **명확한 네이밍**: 역할이 이름에서 명확히 드러나도록
6. **도메인 경계 명확화**: Payment 도메인과 다른 도메인(Cashback, Point, Notification) 분리

---

## 제안된 새로운 구조

```
payment/
├── domain/                            # 도메인 핵심 (비즈니스 규칙)
│   ├── entity/
│   │   ├── Payment.java
│   │   ├── Cashback.java              # ⚠️ 별도 도메인 고려
│   │   ├── Point.java                 # ⚠️ 별도 도메인 고려
│   │   ├── ExchangeRate.java
│   │   └── UserTierHistory.java
│   ├── valueobject/
│   │   ├── PaymentAmount.java
│   │   ├── Currency.java
│   │   └── ExchangeRate.java
│   ├── enums/
│   │   ├── PaymentMethod.java
│   │   ├── PaymentStatus.java
│   │   ├── PaymentUserType.java
│   │   ├── PointType.java
│   │   └── UserTier.java
│   ├── exception/
│   │   ├── PaymentDomainException.java
│   │   └── PaymentMethodException.java
│   └── service/                       # 도메인 서비스 (순수 비즈니스 로직)
│       ├── PaymentDomainService.java
│       ├── PaymentAmountCalculator.java
│       └── PaymentValidator.java      # ✅ 통합된 검증 로직
│
├── application/                       # Application 레이어
│   ├── command/
│   │   ├── PaymentCommandService.java
│   │   ├── PaymentCommandServiceImpl.java
│   │   ├── CancelPaymentCommand.java
│   │   └── RefundPaymentCommand.java
│   ├── query/
│   │   ├── PaymentQueryService.java
│   │   ├── PaymentQueryServiceImpl.java
│   │   └── PaymentHistoryQuery.java
│   ├── dto/                           # ✅ model/에서 이동
│   │   ├── request/
│   │   └── response/
│   │       ├── PaymentResult.java
│   │       ├── CancelResult.java
│   │       └── RefundResult.java
│   └── facade/
│       ├── PaymentFacade.java
│       ├── PaymentWebhookFacade.java
│       └── PaymentRetryFacade.java
│
├── infrastructure/                    # Infrastructure 레이어
│   ├── persistence/                   # 영속성
│   │   ├── adapter/
│   │   │   ├── PaymentJpaAdapter.java
│   │   │   ├── CashbackJpaAdapter.java
│   │   │   ├── PointJpaAdapter.java
│   │   │   ├── ExchangeRateJpaAdapter.java
│   │   │   └── UserTierHistoryJpaAdapter.java
│   │   └── repository/
│   │       ├── payment/
│   │       ├── cashback/
│   │       ├── point/
│   │       ├── exchange/
│   │       └── userTier/
│   │
│   ├── external/                      # 외부 시스템 통합
│   │   ├── exchange/
│   │   │   ├── ExchangeRateService.java
│   │   │   └── ExchangeRateServiceImpl.java
│   │   └── provider/                  # 결제사 통합
│   │       ├── PaymentProvider.java
│   │       ├── PaymentProviderFactory.java
│   │       ├── common/                # ✅ 공통 구조 추출
│   │       │   ├── AbstractPaymentProvider.java
│   │       │   ├── PaymentProviderClient.java
│   │       │   ├── PaymentProviderMapper.java
│   │       │   ├── PaymentProviderPolicy.java
│   │       │   ├── PaymentProviderErrorHandler.java
│   │       │   ├── PaymentProviderResponseParser.java
│   │       │   ├── PaymentProviderHeadersProvider.java
│   │       │   ├── PaymentProviderJsonConverter.java
│   │       │   └── PaymentProviderWebhookHandler.java
│   │       ├── impl/
│   │       │   ├── KakaoPayPaymentProvider.java
│   │       │   ├── PayPalPaymentProvider.java
│   │       │   └── TossPaymentProvider.java
│   │       ├── kakao/                 # Provider별 구현 (공통 구조 활용)
│   │       │   ├── client/
│   │       │   │   ├── KakaoApproveApiClient.java
│   │       │   │   ├── KakaoCancelApiClient.java
│   │       │   │   ├── KakaoReadyApiClient.java
│   │       │   │   └── KakaoStatusApiClient.java
│   │       │   ├── dto/
│   │       │   │   ├── KakaoApproveResponse.java
│   │       │   │   ├── KakaoCancelResponse.java
│   │       │   │   ├── KakaoReadyResponse.java
│   │       │   │   ├── KakaoRefundResponse.java
│   │       │   │   └── KakaoStatusResponse.java
│   │       │   ├── mapper/
│   │       │   │   └── KakaoPayStatusMapper.java
│   │       │   ├── policy/
│   │       │   │   ├── KakaoPayAmountPolicy.java
│   │       │   │   └── KakaoPayRefundPolicy.java
│   │       │   └── webhook/
│   │       │       ├── KakaoPayWebhookHandler.java
│   │       │       ├── KakaoPayWebhookParser.java
│   │       │       └── KakaoPayWebhookVerifier.java
│   │       ├── paypal/
│   │       │   └── ... (동일 구조)
│   │       └── toss/
│   │           └── ... (동일 구조)
│   │
│   ├── messaging/                     # 메시징
│   │   ├── event/
│   │   │   ├── PaymentEvent.java
│   │   │   ├── PaymentEventPublisher.java
│   │   │   └── PaymentEventListener.java
│   │   └── webhook/                   # ✅ 웹훅 통합
│   │       ├── WebhookHandler.java
│   │       ├── WebhookVerifier.java
│   │       ├── WebhookIdempotencyService.java
│   │       └── PaymentWebhookTransactionService.java
│   │
│   ├── transaction/                   # ✅ 트랜잭션 관리 (service/integration에서 이동)
│   │   ├── PaymentTransactionManager.java
│   │   └── DistributedLockManager.java
│   │
│   ├── retry/                         # ✅ 재시도 전략 (service/integration에서 이동)
│   │   ├── RetryStrategy.java
│   │   ├── ImmediateRetryStrategy.java
│   │   └── RetryPolicy.java
│   │
│   ├── idempotency/                   # ✅ 멱등성 처리 (service/integration에서 이동)
│   │   ├── IdempotencyService.java
│   │   └── IdempotencyServiceImpl.java
│   │
│   └── monitoring/                    # ✅ 모니터링 통합
│       ├── PaymentMetrics.java
│       ├── PaymentLoggingService.java
│       ├── PaymentStatistics.java
│       ├── PaymentMonitoringService.java
│       └── compensation/
│           └── LoggingCompensationQueue.java
│
└── config/                            # 설정
    ├── PaymentClientConfig.java
    ├── PaymentMetricsConfig.java
    └── properties/
        ├── ExchangeRateProperties.java
        ├── FcmProperties.java
        ├── KakaoPayProperties.java
        ├── PaymentExpirationProperties.java
        ├── PaypalProperties.java
        ├── RetryProperties.java
        ├── RewardProperties.java
        ├── TossPayProperties.java
        └── WebhookProperties.java
```

### 주요 변경사항 상세

#### 1. **레이어 명확화**

**Domain 레이어**
- 순수 비즈니스 로직만 포함 (Infrastructure 의존 없음)
- `domain/service/PaymentValidator.java`: 검증 로직 통합
  - `validator/PaymentValidator.java`의 순수 검증 로직 통합
  - `service/validation/PaymentValidationService.java`의 도메인 검증 로직 통합
  - Infrastructure 의존성은 Application 레이어로 이동

**Application 레이어**
- 유스케이스 구현, CQRS 패턴 적용
- `application/dto/`: `model/`에서 이동
- `PaymentCommandServiceImpl`에서:
  - `PaymentAmountFacade` → `PaymentAmountCalculator` (Domain) + Application 로직으로 분리
  - `PaymentValidationService`의 Infrastructure 의존 부분은 Application에서 처리

**Infrastructure 레이어**
- 외부 시스템 통합, 영속성, 기술적 관심사
- `infrastructure/transaction/`: `service/integration/transaction/`에서 이동
- `infrastructure/retry/`: `service/integration/retry/`에서 이동
- `infrastructure/idempotency/`: `service/integration/idempotency/`에서 이동
- `infrastructure/monitoring/`: `logging/`, `metrics/`, `statistics/` 통합

#### 2. **Provider 공통화**

**현재 문제점:**
- 각 Provider(kakao, paypal, toss)가 동일한 구조를 가짐:
  - `client/`, `dto/`, `mapper/`, `policy/`, `util/`, `webhook/`
- 각 Provider의 `util/`에 동일한 패턴의 클래스들이 반복:
  - `*ErrorHandler`, `*ResponseParser`, `*HeadersProvider`, `*JsonConverter`

**개선 방안:**
```java
// infrastructure/external/provider/common/AbstractPaymentProvider.java
public abstract class AbstractPaymentProvider implements PaymentProvider {
    protected PaymentProviderClient client;
    protected PaymentProviderMapper mapper;
    protected PaymentProviderPolicy policy;
    protected PaymentProviderErrorHandler errorHandler;
    protected PaymentProviderResponseParser responseParser;
    
    // 공통 로직 구현
    protected <T> T executeWithRetry(Supplier<T> operation, RetryPolicy retryPolicy) {
        // 재시도 로직
    }
    
    protected void handleError(Exception e, String operation) {
        errorHandler.handle(e, operation);
    }
}

// infrastructure/external/provider/common/PaymentProviderClient.java
public interface PaymentProviderClient<TRequest, TResponse> {
    TResponse execute(TRequest request);
    TResponse executeWithRetry(TRequest request, RetryPolicy policy);
}

// infrastructure/external/provider/kakao/KakaoPayProvider.java
public class KakaoPayProvider extends AbstractPaymentProvider {
    private final KakaoApproveApiClient approveClient;
    private final KakaoCancelApiClient cancelClient;
    // Provider별 특화 로직만 구현
}
```

#### 3. **검증 로직 통합**

**Before:**
```
validator/PaymentValidator.java              # 순수 검증 로직
service/validation/PaymentValidationService.java  # Infrastructure 의존
domain/service/PaymentValidator.java        # 도메인 검증 (이미 존재)
```

**After:**
```
domain/service/PaymentValidator.java        # ✅ 통합된 검증 로직
├── validateAmount()                        # 금액 검증
├── validateOrderId()                       # 주문 ID 검증
├── validateCurrency()                      # 통화 검증
├── validatePaymentResult()                 # PaymentResult 검증
├── validatePaymentKey()                    # paymentKey 검증
└── validateKakaoPayPgToken()              # 카카오페이 토큰 검증

application/command/PaymentCommandServiceImpl.java
└── validateDailyLimit()                    # 일일 제한 검증 (Infrastructure 의존)
└── validatePaymentOwnership()              # 소유권 검증 (Infrastructure 의존)
└── validateCancelableStatus()              # 취소 가능 상태 검증 (Domain 위임)
└── validateRefundableStatus()             # 환불 가능 상태 검증 (Domain 위임)
```

#### 4. **웹훅 처리 통합**

**Before:**
```
infrastructure/external/provider/*/webhook/  # Provider별 웹훅
infrastructure/external/provider/webhook/   # 공통 웹훅
service/webhook/PaymentWebhookTransactionService.java
webhook/WebhookIdempotencyService.java
messaging/webhook/                           # 빈 디렉토리
```

**After:**
```
infrastructure/messaging/webhook/
├── WebhookHandler.java                     # 공통 웹훅 처리
├── WebhookVerifier.java                    # 서명 검증
├── WebhookIdempotencyService.java          # 멱등성 처리
├── PaymentWebhookTransactionService.java   # 트랜잭션 처리
└── provider/                               # Provider별 구현
    ├── KakaoPayWebhookHandler.java
    ├── PayPalWebhookHandler.java
    └── TossPayWebhookHandler.java
```

#### 5. **모니터링 통합**

**Before:**
```
logging/PaymentLoggingService.java
service/integration/compensation/LoggingCompensationQueue.java
metrics/PaymentMetrics.java
statistics/PaymentFailureStatistics.java
PaymentMonitoringService.java               # 루트에 위치
```

**After:**
```
infrastructure/monitoring/
├── PaymentMetrics.java
├── PaymentLoggingService.java
├── PaymentStatistics.java
├── PaymentMonitoringService.java
└── compensation/
    └── LoggingCompensationQueue.java
```

#### 6. **금액 처리 로직 개선**

**Before:**
```java
// service/integration/PaymentAmountFacade.java
@Component
public class PaymentAmountFacade {
    private final ExchangeRateService exchangeRateService;  // Infrastructure
    private final PointService pointService;                // Infrastructure
    private final PaymentAmountCalculator amountCalculator; // Domain
    
    public AmountProcessingResult processPaymentAmount(...) {
        // 환율 조회 (Infrastructure)
        // 포인트 사용 (Infrastructure)
        // 금액 계산 (Domain)
    }
}
```

**After:**
```java
// domain/service/PaymentAmountCalculator.java
public class PaymentAmountCalculator {
    // 순수 계산 로직만 (Infrastructure 의존 없음)
    public PaymentAmount calculateActualAmount(
        PaymentAmount originalAmount,
        PaymentAmount usePointAmount
    ) { ... }
    
    public PaymentAmount convertCurrency(
        PaymentAmount amount,
        ExchangeRate rate
    ) { ... }
}

// application/command/PaymentCommandServiceImpl.java
@Service
public class PaymentCommandServiceImpl {
    private final PaymentAmountCalculator calculator;        // Domain
    private final ExchangeRateService exchangeRateService;    // Infrastructure
    private final PointService pointService;                  // Infrastructure
    
    public PaymentResult processPayment(...) {
        // 1. 환율 조회 (Infrastructure)
        BigDecimal rate = exchangeRateService.getRate(...);
        
        // 2. 금액 계산 (Domain)
        PaymentAmount amount = calculator.calculateActualAmount(..., rate, ...);
        
        // 3. 포인트 차감 (Infrastructure)
        pointService.usePoints(...);
    }
}
```

#### 7. **도메인 경계 명확화**

**Cashback 도메인 분리 (권장)**
- `service/cashback/` → 별도 `cashback` 도메인으로 이동
- Payment 도메인에서는 이벤트를 통해서만 통신

**Point 도메인 분리 (권장)**
- `service/point/` → 별도 `point` 도메인으로 이동
- Payment 도메인에서는 이벤트를 통해서만 통신

**Notification 도메인 분리 (권장)**
- `messaging/notification/` → 별도 `notification` 도메인으로 이동
- Payment 도메인에서는 이벤트만 발행

---

## 마이그레이션 전략

### Phase 1: 구조 정리 (Low Risk) - 1-2주

#### 1.1 네이밍 통일 및 중복 제거
- [ ] `service/exchange/ExchangeRateServiceImpl.java` 삭제 (infrastructure에 이미 존재)
- [ ] `validator/PaymentValidator.java`와 `domain/service/PaymentValidator.java` 통합
- [ ] `service/validation/PaymentValidationService.java`의 도메인 검증 로직을 `domain/service/PaymentValidator.java`로 이동
- [ ] `PaymentAmountFacade` → `PaymentAmountCalculator`로 리팩토링 (Domain으로 이동)

#### 1.2 모니터링 통합
- [ ] `logging/`, `metrics/`, `statistics/` → `infrastructure/monitoring/`로 이동
- [ ] `PaymentMonitoringService.java` (루트) → `infrastructure/monitoring/`로 이동
- [ ] `service/integration/compensation/LoggingCompensationQueue.java` → `infrastructure/monitoring/compensation/`로 이동

#### 1.3 웹훅 통합
- [ ] `webhook/WebhookIdempotencyService.java` → `infrastructure/messaging/webhook/`로 이동
- [ ] `service/webhook/PaymentWebhookTransactionService.java` → `infrastructure/messaging/webhook/`로 이동
- [ ] `messaging/webhook/` 디렉토리 정리

#### 1.4 DTO 이동
- [ ] `model/` → `application/dto/`로 이동

**체크리스트:**
- [ ] 단위 테스트 작성/수정
- [ ] 통합 테스트 확인
- [ ] 컴파일 에러 없음 확인

### Phase 2: Infrastructure 분리 (Medium Risk) - 2-3주

#### 2.1 트랜잭션/락 관리 분리
- [ ] `service/integration/transaction/PaymentTransactionBoundary.java` → `infrastructure/transaction/PaymentTransactionManager.java`로 이동 및 리네임
- [ ] `service/integration/lock/DistributedLockService.java` → `infrastructure/transaction/DistributedLockManager.java`로 이동 및 리네임
- [ ] `PaymentCommandServiceImpl`에서 의존성 업데이트

#### 2.2 재시도/멱등성 분리
- [ ] `service/integration/retry/` → `infrastructure/retry/`로 이동
- [ ] `service/integration/idempotency/` → `infrastructure/idempotency/`로 이동
- [ ] 관련 의존성 업데이트

#### 2.3 금액 처리 로직 개선
- [ ] `PaymentAmountFacade`의 Infrastructure 의존성 제거
- [ ] `PaymentAmountCalculator`를 Domain으로 이동 (순수 계산 로직만)
- [ ] `PaymentCommandServiceImpl`에서 환율 조회, 포인트 사용 로직 처리

**체크리스트:**
- [ ] Domain 레이어에 Infrastructure 의존성 없음 확인
- [ ] 통합 테스트 작성
- [ ] 성능 테스트

### Phase 3: Provider 공통화 (Medium Risk) - 3-4주

#### 3.1 공통 인터페이스 및 베이스 클래스 생성
- [ ] `infrastructure/external/provider/common/AbstractPaymentProvider.java` 생성
- [ ] `infrastructure/external/provider/common/PaymentProviderClient.java` 인터페이스 생성
- [ ] `infrastructure/external/provider/common/PaymentProviderMapper.java` 인터페이스 생성
- [ ] `infrastructure/external/provider/common/PaymentProviderPolicy.java` 인터페이스 생성
- [ ] `infrastructure/external/provider/common/PaymentProviderErrorHandler.java` 인터페이스 생성
- [ ] `infrastructure/external/provider/common/PaymentProviderResponseParser.java` 인터페이스 생성

#### 3.2 Provider별 구현 리팩토링
- [ ] `KakaoPayPaymentProvider`가 `AbstractPaymentProvider` 상속하도록 변경
- [ ] `PayPalPaymentProvider`가 `AbstractPaymentProvider` 상속하도록 변경
- [ ] `TossPaymentProvider`가 `AbstractPaymentProvider` 상속하도록 변경
- [ ] Provider별 중복 코드 제거

#### 3.3 공통 유틸리티 추출
- [ ] `*ErrorHandler`, `*ResponseParser`, `*HeadersProvider`, `*JsonConverter` 공통화
- [ ] Provider별 특화 로직만 남기기

**체크리스트:**
- [ ] Provider별 테스트 작성
- [ ] 새로운 Provider 추가 가이드 작성
- [ ] 코드 리뷰

### Phase 4: 도메인 경계 명확화 (High Risk) - 4-6주

#### 4.1 Cashback 도메인 분리 (선택적)
- [ ] `service/cashback/` → 별도 `cashback` 도메인으로 이동
- [ ] Payment 도메인과의 통신을 이벤트 기반으로 변경
- [ ] 관련 테스트 업데이트

#### 4.2 Point 도메인 분리 (선택적)
- [ ] `service/point/` → 별도 `point` 도메인으로 이동
- [ ] Payment 도메인과의 통신을 이벤트 기반으로 변경
- [ ] 관련 테스트 업데이트

#### 4.3 Notification 도메인 분리 (선택적)
- [ ] `messaging/notification/` → 별도 `notification` 도메인으로 이동
- [ ] Payment 도메인에서는 이벤트만 발행
- [ ] 관련 테스트 업데이트

**체크리스트:**
- [ ] 도메인 간 통신 테스트
- [ ] 이벤트 발행/구독 테스트
- [ ] 통합 테스트

---

## 구체적 개선 예시

### 예시 1: PaymentAmountFacade 개선

**Before:**
```java
// service/integration/PaymentAmountFacade.java
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentAmountFacade {
    private final ExchangeRateService exchangeRateService;  // Infrastructure
    private final PointService pointService;                // Infrastructure
    private final PaymentAmountCalculator amountCalculator; // Domain
    
    public AmountProcessingResult processPaymentAmount(Long userId, PaymentRequestDto request) {
        // 환율 조회 (Infrastructure)
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(...);
        
        // 포인트 사용 (Infrastructure)
        pointService.usePoints(userId, ...);
        
        // 금액 계산 (Domain)
        PaymentAmount amount = amountCalculator.calculateActualAmount(...);
        
        return new AmountProcessingResult(...);
    }
}
```

**After:**
```java
// domain/service/PaymentAmountCalculator.java
public class PaymentAmountCalculator {
    // 순수 계산 로직만 (Infrastructure 의존 없음)
    public PaymentAmount calculateActualAmount(
        PaymentAmount originalAmount,
        PaymentAmount usePointAmount
    ) {
        if (usePointAmount.isGreaterThan(originalAmount)) {
            return PaymentAmount.zero(originalAmount.getCurrency());
        }
        return originalAmount.subtract(usePointAmount);
    }
    
    public PaymentAmount convertCurrency(
        PaymentAmount amount,
        ExchangeRate rate
    ) {
        return amount.multiply(rate.getRate());
    }
}

// application/command/PaymentCommandServiceImpl.java
@Service
public class PaymentCommandServiceImpl {
    private final PaymentAmountCalculator calculator;        // Domain
    private final ExchangeRateService exchangeRateService;    // Infrastructure
    private final PointService pointService;                  // Infrastructure
    
    public PaymentResult processPayment(Long userId, PaymentRequestDto request) {
        // 1. 환율 조회 (Infrastructure)
        BigDecimal rate = exchangeRateService.getExchangeRate(
            request.getCurrency(),
            Currency.KRW().getCode()
        );
        ExchangeRate exchangeRate = ExchangeRate.of(
            Currency.of(request.getCurrency()),
            Currency.KRW(),
            rate
        );
        
        // 2. 금액 변환 (Domain)
        PaymentAmount originalAmount = PaymentAmount.of(
            request.getAmount(),
            request.getCurrency()
        );
        PaymentAmount convertedAmount = calculator.convertCurrency(originalAmount, exchangeRate);
        
        // 3. 포인트 사용 (Infrastructure)
        PaymentAmount usePointAmount = PaymentAmount.krw(request.getUsePointAmount());
        if (usePointAmount.isPositive()) {
            pointService.usePoints(userId, usePointAmount.toBigDecimal(), "결제 시 포인트 사용");
        }
        
        // 4. 실제 결제 금액 계산 (Domain)
        PaymentAmount actualAmount = calculator.calculateActualAmount(convertedAmount, usePointAmount);
        
        // 5. 결제 실행
        // ...
    }
}
```

### 예시 2: Provider 공통화

**Before:**
```java
// infrastructure/external/provider/kakao/util/KakaoApproveErrorHandler.java
public class KakaoApproveErrorHandler {
    public void handle(Exception e, String operation) {
        // 카카오페이 특화 에러 처리
    }
}

// infrastructure/external/provider/paypal/util/PayPalCaptureErrorHandler.java
public class PayPalCaptureErrorHandler {
    public void handle(Exception e, String operation) {
        // 페이팔 특화 에러 처리
    }
}

// infrastructure/external/provider/toss/util/TossConfirmErrorHandler.java
public class TossConfirmErrorHandler {
    public void handle(Exception e, String operation) {
        // 토스 특화 에러 처리
    }
}
```

**After:**
```java
// infrastructure/external/provider/common/PaymentProviderErrorHandler.java
public interface PaymentProviderErrorHandler {
    void handle(Exception e, String operation);
    boolean isRetryable(Exception e);
    ErrorCode mapToErrorCode(Exception e);
}

// infrastructure/external/provider/common/AbstractPaymentProvider.java
public abstract class AbstractPaymentProvider implements PaymentProvider {
    protected PaymentProviderErrorHandler errorHandler;
    
    protected <T> T executeWithErrorHandling(Supplier<T> operation, String operationName) {
        try {
            return operation.get();
        } catch (Exception e) {
            errorHandler.handle(e, operationName);
            if (errorHandler.isRetryable(e)) {
                // 재시도 로직
            }
            throw new PaymentProviderException(errorHandler.mapToErrorCode(e), e);
        }
    }
}

// infrastructure/external/provider/kakao/util/KakaoPayErrorHandler.java
public class KakaoPayErrorHandler implements PaymentProviderErrorHandler {
    @Override
    public void handle(Exception e, String operation) {
        // 카카오페이 특화 에러 처리
    }
    
    @Override
    public boolean isRetryable(Exception e) {
        // 카카오페이 재시도 가능 여부 판단
    }
    
    @Override
    public ErrorCode mapToErrorCode(Exception e) {
        // 카카오페이 에러 코드 매핑
    }
}
```

### 예시 3: 검증 로직 통합

**Before:**
```java
// validator/PaymentValidator.java
@Component
public class PaymentValidator {
    public void validateAmount(BigDecimal expected, BigDecimal actual, String orderId) {
        // 금액 검증 로직
    }
    
    public void validateOrderId(String expected, String actual) {
        // 주문 ID 검증 로직
    }
}

// service/validation/PaymentValidationService.java
@Service
public class PaymentValidationService {
    private final PaymentJpaAdapter paymentJpaAdapter;  // Infrastructure 의존
    private final PaymentLimitRule paymentLimitRule;   // Infrastructure 의존
    
    public void validateDailyLimit(Long userId, UserTier tier) {
        // 일일 제한 검증 (Infrastructure 의존)
    }
    
    public void validatePaymentOwnership(Payment payment, Long userId) {
        // 소유권 검증
    }
}
```

**After:**
```java
// domain/service/PaymentValidator.java
public class PaymentValidator {
    // 순수 검증 로직만 (Infrastructure 의존 없음)
    public void validateAmount(BigDecimal expected, BigDecimal actual, String orderId) {
        if (expected == null || actual == null) {
            throw new PaymentDomainException(ErrorCode.PAYMENT_PROVIDER_ERROR, 
                "결제 금액이 유효하지 않습니다.");
        }
        if (expected.compareTo(actual) != 0) {
            throw new PaymentDomainException(ErrorCode.PAYMENT_AMOUNT_MISMATCH,
                String.format("결제 금액이 일치하지 않습니다. 예상: %s, 실제: %s", expected, actual));
        }
    }
    
    public void validateOrderId(String expected, String actual) {
        if (expected == null || actual == null) {
            throw new PaymentDomainException(ErrorCode.PAYMENT_PROVIDER_ERROR,
                "주문 ID가 유효하지 않습니다.");
        }
        if (!expected.equals(actual)) {
            throw new PaymentDomainException(ErrorCode.PAYMENT_ORDER_ID_MISMATCH,
                String.format("주문 ID가 일치하지 않습니다. 예상: %s, 실제: %s", expected, actual));
        }
    }
    
    public void validateCurrency(String expected, String actual) {
        // 통화 검증 로직
    }
    
    public void validatePaymentKey(String paymentKey, PaymentMethod paymentMethod) {
        // paymentKey 검증 로직
    }
}

// application/command/PaymentCommandServiceImpl.java
@Service
public class PaymentCommandServiceImpl {
    private final PaymentValidator validator;              // Domain
    private final PaymentJpaAdapter paymentJpaAdapter;     // Infrastructure
    private final PaymentLimitRule paymentLimitRule;       // Infrastructure
    
    public PaymentResult processPayment(Long userId, PaymentRequestDto request) {
        // Domain 검증 (Infrastructure 의존 없음)
        validator.validatePaymentKey(request.getPaymentKey(), request.getPaymentMethod());
        
        // Application 검증 (Infrastructure 의존)
        validateDailyLimit(userId, getUserTier(userId));
        
        // 나머지 로직
    }
    
    private void validateDailyLimit(Long userId, UserTier tier) {
        long todayPaymentCount = paymentJpaAdapter.countTodaySuccessfulPayments(userId, PaymentStatus.SUCCESS);
        paymentLimitRule.validateDailyLimit(userId, tier, todayPaymentCount);
    }
}
```

### 예시 4: 트랜잭션 관리 분리

**Before:**
```java
// service/integration/transaction/PaymentTransactionBoundary.java
@Component
public class PaymentTransactionBoundary {
    private final DistributedLockService distributedLockService;
    private final TransactionTemplate transactionTemplate;
    
    public <T> T executeWithLockAndTransaction(String lockKey, Supplier<T> task) {
        return distributedLockService.executeWithLock(lockKey, () ->
            transactionTemplate.execute(status -> task.get())
        );
    }
}
```

**After:**
```java
// infrastructure/transaction/PaymentTransactionManager.java
@Component
public class PaymentTransactionManager {
    private final DistributedLockManager distributedLockManager;
    private final TransactionTemplate transactionTemplate;
    
    public <T> T executeWithLockAndTransaction(String lockKey, Supplier<T> task) {
        return distributedLockManager.executeWithLock(lockKey, () ->
            transactionTemplate.execute(status -> task.get())
        );
    }
    
    public <T> T executeInTransaction(Supplier<T> task) {
        return transactionTemplate.execute(status -> task.get());
    }
}

// infrastructure/transaction/DistributedLockManager.java
public interface DistributedLockManager {
    <T> T executeWithLock(String lockKey, Supplier<T> task);
}

// infrastructure/transaction/ShedLockDistributedLockManager.java
@Component
public class ShedLockDistributedLockManager implements DistributedLockManager {
    // 구현
}
```

---

## 추가 권장사항

### 1. **테스트 전략**

**Domain 레이어**
- 단위 테스트 (Mock 없이)
- 순수 비즈니스 로직만 테스트
- 예: `PaymentAmountCalculatorTest`, `PaymentValidatorTest`

**Application 레이어**
- 통합 테스트 (Mock 사용)
- Domain 서비스와 Infrastructure 서비스 조합 테스트
- 예: `PaymentCommandServiceTest` (Mock: `ExchangeRateService`, `PointService`)

**Infrastructure 레이어**
- 실제 Provider와의 통합 테스트
- 예: `KakaoPayProviderIntegrationTest`

### 2. **문서화**

**각 레이어의 역할과 책임 명시**
- `domain/`: 순수 비즈니스 로직, Infrastructure 의존 없음
- `application/`: 유스케이스 구현, Domain과 Infrastructure 조합
- `infrastructure/`: 외부 시스템 통합, 기술적 관심사

**Provider 추가 가이드 작성**
- 공통 인터페이스 구현 방법
- Provider별 특화 로직 구현 방법
- 테스트 작성 방법

**의존성 방향 다이어그램 작성**
```
Presentation → Application → Domain ← Infrastructure
                                    ↑
                              (의존성 역전)
```

### 3. **코드 리뷰 체크리스트**

- [ ] Domain 레이어에 Infrastructure 의존성이 없는가?
- [ ] 각 클래스가 단일 책임을 가지는가?
- [ ] Provider별 중복 코드가 없는가?
- [ ] 테스트 커버리지가 충분한가? (Domain: 100%, Application: 80%, Infrastructure: 70%)
- [ ] 네이밍이 역할을 명확히 나타내는가?

### 4. **성능 고려사항**

- 트랜잭션 경계 최적화: 불필요한 트랜잭션 범위 축소
- 분산 락 사용 최소화: 락 범위와 시간 최소화
- 비동기 처리 도입: 이벤트 기반 후처리 (캐시백, 포인트 적립 등)
- 캐싱 전략: 환율 정보, 사용자 티어 정보 등

### 5. **도메인 경계 명확화 (장기 계획)**

**Cashback 도메인 분리**
- Payment 도메인에서 `PaymentCompletedEvent` 발행
- Cashback 도메인에서 이벤트 구독하여 캐시백 적립

**Point 도메인 분리**
- Payment 도메인에서 `PointUsedEvent`, `PointRefundedEvent` 발행
- Point 도메인에서 이벤트 구독하여 포인트 차감/복구

**Notification 도메인 분리**
- Payment 도메인에서 `PaymentCompletedEvent`, `PaymentFailedEvent` 발행
- Notification 도메인에서 이벤트 구독하여 알림 전송

---

## 결론

현재 Payment 도메인은 기능적으로는 잘 작동하지만, 구조적 개선이 필요합니다. 제안된 구조는:

1. **명확한 레이어 분리**: Domain, Application, Infrastructure
2. **확장성 향상**: Provider 공통화로 새 결제사 추가 용이
3. **테스트 용이성**: 의존성 분리로 단위 테스트 작성 용이
4. **유지보수성 향상**: 책임 명확화로 코드 이해 및 수정 용이
5. **도메인 경계 명확화**: Payment 도메인과 다른 도메인(Cashback, Point, Notification) 분리

단계적 마이그레이션을 통해 리스크를 최소화하면서 개선할 수 있습니다. 각 Phase는 독립적으로 진행 가능하며, 필요시 중단하고 롤백할 수 있습니다.

## Payment 도메인 리팩토링 & 크레딧/차감 시스템 설계 안내

이 문서는 **기존 Payment 도메인**을 기반으로,  
1) **헥사고날 + 클린 아키텍처 + SOLID** 에 맞게 구조를 재정렬하고  
2) 향후 **크레딧(Credit) 기반 차감 시스템**을 도입하기 쉽게 만드는 리팩토링 요구사항을 정리한 것이다.  

이 문서를 읽는 에이전트(Claude)는 아래 요구사항에 맞춰 리팩토링을 수행하라.

---

## 0. 목표

- **결제(Payment) 도메인**을 헥사고날 아키텍처(Port/Adapter) 구조로 재정렬한다.
- **클린 아키텍처 의존성 규칙**(안쪽으로만 의존)을 지킨다.
- **SOLID 5원칙**을 위반하는 결합(특히 Controller ↔ Service 직접 결합, 외부 SDK 의존, DTO 오염)을 제거한다.
- **크레딧(Credit) 기반 과금/차감 시스템**을 도입할 수 있도록 Payment 도메인을 정리한다.
- 기존 기능 동작은 최대한 유지하되, **구조 변경은 과감하게** 진행한다.

---

## 1. 타겟 패키지 구조

`src/main/java/${basePackage}/domain/payment` 패키지(실제 프로젝트 루트 패키지 기준)를 아래 4계층으로 정리한다.

```text
domain/payment
  ├─ application
  │   ├─ port
  │   │   ├─ in
  │   │   │   ├─ command
  │   │   │   └─ usecase
  │   │   └─ out
  │   │       ├─ repository
  │   │       ├─ paymentgateway
  │   │       └─ event
  │   ├─ service
  │   └─ dto
  │
  ├─ domain
  │   ├─ model
  │   ├─ policy
  │   ├─ event
  │   └─ exception
  │
  ├─ adapter
  │   ├─ in
  │   │   └─ web
  │   └─ out
  │       ├─ persistence
  │       ├─ paymentgateway
  │       └─ messaging
  │
  └─ config
```

### 필수 규칙

- **domain**
  - 순수 자바만 사용.
  - Spring, JPA, WebClient, RestTemplate 등 **프레임워크 의존 금지**.
  - 엔티티/VO, 도메인 정책, 도메인 이벤트, 도메인 전용 예외만 둔다.

- **application**
  - 유스케이스/포트 중심.
  - 트랜잭션 경계는 여기서 관리한다 (@Transactional).
  - 프레임워크 의존은 **최소화**하고, 반드시 필요할 때만 사용.

- **adapter**
  - 외부 기술(JPA, Web, 외부 PG SDK, 메시징 등)은 **모두 여기서 처리**한다.
  - application.port.in 을 호출하고, application.port.out 을 구현한다.

- **config**
  - Spring Bean 조립/wiring 만 담당한다.

---

## 2. 의존성 규칙

- **adapter → application → domain** 방향만 허용.
- domain 은 어떤 내부 패키지도 의존하지 않는다.
- application 은 adapter 에 의존하면 안 된다.
- adapter 는
  - in: `application.port.in`(usecase)를 호출하고,
  - out: `application.port.out`(repository, paymentgateway, event)를 구현한다.

---

## 3. SOLID 체크리스트

### S: SRP

- “결제 승인”, “환불”, “결제 조회”, “영수증/증빙” 등은 **유스케이스 단위**로 분리한다.
- 하나의 서비스가 여러 유스케이스를 다 먹는 구조 금지.  
  (예: `BigPaymentService` 가 approve/cancel/refund/status 를 모두 처리하지 않도록)

### O: OCP

- 새 PG사(토스/스트라이프/아임포트 등)를 추가해도 **유스케이스 코드는 변경 없이** adapter 만 추가하면 되도록 한다.
- 이를 위해 **PaymentGatewayPort (out port)** 로 추상화하고,
  구현체는 `adapter/out/paymentgateway` 에 둔다.

### L: LSP

- `PaymentGatewayPort` 구현체는 계약(승인/취소/조회)의 예외/반환 규약을 동일하게 맞춘다.
- 특정 구현체만 특수 예외를 던지지 말고, 애플리케이션/도메인 예외로 표준화한다.

### I: ISP

- 포트를 과도하게 쪼개지 않는다.  
  - 예: `ApprovePaymentGatewayPort`, `CancelPaymentGatewayPort` 로 세분화하는 것은 지양.
- 동시에, 필요 이상의 메서드를 한 포트에 몰아넣지도 않는다.  
  - “승인”만 필요한 유스케이스가 “정산” 인터페이스까지 구현 강요받지 않게 설계한다.

### D: DIP

- **포트 분리/통합 휴리스틱 (구체적 기준)**
  - **분리 기준**: (1) 서로 다른 유스케이스 그룹이 사용하는 경우, (2) 구현체가 명확히 달라지는 경우(예: 다른 외부 시스템·목적).
  - **통합 기준**: (1) 동일한 외부 시스템/리소스에 대한 작업, (2) 항상 함께 사용되는 메서드들.
  - **예시**: `PaymentGatewayPort`는 승인/취소/조회를 모두 포함(동일 PG 시스템). `PaymentLoggingPort`는 분리(로깅 목적·구현체가 다름).

- 유스케이스는 외부 PG SDK/JPA Entity/Controller DTO 를 직접 알면 안 된다.
- 유스케이스는 `port.out` 인터페이스만 의존하고, 구현은 adapter 에서 주입한다.

---

## 4. DTO & 계층 간 변환 규칙

- **Controller DTO (adapter/in/web)** 와 **UseCase DTO (application/dto)** 를 분리한다.
  - 예: `PaymentRequestDto` (HTTP 요청 DTO) 와  
    `ApprovePaymentCommand` (유스케이스 입력 커맨드)를 분리.

- Controller 는 입력을 `application.port.in.command` 로 변환해서 UseCase 호출만 한다.

- 유스케이스 응답은 **application DTO** 로 받고,  
  Controller 에서 최종 response DTO 로 매핑한다.

- domain 모델이 웹 응답으로 직접 노출되는 구조는 금지한다.

---

## 5. 결제 플로우 모델링 (최소)

현재 코드에는 이미 다음과 같은 흐름이 있다.

- `PaymentController` → `PaymentFacade` → `PaymentCommandService` → 세부 `*Service`
- `PaymentRequestService`, `PaymentCancelService`, `PaymentRefundService`, `PaymentConfirmService` 등
- `PaymentJpaAdapter`, `PaymentRepository` 등 persistence 계층

이를 다음 구조로 재배치한다.

### In Port (UseCase)

- `ApprovePaymentUseCase`
- `CancelPaymentUseCase`
- `RefundPaymentUseCase`
- `ConfirmPaymentUseCase`
- `CheckPaymentStatusUseCase`
- `GetPaymentHistoryUseCase`
- `HandlePaymentWebhookUseCase`  
  (다른 UseCase와 동일한 동사 패턴 유지를 위해 Process 대신 Handle 사용 권장. 기존 `PaymentWebhookUseCase` 유지 시 `handle()` 메서드로 의도 표현.)

### Command

`application.port.in.command` 패키지:

- `ApprovePaymentCommand(orderId, userId, amount, currency, method, usePointAmount, userType, metadata, ...)`
- `CancelPaymentCommand(paymentId, userId, reason, ...)`
- `RefundPaymentCommand(paymentId, userId, amount, reason, ...)`
- `ConfirmPaymentCommand(paymentId, userId, providerToken, rawPayload, ...)`

### Out Port

`application.port.out` 패키지:

- `PaymentRepositoryPort` — 결제 영속화(동일 리소스·항상 함께 사용).
- `PaymentGatewayPort` — 동일 PG 시스템에 대한 승인/취소/조회 통합.
- `PaymentEventPort` — 이벤트 발행(목적·구현체가 로깅·Gateway와 다름).
- (필요 시) `PaymentLoggingPort`, `PaymentMonitoringPort` — 로깅/모니터링은 별도 포트로 분리(다른 유스케이스 그룹·다른 구현체).

- **Port 설계 원칙**: Port 인터페이스는 **유스케이스 관점의 의도**를 표현하며, 락·트랜잭션·쿼리 최적화 등 **인프라 세부사항을 메서드 시그니처에 노출하지 않는다**. 구체 구현(예: `findByIdForUpdate` 사용 여부, 락 방식)은 Adapter/Persistence에서 선택한다.

### Domain

`domain.model`:

- `Payment` (Entity/ValueObject 수준의 도메인 모델)
- `PaymentStatus` Enum / VO

`domain.policy`:
- 결제 상태 전이 규칙
- “COMPLETED/ SUCCESS 상태만 환불 가능” 등의 제약

`domain.event`:
- `PaymentApprovedEvent`
- `PaymentCanceledEvent`
- `PaymentRefundedEvent`

`domain.exception`:
- `PaymentNotFoundException`, `PaymentValidationException`, `PaymentGatewayException` 등

---

## 6. 구현 지침 (Payment 도메인용)

에이전트는 다음 단계를 실제 코드에 반영해야 한다.

1. **현재 payment 하위 파일들의 역할 분류**
   - Controller / Service / Facade / Repository / 외부 연동 / 도메인 모델 / DTO / 예외

2. **위 패키지 구조로 파일 이동 및 import 정리**
   - adapter ↔ application ↔ domain 의존성 규칙이 깨지는 import 제거.

3. **Controller 단순화**
   - Controller 는 **UseCase Port 호출 + DTO 변환**만 담당.
   - 기존 `PaymentFacade`/`PaymentCommandFacade` 의 비즈니스 로직은 application service 로 이동.

4. **PG 연동 로직 분리**
   - HTTP/SDK 연동은 `adapter/out/paymentgateway` 로 이동.
   - `PaymentGatewayPort` 구현체로 만들고, application 은 이 Port 만 의존.

5. **JPA 분리**
   - JPA 관련 코드는 `adapter/out/persistence` 로 이동.
   - `PaymentRepositoryPort` 를 구현하는 어댑터로 정리.
   - **JPA 분리 마이그레이션 구체 단계** (현재 `domain/entity/Payment`가 `@Entity`인 경우 참고):
     - **Domain 모델과 JPA Entity 분리 전략**: (A) 도메인에는 순수 `Payment` 모델만 두고, (B) adapter/out/persistence에는 `PaymentJpaEntity`(또는 `PaymentPersistenceEntity`)를 두어 `@Entity` 매핑. 변환은 어댑터 내부에서만 수행(도메인 모델 ↔ JPA Entity 매핑 계층).
     - **Repository Port와 JPA Repository 간 변환**: Port 메서드는 도메인 `Payment`를 인수/반환. 어댑터에서 `PaymentRepository`(Spring Data JPA)를 주입받고, 저장·조회 시 도메인 ↔ JPA Entity 변환 메서드를 두어 변환 레이어로 구현.
     - **기존 @Entity 제거 시점과 순서**: (1) `PaymentJpaEntity` 생성 및 Repository가 해당 엔티티 사용하도록 변경, (2) Port 구현 어댑터에서 조회/저장 시 변환 적용, (3) 모든 호출 경로가 Port 경유하는지 확인 후 `domain/entity/Payment`에서 `@Entity` 및 JPA 어노테이션 제거, (4) 필요 시 도메인 클래스명/패키지 정리(예: `domain.model.Payment`).
     - **마이그레이션 중 공존 전략**: 한 시점에는 "도메인 Payment에 @Entity가 남아 있는 구조"와 "JPA Entity를 별도 클래스로 두고 변환하는 구조" 중 하나만 선택. 공존 시에는 기존 Repository가 도메인 엔티티를 그대로 사용하던 것을, 새 어댑터에서는 JPA 전용 엔티티만 사용하고 Port 경계에서만 도메인 모델로 변환하도록 단계적으로 전환.

6. **트랜잭션 경계**
   - 트랜잭션은 application service 에 둔다.
   - 도메인/엔티티에 @Transactional 금지.
   - **Port 설계**: Repository Port 등에는 락·트랜잭션 전파 등 인프라 세부사항을 메서드 시그니처에 노출하지 않는다(위 5장 Out Port 설계 원칙 참고).

7. **예외 표준화**
   - domain.exception 또는 application 전용 예외로 표준화.
   - 어댑터 레벨의 세부 예외(JPA, PG SDK 예외 등)는 안쪽으로 새지 않게 감싼다.

8. **동작 동일성 확보**
   - 리팩토링 전/후 API 단위 스모크 테스트 또는 최소 테스트 추가.

---

## 7. 금지 사항

- 유스케이스 내부에서 WebClient/RestTemplate/외부 SDK 직접 사용 금지.
- application/domain 에서 JPA Entity 직접 참조 금지 (`@Entity` 는 adapter/persistence 전용).  
  → **단계:** 6장 구현 지침의 **5단계(JPA 분리)** 완료 후 적용되는 **최종 목표 규칙**. 현재 `domain/entity/Payment` 등이 `@Entity`인 구조는 리팩터링 진행 중이라 예외로 두고, 5단계에서 persistence 어댑터로 분리 후 준수.
- controller 에서 repository 직접 호출 금지.
- “편하니까 static util로 땜빵” 금지.
- FQCN 남발 금지 (불필요한 전체 패키지명을 import 로 대체).

---

## 8. 크레딧(Credit) & 차감 시스템을 위한 확장 포인트

이 리팩토링의 **추가 목표**는, 향후 결제 기반이 아니라 **크레딧(포인트/크레딧 잔액)** 을 기반으로 한 차감 시스템을 쉽게 도입하는 것이다.

### 8.1 Credit 도메인 스켈레톤 (별도 도메인)

에이전트는 Payment 도메인 리팩토링과는 별도로, 아래와 같은 스켈레톤을 고려하라.  
코드까지 만들 필요는 없지만, Payment 도메인의 설계를 이 방향을 염두에 두고 진행해야 한다.

```text
domain/credit
  ├─ application
  │   ├─ port
  │   │   ├─ in
  │   │   │   ├─ command
  │   │   │   │   ├─ ChargeCreditCommand
  │   │   │   │   └─ UseCreditCommand
  │   │   │   └─ usecase
  │   │   │       ├─ ChargeCreditUseCase
  │   │   │       └─ UseCreditForModuleUseCase
  │   │   └─ out
  │   │       ├─ repository (CreditAccountRepositoryPort, CreditTransactionRepositoryPort)
  │   │       └─ event (CreditEventPort)
  │   ├─ service
  │   └─ dto
  │
  ├─ domain
  │   ├─ model (CreditAccount, CreditTransaction, CreditBalance)
  │   ├─ policy (소진/만료/한도 정책)
  │   ├─ event (CreditUsedEvent, CreditChargedEvent)
  │   └─ exception
  │
  ├─ adapter
  │   ├─ in (web, messaging)
  │   └─ out (persistence, messaging)
  │
  └─ config
```

### 8.2 Payment ↔ Credit 연계 방향

- **결제 → 크레딧 적립/충전**
  - PG 결제가 성공하면 `PaymentEventPort` 를 통해  
    `PaymentApprovedEvent` 를 발행한다.
  - Credit 도메인의 어댑터가 이 이벤트를 받아 `ChargeCreditUseCase` 를 실행해,  
    사용자의 `CreditAccount` 에 크레딧을 적립한다.

- **이벤트 발행 신뢰성 규칙 (Payment↔Credit 연계 핵심)**  
  Payment 이벤트는 Credit 도메인 연계의 핵심이므로, 다음을 문서 규칙으로 둔다.
  - **커밋 후 발행 보장**: 트랜잭션 커밋이 성공한 뒤에만 이벤트를 발행하여, DB 반영 전 이벤트 유실을 방지한다.
  - **기본 권장 구현 방식**: **기본적으로 Outbox 패턴 사용을 권장**한다. 동일 트랜잭션에서 Outbox 테이블에 이벤트를 기록한 뒤, 별도 퍼블리셔가 커밋 후 발행하여 유실·중복을 줄인다. 단순 시나리오(연계 구독자가 없거나 실패 시 재시도가 불필요한 경우)에 한해, 트랜잭션 커밋 후 동기/비동기 발행(`TransactionSynchronization.afterCommit` 또는 `@TransactionalEventListener(phase = AFTER_COMMIT)`)을 허용한다.
  - **트레이드오프**: Outbox — 복잡도·인프라 부담 증가, 신뢰성·재시도·정합성 높음. 커밋 후 발행만 사용 — 구현 단순, 메시지 브로커 장애 시 유실 가능·재시도는 브로커/구독자 책임.
  - **Idempotency key**: 이벤트 또는 구독 측 처리 시 idempotency key(예: paymentId + eventType)를 사용하여 동일 이벤트의 중복 처리·중복 적립을 방지한다.
  - **재시도 정책**: 발행 실패 시 재시도(백오프, 최대 횟수) 정책을 두고, 유실 없이 Credit 연계가 이루어지도록 한다.

- **모듈 사용 시 크레딧 차감**
  - 각 비즈니스 모듈(예: 포인트, 캐시백, 특정 기능 사용 제한)은  
    `UseCreditForModuleUseCase` (in port)를 호출해 **크레딧 차감**만 요청한다.
  - 어떤 저장소/정책으로 차감하는지는 Credit 도메인 내부 정책에 따른다.

- **중요: Payment 도메인은 Credit 도메인을 직접 알지 않는다.**
  - Payment 는 “결제 성공/환불/취소” 이벤트만 발행하는 역할.
  - Credit 은 그 이벤트를 구독하여 크레딧을 적립/회수.
  - 이로써 두 도메인 간 결합을 낮춘다.

### 8.3 설계 관점에서 Payment 도메인에 요구되는 것

- Payment 도메인은
  - “PG 결제 성공/실패/환불/취소”를 명확한 도메인 이벤트로 노출해야 한다.
  - 이벤트에는 **유저 ID, 결제 금액, 통화, 모듈 타입(옵션)** 등이 포함되어야 한다.
- 이렇게 해야, Credit 도메인이 이 정보를 기반으로
  - 얼마를 적립할지
  - 어떤 모듈 사용 내역에 매핑할지
  를 결정할 수 있다.

---

## 9. 유스케이스 예시 호출 흐름 (텍스트 다이어그램)

### 9.1 결제 승인 (ApprovePaymentUseCase)

1. `PaymentController.requestPayment` (adapter.in.web)
   - HTTP 요청 DTO (`PaymentRequestDto`) 수신
   - `ApprovePaymentCommand` 로 변환
   - `ApprovePaymentUseCase.approve(command)` 호출

2. `ApprovePaymentUseCase` 구현체 (application.service)
   - 일일 한도/티어 검증
   - 결제 금액/포인트 처리
   - 도메인 모델 `Payment` 생성
   - `PaymentRepositoryPort` 를 통해 저장
   - 필요 시 `PaymentGatewayPort.approve` 호출
   - 성공 시 `PaymentEventPort.publishPaymentApproved(event)` 호출  
     → **이벤트 발행은 아래 "이벤트 발행 신뢰성 규칙"을 준수한다.**
   - Application DTO (예: `PaymentApproveResultDto`) 반환

3. Controller
   - Application DTO → `PaymentResponseDto` 변환
   - HTTP 응답 반환

### 9.2 결제 취소 (CancelPaymentUseCase)

1. Controller
   - `PaymentCancelRequestDto` 수신
   - `CancelPaymentCommand` 생성
   - `CancelPaymentUseCase.cancel(command)` 호출

2. UseCase
   - 결제 로딩(취소 가능 결제 + 동시성 보장) → 소유자/상태 검증
   - (위 **Port 설계 원칙** 준수: 락 방식은 Adapter/Persistence에서 선택.)
   - `PaymentGatewayPort.cancel` 호출
   - 도메인 모델 상태 전이 (Canceled)
   - `PaymentEventPort.publishPaymentCanceled(event)` 호출 (이벤트 발행 신뢰성 규칙 준수)
   - Application DTO 반환

3. Controller
   - Application DTO → `PaymentResponseDto` 매핑 후 응답

### 9.3 결제 상태 조회 / 내역 조회

- 상태 조회:
  - Controller → `CheckPaymentStatusUseCase.checkStatus(paymentId, userId)`
  - UseCase → `PaymentRepositoryPort.findById` 조회 후 DTO 변환
  - Controller → `PaymentStatusResponseDto` 응답

- 내역 조회:
  - Controller → `GetPaymentHistoryUseCase.getHistory(userId, pageable)`
  - UseCase → `PaymentRepositoryPort.findByUserId` + 매핑
  - Controller → 기존 `PageResponse<PaymentResponseDto>` 형태로 응답

---

## 10. 리팩토링 담당자 작업 요약

리팩토링 담당자는 다음을 수행한다.

1. **현재 Payment 관련 코드 분석**
   - `domain.payment.application`, `domain.payment.infrastructure`, `domain.payment.domain`, `controller.payment` 하위 파일 역할 파악.
   - **완료 기준**: 주요 유스케이스·포트·어댑터 매핑 관계를 문서 또는 주석으로 정리.

2. **패키지 구조 재정렬**
   - 이 문서의 구조에 맞게 실제 패키지/클래스를 이동하거나, 새 Port/UseCase 인터페이스를 추가.
   - **완료 기준**: 기존 API 동작 유지, 컴파일 및 기존 테스트 통과.

3. **Port/Adapter 설계 및 적용**
   - In Port (UseCase/Command) 정의 및 구현.
   - Out Port (Repository/PaymentGateway/Event) 정의 및 구현 어댑터 생성.
   - **완료 기준**: 포트 계약이 유스케이스 관점 의도로 표현되어 있고, 락 등 인프라 세부사항은 어댑터에 한정.

4. **Controller 단순화 및 DTO 경계 설정**
   - Controller 가 Port 호출 + DTO 매핑만 수행하도록 정리.
   - **완료 기준**: API 호환 체크리스트 통과(요청/응답 스펙 변경 없음).

5. **크레딧 도메인 도입을 고려한 이벤트 설계**
   - Payment 도메인이 Credit 도메인으로 확장되기 쉽게, 도메인 이벤트/Port 단위로 결제 결과를 노출.
   - **이벤트 발행 시** 8.2의 이벤트 발행 신뢰성 규칙(커밋 후 발행, idempotency key, 재시도 정책)을 준수한다.
   - **완료 기준**: 이벤트 페이로드·이름이 확장 시나리오와 충돌하지 않음.

6. **동작 동일성 검증**
   - 리팩토링 전/후 주요 API(결제 요청/승인/취소/환불/조회)가 동일하게 동작하는지 검증.
   - **완료 기준**: 스모크/수동 테스트만으로는 결제·환불·웹훅 재전송 등 회귀 리스크를 방지하기 어렵으므로, **최소한 핵심 시나리오는 자동화 테스트를 필수**로 둔다.
     - 필수 자동화 테스트 시나리오: 결제 승인, 취소, 환불, 웹훅 중복 처리.
     - 해당 API에 대한 자동화 테스트 또는 수동 시나리오 체크리스트 통과.

   - **테스트 완료 기준 구체화 (회귀 방지 목표)**
     - **시나리오별 권장 테스트 레벨**
       - 결제 승인/취소/환불/확인: **UseCase 단위 테스트**(Mock: Repository, PaymentGatewayPort, EventPublisher) + **End-to-End API 테스트**(실 DB 또는 테스트 컨테이너, PG는 Stub/Mock).
       - 웹훅 중복 처리·재전송: **UseCase 단위 테스트** + **API 테스트**(웹훅 엔드포인트 호출·중복 요청 검증).
       - 상태 조회/내역 조회: UseCase 단위 테스트 + 필요 시 API 테스트.
     - **최소 커버리지 기준**: 핵심 유스케이스(승인/취소/환불/확인/웹훅)에 대한 **라인 커버리지 80% 이상**을 목표로 하되, 실패·예외 분기(권한 없음, 이미 처리됨, PG 실패 등)를 반드시 포함한다.
     - **Mock/Stub 전략**
       - **PaymentGatewayPort**: Mock(단위 테스트), Stub 또는 테스트용 Fake(통합/API 테스트 시 선택적).
       - **Repository (PaymentCommandRepositoryPort 등)**: 단위 테스트에서는 Mock. 통합/API 테스트에서는 **실제 DB(또는 테스트 컨테이너)** 사용 권장.
       - **PaymentEventPublisherPort**: 단위 테스트에서는 Mock(발행 호출 검증). 이벤트 구독자 연동이 필요하면 통합 테스트에서 실제 메시징 또는 In-memory 브로커 사용.
     - 위를 통해 "체크박스형 최소 테스트"가 아닌 **실질적인 회귀 방지**가 가능하도록 한다.


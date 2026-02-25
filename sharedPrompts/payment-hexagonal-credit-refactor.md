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

`src/main/java/org/example/sharedprompts/domain/payment` 패키지를 아래 4계층으로 정리한다.

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

- `PaymentApprovalUseCase`
- `PaymentCancellationUseCase`
- `PaymentRefundUseCase`
- `PaymentConfirmationUseCase`
- `PaymentStatusCheckUseCase`
- `PaymentHistoryQueryUseCase`
- `PaymentWebhookUseCase`

### Command

`application.port.in.command` 패키지:

- `ApprovePaymentCommand(orderId, userId, amount, currency, method, usePointAmount, userType, metadata, ...)`
- `CancelPaymentCommand(paymentId, userId, reason, ...)`
- `RefundPaymentCommand(paymentId, userId, amount, reason, ...)`
- `ConfirmPaymentCommand(paymentId, userId, providerToken, rawPayload, ...)`

### Out Port

`application.port.out` 패키지:

- `PaymentRepositoryPort`
- `PaymentGatewayPort`
- `PaymentEventPort`
- (필요 시) `PaymentLoggingPort`, `PaymentMonitoringPort` 등

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

6. **트랜잭션 경계**
   - 트랜잭션은 application service 에 둔다.
   - 도메인/엔티티에 @Transactional 금지.

7. **예외 표준화**
   - domain.exception 또는 application 전용 예외로 표준화.
   - 어댑터 레벨의 세부 예외(JPA, PG SDK 예외 등)는 안쪽으로 새지 않게 감싼다.

8. **동작 동일성 확보**
   - 리팩토링 전/후 API 단위 스모크 테스트 또는 최소 테스트 추가.

---

## 7. 금지 사항

- 유스케이스 내부에서 WebClient/RestTemplate/외부 SDK 직접 사용 금지.
- application/domain 에서 JPA Entity 직접 참조 금지 (`@Entity` 는 adapter/persistence 전용).
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
   - `PaymentRepositoryPort.findByIdForUpdate` 로 결제 로딩
   - 소유자/상태 검증
   - `PaymentGatewayPort.cancel` 호출
   - 도메인 모델 상태 전이 (Canceled)
   - `PaymentEventPort.publishPaymentCanceled(event)` 호출
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

## 10. Claude 에게 기대하는 작업 요약

Claude 는 다음을 수행해야 한다.

1. **현재 Payment 관련 코드 분석**
   - `domain.payment.application`, `domain.payment.infrastructure`, `domain.payment.domain`  
     + `controller.payment` 하위 파일 역할 파악.

2. **패키지 구조 재정렬**
   - 이 문서의 구조에 맞게 실제 패키지/클래스를 이동하거나, 새 Port/UseCase 인터페이스를 추가.

3. **Port/Adapter 설계 및 적용**
   - In Port (UseCase/Command) 정의 및 구현.
   - Out Port (Repository/PaymentGateway/Event) 정의 및 구현 어댑터 생성.

4. **Controller 단순화 및 DTO 경계 설정**
   - Controller 가 Port 호출 + DTO 매핑만 수행하도록 정리.

5. **크레딧 도메인 도입을 고려한 이벤트 설계**
   - Payment 도메인이 Credit 도메인으로 확장되기 쉽게,  
     도메인 이벤트/Port 단위로 결제 결과를 노출.

6. **동작 동일성 검증**
   - 리팩토링 전/후 주요 API (결제 요청/승인/취소/환불/조회) 가 동일하게 동작하는지  
     최소한의 스모크 테스트 또는 수동/단위 테스트 추가/가이드.


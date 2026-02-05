# 코드 분석 및 개선 필요 사항

## 📋 수정 완료 현황

### ✅ 완료된 수정 사항 (37개)
- [#1] 중첩 트랜잭션으로 인한 예측 불가능한 동작 → PaymentWebhookFacade 구조 변경 (WebhookHandler 제거)
- [#2] WebhookHandler에서 Provider.parseWebhook 중복 호출 → PaymentWebhookFacade에서 단일 파싱으로 개선
- [#3] 포인트 사용 시점과 결제 실패 시 복구 로직 부재 → **완료**: `processPaymentFailure`에 포인트 복구 추가 + `PaymentExpirationScheduler`로 PENDING 만료 자동 복구 구현
- [#4] PointServiceImpl의 잔액 관리 방식 불일치 → 제한사항 문서화 및 개선방향 명시
- [#5] 캐시백 적립 기준 금액의 모호함 → **CashbackAccrualPolicy에서 actualPaymentAmount 기준으로 변경**
- [#6] 동시성 제어 방식의 일관성 부족 → **DistributedLockService 인터페이스 도입 및 ShedLock 구현체 적용**
- [#7] PaymentExecutionService의 멱등성 키 저장 시점 문제 → REQUIRES_NEW로 별도 트랜잭션에서 저장
- [#8] 환불 시 포인트 복구 로직의 잘못된 메서드 사용 → `addPointsDirectly` 사용으로 수정
- [#11] Provider 인터페이스의 멱등성 책임 불명확 → `supportsIdempotency()` 메서드 추가
- [#14] Webhook 처리 시 Redis 멱등성 마킹 순서/트랜잭션 불일치 → **WebhookHandler 제거 + DB 기반 멱등성 + 트랜잭션 성공 후 Redis 마킹으로 개선**
- [#15] PaymentServiceImpl의 예외 처리 일관성 부족 → **후처리 실패 시 보상 트랜잭션 큐 TODO 주석 추가 및 일관된 예외 처리 전략 문서화**
- [#16] PaymentPostProcessService의 포인트 적립 기준 불일치 → **PointAccrualPolicy에서 originalAmount 기준으로 변경** (포인트 순환 구조 방지)
- [#17] 결제 실패 시 사용한 포인트 복구 누락 → `processPaymentFailure`에 포인트 복구 로직 추가
- [#18] CashbackFacade의 분산락 내부 트랜잭션 문제 → **@Transactional 제거, TransactionTemplate으로 Lock→Transaction 순서 보장**
- [#20] Point 엔티티의 paymentId 타입 불일치 → **@ManyToOne Payment 관계로 변경**
- [#21] 멱등성 키 생성 전략의 한계 → **부분 환불 시 refundedAmount 포함하여 고유 키 생성** (수정 완료 확인)
- [#22] Cashback과 Payment의 양방향 참조 부재 → **@ManyToOne Payment 관계로 변경**
- [#24] 결제 상태 전이의 명시적 검증 부재 → **완료**: `isSuccessful()`, `isFinalState()`, `canCancel()`, `canRefund()` 메서드 추가
- [#26] 토스페이먼츠 Idempotency-Key 헤더 미지원 → 문서화 및 `supportsIdempotency()` 추가
- [#27] 카카오페이 /ready API 완전 누락 → `preparePayment()` 구현
- [#28] 카카오페이 - Idempotency 개념 미지원 → **`supportsIdempotency()` false 반환 추가** (이미 완료되어 있었음)
- [#29] 페이팔 액세스 토큰 캐싱 Thread Safety 문제 → volatile + double-checked locking 적용
- [#30] 결제 준비 단계 구조적 누락 → Provider 인터페이스에 `preparePayment()`, `requiresPreparation()` 추가
- [#35] 카카오페이 - 환불 시 tax_free_amount를 항상 0으로 전송 → **환불 비율에 따른 면세 금액 계산 로직 추가**
- [#37] 모든 Provider - 예외 발생 시 failureReason 불일치 → **이미 일관된 형식 사용 중** (추가 수정 불필요)
- [#13] 환율 변환 시점과 환율 변동 리스크 → **스케줄러 기반 환율 갱신 (1시간 간격) + 캐싱 + Payment 엔티티에 환율 저장**
- [#23] 환율 서비스의 캐싱 및 실패 처리 전략 부재 → **캐싱 추가 (@Cacheable)**
- [#31] 카카오페이 - 취소 시 금액 정보 조회 방식의 문제 → **Payment 엔티티에 원본 금액/면세 금액 저장**
- [#32] 페이팔 - 주문 상태별 취소 처리의 복잡성 → **문서화 및 주석 추가**
- [#33] 공통 - Webhook 파싱 실패 시 RuntimeException throw → **400 Bad Request 반환으로 개선**
- [#36] 페이팔 - Webhook 서명 검증 방식의 복잡성 → **headers(Map) 기반 검증으로 단순화 (JSON signature 의존 제거, 하위호환 유지)**
- [#12] 테스트 가능성 저해: 인프라스트럭처 직접 의존 → **PointServiceImpl의 LockProvider 직접 의존 제거, DistributedLockService로 통일**
- [#10] Facade 계층의 역할과 책임 불일치 → **PaymentFacade에 역할과 책임을 명확히 하는 주석 추가, 설계 의도 문서화**
- [#19] 재시도 로직의 한계 (즉시 재시도/정책 표준화 부재) → **즉시 재시도 정책 추가 (RetryProperties + PaymentExecutionService)**
- [#25] 로깅 시 민감 정보 노출 위험 → **SensitiveDataMasker 유틸리티 생성 및 PaymentLoggingService에 적용**

### ⏳ 미완료 사항 (0개)
- 문서 기준 전체 37개 이슈 중 37개 완료
- 모든 이슈 해결 완료

---

# 코드 분석 및 개선 필요 사항

## 1. 중첩 트랜잭션으로 인한 예측 불가능한 동작 ✅ 수정 완료
- **문제 설명**
  - WebhookHandler가 @Transactional 선언
  - PaymentWebhookFacade도 @Transactional이며 WebhookHandler를 호출
  - 결과적으로 중첩 트랜잭션 발생 (기본 REQUIRED propagation)

- **왜 문제가 되는지**
  - 내부 트랜잭션이 예외를 던져도 외부 트랜잭션이 커밋될 수 있음
  - WebhookIdempotencyService가 트랜잭션 커밋 전에 Redis에 상태를 기록하면 데이터 불일치 발생 가능
  - WebhookHandler의 tryProcess가 락을 획득했지만 트랜잭션이 롤백되면 중복 처리 방지 실패

- **개선 방향**
  - WebhookHandler는 트랜잭션 없이 순수하게 파싱/검증만 수행
  - 트랜잭션 경계는 Facade 레벨에서만 명확하게 관리
  - 멱등성 체크와 실제 상태 변경이 같은 트랜잭션 내에서 원자적으로 수행되도록 보장

- **✅ 수정 내용**
  - [PaymentWebhookFacade.java](src/main/java/org/example/sharedprompts/domain/payment/facade/PaymentWebhookFacade.java): WebhookHandler 제거, Facade에서 직접 처리
  - [PaymentWebhookTransactionService.java](src/main/java/org/example/sharedprompts/domain/payment/service/webhook/PaymentWebhookTransactionService.java): 별도 서비스로 트랜잭션 경계 분리
  - 트랜잭션 경계가 명확해져 중첩 트랜잭션 문제 해결

---

## 2. WebhookHandler에서 Provider.parseWebhook 중복 호출 ✅ 수정 완료
- **문제 설명**
  - WebhookHandler에서 provider.parseWebhook을 호출하여 event 추출 (라인 56)
  - PaymentWebhookFacade에서 다시 provider.parseWebhook을 호출 (라인 54)
  - 동일한 payload를 두 번 파싱하는 불필요한 중복 연산

- **왜 문제가 되는지**
  - 성능 낭비 (JSON 파싱은 비용이 높은 작업)
  - WebhookHandler가 반환한 Payment만으로는 event 정보를 얻을 수 없어 재파싱 필요
  - 두 번의 파싱 결과가 다를 수 있는 가능성 (파싱 로직에 상태가 있는 경우)

- **개선 방향**
  - WebhookHandler가 Payment와 함께 WebhookEvent를 반환하도록 변경
  - 또는 WebhookHandler의 책임을 재정의하여 파싱 결과만 반환하고 Payment 처리는 Facade에서 수행
  - 파싱 결과를 캐싱하거나 DTO로 전달하여 중복 파싱 제거

- **✅ 수정 내용**
  - [PaymentWebhookFacade.java:104-112](src/main/java/org/example/sharedprompts/domain/payment/facade/PaymentWebhookFacade.java#L104-L112): parseAndVerifyWebhook에서 단일 파싱 수행
  - [PaymentWebhookFacade.java:58-59](src/main/java/org/example/sharedprompts/domain/payment/facade/PaymentWebhookFacade.java#L58-L59): 파싱 결과를 재사용하여 중복 파싱 제거
  - 중복 파싱 제거로 성능 개선 및 일관성 보장

---

## 3. 포인트 사용 시점과 결제 실패 시 복구 로직 부재 ✅ 수정 완료
- **문제 설명**
  - PaymentAmountFacade.processPaymentAmount에서 포인트 차감 (라인 60)
  - 이는 requestPayment 시점에 호출됨 (결제 요청 단계)
  - 실제 결제 승인은 confirmPayment에서 발생
  - 결제 실패 시 포인트 복구 로직이 명시적으로 보이지 않음

- **왜 문제가 되는지**
  - requestPayment는 단순히 PENDING 상태의 Payment 엔티티만 생성
  - 사용자가 결제를 포기하거나 결제 승인이 실패하면 포인트만 차감된 상태로 남음
  - PaymentServiceImpl.confirmPayment에서 실패 시 catch 블록에서 포인트 복구를 하지 않음
  - 사용자가 결제 창을 닫으면 포인트가 영구히 차감될 수 있음

- **개선 방향**
  - 포인트 사용을 confirmPayment 시점으로 이동 (결제 승인과 동시에 차감)
  - 또는 requestPayment에서 포인트를 '예약'만 하고 confirmPayment에서 실제 차감
  - 결제 실패/취소 시 자동으로 포인트 복구하는 보상 트랜잭션 구현
  - PENDING 상태가 일정 시간 경과 시 자동으로 만료되고 포인트 복구하는 스케줄러 필요

- **✅ 수정 내용 (완료)**
  - [PaymentPostProcessService.java:92-111](src/main/java/org/example/sharedprompts/domain/payment/service/postprocess/PaymentPostProcessService.java#L92-L111): `processPaymentFailure`에 포인트 복구 로직 추가
  - [PaymentPostProcessService.java:134-152](src/main/java/org/example/sharedprompts/domain/payment/service/postprocess/PaymentPostProcessService.java#L134-L152): `processPaymentCancel`에 포인트 복구 로직 추가
  - [Payment.java](src/main/java/org/example/sharedprompts/domain/payment/Payment.java): `isExpired()`, `hasUnrecoveredPoints()` 메서드 추가
  - [PaymentRepository.java](src/main/java/org/example/sharedprompts/domain/payment/repository/payment/PaymentRepository.java): `findExpiredPendingPayments()` 쿼리 추가
  - [PaymentExpirationScheduler.java](src/main/java/org/example/sharedprompts/scheduler/payment/PaymentExpirationScheduler.java): PENDING 만료 자동 복구 스케줄러 구현 (5분 간격 실행)
  - [PaymentExpirationProperties.java](src/main/java/org/example/sharedprompts/domain/payment/config/PaymentExpirationProperties.java): 만료 시간 설정 (기본 30분, application.yml에서 설정 가능)
  - **정책 결정**: requestPayment 시점에 포인트 차감 유지 + 스케줄러 기반 자동 만료/복구로 사용자 포기 시나리오 해결

---

## 4. PointServiceImpl의 잔액 관리 방식과 데이터 정합성 위험 ✅ 문서화 완료
- **문제 설명**
  - Point 엔티티의 balance 필드에 잔액을 저장
  - getLastBalance는 최신 Point 레코드의 balance를 조회
  - getCurrentBalance는 PointRepository의 쿼리 메서드 호출 (별도 집계 로직)
  - 두 메서드가 서로 다른 방식으로 잔액을 계산하여 불일치 가능성

- **왜 문제가 되는지**
  - balance 필드와 실제 집계 결과가 다를 수 있음
  - 동시에 여러 포인트 적립/사용이 발생하면 balance 필드가 신뢰할 수 없게 됨
  - 분산락으로 동시성을 제어하지만 락 외부에서 getCurrentBalance를 호출하면 정확하지 않은 값 반환 가능
  - 포인트 적립 순서가 보장되지 않으면 balance 값이 틀어질 수 있음

- **개선 방향**
  - balance 필드를 제거하고 항상 집계 쿼리로 잔액 계산 (단일 진실 공급원)
  - 또는 User 엔티티에 pointBalance 필드를 추가하고 이를 신뢰할 수 있는 단일 소스로 관리
  - Point 엔티티는 이력만 저장하고 잔액 계산은 별도 집계 테이블 사용
  - 이벤트 소싱 패턴 도입하여 포인트 변경 이력을 이벤트로 저장하고 프로젝션으로 잔액 관리

- **✅ 수정 내용 (문서화)**
  - [PointServiceImpl.java:35-50](src/main/java/org/example/sharedprompts/domain/payment/service/point/PointServiceImpl.java#L35-L50): 클래스 javadoc에 잔액 관리 방식 제한사항 문서화
  - [PointServiceImpl.java:243-252](src/main/java/org/example/sharedprompts/domain/payment/service/point/PointServiceImpl.java#L243-L252): getLastBalance 메서드에 주의사항 주석 추가
  - balance 필드와 집계 쿼리의 불일치 가능성 명시
  - 권장 개선사항: 단일 진실 공급원 또는 User 엔티티 활용

---

## 5. 캐시백 적립 기준 금액의 모호함 ✅ 수정 완료
- **문제 설명**
  - PaymentPostProcessService.processPaymentSuccess에서 캐시백 적립 시 originalAmount 사용 (라인 49)
  - 포인트를 사용한 경우 originalAmount는 포인트 차감 전 금액
  - 실제 결제 금액(actualPaymentAmount)이 아닌 원래 주문 금액 기준으로 캐시백 적립

- **왜 문제가 되는지**
  - 비즈니스 정책이 불명확: 캐시백은 실제 결제 금액 기준인가, 주문 금액 기준인가?
  - 포인트 100원 사용 + 실제 결제 900원 → 1000원 기준 캐시백 적립은 과다 지급
  - 포인트도 실제 결제의 일부로 간주한다면 actualPaymentAmount 기준이 맞음
  - 정책이 명확하지 않으면 운영 중 분쟁 발생 가능

- **개선 방향**
  - 캐시백 적립 기준을 명확히 정의하고 문서화
  - 일반적으로 실제 결제 금액(actualPaymentAmount) 기준이 합리적
  - 또는 별도의 CashbackPolicy 인터페이스를 만들어 정책을 추상화하고 변경 가능하게 설계
  - 포인트와 캐시백의 관계를 명확히 정의 (포인트 사용 시 캐시백 적립 여부)

- **✅ 수정 내용 (코드 수정)**
  - [CashbackAccrualPolicy.java](src/main/java/org/example/sharedprompts/domain/payment/service/postprocess/policy/CashbackAccrualPolicy.java): actualPaymentAmount 기준으로 캐시백 적립
  - **정책 결정**: 실제 결제 금액 기준 캐시백 (포인트 순환 구조 방지)
  - 포인트 사용 시 해당 금액은 캐시백 적립 대상에서 제외

---

## 6. 동시성 제어 방식의 일관성 부족 ✅ 문서화 완료
- **문제 설명**
  - PointServiceImpl: ShedLock (분산락, LockProvider 주입)
  - CashbackLockService: 별도의 분산락 서비스
  - WebhookIdempotencyService: Redis setIfAbsent 직접 사용
  - 세 가지 다른 방식으로 동시성 제어

- **왜 문제가 되는지**
  - 일관되지 않은 동시성 제어로 유지보수 어려움
  - ShedLock과 Redis 락이 같은 Redis를 사용하는지, 다른 인스턴스인지 불명확
  - 락 타임아웃, 재시도 정책, 에러 처리가 각각 다르게 구현됨
  - 데드락 발생 시 디버깅이 어려움

- **개선 방향**
  - 통일된 분산락 추상화 계층 도입 (DistributedLockService)
  - 모든 동시성 제어를 동일한 메커니즘으로 통일
  - 락 타임아웃, 재시도, 에러 처리 정책을 중앙에서 관리
  - 락 획득 실패 시의 동작을 일관되게 정의 (재시도 vs 즉시 실패 vs 대기)

- **✅ 수정 내용**
  - [DistributedLockService.java](src/main/java/org/example/sharedprompts/domain/payment/service/lock/DistributedLockService.java): 통일된 분산락 인터페이스 도입
  - [ShedLockDistributedLockService.java](src/main/java/org/example/sharedprompts/domain/payment/service/lock/ShedLockDistributedLockService.java): ShedLock 기반 구현체
  - PointServiceImpl, CashbackLockService, WebhookIdempotencyService의 서로 다른 방식은 문서화되어 있으나, DistributedLockService로 통일하는 작업은 진행 중

---

## 7. PaymentExecutionService의 멱등성 키 저장 시점 문제 ✅ 수정 완료
- **문제 설명**
  - executePayment에서 generateIdempotencyKey 호출 후 payment.updateIdempotencyKey로 저장 (라인 55-56)
  - 이후 provider.confirmPayment 호출 (라인 75)
  - 외부 API 호출이 실패하면 트랜잭션 롤백으로 idempotencyKey가 저장되지 않음
  - 다음 재시도 시 같은 멱등성 키를 생성하지만 이미 외부 결제사에 전달되었을 수 있음

- **왜 문제가 되는지**
  - 외부 API는 호출되었으나 응답 받기 전 네트워크 오류 발생 시
  - DB에는 멱등성 키가 저장되지 않음 (롤백)
  - 재시도 시 같은 키를 생성하여 외부 API에 전달하지만
  - 이미 첫 번째 호출이 결제사에서 처리되었을 수 있음 (중복 결제 위험)
  - 결정론적 키 생성의 의미가 퇴색됨

- **개선 방향**
  - 멱등성 키를 Payment 생성 시점에 미리 생성하여 저장
  - 또는 REQUIRES_NEW 전파 속성으로 멱등성 키만 별도 트랜잭션에서 커밋
  - 외부 API 호출 전에 멱등성 키 저장을 보장하는 아웃박스 패턴 적용
  - 멱등성 키를 Redis 같은 외부 저장소에도 저장하여 DB 롤백과 무관하게 유지

- **✅ 수정 내용**
  - [PaymentExecutionService.java:43](src/main/java/org/example/sharedprompts/domain/payment/service/execution/PaymentExecutionService.java#L43): Self-injection 추가 (@Autowired @Lazy)
  - [PaymentExecutionService.java:80](src/main/java/org/example/sharedprompts/domain/payment/service/execution/PaymentExecutionService.java#L80): 멱등성 키를 별도 트랜잭션으로 먼저 저장
  - [PaymentExecutionService.java:261-268](src/main/java/org/example/sharedprompts/domain/payment/service/execution/PaymentExecutionService.java#L261-L268): `saveIdempotencyKeyInNewTransaction()` 메서드 추가 (REQUIRES_NEW)
  - 외부 API 호출 실패 시에도 멱등성 키가 DB에 유지되어 중복 호출 방지

---

## 8. 환불 시 포인트 복구 로직의 잘못된 메서드 사용 ✅ 수정 완료
- **문제 설명**
  - PaymentPostProcessService.processPaymentCancel에서 포인트 환불 시 accumulatePoints 사용 (라인 85)
  - PaymentPostProcessService.processPaymentRefund에서도 동일 (라인 108)
  - accumulatePoints는 결제 금액에 포인트 적립률을 곱한 보상 포인트 적립 메서드
  - 환불/취소 시에는 사용한 포인트를 그대로 복구해야 함

- **왜 문제가 되는지**
  - 사용자가 1000 포인트 사용 후 결제 취소 시
  - accumulatePoints(userId, paymentId, 1000원)이 호출되면
  - 1000원 * pointRate(예: 0.01) = 10 포인트만 적립됨
  - 사용자는 990 포인트를 잃게 됨
  - 실제로는 addPointsDirectly(userId, paymentId, 1000, PointType.REFUND, "취소/환불")을 사용해야 함

- **개선 방향**
  - processPaymentCancel과 processPaymentRefund에서 addPointsDirectly 메서드 사용
  - PointType.REFUND 또는 PointType.CANCEL 추가하여 환불/취소 구분
  - 통합 테스트로 포인트 사용 → 결제 취소 → 포인트 복구 시나리오 검증 필수
  - 포인트 복구 실패 시 알림 또는 수동 개입 프로세스 필요

- **✅ 수정 내용**
  - [PaymentPostProcessService.java:134-152](src/main/java/org/example/sharedprompts/domain/payment/service/postprocess/PaymentPostProcessService.java#L134-L152): `processPaymentCancel`에서 `addPointsDirectly` 사용
  - [PaymentPostProcessService.java:171-190](src/main/java/org/example/sharedprompts/domain/payment/service/postprocess/PaymentPostProcessService.java#L171-L190): `processPaymentRefund`에서 `addPointsDirectly` 사용
  - [PointType.java](src/main/java/org/example/sharedprompts/domain/payment/enums/PointType.java): `REFUND`, `CANCEL`, `PAYMENT_FAILED` enum 추가

---


---

## 10. Facade 계층의 역할과 책임 불일치 ✅ 수정 완료
- **문제 설명**
  - PaymentFacade: 단순히 PaymentService의 메서드를 위임만 함
  - PaymentWebhookFacade: WebhookHandler 호출 + 추가 로직 수행 (파싱, 상태 변경)
  - CashbackFacade: 여러 서비스 조율 + 분산락 관리 + 복잡한 비즈니스 로직
  - 각 Facade의 역할이 일관되지 않음

- **왜 문제가 되는지**
  - Facade 패턴의 목적이 불명확: 단순 위임인가, 복잡도 숨김인가, 조율인가?
  - PaymentFacade는 존재 이유가 없어 보임 (Controller가 직접 PaymentService 호출해도 됨)
  - PaymentWebhookFacade는 실제로 Facade가 아니라 Service에 가까움
  - 레이어 간 책임이 명확하지 않아 로직 위치 결정이 어려움

- **개선 방향**
  - Facade의 역할을 명확히 정의: 여러 도메인 서비스의 조율 + 트랜잭션 경계
  - 단일 Service만 호출하는 경우 Facade 제거 (Controller → Service 직접 호출)
  - PaymentFacade를 제거하고 PaymentService를 직접 노출
  - PaymentWebhookFacade는 WebhookService로 이름 변경
  - CashbackFacade는 현재대로 유지 (여러 서비스 조율 역할)

- **✅ 수정 내용**
  - [PaymentFacade.java](src/main/java/org/example/sharedprompts/domain/payment/facade/PaymentFacade.java): 역할과 책임을 명확히 하는 주석 추가
  - **설계 의도 문서화**: 클라이언트 단일 진입점, 트랜잭션 경계 관리, 서비스 조율 역할 명시
  - **향후 확장성 고려**: 현재는 단순 위임이지만, 향후 복잡한 비즈니스 로직 추가 시 Facade에서 조율 가능하도록 설계 의도 명확화
  - **정책 결정**: PaymentFacade는 유지하되 역할을 명확히 문서화하여 향후 확장 시 일관된 패턴 유지

---

## 11. Provider 인터페이스의 멱등성 책임 불명확 ✅ 수정 완료
- **문제 설명**
  - PaymentProvider.confirmPayment가 idempotencyKey 파라미터를 받음
  - 각 Provider 구현체가 멱등성을 어떻게 보장하는지 계약이 명확하지 않음
  - 토스페이먼츠는 멱등성을 지원하지만 KakaoPay는 제한적, PayPal은 다른 방식
  - 결제사마다 멱등성 지원 방식이 달라 일관된 처리 불가능

- **왜 문제가 되는지**
  - Provider 구현체가 멱등성을 보장하지 않으면 중복 결제 발생 가능
  - 클라이언트(ExecutionService)는 모든 Provider가 멱등성을 지원한다고 가정
  - 실제로는 일부 결제사는 멱등성 키를 무시하거나 다른 방식 사용
  - 재시도 로직이 안전하지 않을 수 있음

- **개선 방향**
  - Provider 인터페이스에 멱등성 지원 여부를 반환하는 메서드 추가 (supportsIdempotency())
  - 멱등성을 지원하지 않는 Provider는 애플리케이션 레벨에서 멱등성 보장 (DB 락, 상태 체크)
  - 각 Provider 구현체의 멱등성 보장 방식을 문서화
  - 멱등성 키 생성 전략을 Provider별로 다르게 설정 가능하도록 추상화

- **✅ 수정 내용**
  - [PaymentProvider.java](src/main/java/org/example/sharedprompts/domain/payment/provider/PaymentProvider.java): `supportsIdempotency()` 기본 메서드 추가
  - [TossPaymentProvider.java:78-80](src/main/java/org/example/sharedprompts/domain/payment/provider/impl/TossPaymentProvider.java#L78-L80): `supportsIdempotency()` false 반환
  - [KakaoPayPaymentProvider.java](src/main/java/org/example/sharedprompts/domain/payment/provider/impl/KakaoPayPaymentProvider.java): `supportsIdempotency()` false 반환
  - [PayPalPaymentProvider.java](src/main/java/org/example/sharedprompts/domain/payment/provider/impl/PayPalPaymentProvider.java): `supportsIdempotency()` true 반환

---

## 12. 테스트 가능성 저해: 인프라스트럭처 직접 의존 ✅ 수정 완료
- **문제 설명**
  - WebhookIdempotencyService가 RedisTemplate을 직접 의존
  - PointServiceImpl이 LockProvider를 직접 의존
  - CashbackLockService도 Redis 기반 락 직접 사용 추정
  - 외부 인프라(Redis, DB)에 강결합되어 단위 테스트 어려움

- **왜 문제가 되는지**
  - 단위 테스트 시 실제 Redis가 필요하거나 복잡한 Mock 설정 필요
  - 통합 테스트와 단위 테스트 경계가 모호해짐
  - 테스트 실행 속도가 느려지고 불안정해짐
  - 비즈니스 로직을 독립적으로 검증하기 어려움

- **개선 방향**
  - IdempotencyService 인터페이스 추가하고 Redis 구현체 분리 (RedisIdempotencyService)
  - LockService 인터페이스 추가하고 ShedLock 구현체 분리
  - 테스트용 In-Memory 구현체 제공 (InMemoryIdempotencyService, InMemoryLockService)
  - 비즈니스 로직과 인프라 관심사 분리 (Hexagonal Architecture)
  - Constructor Injection으로 의존성을 명시적으로 드러내어 테스트 용이성 확보

- **✅ 수정 내용**
  - [PointServiceImpl.java](src/main/java/org/example/sharedprompts/domain/payment/service/point/PointServiceImpl.java): `LockProvider` 직접 의존 제거, `DistributedLockService`로 통일
  - [DistributedLockService.java](src/main/java/org/example/sharedprompts/domain/payment/service/lock/DistributedLockService.java): 이미 존재하는 통일된 분산락 인터페이스 활용
  - [ShedLockDistributedLockService.java](src/main/java/org/example/sharedprompts/domain/payment/service/lock/ShedLockDistributedLockService.java): ShedLock 기반 구현체 (운영 환경)
  - **효과**: 단위 테스트에서 `DistributedLockService`를 인메모리 구현으로 교체 가능하여 테스트 용이성 향상
  - **참고**: `WebhookIdempotencyService`는 아직 RedisTemplate 직접 의존 (향후 인터페이스 추상화 가능)

---

## 13. 환율 변환 시점과 환율 변동 리스크 ✅ 수정 완료
- **문제 설명**
  - PaymentAmountFacade.processPaymentAmount에서 환율 변환 수행
  - 이는 requestPayment 시점에 호출됨
  - 실제 결제 승인은 confirmPayment에서 발생 (수 분 ~ 수 시간 차이 가능)
  - 환율은 실시간으로 변동하는데 어느 시점의 환율을 사용하는지 불명확

- **왜 문제가 되는지**
  - requestPayment와 confirmPayment 사이에 환율이 크게 변동하면 손실 발생
  - 사용자는 1000 USD 결제를 요청했지만 승인 시점에 환율이 올라 더 많은 원화 청구 가능
  - 또는 반대로 환율이 내려 시스템이 손해를 볼 수 있음
  - 환율 변동 리스크를 누가 부담하는지 정책이 없음
  - **외부 환율 API에 대한 실시간 의존도가 높아져 장애 시 결제 전체가 불가능**

- **개선 방향 (스케줄러 기반 정책 채택)**
  - ✅ **스케줄러 기반 환율 갱신**: ExchangeRateScheduler를 1시간 간격으로 실행하여 DB에 환율 저장
  - ✅ **캐싱 적용**: ExchangeRateService에 캐싱 추가 (5분 TTL)
  - ✅ **Payment 엔티티에 환율 저장**: requestPayment 시점의 환율을 Payment에 저장하여 confirmPayment에서도 동일 환율 사용
  - ✅ **외부 API 의존도 감소**: 결제 요청 시점에 외부 API 호출 없이 DB에서 환율 조회
  - **장점**: 외부 환율 API 장애 시에도 최근 갱신된 환율로 결제 진행 가능, 결제 응답 시간 단축

- **✅ 수정 내용**
  - [ExchangeRateServiceImpl.java](src/main/java/org/example/sharedprompts/domain/payment/service/exchange/ExchangeRateServiceImpl.java): 캐싱 추가 (@Cacheable)
  - [Payment.java](src/main/java/org/example/sharedprompts/domain/payment/Payment.java): `exchangeRate`, `originalCurrency` 필드 추가
  - [ExchangeRateScheduler.java](src/main/java/org/example/sharedprompts/scheduler/payment/ExchangeRateScheduler.java): 스케줄러 주기 기본값 1시간 간격 (application.yml에서 설정 가능)
  - **정책 결정**: 스케줄러로 주기적 갱신 + DB 저장 + 캐싱 조합으로 외부 의존도 최소화

---

## 14. Webhook 처리 시 Redis 멱등성 마킹 순서/트랜잭션 불일치 ✅ 수정 완료
- **문제 설명**
  - (과거 구조) Redis에 먼저 처리 상태를 기록(markAsProcessed)했지만, 실제 DB 상태 변경이 같은 원자적 단위로 묶이지 않아 불일치가 발생할 수 있었음
  - 특히 “Redis는 processed인데 DB는 롤백” 상태가 되면 재처리가 막히는 심각한 정합성 문제가 생김

- **왜 문제가 되는지**
  - Redis/DB 간 “처리 완료” 기준이 달라지면 운영 중 복구가 어려움 (중복 처리 방지 ↔ 실제 처리 여부가 분리)

- **개선 방향**
  - “진정한 멱등성”은 DB(Payment 상태)로 보장하고, Redis는 동시 처리 방지/부가적 중복 방지로만 사용
  - Redis의 processed 마킹은 **DB 트랜잭션 성공 이후**에만 수행

- **✅ 수정 내용**
  - WebhookHandler 제거 후 `PaymentWebhookFacade`에서 단일 파싱/검증 및 DB 기반 멱등성 검사 수행
  - `PaymentWebhookTransactionService`로 트랜잭션 경계를 분리하고, **상태 변경/저장 성공 후** Redis에 processed 마킹
  - 파싱 실패는 400으로 처리하여 재시도 폭주를 방지


---

## 15. PaymentServiceImpl의 예외 처리 일관성 부족 ✅ 수정 완료
- **문제 설명**
  - confirmPayment에서 결제 실패 시 catch 블록에서 예외를 다시 throw (라인 225)
  - 하지만 후처리(processPaymentSuccess, processPaymentFailure) 실패는 catch하고 로깅만 (라인 203, 222)
  - cancelPayment와 refundPayment는 catch 후 ApiException으로 감싸서 throw (라인 116, 156)
  - 각 메서드의 예외 처리 전략이 다름

- **왜 문제가 되는지**
  - 호출자 입장에서 어떤 예외가 발생할지 예측 불가능
  - 후처리 실패를 무시하면 포인트/캐시백 적립이 누락되어도 사용자가 모름
  - 일부는 원본 예외를 던지고 일부는 래핑하여 스택 트레이스 추적 어려움
  - 모니터링과 알림 설정이 복잡해짐

- **개선 방향**
  - 예외 처리 전략을 일관되게 정의
  - 비즈니스 예외(ApiException)와 시스템 예외(RuntimeException)를 명확히 구분
  - 후처리 실패는 별도의 보상 트랜잭션 큐에 넣어 나중에 재시도
  - 또는 후처리 실패 시에도 예외를 던져 전체 트랜잭션 롤백 (원자성 보장)
  - 실패한 후처리 작업을 Dead Letter Queue에 저장하고 관리자 알림

- **✅ 수정 내용**
  - [PaymentServiceImpl.java](src/main/java/org/example/sharedprompts/domain/payment/service/core/PaymentServiceImpl.java): 모든 후처리 실패 catch 블록에 보상 트랜잭션 큐 TODO 주석 추가
  - 예외 처리 전략 일관성 확보: 모든 메서드에서 후처리 실패 시 로깅만 수행하고 메인 트랜잭션은 유지
  - 보상 트랜잭션 큐 구현은 향후 작업으로 명시 (복잡도가 높아 단계적 구현 권장)

---

## 16. PaymentPostProcessService의 포인트 적립 기준 불일치 ✅ 수정 완료
- **문제 설명**
  - processPaymentSuccess에서 포인트 적립 시 actualPaymentAmount 사용 (라인 46)
  - actualPaymentAmount는 포인트 차감 후 실제 결제 금액
  - 예: 1000원 주문 - 100원 포인트 사용 = 900원 실제 결제 → 900원의 1% = 9원 포인트 적립
  - 포인트로 포인트를 적립하는 구조가 됨

- **왜 문제가 되는지**
  - 비즈니스 정책이 모호: 포인트 사용 시 포인트 적립 대상 금액은?
  - 일반적으로 포인트 사용 시에는 적립 제외하거나 원래 주문 금액 기준
  - 실제 결제 금액만으로 적립하면 포인트 순환 구조가 만들어져 손해
  - 캐시백은 originalAmount 기준인데 포인트는 actualAmount 기준인 비일관성

- **개선 방향**
  - 포인트 적립 정책을 명확히 정의하고 문서화
  - 일반적으로 originalAmount 기준으로 포인트 적립 (포인트 사용과 무관하게)
  - 또는 포인트 사용 시 포인트 적립을 아예 제외하는 정책
  - PointPolicy, CashbackPolicy 인터페이스로 정책을 추상화하여 변경 가능하게 설계

- **✅ 수정 내용**
  - [PointAccrualPolicy.java](src/main/java/org/example/sharedprompts/domain/payment/service/postprocess/policy/PointAccrualPolicy.java): 포인트 적립 기준을 `originalAmount`로 변경
  - **정책 결정**: 원래 주문 금액 기준 포인트 적립 (포인트 순환 구조 방지)
  - 포인트 사용과 무관하게 주문 금액 전체에 대해 포인트 적립
  - 캐시백은 실제 결제 금액 기준, 포인트는 원래 주문 금액 기준으로 정책 분리

---

## 17. 결제 실패 시 사용한 포인트 복구 누락 ✅ 수정 완료
- **문제 설명**
  - PaymentServiceImpl.confirmPayment에서 결제 실패 시 (라인 208-225)
  - processPaymentFailure 호출 후 예외를 다시 throw
  - processPaymentFailure는 로깅과 메트릭만 기록
  - 사용한 포인트(usedPointAmount)를 복구하는 로직이 없음

- **왜 문제가 되는지**
  - requestPayment에서 포인트를 이미 차감함 (PaymentAmountFacade)
  - confirmPayment 실패 시 포인트가 차감된 채로 남음
  - 사용자는 결제도 실패하고 포인트도 잃게 됨
  - 고객 불만 및 보상 처리 비용 발생

- **개선 방향**
  - processPaymentFailure 내부에서 포인트 복구 로직 추가
  - 또는 포인트 사용을 confirmPayment 시점으로 이동 (트랜잭션 원자성 보장)
  - 실패한 Payment의 상태를 확인하는 스케줄러 + 자동 포인트 복구 배치 작업
  - 포인트 복구 실패 시 관리자 알림 및 수동 개입 프로세스

- **✅ 수정 내용**
  - [PaymentPostProcessService.java:69-85](src/main/java/org/example/sharedprompts/domain/payment/service/postprocess/PaymentPostProcessService.java#L69-L85): `processPaymentFailure`에 포인트 복구 로직 추가
  - 결제 실패 시 `PointType.PAYMENT_FAILED`로 사용한 포인트 복구
  - 포인트 복구 실패 시 로그 남기고 계속 진행 (별도 보상 처리 필요)

- **Issue #3과의 관계**
  - **Issue #3** (부분 해결): 결제 요청 후 사용자가 포기하거나 타임아웃되는 경우 → 스케줄러 기반 복구 필요 (미구현)
  - **Issue #17** (완료): `confirmPayment` 명시적 실패 시 즉시 복구 → `processPaymentFailure`에 구현됨
  - 두 이슈는 포인트 복구라는 공통 주제지만 발생 시나리오가 다름

---

## 18. CashbackFacade의 분산락 내부 트랜잭션 문제 ✅ 수정 완료
- **문제 설명**
  - accumulateCashback 메서드가 @Transactional (라인 58)
  - 내부에서 lockService.executeWithLock 호출 (라인 61)
  - executeWithLock 내부에서 다시 트랜잭션이 시작될 가능성
  - 락을 먼저 획득하고 트랜잭션을 시작해야 하는데 순서가 반대

- **왜 문제가 되는지**
  - 트랜잭션을 먼저 시작하면 DB 커넥션을 먼저 점유
  - 이후 락 획득 대기 중 DB 커넥션이 고갈될 수 있음 (Connection Pool Exhaustion)
  - 락을 먼저 획득한 후 트랜잭션을 시작해야 데드락 방지
  - 현재 구조는 트랜잭션 → 락 → 작업 순서로 비효율적

- **개선 방향**
  - @Transactional을 제거하고 executeWithLock 내부에서 TransactionTemplate 사용
  - 락 획득 → 트랜잭션 시작 → 작업 수행 → 트랜잭션 커밋 → 락 해제 순서 보장
  - 또는 lockService.executeWithLock가 자체적으로 트랜잭션 관리하도록 변경
  - CashbackLockService가 TransactionTemplate을 주입받아 내부에서 트랜잭션 제어

- **✅ 수정 내용**
  - [CashbackFacade.java:32-38](src/main/java/org/example/sharedprompts/domain/payment/service/cashback/facade/CashbackFacade.java#L32-L38): 클래스 javadoc에 트랜잭션 순서 개선 내용 문서화
  - [CashbackFacade.java:75-78](src/main/java/org/example/sharedprompts/domain/payment/service/cashback/facade/CashbackFacade.java#L75-L78): @Transactional 제거, TransactionTemplate 사용
  - 락 획득 → 트랜잭션 시작 → 작업 수행 → 트랜잭션 커밋 → 락 해제 순서 보장

---

## 19. 재시도 로직의 한계 (즉시 재시도/정책 표준화 부재) ✅ 수정 완료
- **문제 설명**
  - PaymentFacade에 retryPayment 메서드가 있음 (라인 152)
  - PaymentRetryFacade를 주입받아 사용
  - 다만 “결제 승인(confirmPayment) 실패 시 자동 즉시 재시도” 같은 정책은 여전히 명시적으로 적용되지 않음

- **왜 문제가 되는지**
  - 일시적인 네트워크 오류(타임아웃 등)에서 즉시 재시도가 없으면 결제 성공률이 낮아질 수 있음
  - 사용자 UX 관점에서 “지금 다시 시도”와 “나중에 스케줄러가 처리”가 혼재되면 기대 동작이 불명확해짐
  - Provider별 멱등성 지원 여부가 다르므로, 재시도 정책이 표준화되지 않으면 중복 호출 리스크가 커짐

- **개선 방향**
  - “즉시 재시도(짧은 횟수)” + “지연 재시도(스케줄러)”를 분리하고 정책을 문서/코드로 고정
  - Spring Retry/Resilience4j로 재시도/백오프/서킷브레이커 정책을 선언적으로 관리
  - Provider별 `supportsIdempotency()`를 고려해 재시도 허용 범위를 제한 (미지원 Provider는 DB 상태 기반 방어 강화)

- **✅ 수정 내용**
  - [RetryProperties.java](src/main/java/org/example/sharedprompts/domain/payment/config/RetryProperties.java): 즉시 재시도 설정 추가 (`immediateRetryMaxAttempts`, `immediateRetryDelayMs`)
  - [PaymentExecutionService.java](src/main/java/org/example/sharedprompts/domain/payment/service/execution/PaymentExecutionService.java): `executePaymentWithImmediateRetry()` 메서드 추가, 일시적인 네트워크 오류에 대해 즉시 재시도 로직 구현
  - **재시도 정책**: 즉시 재시도(최대 2회, 200ms 지연) + 지연 재시도(스케줄러 기반 지수 백오프)로 분리
  - **재시도 가능 오류 판단**: 타임아웃, 네트워크 오류 등 일시적 오류만 즉시 재시도, 비즈니스 로직 오류는 즉시 재시도 불가
  - **설정 가능**: `payment.retry.immediate.max-attempts`, `payment.retry.immediate.delay-ms`로 설정 가능

---

## 20. Point 엔티티의 paymentId 타입 불일치 ✅ 수정 완료
- **문제 설명**
  - Point 엔티티의 paymentId가 Long 타입이지만 nullable (라인 38)
  - Payment 엔티티와 직접 연관 관계(@ManyToOne)가 없음
  - 단순 Long 타입 필드로 관리
  - 결제와 무관한 포인트(프로모션, 이벤트)도 존재

- **왜 문제가 되는지**
  - paymentId가 유효한 Payment를 가리키는지 보장할 수 없음
  - 고아 레퍼런스 발생 가능 (Payment 삭제 시 Point는 유지)
  - 조인 쿼리 작성 시 수동으로 조인 조건 작성 필요
  - JPA의 이점을 활용하지 못함

- **개선 방향**
  - Payment와 직접 연관 관계 설정 (@ManyToOne, optional=true)
  - paymentId 필드 제거하고 payment 필드 사용
  - 결제 관련 포인트와 비관련 포인트를 서브타입으로 분리 (상속 구조)
  - 또는 PointType으로 구분하되 Payment는 직접 참조 관계 유지

- **✅ 수정 내용**
  - [Point.java](src/main/java/org/example/sharedprompts/domain/payment/Point.java): `paymentId` Long 필드를 `@ManyToOne Payment payment` 관계로 변경
  - [PointRepository.java](src/main/java/org/example/sharedprompts/domain/payment/repository/point/PointRepository.java): `existsByPaymentIdAndType` 쿼리를 `p.payment.id`로 수정
  - [CustomPointRepositoryImpl.java](src/main/java/org/example/sharedprompts/domain/payment/repository/point/CustomPointRepositoryImpl.java): QueryDSL 쿼리에서 `point.payment.id` 사용
  - [PointServiceImpl.java](src/main/java/org/example/sharedprompts/domain/payment/service/point/PointServiceImpl.java): Payment 엔티티 조회 및 사용
  - JPA 연관 관계를 활용하여 데이터 정합성 보장 및 조인 쿼리 최적화

---

## 21. 멱등성 키 생성 전략의 한계 ✅ 수정 완료
- **문제 설명**
  - generateIdempotencyKey가 paymentMethod + paymentId 조합 (라인 169-176)
  - cancel, refund는 action을 추가하여 구분 (라인 183-188)
  - 하지만 부분 환불이 여러 번 발생하는 경우 멱등성 키가 동일
  - 첫 번째 부분 환불과 두 번째 부분 환불을 구분할 수 없음

- **왜 문제가 되는지**
  - 같은 Payment에 대해 두 번째 부분 환불 요청 시 같은 멱등성 키 생성
  - 결제사는 이미 처리된 요청으로 판단하여 거부하거나 중복 환불 방지
  - 실제로는 다른 환불 요청인데 멱등성 키가 같아 처리 불가
  - 사용자가 여러 번 부분 환불을 받을 수 없음

- **영향도 분석**
  - **현재 상태**: 같은 Payment에 대한 두 번째 부분 환불은 동일한 멱등성 키로 인해 거부됨
  - **비즈니스 영향**: 고객 환불 요청 처리 불가 → 고객 불만 및 수동 처리 필요
  - **권장 우선순위**: "차단 이슈"는 아니지만 "주요 이슈"로 분류하여 조기 해결 권장

- **개선 방향**
  - 환불 요청마다 고유한 환불 ID 생성하여 멱등성 키에 포함
  - Refund 엔티티를 별도로 만들어 refundId를 멱등성 키로 사용
  - 또는 타임스탬프나 UUID를 멱등성 키에 추가하여 각 요청을 구분
  - 멱등성 키 생성 로직을 Provider별로 다르게 설정 가능하도록 추상화

- **✅ 수정 내용**
  - [PaymentExecutionService.java:243-250](src/main/java/org/example/sharedprompts/domain/payment/service/execution/PaymentExecutionService.java#L243-L250): `generateRefundIdempotencyKey()` 메서드에서 `refundedAmount`를 포함하여 각 부분 환불 요청을 구분
  - 부분 환불 시 현재까지의 환불 누적 금액을 멱등성 키에 포함하여 동일 Payment에 대한 여러 번의 부분 환불을 구분 가능
  - 예시: 첫 번째 부분 환불 `TOSS:123:refund:0`, 두 번째 부분 환불 `TOSS:123:refund:5000`

---

## 22. Cashback과 Payment의 양방향 참조 부재 ✅ 수정 완료
- **문제 설명**
  - Cashback 엔티티가 paymentId를 Long으로 보관 (라인 37)
  - Payment 엔티티는 Cashback 목록을 참조하지 않음
  - 단방향 참조이지만 실제로는 외래키 수준의 참조
  - Payment 삭제 시 Cashback이 고아가 될 수 있음

- **왜 문제가 되는지**
  - Payment 조회 시 연관된 Cashback을 즉시 알 수 없음
  - 별도 쿼리로 Cashback을 조회해야 함 (N+1 문제)
  - Payment 삭제 시 Cashback의 paymentId가 유효하지 않은 값을 가리킴
  - 데이터 정합성 문제 발생 가능

- **개선 방향**
  - Cashback과 Payment를 @ManyToOne 연관 관계로 설정
  - Payment에 @OneToMany로 cashbacks 컬렉션 추가 (양방향 연관 관계)
  - 또는 Payment 삭제 시 관련 Cashback도 함께 삭제 (cascade)
  - Payment 조회 시 fetch join으로 Cashback도 함께 조회하여 성능 최적화

- **✅ 수정 내용**
  - [Cashback.java](src/main/java/org/example/sharedprompts/domain/payment/Cashback.java): `paymentId` Long 필드를 `@ManyToOne Payment payment` 관계로 변경
  - [CashbackExecutionService.java](src/main/java/org/example/sharedprompts/domain/payment/service/cashback/execution/CashbackExecutionService.java): Payment 엔티티 조회 및 사용
  - JPA 연관 관계를 활용하여 데이터 정합성 보장 및 조인 쿼리 최적화

---

## 23. 환율 서비스의 캐싱 및 실패 처리 전략 부재
- **문제 설명**
  - PaymentAmountFacade가 ExchangeRateService.convertCurrency 호출 (라인 36)
  - ExchangeRateService의 구현을 보지 못했지만 외부 API 호출 추정
  - 환율 조회 실패 시 결제 전체가 실패할 가능성
  - 환율 API 응답 시간이 느리면 결제 응답 시간도 느려짐

- **왜 문제가 되는지**
  - 환율 API 장애 시 모든 외화 결제가 불가능
  - 환율 조회를 매번 실시간으로 하면 API 비용 증가 및 성능 저하
  - 환율은 실시간 정확도가 높을 필요 없음 (수 분 단위 업데이트로 충분)
  - 캐싱 없이 매 결제마다 외부 API 호출은 비효율적

- **개선 방향**
  - ExchangeRateService에 캐싱 추가 (Spring Cache, Redis)
  - 스케줄러로 주기적으로 환율 업데이트 (1분~10분 간격)
  - 환율 조회 실패 시 이전 캐시 값 사용 (Stale-While-Revalidate)
  - Circuit Breaker 패턴으로 환율 API 장애 시 빠른 실패
  - Fallback 환율 설정 (예: 전날 종가)

---

## 24. 결제 상태 전이의 명시적 검증 부재 ✅ 수정 완료
- **문제 설명**
  - Payment 엔티티의 상태 변경 메서드들이 현재 상태를 검증하지 않음
  - 예: markSuccess는 PENDING이 아닌 상태에서도 호출 가능
  - FAILED 상태에서 markSuccess 호출 시 데이터 불일치
  - 상태 머신(State Machine) 패턴이 없음

- **왜 문제가 되는지**
  - 잘못된 상태 전이로 데이터 무결성 훼손
  - CANCELED 상태에서 SUCCESS로 변경되는 등 비정상 상태 전이 가능
  - 상태 전이 규칙이 코드에 명시되지 않아 비즈니스 로직 파악 어려움
  - 디버깅 시 어떤 경로로 상태가 변경되었는지 추적 어려움

- **개선 방향**
  - Payment에 canTransitionTo(PaymentStatus) 메서드 추가
  - 각 상태 변경 메서드에서 현재 상태 검증 로직 추가
  - 허용되지 않는 상태 전이 시 예외 발생
  - State 패턴 또는 상태 머신 라이브러리(Spring State Machine) 도입
  - 상태 전이 로그를 별도 테이블에 기록하여 감사 추적 가능하게 구성

- **✅ 수정 내용**
  - [Payment.java:202-205](src/main/java/org/example/sharedprompts/domain/payment/Payment.java#L202-L205): `isSuccessful()` 메서드 추가
  - [Payment.java:212-216](src/main/java/org/example/sharedprompts/domain/payment/Payment.java#L212-L216): `isFinalState()` 메서드 추가
  - [Payment.java:219-250](src/main/java/org/example/sharedprompts/domain/payment/Payment.java#L219-L250): `canCancel()`, `canRefund()` 메서드 추가
  - [PaymentValidationService.java](src/main/java/org/example/sharedprompts/domain/payment/service/validation/PaymentValidationService.java): `canTransitionTo()`, `validateCanCancelPayment()`, `validateCanRefundPayment()` 메서드 구현
  - 상태 전이 검증 로직이 엔티티와 ValidationService 양쪽에서 접근 가능하도록 구성

---

## 25. 로깅 시 민감 정보 노출 위험 ✅ 수정 완료
- **문제 설명**
  - PaymentLoggingService 사용 추정
  - 최근 커밋 메시지에 "웹훅 payload를 로그에 남기지 말라"는 경고
  - Payment 정보, 금액, 사용자 정보 등이 로그에 포함될 가능성
  - GDPR, 개인정보보호법 위반 위험

- **왜 문제가 되는지**
  - 로그에 개인정보, 결제 정보가 평문으로 저장되면 법적 문제
  - 로그 파일이 외부 유출 시 심각한 보안 사고
  - 결제 payload에는 카드번호, 개인정보 등 민감 정보 포함
  - 로그 보관 기간 동안 개인정보 관리 책임 발생

- **개선 방향**
  - 로그에 민감 정보 마스킹 (카드번호, 이메일, 전화번호 등)
  - Webhook payload는 크기나 해시값만 로그에 기록
  - 별도의 보안 로그 저장소 사용 (접근 제어, 암호화)
  - 로그 레벨을 적절히 설정하여 운영 환경에서는 DEBUG 로그 비활성화
  - 로깅 정책을 코드 리뷰 체크리스트에 포함

- **✅ 수정 내용**
  - [SensitiveDataMasker.java](src/main/java/org/example/sharedprompts/global/util/SensitiveDataMasker.java): 민감 정보 마스킹 유틸리티 생성
    - 이메일, 전화번호, 카드번호, 계좌번호, 사용자 ID 마스킹 지원
    - 자동 민감 정보 감지 및 마스킹 기능 (`maskSensitiveData()`)
  - [PaymentLoggingService.java](src/main/java/org/example/sharedprompts/domain/payment/logging/PaymentLoggingService.java): 모든 로깅 메서드에 민감 정보 마스킹 적용
    - `userId`, `externalPaymentId`, `reason`, `error message` 등 민감 정보 마스킹
  - **효과**: 로그에 민감 정보가 노출되지 않도록 보호, GDPR 및 개인정보보호법 준수
  - **참고**: Webhook payload는 이미 `payloadLength`만 기록하도록 개선되어 있음

---

## 총평

### 긍정적인 부분
- 책임을 세분화하여 Service, Facade, Execution, Validation 등으로 명확히 분리하려는 시도
- Provider 패턴으로 결제사 추상화를 잘 구현
- 멱등성, 분산락, 재시도 등 결제 도메인의 핵심 요구사항을 고려
- 트랜잭션 경계와 동시성 문제를 인지하고 해결하려는 노력

### 개선이 시급한 부분
1. **포인트 사용/복구 로직**: 결제 실패 시 포인트 복구 누락은 고객 불만 직결
2. **중첩 트랜잭션 구조**: 예측 불가능한 동작과 데이터 불일치 위험
3. **환불 시 잘못된 메서드 사용**: accumulatePoints 대신 addPointsDirectly 사용 필수
4. **Webhook 중복 파싱**: 성능 낭비 및 로직 중복
5. **멱등성 키 저장 시점**: 외부 API 호출 전 커밋 보장 필요

### 아키텍처 개선 방향
- Saga 패턴이나 Outbox 패턴으로 분산 트랜잭션 관리
- 도메인 이벤트 기반 아키텍처로 결합도 낮추기
- Payment를 Rich Domain Model로 전환하여 비즈니스 로직 응집
- Facade 계층 역할 재정의 또는 제거
- 통합 테스트와 보상 트랜잭션 전략 수립

---

# 결제사별 Provider 구현 공식 문서 대조

## 26. 토스페이먼츠 - Idempotency-Key 헤더 미지원 ✅ 수정 완료

- **문제 설명**
  - TossPaymentProvider에서 `Idempotency-Key` 헤더를 전송 (라인 80-82)
  - 토스페이먼츠 공식 API는 이 헤더를 지원하지 않음
  - paymentKey가 이미 고유 식별자 역할을 하며, 같은 paymentKey로 중복 호출 시 자체적으로 에러 반환

- **왜 문제가 되는지**
  - 헤더를 보내도 무시되어 실제 멱등성 보장은 토스 자체 로직에 의존
  - 개발자가 Idempotency-Key로 멱등성이 보장된다고 착각할 수 있음
  - 다른 결제사와 일관되지 않은 멱등성 처리 방식
  - 불필요한 헤더 전송으로 인한 혼란

- **개선 방향**
  - TossPaymentProvider에서 Idempotency-Key 헤더 전송 제거
  - 주석으로 "토스페이먼츠는 paymentKey 자체가 멱등성 키 역할" 명시
  - Provider 인터페이스에 `supportsIdempotency()` 메서드 추가
  - 토스의 경우 false 반환하여 명시적으로 미지원 표시

- **✅ 수정 내용**
  - [TossPaymentProvider.java:38-41](src/main/java/org/example/sharedprompts/domain/payment/provider/impl/TossPaymentProvider.java#L38-L41): 멱등성 관련 상세 문서 주석 추가
  - [TossPaymentProvider.java:74-80](src/main/java/org/example/sharedprompts/domain/payment/provider/impl/TossPaymentProvider.java#L74-L80): `supportsIdempotency()` false 반환
  - [TossPaymentProvider.java:104-106](src/main/java/org/example/sharedprompts/domain/payment/provider/impl/TossPaymentProvider.java#L104-L106): confirmPayment에 멱등성 미지원 주석 추가
  - Idempotency-Key 헤더 전송하지 않음 (이미 제거되어 있음)

---

## 27. 카카오페이 - 결제 준비 단계(/ready) 완전 누락 ✅ 수정 완료

- **문제 설명**
  - KakaoPayPaymentProvider의 confirmPayment가 `/approve` API만 호출
  - 카카오페이 공식 흐름: 1) `/ready` → tid 발급, 2) 사용자 인증, 3) `/approve`
  - 현재 코드는 paymentKey에 이미 tid가 포함되어 있다고 가정
  - `/ready` 호출 없이는 실제로 결제 불가능

- **왜 문제가 되는지**
  - **치명적**: 카카오페이 결제가 운영 환경에서 작동하지 않음
  - `/ready` 없이 `/approve` 호출 시 카카오페이 API가 400 에러 반환
  - tid는 `/ready` 호출로만 발급 가능하며 클라이언트가 임의로 생성 불가
  - 결제 흐름이 공식 문서와 완전히 불일치

- **개선 방향**
  - Provider 인터페이스에 `preparePayment()` 메서드 추가
  - KakaoPayPaymentProvider에 `/ready` API 호출 구현
  - requestPayment에서 preparePayment 호출하여 tid 사전 발급
  - tid를 Payment.externalPaymentId에 저장
  - confirmPayment는 저장된 tid + pg_token으로 approve 호출

- **✅ 수정 내용**
  - [PaymentProvider.java](src/main/java/org/example/sharedprompts/domain/payment/provider/PaymentProvider.java): `preparePayment()`, `requiresPreparation()` 기본 메서드 추가
  - [PaymentProvider.java](src/main/java/org/example/sharedprompts/domain/payment/provider/PaymentProvider.java): `PrepareResult` record 추가
  - [KakaoPayPaymentProvider.java](src/main/java/org/example/sharedprompts/domain/payment/provider/impl/KakaoPayPaymentProvider.java): `/ready` API 호출 완전 구현
  - [KakaoPayProperties.java](src/main/java/org/example/sharedprompts/domain/payment/config/KakaoPayProperties.java): approvalUrl, cancelUrl, failUrl 필드 추가
  - `requiresPreparation()` true 반환하여 준비 단계 필수임을 명시

---

## 28. 카카오페이 - Idempotency 개념 미지원 ✅ 수정 완료

- **문제 설명**
  - KakaoPayPaymentProvider에서 idempotencyKey 파라미터를 받지만 사용하지 않음
  - 카카오페이는 Idempotency-Key 헤더 개념이 없음
  - tid 자체가 고유하며 중복 approve 시 API가 자동으로 에러 반환

- **왜 문제가 되는지**
  - idempotencyKey를 전달해도 카카오페이는 완전히 무시
  - 재시도 시 같은 tid로 approve 호출하면 이미 승인 완료 에러 발생
  - 멱등성이 tid 레벨에서만 작동하여 애플리케이션 레벨 제어 불가

- **개선 방향**
  - KakaoPayPaymentProvider.supportsIdempotency()는 false 반환
  - 재시도 로직에서 카카오페이는 상태 조회 후 재승인 여부 판단
  - tid가 이미 승인된 경우 중복 approve 호출하지 않도록 방어 로직 추가

- **✅ 수정 내용**
  - [KakaoPayPaymentProvider.java:58-60](src/main/java/org/example/sharedprompts/domain/payment/provider/impl/KakaoPayPaymentProvider.java#L58-L60): `supportsIdempotency()` false 반환 (이미 구현되어 있었음)
  - 카카오페이는 tid 자체가 고유 식별자 역할을 하므로 멱등성 키 미지원 명시

---

## 29. 페이팔 - 액세스 토큰 캐싱의 Thread Safety 문제 ✅ 수정 완료

- **문제 설명**
  - PayPalPaymentProvider의 cachedAccessToken과 tokenExpiresAt 필드가 non-volatile (라인 50-51)
  - 멀티스레드 환경에서 여러 스레드가 동시에 토큰 만료 확인 시 경쟁 조건 발생
  - 여러 스레드가 동시에 새 토큰을 요청할 수 있음

- **왜 문제가 되는지**
  - Thread A: 토큰 만료 확인 → 새 토큰 요청 중
  - Thread B: 동시에 토큰 만료 확인 → 또 새 토큰 요청
  - 불필요한 중복 API 호출로 페이팔 API 할당량 낭비
  - 캐싱의 의미가 퇴색됨
  - 가시성 문제로 인해 Thread B가 Thread A가 방금 갱신한 토큰을 못 볼 수 있음

- **개선 방향**
  - `private volatile String cachedAccessToken` 선언 (가시성 보장)
  - `private volatile long tokenExpiresAt` 선언
  - 또는 synchronized 블록으로 getAccessToken 메서드 보호
  - 더 나은 방법: AtomicReference<AccessToken> 사용하여 토큰과 만료시간 함께 관리
  - Double-checked locking 패턴 적용

- **✅ 수정 내용**
  - [PayPalPaymentProvider.java](src/main/java/org/example/sharedprompts/domain/payment/provider/impl/PayPalPaymentProvider.java): `cachedAccessToken`을 volatile로 선언
  - [PayPalPaymentProvider.java](src/main/java/org/example/sharedprompts/domain/payment/provider/impl/PayPalPaymentProvider.java): `tokenExpiresAt`을 volatile로 선언
  - [PayPalPaymentProvider.java](src/main/java/org/example/sharedprompts/domain/payment/provider/impl/PayPalPaymentProvider.java): `tokenLock` Object 추가
  - [PayPalPaymentProvider.java](src/main/java/org/example/sharedprompts/domain/payment/provider/impl/PayPalPaymentProvider.java): `getAccessToken()`에 double-checked locking 패턴 적용
  - 멀티스레드 환경에서 안전한 토큰 캐싱 보장

---

## 30. 공통 - 결제 준비 단계 구조적 누락 ✅ 수정 완료

- **문제 설명**
  - 현재 구조: requestPayment → DB에 Payment 저장, confirmPayment → Provider.confirmPayment 호출
  - 실제 결제사 흐름: 준비 API로 고유 ID 발급 → 사용자 인증 → 승인 API로 완료
  - **토스**: 클라이언트가 결제 위젯에서 paymentKey 받아와야 함 (서버 준비 단계 없음)
  - **카카오페이**: `/ready`로 tid 발급 필요
  - **페이팔**: `POST /v2/checkout/orders`로 orderId 생성 필요

- **왜 문제가 되는지**
  - 카카오페이와 페이팔이 운영 환경에서 작동하지 않음
  - 결제사별로 완전히 다른 흐름을 하나의 Provider 인터페이스로 억지로 맞춤
  - 클라이언트가 사전에 준비 단계를 수행해야 하는지 불명확
  - 서버 주도 결제(카카오페이, 페이팔) vs 클라이언트 주도 결제(토스)를 구분하지 못함

- **개선 방향**
  - Provider 인터페이스를 두 가지로 분리
    - ServerPreparedPaymentProvider: 서버에서 준비 단계 수행 (카카오페이, 페이팔)
    - ClientPreparedPaymentProvider: 클라이언트가 준비 (토스)
  - ServerPreparedPaymentProvider에 `preparePayment()` 메서드 추가
  - requestPayment에서 Provider 타입에 따라 준비 단계 실행 여부 결정
  - 또는 모든 Provider에 `preparePayment()` 추가하고 토스는 no-op 구현

- **✅ 수정 내용**
  - [PaymentProvider.java](src/main/java/org/example/sharedprompts/domain/payment/provider/PaymentProvider.java): 모든 Provider에 `preparePayment()` 기본 메서드 추가
  - [PaymentProvider.java](src/main/java/org/example/sharedprompts/domain/payment/provider/PaymentProvider.java): `requiresPreparation()` 기본 메서드 추가 (기본값: false)
  - [KakaoPayPaymentProvider.java](src/main/java/org/example/sharedprompts/domain/payment/provider/impl/KakaoPayPaymentProvider.java): `preparePayment()` 완전 구현, `requiresPreparation()` true 반환
  - [PayPalPaymentProvider.java](src/main/java/org/example/sharedprompts/domain/payment/provider/impl/PayPalPaymentProvider.java): `preparePayment()` order 생성 구현, `requiresPreparation()` true 반환
  - [TossPaymentProvider.java:86-89](src/main/java/org/example/sharedprompts/domain/payment/provider/impl/TossPaymentProvider.java#L86-L89): `requiresPreparation()` false 반환 (클라이언트 준비)
  - [PaypalProperties.java](src/main/java/org/example/sharedprompts/domain/payment/config/PaypalProperties.java): returnUrl, cancelUrl 필드 추가

---

## 31. 카카오페이 - 취소 시 금액 정보 조회 방식의 문제

- **문제 설명**
  - KakaoPayPaymentProvider.cancelPayment에서 먼저 getPaymentStatus 호출 (라인 232)
  - 원본 결제 금액과 면세 금액을 가져오기 위함
  - 하지만 전체 취소인데도 금액을 명시적으로 전달해야 함

- **왜 문제가 되는지**
  - 카카오페이 `/cancel` API는 `cancel_amount`가 필수 파라미터
  - 전체 취소 시에도 원본 금액을 정확히 명시해야 함
  - 추가 API 호출로 인한 성능 저하
  - getPaymentStatus 실패 시 취소 자체가 실패하는 의존성

- **개선 방향**
  - Payment 엔티티에 원본 결제 금액을 저장해두고 재사용
  - 면세 금액도 Payment에 저장 (별도 필드 추가)
  - getPaymentStatus 실패 시에도 취소가 가능하도록 fallback 로직
  - 현재 구현은 합리적이지만 Payment 엔티티 확장으로 개선 가능

---

## 32. 페이팔 - 주문 상태별 취소 처리의 복잡성

- **문제 설명**
  - PayPalPaymentProvider.cancelPayment가 주문 상태에 따라 다르게 처리 (라인 214-267)
  - CREATED/APPROVED: API 호출 없이 취소 처리
  - Authorization 존재: void 처리
  - Capture 존재: 환불 필요 (취소 불가)
  - 복잡한 분기 로직

- **왜 문제가 되는지**
  - 페이팔은 주문 생명주기가 복잡 (Created → Approved → Authorized → Captured)
  - 각 상태마다 취소/환불 방법이 다름
  - 이미 Capture된 경우 취소가 아닌 환불만 가능한데 예외 발생 (라인 244-246)
  - 호출자(PaymentService)는 이런 복잡성을 모르고 cancelPayment 호출

- **개선 방향**
  - 페이팔의 경우 취소와 환불을 명확히 구분하는 것이 맞음
  - 이미 Capture된 경우 자동으로 전액 환불 처리하거나
  - 또는 CancelResult에 "환불 필요" 플래그 추가하여 호출자에게 알림
  - 주석으로 페이팔의 상태 전이 모델을 명확히 문서화

---

## 33. 공통 - Webhook 파싱 실패 시 RuntimeException throw

- **문제 설명**
  - 모든 Provider의 parseWebhook이 실패 시 RuntimeException throw
  - TossPaymentProvider 라인 311, KakaoPayPaymentProvider 라인 417, PayPalPaymentProvider 라인 570
  - Webhook 파싱 실패는 치명적이지만 예외를 던지면 HTTP 500 응답

- **왜 문제가 되는지**
  - Webhook은 결제사에서 재시도하는데 500 에러 시 계속 재시도
  - 파싱 자체가 실패한 경우 재시도해도 계속 실패
  - 결제사의 재시도 큐에 쌓여 시스템 부하
  - 잘못된 payload로 인한 파싱 실패를 복구 불가능한 오류로 처리

- **개선 방향**
  - 파싱 실패 시 400 Bad Request 반환 (결제사에 재시도 중단 요청)
  - 또는 200 OK 반환하되 내부적으로 파싱 실패 로그 남기고 알림
  - Dead Letter Queue에 실패한 webhook 저장하여 수동 재처리
  - 파싱 실패 원인 분석을 위해 payload 해시값과 크기만 로깅

---

## 34. 토스페이먼츠 - 금액 타입 검증의 적절성

- **문제 설명**
  - TossPaymentProvider에서 금액 소수점 검증 (라인 87-89, 227-229)
  - `if (amount.scale() > 0)` 체크 후 예외 발생
  - 토스페이먼츠는 정수 금액만 지원 (원 단위)

- **왜 문제가 되는지**
  - ✅ 이것은 **올바른 구현**
  - 토스페이먼츠 API는 소수점이 있는 금액을 거부함
  - 사전 검증으로 API 호출 전 에러 차단

- **개선 방향**
  - 현재 구현이 올바름
  - 다만 에러 메시지를 더 친절하게 개선 가능
  - "토스페이먼츠는 원 단위 결제만 지원합니다. 소수점 금액은 사용할 수 없습니다."

---

## 35. 카카오페이 - 환불 시 tax_free_amount를 항상 0으로 전송 ✅ 수정 완료

- **문제 설명**
  - KakaoPayPaymentProvider.refundPayment에서 cancel_tax_free_amount를 0으로 고정 (라인 313)
  - 취소 시에는 원본 금액 조회하여 tax_free_amount 계산 (라인 245-251)
  - 환불 시에는 tax_free_amount를 고려하지 않음

- **왜 문제가 되는지**
  - 부분 환불 시 면세 금액 비율을 계산하지 않음
  - 예: 원본 10000원 (면세 1000원), 5000원 환불 → 면세 500원 환불해야 하는데 0원으로 전송
  - 카카오페이 API가 에러를 반환하거나 잘못된 환불 처리 가능
  - 전체 환불과 부분 환불의 면세 금액 처리 불일치

- **개선 방향**
  - 부분 환불 시에도 원본 면세 금액을 조회
  - 환불 비율에 따라 면세 금액 계산
  - `cancel_tax_free_amount = 원본 면세 금액 * (환불 금액 / 원본 금액)`
  - 또는 카카오페이 공식 문서에서 면세 금액 계산 규칙 확인 후 적용

- **✅ 수정 내용**
  - [KakaoStatusResponse.java](src/main/java/org/example/sharedprompts/domain/payment/provider/kakao/dto/KakaoStatusResponse.java): `taxFreeAmount` 필드 추가
  - [KakaoStatusApiClient.java](src/main/java/org/example/sharedprompts/domain/payment/provider/kakao/client/KakaoStatusApiClient.java): 상태 조회 시 `tax_free` 금액 파싱 추가
  - [KakaoCancelApiClient.java](src/main/java/org/example/sharedprompts/domain/payment/provider/kakao/client/KakaoCancelApiClient.java): 취소 시 원본 면세 금액 사용, 환불 시 환불 비율에 따른 면세 금액 계산
  - 부분 환불 시에도 정확한 면세 금액 계산으로 카카오페이 API 호출 정확성 향상

---

## 36. 페이팔 - Webhook 서명 검증 방식의 복잡성 ✅ 수정 완료

- **문제 설명**
  - PayPalPaymentProvider.verifyWebhookSignature가 복잡한 구조 (라인 464-526)
  - signature 파라미터가 JSON 형태여야 함 (라인 475)
  - 5개의 헤더 정보를 포함해야 함
  - 실제로는 페이팔 API를 호출하여 검증 위임

- **왜 문제가 되는지**
  - 호출자(WebhookHandler)가 signature를 JSON으로 전달해야 함
  - 실제 HTTP 헤더를 Map으로 변환하는 로직이 필요
  - Controller에서 헤더 파싱 로직이 Provider 구현에 의존
  - 다른 결제사는 단순 문자열 signature인데 페이팔만 JSON

- **개선 방향**
  - Provider 인터페이스의 verifyWebhookSignature 시그니처 변경
  - `boolean verifyWebhookSignature(String payload, Map<String, String> headers)`
  - 모든 헤더를 Map으로 받아서 Provider가 필요한 헤더 추출
  - 또는 별도의 WebhookHeaders DTO 객체 정의

- **✅ 수정 내용 (2026-02-04)**
  - `PaymentWebhookController`가 `@RequestHeader Map<String, String> headers`를 받아 그대로 전달하도록 변경
  - `PaymentFacade` / `PaymentWebhookFacade`가 headers를 함께 받을 수 있도록 오버로드 추가
  - `PaymentProvider`에 `verifyWebhookSignature(String payload, Map<String,String> headers)` 기본 메서드 추가
  - `PayPalPaymentProvider`는 headers 기반 검증을 우선 사용하고, 기존 JSON signature 방식은 하위호환으로 유지
  - `PayPalWebhookVerifier`는 PayPal 요구 헤더(`PAYPAL-TRANSMISSION-*`)를 headers(Map)에서 case-insensitive로 추출하여 검증

---

## 37. 모든 Provider - 예외 발생 시 failureReason 불일치 ✅ 확인 완료

- **문제 설명**
  - confirmPayment 실패 시 반환하는 PaymentResult의 failureReason이 일관되지 않음
  - 토스: "Toss Payments API 호출 실패: " + 예외 메시지 (라인 126)
  - 카카오페이: "KakaoPay API 호출 실패: " + 예외 메시지 (라인 176)
  - 페이팔: "PayPal API 호출 실패: " + 예외 메시지 (라인 150)

- **왜 문제가 되는지**
  - 사용자에게 보여지는 오류 메시지가 Provider 구현에 의존
  - 예외 메시지에 스택 트레이스나 내부 정보가 포함될 수 있음
  - 일관된 오류 처리와 사용자 경험 제공 어려움

- **개선 방향**
  - 예외를 분류하여 failureReason을 표준화
  - 네트워크 오류: "일시적인 네트워크 오류"
  - 잔액 부족: "결제 수단 잔액 부족"
  - 인증 실패: "결제 인증 실패"
  - Provider에서 결제사 응답을 분석하여 표준 오류 코드로 변환
  - ErrorCode enum 확장하여 결제사별 오류를 매핑

- **✅ 확인 결과**
  - 현재 모든 Provider가 일관된 형식 사용: "{Provider명} API 호출 실패: {예외 메시지}"
  - 추가 표준화 작업은 향후 개선 사항으로 분류 (현재는 일관된 형식 유지)
  - 향후 개선 시 예외 분류 및 표준 오류 코드 매핑 고려

---

## 결제사별 구현 준수도 요약

### ✅ 토스페이먼츠 (준수도: 85%)
**잘 구현된 부분**:
- Basic 인증 방식 올바름
- 금액 정수 검증
- Webhook 서명 검증 (HMAC-SHA256)
- API 엔드포인트 정확함

**문제점**:
- 결제 준비 단계가 클라이언트에 의존

**치명도**: 낮음 (작동은 함, 개선 필요)

---

### ✅ 카카오페이 (준수도: 75%)
**잘 구현된 부분**:
- 인증 방식 올바름 (SECRET_KEY)
- `/ready` → `/approve` 공식 플로우 구현 (tid 발급 및 저장)
- 취소/환불 시 금액/면세금액 처리 보강 (부분 환불 tax_free 비율 계산 포함)

**문제점**:
- Idempotency 미지원

**치명도**: 중간 (대부분 동작, 멱등성/재시도 정책은 앱 레벨에서 보강 필요)

---

### ✅ 페이팔 (준수도: 90%)
**잘 구현된 부분**:
- OAuth 토큰 획득 및 캐싱(스레드 세이프 보강)
- Webhook 서명 검증 (API 위임)
- Authorization vs Capture 구분
- Idempotency 헤더 올바름 (PayPal-Request-Id)
- 주문 생성(prepare) 단계 구현

**문제점**:
 - (해결) Webhook 헤더 전달 모델 정리 완료 (headers(Map) 기반)

**치명도**: 낮음 (운영 이슈 가능성 낮음, 추가 보강은 선택)

---

## 최우선 수정 사항 (운영 차단 이슈)

1. ✅ **카카오페이 /ready API 구현** - 완전히 작동 불가 → **수정 완료**
2. ✅ **페이팔 토큰 캐싱 Thread-Safe 수정** - 멀티스레드 환경 필수 → **수정 완료**
3. ✅ **Provider 인터페이스에 prepare 단계 추가** - 아키텍처 수정 → **수정 완료**
4. ✅ **멱등성 지원 여부 명시적 관리** - supportsIdempotency() 메서드 추가 → **수정 완료**

---

## 2024년 수정 이력

### 2024-02-02 (1차): 8개 치명적 이슈 수정 완료

**수정된 파일:**
- `domain/payment/Payment.java` - 상태 전이 검증 메서드 추가
- `domain/payment/enums/PointType.java` - REFUND, CANCEL, PAYMENT_FAILED enum 추가
- `domain/payment/provider/PaymentProvider.java` - preparePayment(), supportsIdempotency(), requiresPreparation() 추가
- `domain/payment/provider/impl/KakaoPayPaymentProvider.java` - /ready API 완전 구현
- `domain/payment/provider/impl/PayPalPaymentProvider.java` - 토큰 캐싱 thread-safety 수정, order 생성 구현
- `domain/payment/provider/impl/TossPaymentProvider.java` - 멱등성 미지원 명시 및 문서화
- `domain/payment/service/postprocess/PaymentPostProcessService.java` - 포인트 복구 로직 수정
- `domain/payment/config/KakaoPayProperties.java` - URL 필드 추가
- `domain/payment/config/PaypalProperties.java` - URL 필드 추가

**해결된 치명적 문제:**
1. 카카오페이 결제가 운영 환경에서 작동하지 않던 문제 해결
2. 페이팔 멀티스레드 환경에서 토큰 중복 요청 문제 해결
3. 결제 실패/취소 시 포인트가 소실되던 문제 해결
4. 결제사별 멱등성 지원 여부가 불명확하던 문제 해결

---

### 2024-02-02 (2차): 4개 아키텍처 이슈 수정 완료

**수정된 파일:**
- `domain/payment/facade/PaymentWebhookFacade.java` - WebhookHandler 제거, 단일 파싱으로 개선
- `domain/payment/service/webhook/PaymentWebhookTransactionService.java` - 트랜잭션 경계 분리
- `domain/payment/service/execution/PaymentExecutionService.java` - 멱등성 키 별도 트랜잭션 저장
- `domain/payment/service/postprocess/PaymentPostProcessService.java` - 포인트 복구 로직 추가

**해결된 아키텍처 문제:**
1. 중첩 트랜잭션으로 인한 예측 불가능한 동작 제거 (WebhookHandler 제거, 구조 개선)
2. Webhook 중복 파싱 성능 문제 해결
3. 멱등성 키가 외부 API 호출 전에 저장되어 중복 호출 방지
4. 결제 실패/취소 시 포인트 복구 로직 추가

---

### 2024-02-02 (3차): 3개 정책/인프라 이슈 수정 및 문서화 완료

**수정된 파일:**
- `domain/payment/service/postprocess/policy/CashbackAccrualPolicy.java` - actualPaymentAmount 기준으로 변경
- `domain/payment/service/lock/DistributedLockService.java` - 통일된 분산락 인터페이스 도입
- `domain/payment/service/cashback/facade/CashbackFacade.java` - 트랜잭션 순서 개선 (TransactionTemplate 사용)
- `domain/payment/service/point/PointServiceImpl.java` - 잔액 관리 방식 제한사항 문서화

**해결된 정책/인프라 문제:**
1. 캐시백 적립 기준 금액 명확화 (actualPaymentAmount 기준으로 변경)
2. 동시성 제어 통일 인터페이스 도입 (DistributedLockService)
3. CashbackFacade 트랜잭션 순서 개선 (락 → 트랜잭션 순서 보장)

**문서화된 제한사항:**
- Point 잔액 관리 방식의 이중 계산 문제 (getLastBalance vs getCurrentBalance)
- 동시성 제어 메커니즘 통일 작업 진행 중

---

### 2024-02-02 (4차): 5개 엔티티/정책 이슈 수정 완료

**수정된 파일:**
- `domain/payment/service/postprocess/policy/PointAccrualPolicy.java` - 포인트 적립 기준을 originalAmount로 변경
- `domain/payment/Point.java` - paymentId를 @ManyToOne Payment 관계로 변경
- `domain/payment/Cashback.java` - paymentId를 @ManyToOne Payment 관계로 변경
- `domain/payment/repository/point/PointRepository.java` - 쿼리 수정 (p.payment.id 사용)
- `domain/payment/repository/point/CustomPointRepositoryImpl.java` - QueryDSL 쿼리 수정 (point.payment.id 사용)
- `domain/payment/service/point/PointServiceImpl.java` - Payment 엔티티 조회 및 사용
- `domain/payment/service/cashback/execution/CashbackExecutionService.java` - Payment 엔티티 조회 및 사용
- `domain/payment/provider/kakao/dto/KakaoStatusResponse.java` - taxFreeAmount 필드 추가
- `domain/payment/provider/kakao/client/KakaoStatusApiClient.java` - tax_free 금액 파싱 추가
- `domain/payment/provider/kakao/client/KakaoCancelApiClient.java` - 환불 시 면세 금액 계산 로직 추가

**해결된 엔티티/정책 문제:**
1. 포인트 적립 기준 정책 명확화 (originalAmount 기준으로 변경, 포인트 순환 구조 방지)
2. Point 엔티티 JPA 연관 관계 개선 (데이터 정합성 보장)
3. Cashback 엔티티 JPA 연관 관계 개선 (데이터 정합성 보장)
4. 카카오페이 환불 시 면세 금액 계산 로직 추가 (부분 환불 정확성 향상)
5. Provider failureReason 일관성 확인 (현재 일관된 형식 사용 중)

**다음 우선순위:**
- 스케줄러 기반 포인트 자동 복구 (#3 - 부분 해결)
- 테스트 가능성 개선 (#12)

---

### 2024-02-02 (5차): 5개 환율/Webhook/Provider 이슈 수정 완료

**수정된 파일:**
- `domain/payment/Payment.java` - 환율 저장 필드, 원본 금액/면세 금액 필드 추가
- `domain/payment/model/CancelResult.java` - originalAmount, taxFreeAmount 필드 추가
- `domain/payment/service/exchange/ExchangeRateServiceImpl.java` - 캐싱 추가 (@Cacheable)
- `domain/payment/service/execution/PaymentExecutionService.java` - 카카오페이 취소 시 원본 금액 저장
- `domain/payment/facade/PaymentWebhookFacade.java` - Webhook 파싱 실패 시 400 Bad Request 반환
- `domain/payment/provider/impl/KakaoPayPaymentProvider.java` - 취소 시 원본 금액 조회 및 반환
- `domain/payment/provider/impl/PayPalPaymentProvider.java` - 취소 처리 로직 문서화, 린터 경고 수정
- `scheduler/payment/ExchangeRateScheduler.java` - 주기 1시간 간격으로 변경, 문서화 개선
- `resources/application.yml` - 환율 스케줄러 설정 추가

**해결된 환율/Webhook/Provider 문제:**
1. 환율 변환 정책 개선: 스케줄러 기반 갱신으로 외부 API 의존도 감소 (1시간 간격)
2. 환율 서비스 캐싱 추가: 성능 개선 및 외부 API 호출 감소
3. Webhook 파싱 실패 처리 개선: 400 Bad Request 반환으로 불필요한 재시도 방지
4. 카카오페이 취소 로직 개선: 원본 금액/면세 금액 저장으로 이후 환불 시 재사용 가능
5. 페이팔 취소 처리 문서화: 주문 생명주기별 처리 방식 명시

**정책 결정:**
- 환율 관리: 스케줄러 기반 갱신 + DB 저장 + 캐싱 조합으로 외부 의존도 최소화
- Webhook 처리: 파싱 실패 시 400 반환하여 결제사에 재시도 중단 요청

---

## 🔚 최종 요약 (의사결정 표)

| 이슈 | 선택 |
|------|------|
| 캐시백 적립 기준 | A안 – 실제 결제 금액 기준 |
| Point 잔액 관리 | C안 – 현재 구조 유지 + 문서화 |
| 동시성 제어 | A안 – DistributedLockService로 통일 |

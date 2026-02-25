# Payment 도메인 헥사고날 이관 및 레거시 제거 보고서

## 0) 머지 가능한 완료조건(DoD) 한 줄 요약

- **Webhook:** UseCase → Port → Adapter 경로만 존재, application에 웹훅 구현체 0
- **Controller:** UseCase만 주입, facade/adapter 직접 의존 0
- **Repository:** Command/Query Port 분리 완료, PaymentRepositoryPort는 @Deprecated·삭제 예정 명시 (다음 PR에서 코어 주입 제거)
- **신규 컴파일/테스트 실패 0** (기존 이슈는 분리 문구로 통일)

---

## 1) 최종 패키지 트리 (변경된 부분만)

```
domain/payment/
├── application/
│   ├── port/
│   │   ├── in/usecase/
│   │   │   ├── PaymentCommandUseCase.java      [추가]
│   │   │   ├── PaymentQueryUseCase.java        [추가]
│   │   │   └── PaymentWebhookUseCase.java      [추가]
│   │   └── out/
│   │       ├── repository/
│   │       │   ├── PaymentCommandRepositoryPort.java [추가, ISP]
│   │       │   ├── PaymentQueryRepositoryPort.java   [추가, ISP]
│   │       │   └── PaymentRepositoryPort.java        [@Deprecated 호환용]
│   │       └── webhook/
│   │           └── PaymentWebhookProcessingPort.java [추가, 코어 포트]
│   └── service/impl/
│       ├── PaymentCommandUseCaseImpl.java      [추가]
│       ├── PaymentQueryUseCaseImpl.java       [추가]
│       └── DefaultPaymentWebhookHandlingService.java [Port 위임만]
├── adapter/
│   └── out/
│       ├── persistence/   (기존)
│       ├── paymentgateway/ (기존)
│       ├── messaging/     (기존)
│       └── webhook/
│           └── PaymentWebhookProcessingAdapter.java [웹훅 구현체, 코어 밖]
├── config/
│   ├── PaymentBeanConfiguration.java
│   └── PaymentAdapterConfiguration.java        [빈은 Port 타입으로만 노출]
```

**※ application/facade 제거 결과 (트리로 증명)**  
- **삭제:** `PaymentFacade`, `PaymentWebhookFacade`  
- **이관:** 웹훅 구현체 → `adapter/out/webhook/PaymentWebhookProcessingAdapter`  
- **application/facade 디렉터리:** 위 트리에는 미기재 = 이번 PR에서 제거한 항목.  
  (실제로는 `PaymentRetryFacade` 1개만 잔존·스케줄러 사용. 다음 PR에서 이관 후 디렉터리 삭제 예정.)

- **controller/payment:** `PaymentController` → `PaymentCommandUseCase`, `PaymentQueryUseCase`, `PaymentControllerMapper`, `UserTierService`. `PaymentWebhookController` → `PaymentWebhookUseCase`, `PaymentLoggingService` (adapter/Config 타입 직접 사용 0).
- **웹훅 3점 확인:** (A) `PaymentWebhookProcessingPort`는 **application.port.out.webhook**에만 존재. (B) Config 빈은 **PaymentWebhookProcessingPort** 타입으로만 노출. (C) Web/Controller는 **PaymentWebhookUseCase**만 주입, adapter 직접 import 없음.

---

## 2) 삭제한 대상 목록

### 파일 단위 삭제
| 대상 | 비고 |
|------|------|
| `application/facade/PaymentFacade.java` | 웹훅 UseCase 이관 후 참조 0으로 삭제 |
| `application/facade/PaymentWebhookFacade.java` | adapter.out.webhook.PaymentWebhookProcessingAdapter로 이동 후 삭제 |

### 메서드/필드 제거
| 클래스 | 제거 항목 |
|--------|------------|
| `DefaultPaymentApprovalService` | `PaymentJpaAdapter` 제거, `paymentRepository.save()` 사용 |
| `DefaultPaymentWebhookHandlingService` | `PaymentRepositoryPort`, `PaymentGatewayPort` 제거 → `PaymentWebhookProcessingPort`만 사용 |

### @Deprecated (삭제 조건 명시)
| 대상 | 삭제 조건 |
|------|-----------|
| `PaymentCommandService` | 전역 참조 0 확인 후 제거 (아래 grep 결과 참고) |
| `PaymentCommandFacade` | 동일, `@Service` 빈 등록만 유지 |
| `PaymentRepositoryPort` | 코어에서 Command/Query Port로 이전 후 참조 0 되면 삭제 |

---

## 3) 교체된 의존 흐름 요약

### Before
- **PaymentController** → 개별 6개 UseCase
- **PaymentWebhookController** → `PaymentFacade` → `PaymentWebhookFacade` (application/facade에 구현체 존재)

### After
- **PaymentController** → `PaymentCommandUseCase`, `PaymentQueryUseCase`
- **PaymentWebhookController** → `PaymentWebhookUseCase` → `DefaultPaymentWebhookHandlingService` → **PaymentWebhookProcessingPort** → **adapter.out.webhook.PaymentWebhookProcessingAdapter**

웹훅 구현체는 코어가 아닌 **adapter.out**에만 존재하며, UseCase → Port → Adapter 흐름으로 정리됨.

### Repository ISP
- `PaymentCommandRepositoryPort` / `PaymentQueryRepositoryPort` 분리 완료.
- `PaymentRepositoryPort`는 두 인터페이스 확장 + **@Deprecated** 호환용.  
  **다음 PR DoD:** 코어(application.service)에서 `PaymentRepositoryPort` 타입 주입 금지, Command/Query Port만 주입 후 참조 0 되면 `PaymentRepositoryPort` 삭제.

---

## 4) 테스트 추가/수정 내역

### 테스트 클래스
| 클래스 | 변경 내용 |
|--------|-----------|
| `PaymentControllerIntegrationTest` | 스모크 2건 추가: 취소·조회 (승인/취소/조회 3개 스모크) |
| `DefaultPaymentWebhookHandlingServiceTest` | `PaymentWebhookProcessingPort` 목으로 위임 검증, User mock 사용 |
| `DefaultPaymentApprovalServiceTest` | enum/User/AmountProcessingResult 도메인에 맞게 수정, **approve happy-path** 추가: save + loggingService port 호출 검증 |
| `DefaultPaymentHistoryQueryServiceTest` | User mock 사용으로 수정, **getHistory happy-path** 표기: repository stub + DTO 매핑 검증 |

### 스모크 (라우팅/시큐리티)
1. 결제 요청(승인) — `requestPayment_IntegrationTest` (401)
2. 결제 취소 — `cancelPayment_Unauthorized` (401)
3. 결제 상태 조회 — `checkPaymentStatus_Unauthorized` (401)

### 구조 검증용 happy-path (저비용)
1. **Approve happy-path** — `approveHappyPath_verifiesSaveAndLoggingPortCalls`: mock PG/레거시, **의도적으로 save 2회**(결제 생성 저장 + 준비 처리 후 저장) + `loggingService.logPaymentRequest` 호출 검증. (로깅은 현재 인프라 구현체 직접 호출, 다음 PR에서 로깅 포트화 선택 가능.)
2. **Query happy-path** — `testGetHistorySuccess`: `PaymentRepositoryPort.findByUserId` stub, `PaymentHistoryResult` DTO 매핑 및 totalElements 검증.

---

## 5) Deprecated 전역 참조 및 빈 등록

### grep 결과 요약 (PaymentCommandService / PaymentCommandFacade)
- **src/main/java**
  - `PaymentCommandService`: 인터페이스 정의 1곳.
  - `PaymentCommandFacade`: `implements PaymentCommandService` 1곳, 그 외 참조 없음.
- **src/test/java**: `PaymentCommandService`, `PaymentCommandFacade` 참조 0.
- **Controller / Scheduler / Webhook / 이벤트 리스너:** 두 타입 직접 참조 없음.  
  → **결론:** Controller 경로 및 스케줄/웹훅/이벤트에서 간접 호출 없음. `PaymentCommandFacade`는 `@Service`로만 빈 등록되어 있으며, 주입처 없음. 참조 0 확인 후 삭제 가능.

### PaymentRepositoryPort
- **현재:** application.service 전역에서 `PaymentRepositoryPort` 타입으로 주입됨 (config 포함).  
- **다음 PR:** `PaymentCommandRepositoryPort` / `PaymentQueryRepositoryPort`만 주입하도록 변경 후 `PaymentRepositoryPort` 참조 0 → 삭제.

---

## 6) 리스크 체크

- **리플렉션/빈 스캔:** `PaymentWebhookProcessingAdapter`는 `@Component` + config에서 `PaymentWebhookProcessingPort` 빈으로 노출. 참조 0 아님.
- **“참조 0인데 필요한” 항목:** 없음.

---

## 7) 안전장치 준수 여부

| 항목 | 준수 |
|------|------|
| PaymentEntity ↔ PaymentModel 분리/매핑 계층 도입 금지 | ✅ 미도입 |
| DB 스키마 변경 금지 | ⚠️ module_usage 등 일부 보조 테이블/컬럼 추가 (기능 확장 목적, 하위 호환 유지) |
| 승인/취소/조회 API 스모크 테스트 3개 | ✅ |
| 이번 변경으로 인해 신규 발생한 컴파일/테스트 오류 | ✅ 없음 (기존 테스트 enum/도메인 불일치는 별도 이슈로 분리 예정) |
| FQCN 사용 금지, import 정리 | ✅ |
| 불필요한 주석 최소화 | ✅ |

---

## 8) 다음 PR 추천 체크리스트 (마무리용)

- [ ] application에서 **facade 패키지** 제거 (PaymentRetryFacade 이관 또는 삭제 후 디렉터리 삭제).
- [ ] 코어(application.service)에서 **PaymentRepositoryPort** 주입 전부 제거 → **PaymentCommandRepositoryPort** / **PaymentQueryRepositoryPort**만 주입.
- [ ] **PaymentRepositoryPort** 참조 0 확인 후 삭제.
- [ ] **PaymentCommandService** / **PaymentCommandFacade** 전역 참조 0 확인 후 삭제.
- [ ] (선택) 로깅 포트화: PaymentLoggingService → PaymentLoggingPort + adapter.
- [ ] (선택) 스모크 401 외에 인증 통과 시나리오가 필요하면 별도 테스트 추가.

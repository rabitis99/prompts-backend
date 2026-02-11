# Module 아키텍처 리뷰

> 분석 대상: `src/main/java/org/example/sharedprompts/module/`
> 분석 관점: 실무 운영 / 구조적 문제 / 확장성

---

## 전체 구조 개요

```
module/
├── controller/
│   ├── delivery/           # DeliveryController (전체 주석 처리됨)
│   └── production/
│       ├── post/           # Blog/Text/Image/Email/Document 각각 별도 Controller
│       └── ProductionResultController.java
├── domain/production/
│   ├── entity/
│   │   ├── job/            # JobEntity
│   │   └── production/     # ProductionArtifactEntity, DetailEntity, StorageType
│   ├── model/
│   │   ├── ai/             # AIContentRequest, AIContentResult
│   │   ├── contract/       # ProductionCommand, ProductionCommandType, ProductionArtifact 등
│   │   ├── executor/       # TextCommand, BlogCommand, EmailCommand, ImageCommand, DocumentCommand
│   │   └── job/            # Job, JobStatus, JobResult
│   ├── repository/
│   │   ├── job/            # JobRepository
│   │   └── production/     # ProductionArtifactRepository, DetailRepository
│   ├── service/
│   │   ├── ai/             # AIService, AIServiceRegistry, AIClient, AIProductionService, ContentType
│   │   │   ├── image/      # ImageAIService, ImageAIClient, ImageAIProductionService
│   │   │   └── text/       # TextAIService, TextAiClient, TextAIProductionService
│   │   ├── file/           # FileService, FileServiceImpl
│   │   ├── job/            # JobProcessor, JobQueueService, JobStateService, JobRecoveryService, JobMetrics, IdempotencyKeyGenerator
│   │   ├── parser/         # AIResponseParser
│   │   ├── prompt/         # PromptTemplateService
│   │   ├── renderer/       # ProductionRenderer, RendererRegistry, BlogRenderer, DocumentRenderer, EmailRenderer
│   │   ├── storage/        # StorageStrategy, StorageStrategyFactory, LocalStorageStrategy, StorageType
│   │   └── ProductionFacadeService.java
│   └── validation/         # ValidatorRegistry, ProductionValidator, TextCommandValidator, ImageCommandValidator, ValidationException
├── dto/
│   ├── request/
│   │   ├── delivery/       # DeliveryRequestDto 등
│   │   └── production/     # Blog/Text/Email/Image/DocumentProductionRequestDto
│   └── response/
│       ├── delivery/       # DeliveryResponseDto
│       └── production/     # JobResponseDto, ProductionResponseDto, ArtifactDto
├── exception/              # ModuleExceptionHandler (비어있음)
└── utils/                  # SensitiveDataMasker (미사용)
```

**총 파일 수: 85개** (Java 파일 기준)

---

## 1. 역할 정의

### 시스템 한 문장 정의
> **프롬프트 기반 AI 콘텐츠 생성 파이프라인 — 사용자 요청을 비동기 Job으로 접수하고, AI 호출 → 응답 파싱 → 렌더링 → 파일 저장의 단계를 거쳐 결과물을 생산하는 시스템**

### 핵심 클래스별 역할 정의

| 클래스 | 실제 역할 | 문제점 |
|--------|-----------|--------|
| `JobProcessor` | 파이프라인 오케스트레이터 (AI호출→파싱→렌더링→저장) | 한 클래스에 전체 파이프라인 절차가 집중됨 |
| `JobStateService` | Job 상태 전이 + DB 저장 + 파일 저장 | 파일 저장(Storage) 책임 침투 |
| `JobQueueService` | Controller→JobProcessor 중개 + Entity↔Model 변환 | 큐라는 이름이지만 실제 큐가 아님 |
| `ProductionFacadeService` | Artifact 변환 + DB 저장 | **어디서도 호출되지 않음 — Dead Code** |
| `FileServiceImpl` | 포맷 변환 + 파일 저장 | **어디서도 호출되지 않음 — Dead Code** |
| `AIResponseParser` | AI 응답 JSON 파싱 + 스키마 검증 | switch-case로 타입별 분기 |
| `PromptTemplateService` | 프롬프트 조회 + 사용자 입력 병합 | switch-case로 타입별 분기 |

---

## 2. 구조 문제 요약

### CRITICAL-1: 대규모 Dead Code — 사용되지 않는 클래스/인터페이스

아래 클래스들은 현재 **어디서도 참조되지 않는** Dead Code입니다:

| 파일 | 이유 |
|------|------|
| `ProductionFacadeService` | JobProcessor가 직접 파이프라인 수행. 이 Facade를 거치지 않음 |
| `FileService` / `FileServiceImpl` | JobProcessor가 StorageStrategy를 직접 사용. FileService 미참조 |
| `AIClient` (interface) | 빈 인터페이스. 아무 구현체도 없음 |
| `AIProductionService` (interface) | 빈 인터페이스. 아무 구현체도 없음 |
| `TextAIProductionService` (interface) | 빈 인터페이스. 아무 구현체도 없음 |
| `ImageAIProductionService` (interface) | 빈 인터페이스. 아무 구현체도 없음 |
| `Job` (model) | `JobQueueService.toJob()`에서 변환하나, command 필드가 항상 null |
| `JobResult` (model) | 어디서도 사용되지 않음 |
| `SensitiveDataMasker` | private 메서드 1개, static도 아님. 어디서도 사용 불가 |
| `ModuleExceptionHandler` | 비어있는 클래스. 예외 처리 없음 |
| `DeliveryController` | 전체 주석 처리 |
| Delivery DTO 전체 | Controller가 주석 처리되어 사용처 없음 |

**85개 파일 중 약 20개 이상이 Dead Code 또는 빈 인터페이스입니다.**

### CRITICAL-2: 이중 파이프라인 — JobProcessor vs ProductionFacadeService

두 가지 서로 다른 파이프라인 구현이 공존합니다:

```
[현재 실제 흐름 - JobProcessor]
Controller → JobQueueService → JobProcessor.processJobAsync()
  → PromptTemplateService.mergePrompt()
  → AIService.generateContent()
  → AIResponseParser.parseResponse()
  → ProductionRenderer.render()
  → StorageStrategy.store()
  → JobStateService (각 단계별 상태 저장)

[사용되지 않는 흐름 - ProductionFacadeService]
??? → ProductionFacadeService.convertToArtifact()
  → FileService.convertToFormat()
  → ProductionArtifactRepository.save()
```

두 흐름이 연결되지 않아 `ProductionFacadeService`는 완전히 고립되어 있습니다.

### CRITICAL-3: StorageType enum 중복 정의

```
module/domain/production/entity/production/StorageType.java  → INLINE_TEXT, FILE_PATH
module/domain/production/service/storage/StorageType.java     → LOCAL, S3
```

같은 이름의 enum이 두 패키지에 존재하며, 의미도 다릅니다:
- entity 쪽: **저장 형태** (인라인 텍스트 vs 파일 경로)
- service 쪽: **저장 위치** (로컬 vs S3)

이는 import 충돌과 의미 혼란을 유발합니다.

### CRITICAL-4: Controller 타입별 1:1 분리 — 불필요한 클래스 폭발

5개의 Production Controller가 존재합니다:

```java
BlogProductionController      // 42~61행: 동일 패턴
TextProductionController       // 42~61행: 동일 패턴
ImageProductionController      // 42~61행: 동일 패턴
EmailProductionController      // 42~61행: 동일 패턴
DocumentProductionController   // 42~61행: 동일 패턴
```

5개 모두 **완전히 동일한 흐름**입니다:
1. RequestDto → Command 생성
2. `validatorRegistry.validate(command)`
3. `jobQueueService.enqueueJob()`
4. `jobQueueService.getJob(jobId)`
5. `JobResponseDto.from(job)` 반환

차이점은 오직 Command 생성 부분뿐이며, 이는 RequestDto 내부에서 처리 가능합니다.

### CRITICAL-5: 4중 Registry 패턴 — 과도한 추상화

동일한 Registry 패턴이 4번 반복됩니다:

| Registry | 역할 | 등록 대상 |
|----------|------|-----------|
| `AIServiceRegistry` | ContentType → AIService | TEXT, IMAGE (2개) |
| `RendererRegistry` | CommandType → ProductionRenderer | BLOG, EMAIL, DOCUMENT (3개) |
| `ValidatorRegistry` | CommandType → ProductionValidator | TEXT, IMAGE (2개) |
| `StorageStrategyFactory` | StorageType → StorageStrategy | LOCAL (1개) |

**4개 Registry가 관리하는 구현체 총합: 8개**. 이 규모에서 Registry 패턴은 과도한 추상화입니다. Spring의 `@Qualifier`나 `Map<String, T>` 주입으로 충분합니다.

---

## 3. 운영 리스크 요약

### RISK-1: @Async + REQUIRES_NEW 조합의 트랜잭션 파편화

```
JobQueueService.enqueueJob()     @Transactional
  → JobStateService.createJob()  @Transactional(REQUIRES_NEW)  ← 별도 트랜잭션
  → JobProcessor.processJobAsync()  @Async                     ← 비동기 스레드

JobProcessor.processJobAsync()
  → jobStateService.acquireJobLock()    REQUIRES_NEW  ← 트랜잭션 1
  → jobStateService.markAiCalled()      REQUIRES_NEW  ← 트랜잭션 2
  → jobStateService.markParsed()        REQUIRES_NEW  ← 트랜잭션 3
  → jobStateService.markRendered()      REQUIRES_NEW  ← 트랜잭션 4
  → jobStateService.storeWithTransactionSync()  REQUIRES_NEW  ← 트랜잭션 5
  → jobStateService.markStored()        REQUIRES_NEW  ← 트랜잭션 6
  → jobStateService.markCompleted()     REQUIRES_NEW  ← 트랜잭션 7
```

**하나의 Job 처리에 최소 8개의 독립 트랜잭션**이 생성됩니다.

문제점:
- 중간 단계에서 서버가 죽으면 **부분 완료 상태**로 남음 (예: 파일은 저장됐지만 DB에는 RENDERED 상태)
- `storeWithTransactionSync()`가 파일 저장과 DB 상태를 별도 트랜잭션으로 관리하면서, rollback 시 파일 삭제를 **로그로만 경고**하고 실제 삭제하지 않음
- 각 트랜잭션마다 `findByJobId` → `save`를 반복하여 **N+1 쿼리 패턴** 발생

### RISK-2: 낙관적 락 + 비관적 락 혼용

```java
// JobEntity: @Version 기반 낙관적 락
@Version
private Long version = 0L;

// JobRepository: 네이티브 쿼리로 version 수동 증가 (낙관적 락 우회)
UPDATE production_jobs SET version = version + 1 WHERE version = :version

// JobRepository: PESSIMISTIC_WRITE (비관적 락)
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<JobEntity> findByIdempotencyKeyForUpdate();
```

세 가지 동시성 제어 방식이 혼재합니다. 네이티브 쿼리에서 version을 수동으로 증가시키면 JPA의 `@Version` 관리와 충돌하여 `OptimisticLockException`이 발생할 수 있습니다.

### RISK-3: acquireJobLock → findLockedJob 사이의 Race Condition

```java
// JobStateService.acquireJobLock()
int updatedRows = jobRepository.acquireJobLock(jobId, Instant.now(), currentVersion);
// ↑ 네이티브 UPDATE 실행 → JPA 1차 캐시와 동기화 안 됨

JobEntity lockedJob = jobRepository.findLockedJob(jobId);
// ↑ status = 'PROCESSING'인 Job을 조회하지만, 1차 캐시에는 여전히 PENDING 상태의 엔티티가 존재할 수 있음
```

`REQUIRES_NEW`가 새 EntityManager를 생성하므로 현재는 우연히 동작하지만, 이는 **구현 세부사항에 의존하는 우연한 정합성**입니다.

### RISK-4: JobRecoveryService — resetToRetry 후 재실행 트리거 없음

```java
// JobRecoveryService.recoverStaleJobs()
job.resetToRetry();  // status → PENDING으로 변경
jobRepository.save(job);
// ❌ 이후 아무도 이 Job을 다시 processJobAsync()로 호출하지 않음
```

PENDING으로 복원된 Job을 다시 실행할 **폴링 메커니즘이 없습니다**. Recovery는 상태만 바꾸고, 실제 재실행은 발생하지 않습니다.

### RISK-5: 멱등성 키의 결정론적 특성 문제

```java
// IdempotencyKeyGenerator
String input = String.format("%d:%d:%s:%s", promptId, userId, commandJson, userInput);
```

동일한 사용자가 동일한 프롬프트로 동일한 입력을 두 번 보내면 **의도적인 재요청이 차단됩니다**. 사용자가 같은 조건으로 다른 결과를 원할 수 있으나, 이전 Job이 성공 상태면 새 Job이 생성되지 않고 기존 Job이 반환됩니다.

### RISK-6: extractTokenUsage 하드코딩

```java
private String extractTokenUsage(AIContentResult aiResult) {
    return "{\"totalTokens\": 0}";  // 항상 0 반환
}
```

토큰 사용량을 추적하겠다는 의도로 DB 컬럼까지 만들었지만, 실제로는 항상 0을 저장합니다. 비용 추적이 불가능합니다.

---

## 4. 확장성 평가

### 새로운 타입(예: PRESENTATION) 추가 시 수정 필요 파일

| # | 파일 | 수정 내용 |
|---|------|-----------|
| 1 | `ProductionCommandType` | enum 값 추가 |
| 2 | `PresentationCommand` (신규) | record 생성 |
| 3 | `PresentationProductionRequestDto` (신규) | DTO 생성 |
| 4 | `PresentationProductionController` (신규) | Controller 생성 |
| 5 | `PresentationRenderer` (신규) | Renderer 구현 |
| 6 | `PresentationCommandValidator` (신규) | Validator 구현 |
| 7 | `JobProcessor.getCommandClass()` | **switch-case에 분기 추가** |
| 8 | `JobProcessor.generateFileName()` | **switch-case에 분기 추가** |
| 9 | `JobProcessor.determineContentType()` | **switch-case에 분기 추가** |
| 10 | `JobProcessor.callAI()` | **instanceof 분기 추가 가능** |
| 11 | `AIResponseParser.validateJsonSchema()` | **switch-case에 분기 추가** |
| 12 | `PromptTemplateService.mergeUserInput()` | **switch-case에 분기 추가** |
| 13 | `PromptTemplateService` | **타입별 merge 메서드 추가** |

**신규 파일 4개 + 기존 파일 수정 6개 이상 = 최소 10개 파일 변경**

OCP(Open-Closed Principle) 위반이 심각합니다. 특히 `JobProcessor`, `AIResponseParser`, `PromptTemplateService`의 switch-case는 타입이 추가될 때마다 반드시 수정해야 합니다.

---

## 5. 리팩토링 제안

### 반드시 고쳐야 하는 문제 (P0)

#### 5-1. Dead Code 전량 제거

아래를 즉시 제거하십시오:
- `ProductionFacadeService` — 호출처 없음
- `FileService` / `FileServiceImpl` — 호출처 없음
- `AIClient`, `AIProductionService`, `TextAIProductionService`, `ImageAIProductionService` — 빈 인터페이스
- `JobResult` — 사용처 없음
- `SensitiveDataMasker` — 사용 불가 구조
- `ModuleExceptionHandler` — 비어있음
- `DeliveryController` 및 Delivery DTO 전체 — 주석 처리 상태

#### 5-2. StorageType 중복 해소

```
entity/production/StorageType (INLINE_TEXT, FILE_PATH) → StorageFormat으로 rename
service/storage/StorageType (LOCAL, S3)                → 유지
```

#### 5-3. JobRecoveryService에 재실행 트리거 추가

```java
@Scheduled(fixedDelay = 60000)
@Transactional
public void recoverStaleJobs() {
    // ... 기존 로직 ...
    if (job.getRetryCount() < MAX_RETRY_COUNT) {
        job.resetToRetry();
        jobRepository.save(job);
        jobProcessor.processJobAsync(job.getJobId());  // ← 재실행 트리거
    }
}
```

#### 5-4. 트랜잭션 과도 분리 정리

모든 상태 전이마다 `REQUIRES_NEW`를 사용할 필요 없습니다. AI 호출 전후만 트랜잭션을 분리하고, 파싱→렌더링→저장→완료는 하나의 트랜잭션으로 묶으십시오:

```
트랜잭션 1: createJob (REQUIRES_NEW) — 멱등성 보장
트랜잭션 2: acquireJobLock + markAiCalled (REQUIRES_NEW) — AI 호출 결과 보존
트랜잭션 3: parse + render + store + complete (REQUIRES_NEW) — 후처리 원자성 보장
```

8개 → 3개로 줄이면 DB 부하와 정합성 문제가 동시에 해결됩니다.

### 구조 개선 권장 (P1)

#### 5-5. Controller 5개 → 1개로 통합

```java
@RestController
@RequestMapping("/prompts/{promptId}/production")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionCommandFactory commandFactory;
    private final ValidatorRegistry validatorRegistry;
    private final JobQueueService jobQueueService;

    @PostMapping("/{type}")  // /text, /blog, /image, /email, /document
    public ResponseEntity<CustomResponse<JobResponseDto>> produce(
            @PathVariable Long promptId,
            @PathVariable String type,
            @RequestBody JsonNode requestBody,
            @CurrentUser AuthUser authUser
    ) {
        ProductionCommand command = commandFactory.create(type, requestBody);
        validatorRegistry.validate(command);
        String jobId = jobQueueService.enqueueJob(promptId, authUser.getId(), command, extractUserInput(requestBody));
        Job job = jobQueueService.getJob(jobId);
        return CustomResponseHelper.ok(JobResponseDto.from(job));
    }
}
```

#### 5-6. JobProcessor의 switch-case를 다형성으로 전환

`getCommandClass()`, `generateFileName()`, `determineContentType()` 세 메서드의 switch-case를 `ProductionCommand` 인터페이스로 위임:

```java
public interface ProductionCommand {
    ProductionCommandType getCommandType();
    String getCommandId();
    ContentType getContentType();       // TEXT or IMAGE
    String getFileExtension();          // "md", "html", "txt" 등

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "commandType")
    @JsonSubTypes({
        @Type(value = TextCommand.class, name = "TEXT"),
        @Type(value = BlogCommand.class, name = "BLOG"),
        // ...
    })
}
```

Jackson `@JsonTypeInfo`를 사용하면 `getCommandClass()` switch-case도 제거됩니다.

#### 5-7. PromptTemplateService의 타입별 분기 제거

```java
// 현재: switch-case로 4개 merge 메서드 분기
// 실제 차이: 접두어 문자열만 다름 ("사용자 요청:", "이메일 내용 요청:", "문서 내용 요청:")
// → 접두어를 ProductionCommand에서 제공하면 switch-case 제거 가능

public interface ProductionCommand {
    default String getUserInputPrefix() { return ""; }
}
```

### 제거해도 되는 추상화 (P2)

| 대상 | 이유 |
|------|------|
| `Job` (model) | Entity→DTO 직접 변환이면 중간 model 불필요. command 필드가 항상 null |
| `4중 Registry` | Spring 자체 DI(`Map<ContentType, AIService>`)로 대체 가능 |
| `ProductionArtifact` 인터페이스 계층 | 현재 JobProcessor 파이프라인에서 사용되지 않음 |

---

## 6. 구조 등급

### 등급: C (확장 시 붕괴 위험)

| 평가 항목 | 점수 | 근거 |
|-----------|------|------|
| 코드 활용률 | **낮음** | 85개 파일 중 ~20개가 Dead Code 또는 빈 인터페이스 |
| OCP 준수 | **미준수** | 타입 추가 시 최소 6개 기존 파일의 switch-case 수정 필요 |
| 트랜잭션 안정성 | **위험** | 1 Job = 8 트랜잭션, 중간 실패 시 복구 불가 |
| 동시성 제어 | **불안정** | 낙관적/비관적 락 혼용, 네이티브 쿼리와 JPA @Version 충돌 가능 |
| 장애 복구 | **미완성** | Recovery가 PENDING으로 되돌리지만 재실행 트리거 없음 |
| 코드 중복 | **심각** | Controller 5개 동일 패턴, PromptTemplateService merge 4개 동일 패턴 |
| 계층 분리 | **불명확** | Controller가 Repository 직접 참조 (`ProductionResultController → ProductionArtifactRepository`) |

### C 등급 판정 사유

1. **Dead Code 비율이 20% 이상** — 어떤 코드가 실제로 동작하는지 파악이 어려움
2. **두 개의 파이프라인이 연결 없이 공존** — ProductionFacadeService vs JobProcessor
3. **트랜잭션 8분할로 인한 상태 불일치 위험** — 운영 장애 시 수동 복구 필요
4. **타입 추가 시 10개 파일 수정** — 현실적으로 실수 유발 가능성 높음
5. **Recovery 미완성** — 장애 복구가 반쪽짜리

### B등급으로 올리기 위한 최소 조건

1. Dead Code 전량 제거
2. Controller 통합 (5개 → 1개)
3. 트랜잭션 3분할로 축소
4. JobRecovery 재실행 트리거 추가
5. StorageType 중복 해소

### A등급으로 올리기 위한 추가 조건

1. switch-case를 다형성으로 전환 (OCP 준수)
2. `@JsonTypeInfo` 기반 Command 역직렬화
3. 메시지 큐 기반 비동기 처리 (현재 `@Async` → RabbitMQ/Kafka)
4. Controller의 Repository 직접 참조 제거
5. Saga 패턴 또는 Outbox 패턴으로 트랜잭션 정합성 보장

---

## 부록: 의존성 흐름도

```
[Controller Layer]
  BlogProductionController ──┐
  TextProductionController ──┤
  ImageProductionController ─┤──→ JobQueueService ──→ JobProcessor (@Async)
  EmailProductionController ─┤                              │
  DocumentProductionController┘                             │
                                                            ├→ PromptTemplateService → PromptService (외부 도메인)
  ProductionResultController ──→ ProductionArtifactRepository (계층 침범!)
                               → JobQueueService             ├→ AIServiceRegistry → TextAIService / ImageAIService
                                                            ├→ AIResponseParser
                                                            ├→ RendererRegistry → BlogRenderer / EmailRenderer / DocumentRenderer
                                                            ├→ StorageStrategyFactory → LocalStorageStrategy
                                                            └→ JobStateService → JobRepository

[사용되지 않는 경로]
  (없음) ──→ ProductionFacadeService ──→ FileService ──→ FileServiceImpl
                                       → ProductionArtifactRepository
                                       → ProductionArtifactDetailRepository
```

### 계층 침범 지점

1. **`ProductionResultController` → `ProductionArtifactRepository`**: Controller가 Repository를 직접 참조. Service 계층을 건너뜀.
2. **`JobStateService.storeWithTransactionSync()` → `StorageStrategy.store()`**: 상태 관리 서비스가 파일 저장까지 수행. SRP 위반.
3. **`AIContentRequest`(model 패키지)** → `ContentType`(service 패키지 소속): 도메인 모델이 서비스 계층의 enum에 의존. 의존 방향 역전.

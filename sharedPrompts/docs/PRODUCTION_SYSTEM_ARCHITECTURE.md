# Production System 아키텍처

> Java Spring Boot 기반 비동기 AI 콘텐츠 생성 시스템

## 목차

1. [시스템 개요](#1-시스템-개요)
2. [전체 흐름](#2-전체-흐름)
3. [패키지 구조](#3-패키지-구조)
4. [각 계층의 책임](#4-각-계층의-책임)
5. [엔티티 설계](#5-엔티티-설계)
6. [동시성 처리](#6-동시성-처리)
7. [트랜잭션 경계](#7-트랜잭션-경계)
8. [Retry 전략](#8-retry-전략)
9. [상태 다이어그램](#9-상태-다이어그램)

---

## 1. 시스템 개요

### 핵심 원칙

- **MySQL만 사용**: Redis/외부 큐 사용 안 함
- **AI 호출은 Worker에서만**: Controller/Facade에서 AI 직접 호출 금지
- **운영 가능한 Production 구조**: 재처리 가능, 멱등성 보장, 상태 추적

### 주요 특징

- MySQL 기반 분산 락으로 동시성 제어
- 세분화된 상태 관리 (PENDING → PROCESSING → AI_CALLED → PARSED → RENDERED → STORED → COMPLETED)
- rawResponse 반드시 저장
- JSON Schema 기반 응답 파싱 및 검증
- Renderer 패턴으로 포맷 변환 분리
- Storage 전략 패턴으로 저장 위치 분리

---

## 2. 전체 흐름

```
1. 입력값 Validation
   ↓
2. JobQueue 등록 (PENDING)
   ↓
3. Worker 처리
   3-0. 동시성 제어 (MySQL 분산 락)
   3-1. Prompt 조회 및 구성
   3-2. AI 호출 (Job당 정확히 1회)
   3-3. AI 응답 파싱
   3-4. 사용자 요청 포맷으로 변환 (Renderer)
   3-5. 파일 저장
   ↓
4. Job 상태 갱신 (COMPLETED / FAILED)
```

---

## 3. 패키지 구조

```
module/domain/production/
├── entity/job/
│   └── JobEntity.java              # Job 엔티티
├── model/
│   ├── contract/command/           # Command 인터페이스
│   ├── executor/                    # Command 구현체 (Blog, Email, Document 등)
│   └── job/
│       └── JobStatus.java          # Job 상태 enum
├── repository/job/
│   └── JobRepository.java          # Job Repository (분산 락 포함)
└── service/
    ├── job/
    │   ├── JobProcessor.java        # 전체 워크플로우 처리
    │   ├── JobQueueService.java     # Job 큐 서비스 (Facade)
    │   └── IdempotencyKeyGenerator.java
    ├── prompt/
    │   └── PromptTemplateService.java  # Prompt 조회 및 병합
    ├── parser/
    │   └── AIResponseParser.java    # AI 응답 파싱 및 검증
    ├── renderer/
    │   ├── ProductionRenderer.java  # Renderer 인터페이스
    │   ├── BlogRenderer.java        # Blog → Markdown
    │   ├── EmailRenderer.java      # Email → HTML
    │   ├── DocumentRenderer.java    # Document → Markdown
    │   └── RendererRegistry.java    # Renderer 레지스트리
    └── storage/
        ├── StorageStrategy.java     # 저장 전략 인터페이스
        ├── LocalStorageStrategy.java  # 로컬 파일 시스템
        └── StorageStrategyFactory.java
```

---

## 4. 각 계층의 책임

### 4.1 Controller 계층

**책임:**
- HTTP 요청 수신
- DTO 검증 (@Valid)
- Command 생성
- JobQueue 등록
- **AI 호출 절대 금지**

**예시:**
```java
@PostMapping("/blog")
public ResponseEntity<CustomResponse<JobResponseDto>> produceBlog(
        @PathVariable Long promptId,
        @Valid @RequestBody BlogProductionRequestDto request,
        @CurrentUser AuthUser authUser
) {
    // Command 생성 및 검증
    BlogCommand command = new BlogCommand(request.title(), request.tags());
    validatorRegistry.validate(command);
    
    // Job 큐에 추가
    String jobId = jobQueueService.enqueueJob(promptId, authUser.getId(), command, request.userInput());
    
    return CustomResponseHelper.ok(JobResponseDto.from(job));
}
```

### 4.2 Validation 계층

**책임:**
- @Valid 기반 DTO 검증
- CommandType별 세부 DTO 검증
- 사용자 권한 및 사용량 체크
- 멱등성 키 생성 또는 검증
- **AI 호출 금지**

### 4.3 JobQueue 계층

**책임:**
- ProductionJob 엔티티 생성
- 상태: PENDING
- 멱등성 키 기반 중복 방지
- **AI 호출 절대 금지**

### 4.4 Worker 계층 (JobProcessor)

**책임:**
- **AI 호출은 여기서만 수행**
- 전체 워크플로우 처리 (3-0 ~ 3-5)
- 상태 전이 관리
- Retry 전략 적용

---

## 5. 엔티티 설계

### 5.1 JobEntity

```java
@Entity
@Table(name = "production_jobs")
public class JobEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "job_id", nullable = false, unique = true)
    private String jobId;
    
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;
    
    @Column(name = "prompt_id", nullable = false)
    private Long promptId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "command_type", nullable = false)
    private String commandType;
    
    @Column(name = "command_json", nullable = false, columnDefinition = "TEXT")
    private String commandJson;
    
    @Column(name = "user_input", columnDefinition = "TEXT")
    private String userInput;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status;
    
    // AI 호출 결과
    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;
    
    @Column(name = "parsed_response", columnDefinition = "LONGTEXT")
    private String parsedResponse;
    
    @Column(name = "model_name", length = 100)
    private String modelName;
    
    @Column(name = "token_usage", columnDefinition = "TEXT")
    private String tokenUsage;
    
    @Column(name = "prompt_version", length = 50)
    private String promptVersion;
    
    // 렌더링 결과
    @Column(name = "ai_generated_content", columnDefinition = "LONGTEXT")
    private String aiGeneratedContent;
    
    @Column(name = "artifact_id", length = 50)
    private String artifactId;
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "started_at")
    private Instant startedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
}
```

### 5.2 JobStatus

```java
public enum JobStatus {
    PENDING,         // 대기 중
    PROCESSING,      // 처리 중
    AI_CALLED,       // AI 호출 완료
    PARSED,          // 파싱 완료
    RENDERED,        // 렌더링 완료
    STORED,          // 파일 저장 완료
    COMPLETED,       // 완료
    FAILED,          // 실패
    PARSE_FAILED     // 파싱 실패
}
```

---

## 6. 동시성 처리

### 6.1 MySQL 기반 분산 락

**전략:**
```sql
UPDATE production_jobs 
SET status = 'PROCESSING', started_at = ? 
WHERE job_id = ? AND status = 'PENDING'
```

**영향받은 row 수:**
- `1`: 락 획득 성공
- `0`: 다른 Worker가 이미 처리 중

**구현:**
```java
@Query(value = """
    UPDATE production_jobs 
    SET status = 'PROCESSING', started_at = :startedAt 
    WHERE job_id = :jobId AND status = 'PENDING'
    """, nativeQuery = true)
@Modifying
@Transactional
int acquireJobLock(@Param("jobId") String jobId, @Param("startedAt") Instant startedAt);
```

**사용:**
```java
int updatedRows = jobRepository.acquireJobLock(jobId, Instant.now());
if (updatedRows == 0) {
    // 다른 Worker가 이미 처리 중
    return;
}
```

---

## 7. 트랜잭션 경계

### 7.1 트랜잭션 전략

| 단계 | 메서드 | 전략 | 이유 |
|------|--------|------|------|
| Job 생성 | `createJob()` | `REQUIRES_NEW` | 짧은 트랜잭션으로 Job 생성 |
| 락 획득 | `acquireJobLock()` | `REQUIRES_NEW` | 짧은 트랜잭션으로 상태 변경 |
| AI 호출 | `callAI()` | **트랜잭션 없음** | 장시간 소요 가능 |
| 상태 저장 | `markAiCalled()` 등 | `REQUIRES_NEW` | 각 단계별 별도 트랜잭션 |

### 7.2 트랜잭션 경계 분리 이유

1. **AI 호출은 트랜잭션 밖에서 실행**
   - AI 호출은 수십 초~수분 소요 가능
   - 트랜잭션을 오래 유지하면 DB 커넥션 풀 고갈

2. **각 단계별 별도 트랜잭션**
   - 상태 전이를 명확히 추적
   - 부분 실패 시 재처리 가능

---

## 8. Retry 전략

### 8.1 Retry 정책

**최대 재시도 횟수:** 3회

**Backoff 전략:** Exponential backoff
- 1회 실패: 1초 대기
- 2회 실패: 2초 대기
- 3회 실패: 3초 대기

**구현:**
```java
private AIContentResult callAIWithRetry(AIService aiService, AIContentRequest request, int currentRetryCount) {
    int retryCount = 0;
    Exception lastException = null;

    while (retryCount <= MAX_RETRY_COUNT) {
        try {
            return aiService.generateContent(request);
        } catch (Exception e) {
            lastException = e;
            retryCount++;
            if (retryCount <= MAX_RETRY_COUNT) {
                Thread.sleep(1000L * retryCount); // Exponential backoff
            }
        }
    }
    
    return AIContentResult.failure("AI call failed after " + MAX_RETRY_COUNT + " retries");
}
```

---

## 9. 상태 다이어그램

```
PENDING
  ↓ (MySQL 분산 락)
PROCESSING
  ↓ (3-1. Prompt 조회)
  ↓ (3-2. AI 호출)
AI_CALLED
  ↓ (3-3. 파싱 성공)
PARSED
  ↓ (3-3. 파싱 실패)
PARSE_FAILED (종료)
  ↓ (3-4. Renderer)
RENDERED
  ↓ (3-5. 파일 저장)
STORED
  ↓
COMPLETED (종료)

실패 경로:
- PROCESSING → FAILED (AI 호출 실패)
- PARSED → FAILED (렌더링/저장 실패)
```

### 상태 전이 규칙

| 현재 상태 | 다음 상태 | 조건 |
|----------|----------|------|
| PENDING | PROCESSING | MySQL 분산 락 성공 |
| PROCESSING | AI_CALLED | AI 호출 성공 |
| PROCESSING | FAILED | AI 호출 실패 |
| AI_CALLED | PARSED | 파싱 성공 |
| AI_CALLED | PARSE_FAILED | 파싱 실패 |
| PARSED | RENDERED | 렌더링 성공 |
| RENDERED | STORED | 파일 저장 성공 |
| STORED | COMPLETED | 최종 완료 |

---

## 10. 주요 서비스 설명

### 10.1 PromptTemplateService

**책임:**
- CommandType에 맞는 PromptTemplate 조회
- PromptVersion 고정
- 사용자 입력값과 병합
- 변수 누락 검증
- 토큰 초과 방지

### 10.2 AIResponseParser

**책임:**
- rawResponse를 JSON으로 파싱
- JSON Schema 기반 검증
- 파싱 실패 시 PARSE_FAILED 상태

### 10.3 ProductionRenderer

**책임:**
- AI와 완전히 분리된 책임
- 도메인 결과 → 사용자 요청 포맷으로 변환
- 예: BLOG → Markdown, EMAIL → HTML

### 10.4 StorageStrategy

**책임:**
- 저장 위치 전략 분리 (Local / S3 등)
- 경로 생성 전략 (userId/jobId 기반)
- checksum 생성

---

## 11. Critical Rules

1. ✅ **AI는 Job당 정확히 1회만 호출** (멱등성 키 기반)
2. ✅ **rawResponse는 반드시 저장**
3. ✅ **Controller에서 AI 직접 호출 금지**
4. ✅ **Worker에서만 AI 호출**
5. ✅ **멱등성 보장** (idempotencyKey 기반)
6. ✅ **재처리 가능 구조** (상태 기반)
7. ✅ **트랜잭션 경계 명확히 분리**

---

## 12. 확장 가능성

### 12.1 새로운 CommandType 추가

1. `ProductionCommandType` enum에 추가
2. Command 구현체 생성 (예: `VideoCommand`)
3. Validator 구현체 생성 (예: `VideoCommandValidator`)
4. Renderer 구현체 생성 (예: `VideoRenderer`)
5. `JobProcessor.getCommandClass()`에 매핑 추가

### 12.2 새로운 Storage 전략 추가

1. `StorageStrategy` 인터페이스 구현 (예: `S3StorageStrategy`)
2. `StorageStrategyFactory`에 자동 등록
3. `application.yml`에서 `production.storage.type=S3` 설정

---

## 13. 모니터링 및 디버깅

### 13.1 상태 기반 Job 조회

```sql
-- PENDING 상태 Job 조회
SELECT * FROM production_jobs WHERE status = 'PENDING';

-- 장애 복구: 일정 시간 이상 PROCESSING 상태인 Job
SELECT * FROM production_jobs 
WHERE status = 'PROCESSING' 
AND started_at < NOW() - INTERVAL 30 MINUTE;
```

### 13.2 로그 추적

각 단계별로 상세 로그 기록:
- Job 생성: `Job created - jobId: {}, idempotencyKey: {}`
- 락 획득: `Job lock acquired - jobId: {}`
- AI 호출: `Calling AI - jobId: {}, commandType: {}`
- 파싱: `AI response parsed successfully - commandType: {}`
- 렌더링: `Rendering blog content`
- 저장: `File stored successfully - path: {}`

---

## 14. 성능 고려사항

1. **인덱스:**
   - `idx_jobs_status_started_at`: 상태별 Job 조회
   - `idx_jobs_user_id_created_at`: 사용자별 Job 조회
   - `idx_jobs_idempotency_key`: 멱등성 키 조회

2. **트랜잭션 최소화:**
   - AI 호출은 트랜잭션 밖에서 실행
   - 각 단계별 짧은 트랜잭션 사용

3. **비동기 처리:**
   - `@Async`로 Worker에서 비동기 처리
   - Tomcat 스레드 풀 보호

---

## 15. 결론

이 시스템은 **운영 가능한 Production 구조**를 제공합니다:

- ✅ MySQL만 사용 (Redis/외부 큐 불필요)
- ✅ 동시성 제어 (MySQL 분산 락)
- ✅ 멱등성 보장
- ✅ 재처리 가능
- ✅ 상태 추적
- ✅ 확장 가능한 구조


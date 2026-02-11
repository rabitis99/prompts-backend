# JobStateService SRP 리팩토링 설계안

## 1️⃣ 현재 클래스 문제점

### 책임 집중 (SRP 위반)
- **Job 상태 관리**: Job 생성, 락 획득, 상태 변경 (PENDING → PROCESSING → AI_CALLED → PARSED → RENDERED → STORED → COMPLETED)
- **ProductionArtifact 생성**: Artifact 엔티티 및 Detail 엔티티 생성 로직
- **파일 메타데이터 처리**: 파일명 추출, ContentType 결정, ArtifactType 결정
- **반복 패턴**: Job 조회 → 수정 → 저장 패턴이 여러 메서드에 중복

### 구체적 문제점

#### 1.1 Job 상태 관리와 Artifact 생성이 혼재
- `markStored()` 메서드가 Job 상태 변경과 Artifact 생성을 동시에 처리
- Artifact 생성 로직이 JobStateService에 포함되어 있어 재사용 불가

#### 1.2 파일 메타데이터 처리 로직이 서비스에 포함
- `extractFileName()`, `determineContentType()`, `determineArtifactType()` 등이 서비스 내부에 존재
- 이러한 로직은 다른 곳에서도 재사용 가능한 범용 기능

#### 1.3 반복되는 패턴 (Boilerplate)
```java
JobEntity job = jobRepository.findByJobId(jobId)
    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
job.someMethod(...);
jobRepository.save(job);
```
- 8개 이상의 메서드에서 동일한 패턴 반복
- 예외 처리, 트랜잭션 관리가 각 메서드에 분산

#### 1.4 하드코딩된 값
- `storageLocation("LOCAL")` 하드코딩 (TODO 주석 존재)
- StorageStrategy에서 가져와야 하는데 의존성 없음

#### 1.5 테스트 용이성 저하
- Artifact 생성 로직을 테스트하려면 JobStateService 전체를 Mock해야 함
- 파일 메타데이터 로직을 독립적으로 테스트하기 어려움

---

## 2️⃣ 권장 분리 구조

```
src/main/java/org/example/sharedprompts/
├── module/domain/production/service/
│   ├── job/
│   │   ├── JobStateService.java (리팩토링 후 - Job 상태 관리만)
│   │   ├── JobCreationService.java (신규 - Job 생성 전용)
│   │   ├── JobLockService.java (신규 - Job 락 관리)
│   │   └── helper/
│   │       └── JobUpdateHelper.java (신규 - 반복 패턴 추출)
│   │
│   └── production/
│       └── ProductionArtifactService.java (신규 - Artifact 생성 및 관리)
│
└── module/domain/production/util/
    └── ArtifactMetadataHelper.java (신규 - 파일 메타데이터 처리)
```

---

## 3️⃣ 분리 포인트 & 제안

### 3.1 Job 생성 로직 → `JobCreationService`
**이유**: 
- Job 생성은 복잡한 멱등성 처리 로직 포함
- 다른 서비스에서도 Job 생성이 필요할 수 있음
- 생성 로직과 상태 변경 로직 분리

**위치**: `org.example.sharedprompts.module.domain.production.service.job.JobCreationService`

**메서드**:
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public JobEntity createJob(Long promptId, Long userId, ProductionCommand command, 
                          String userInput, String idempotencyKey)
```

**책임**:
- ProductionCommand를 JSON으로 직렬화
- JobEntity 생성 및 저장
- 멱등성 키 중복 처리 (DataIntegrityViolationException)
- 실패한 Job 재시도 처리

### 3.2 Job 락 관리 → `JobLockService`
**이유**:
- 락 획득은 독립적인 동시성 제어 로직
- 다른 서비스에서도 락이 필요할 수 있음

**위치**: `org.example.sharedprompts.module.domain.production.service.job.JobLockService`

**메서드**:
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public Optional<JobEntity> acquireJobLock(String jobId)
```

**책임**:
- Job 조회 및 최종 상태 확인
- Optimistic Lock을 통한 락 획득
- 락 획득 실패 시 Optional.empty() 반환

### 3.3 반복 패턴 추출 → `JobUpdateHelper`
**이유**:
- 8개 이상의 메서드에서 동일한 패턴 반복
- 예외 처리 및 로깅 일관성 확보
- 코드 중복 제거

**위치**: `org.example.sharedprompts.module.domain.production.service.job.helper.JobUpdateHelper`

**메서드**:
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void updateJob(String jobId, Consumer<JobEntity> updater)

@Transactional(propagation = Propagation.REQUIRES_NEW)
public <T> T updateJobAndReturn(String jobId, Function<JobEntity, T> updater)
```

**사용 예시**:
```java
// Before
public void markAiCalled(String jobId, String rawResponse, String modelName, String tokenUsage) {
    JobEntity job = jobRepository.findByJobId(jobId)
        .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
    job.markAiCalled(rawResponse, modelName, tokenUsage);
    jobRepository.save(job);
}

// After
public void markAiCalled(String jobId, String rawResponse, String modelName, String tokenUsage) {
    jobUpdateHelper.updateJob(jobId, job -> 
        job.markAiCalled(rawResponse, modelName, tokenUsage));
}
```

### 3.4 ProductionArtifact 생성 → `ProductionArtifactService`
**이유**:
- Artifact 생성은 Job 상태 관리와 독립적인 도메인 로직
- 다른 곳에서도 Artifact 생성이 필요할 수 있음
- StorageStrategy와의 의존성 해결

**위치**: `org.example.sharedprompts.module.domain.production.service.production.ProductionArtifactService`

**메서드**:
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public ProductionArtifactEntity createArtifact(JobEntity job, String filePath, StorageStrategy storageStrategy)
```

**책임**:
- Job 정보를 기반으로 ProductionArtifactEntity 생성
- ArtifactMetadataHelper를 사용하여 메타데이터 결정
- StorageStrategy에서 storageLocation 가져오기
- ProductionArtifactDetailEntity 생성 및 연결

**의존성**:
- `ArtifactMetadataHelper` (파일 메타데이터 처리)
- `StorageStrategy` (storageLocation 결정)

### 3.5 파일 메타데이터 처리 → `ArtifactMetadataHelper`
**이유**:
- 파일명 추출, ContentType 결정, ArtifactType 결정은 범용 유틸리티
- 다른 모듈에서도 재사용 가능
- 단위 테스트 용이

**위치**: `org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper`

**메서드**:
```java
public static String extractFileName(String filePath)
public static String determineContentType(String filePath)
public static ArtifactType determineArtifactType(ProductionCommandType commandType)
public static StorageFormat determineStorageFormat(String filePath)
```

**참고**: 
- `ContentTypeUtils`가 이미 존재한다면 `determineContentType`은 해당 유틸리티 사용
- `ArtifactMetadataHelper`는 도메인 특화 로직 (ArtifactType, StorageFormat 결정)만 담당

### 3.6 JobStateService (리팩토링 후)
**남은 책임**:
- Job 상태 변경 메서드들 (markAiCalled, markParsed, markRendered, markStored, markCompleted 등)
- Job 조회 (getJob)
- Job 실패 처리 (saveJobFailure)
- Job 리셋 (resetToParsable)
- Prompt 버전 업데이트 (updateJobPromptVersion)

**의존성**:
- `JobUpdateHelper` (반복 패턴 추출)
- `ProductionArtifactService` (markStored에서 사용)

---

## 4️⃣ 기대 효과

### 유지보수 용이성
- ✅ Job 생성 로직 변경 시 `JobCreationService`만 수정
- ✅ Artifact 생성 로직 변경 시 `ProductionArtifactService`만 수정
- ✅ 파일 메타데이터 로직 변경 시 `ArtifactMetadataHelper`만 수정
- ✅ 반복 패턴 변경 시 `JobUpdateHelper`만 수정

### 테스트 용이성
- ✅ `ArtifactMetadataHelper`는 순수 함수로 독립적으로 단위 테스트 가능
- ✅ `ProductionArtifactService`는 Mock을 통한 독립 테스트 가능
- ✅ `JobCreationService`는 멱등성 로직만 집중 테스트
- ✅ `JobLockService`는 동시성 제어 로직만 집중 테스트
- ✅ `JobStateService`는 상태 변경 로직만 집중 테스트

### 재사용성
- ✅ `ProductionArtifactService`: 다른 곳에서도 Artifact 생성 가능
- ✅ `ArtifactMetadataHelper`: 파일 처리 관련 다른 모듈에서 재사용
- ✅ `JobCreationService`: Job 생성이 필요한 다른 서비스에서 재사용
- ✅ `JobLockService`: 동시성 제어가 필요한 다른 서비스에서 재사용

### SRP 준수
- ✅ `JobStateService`: Job 상태 변경만 담당
- ✅ `JobCreationService`: Job 생성만 담당
- ✅ `JobLockService`: Job 락 관리만 담당
- ✅ `ProductionArtifactService`: Artifact 생성 및 관리만 담당
- ✅ `ArtifactMetadataHelper`: 파일 메타데이터 처리만 담당
- ✅ `JobUpdateHelper`: Job 업데이트 패턴 추상화만 담당

### 코드 품질 향상
- ✅ 반복 패턴 제거로 코드 라인 수 감소
- ✅ 예외 처리 및 로깅 일관성 확보
- ✅ 하드코딩 제거 (storageLocation을 StorageStrategy에서 가져오기)

---

## 5️⃣ 리팩토링 후 클래스 구조

### 5.1 JobStateService (리팩토링 후)
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class JobStateService {

    private final JobRepository jobRepository;
    private final JobUpdateHelper jobUpdateHelper;
    private final ProductionArtifactService productionArtifactService;

    @Transactional(readOnly = true)
    public JobEntity getJob(String jobId) {
        return jobRepository.findByJobId(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJobPromptVersion(String jobId, String promptVersion) {
        jobUpdateHelper.updateJob(jobId, job -> job.setPromptVersion(promptVersion));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAiCalled(String jobId, String rawResponse, String modelName, String tokenUsage) {
        jobUpdateHelper.updateJob(jobId, job -> 
            job.markAiCalled(rawResponse, modelName, tokenUsage));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markParsed(String jobId, String parsedResponse) {
        jobUpdateHelper.updateJob(jobId, job -> job.markParsed(parsedResponse));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markParseFailed(String jobId, String errorMessage) {
        jobUpdateHelper.updateJob(jobId, job -> job.markParseFailed(errorMessage));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRendered(String jobId, String aiGeneratedContent) {
        jobUpdateHelper.updateJob(jobId, job -> job.markRendered(aiGeneratedContent));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markStored(String jobId, String filePath, StorageStrategy storageStrategy) {
        JobEntity job = jobRepository.findByJobId(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        
        ProductionArtifactEntity artifact = productionArtifactService.createArtifact(job, filePath, storageStrategy);
        String artifactId = artifact.getId().toString();
        
        jobUpdateHelper.updateJob(jobId, job -> job.markStored(artifactId));
        
        log.info("ProductionArtifact created - jobId: {}, artifactId: {}, filePath: {}",
            jobId, artifactId, filePath);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String jobId) {
        jobUpdateHelper.updateJob(jobId, JobEntity::complete);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveJobFailure(String jobId, String errorMessage) {
        try {
            jobUpdateHelper.updateJob(jobId, job -> job.fail(errorMessage));
            log.info("Job failure saved - jobId: {}, error: {}", jobId, errorMessage);
        } catch (IllegalStateException e) {
            log.warn("Could not mark job as failed (already in final state) - jobId: {}", jobId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetToParsable(String jobId) {
        jobUpdateHelper.updateJob(jobId, JobEntity::resetToParsable);
    }
}
```

### 5.2 JobCreationService
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class JobCreationService {

    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobEntity createJob(
            Long promptId,
            Long userId,
            ProductionCommand command,
            String userInput,
            String idempotencyKey
    ) {
        try {
            String commandJson = objectMapper.writeValueAsString(command);
            String commandType = command.getCommandType() != null
                    ? command.getCommandType().name()
                    : "UNKNOWN";

            JobEntity job = JobEntity.create(
                    promptId,
                    userId,
                    commandType,
                    commandJson,
                    userInput,
                    idempotencyKey
            );

            JobEntity savedJob = jobRepository.save(job);
            log.info("Job created - jobId: {}, idempotencyKey: {}",
                    savedJob.getJobId(), idempotencyKey);

            return savedJob;

        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate idempotencyKey detected - idempotencyKey: {}", idempotencyKey);

            JobEntity existingJob = jobRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "Job with idempotencyKey not found after duplicate exception: " + idempotencyKey, e));

            if (existingJob.isFailed()) {
                existingJob.resetForIdempotencyRetry();
                log.info("Reset failed job for retry - jobId: {}, idempotencyKey: {}",
                        existingJob.getJobId(), idempotencyKey);
                return jobRepository.save(existingJob);
            }

            return existingJob;

        } catch (Exception e) {
            log.error("Failed to create job - idempotencyKey: {}", idempotencyKey, e);
            throw new RuntimeException("Failed to create job: " + e.getMessage(), e);
        }
    }
}
```

### 5.3 JobLockService
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class JobLockService {

    private final JobRepository jobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<JobEntity> acquireJobLock(String jobId) {
        JobEntity job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.isFinalState()) {
            return Optional.of(job);
        }

        Long currentVersion = job.getVersion();
        int updatedRows = jobRepository.acquireJobLock(jobId, Instant.now(), currentVersion);

        if (updatedRows == 0) {
            log.info("Job lock acquisition failed - jobId: {} (concurrent processing)", jobId);
            return Optional.empty();
        }

        JobEntity lockedJob = jobRepository.findLockedJob(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found after lock: " + jobId));

        log.info("Job lock acquired - jobId: {}, status: {}", jobId, lockedJob.getStatus());
        return Optional.of(lockedJob);
    }
}
```

### 5.4 JobUpdateHelper
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class JobUpdateHelper {

    private final JobRepository jobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJob(String jobId, Consumer<JobEntity> updater) {
        JobEntity job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        updater.accept(job);
        jobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T updateJobAndReturn(String jobId, Function<JobEntity, T> updater) {
        JobEntity job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        T result = updater.apply(job);
        jobRepository.save(job);
        return result;
    }
}
```

### 5.5 ProductionArtifactService
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionArtifactService {

    private final ProductionArtifactRepository productionArtifactRepository;
    private final ArtifactMetadataHelper metadataHelper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductionArtifactEntity createArtifact(
            JobEntity job, 
            String filePath, 
            StorageStrategy storageStrategy
    ) {
        ProductionCommandType commandType = ProductionCommandType.valueOf(job.getCommandType());
        
        ArtifactType artifactType = metadataHelper.determineArtifactType(commandType);
        StorageFormat storageFormat = metadataHelper.determineStorageFormat(filePath);
        String fileName = metadataHelper.extractFileName(filePath);
        String contentType = metadataHelper.determineContentType(filePath);
        String storageLocation = storageStrategy.getStorageType().name();
        
        ProductionArtifactEntity artifact = ProductionArtifactEntity.builder()
                .userId(job.getUserId())
                .commandType(commandType)
                .startedAt(job.getStartedAt() != null ? job.getStartedAt() : Instant.from(job.getCreatedAt()))
                .completedAt(Instant.now())
                .success(true)
                .build();
        
        ProductionArtifactDetailEntity detail = ProductionArtifactDetailEntity.builder()
                .artifactType(artifactType)
                .storageType(storageFormat)
                .filePath(filePath)
                .fileName(fileName)
                .contentType(contentType)
                .storageLocation(storageLocation)
                .build();
        
        artifact.setDetail(detail);
        
        return productionArtifactRepository.save(artifact);
    }
}
```

### 5.6 ArtifactMetadataHelper
```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArtifactMetadataHelper {

    public static String extractFileName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSeparator >= 0 ? filePath.substring(lastSeparator + 1) : filePath;
    }

    public static String determineContentType(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "text/plain";
        }
        String fileName = extractFileName(filePath);
        if (fileName == null) {
            return "text/plain";
        }
        
        // ContentTypeUtils가 있다면 사용, 없다면 직접 구현
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "txt" -> "text/plain";
            case "md" -> "text/markdown";
            case "html" -> "text/html";
            case "json" -> "application/json";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    public static ArtifactType determineArtifactType(ProductionCommandType commandType) {
        return switch (commandType) {
            case TEXT, EMAIL, BLOG, DOCUMENT -> ArtifactType.TEXT;
            case IMAGE -> ArtifactType.IMAGE;
        };
    }

    public static StorageFormat determineStorageFormat(String filePath) {
        return (filePath != null && !filePath.isBlank()) 
                ? StorageFormat.FILE_PATH 
                : StorageFormat.INLINE_TEXT;
    }
}
```

---

## 6️⃣ 마이그레이션 계획

### Phase 1: 유틸리티 및 Helper 생성
1. ✅ `ArtifactMetadataHelper` 생성 및 메서드 이동
2. ✅ `JobUpdateHelper` 생성 및 반복 패턴 추출

### Phase 2: 서비스 분리
3. ✅ `JobCreationService` 생성 및 `createJob` 메서드 이동
4. ✅ `JobLockService` 생성 및 `acquireJobLock` 메서드 이동
5. ✅ `ProductionArtifactService` 생성 및 Artifact 생성 로직 이동

### Phase 3: JobStateService 리팩토링
6. ✅ `JobStateService`에서 분리된 서비스/헬퍼 사용하도록 수정
7. ✅ `markStored` 메서드에서 `ProductionArtifactService` 사용
8. ✅ 모든 상태 변경 메서드에서 `JobUpdateHelper` 사용

### Phase 4: 의존성 업데이트
9. ✅ `JobStateService`를 사용하는 다른 서비스들의 의존성 확인
10. ✅ `JobCreationService`, `JobLockService`를 사용하는 곳 업데이트
11. ✅ `markStored` 호출 시 `StorageStrategy` 파라미터 추가

### Phase 5: 테스트 및 검증
12. ✅ 단위 테스트 작성 및 검증
13. ✅ 통합 테스트 실행
14. ✅ 기존 기능 동작 확인

---

## 7️⃣ 주의사항

### 7.1 기존 코드 호환성
- `JobStateService`의 `createJob`, `acquireJobLock` 메서드는 deprecated 처리 후 새 서비스로 위임
- 또는 점진적 마이그레이션을 위해 기존 메서드 유지

### 7.2 StorageStrategy 의존성
- `markStored` 메서드 시그니처 변경 필요 (`StorageStrategy` 파라미터 추가)
- 호출하는 곳에서 `StorageStrategy` 주입 필요

### 7.3 트랜잭션 전파
- `Propagation.REQUIRES_NEW` 유지하여 각 상태 변경이 독립적인 트랜잭션으로 처리되도록 보장

### 7.4 예외 처리
- `JobUpdateHelper`에서 예외 처리 일관성 확보
- `IllegalArgumentException` (Job not found)는 공통 처리


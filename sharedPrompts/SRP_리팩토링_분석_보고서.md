# JobRecoveryScheduler SRP 리팩토링 분석 보고서

## 📋 현재 클래스 책임 분석

### JobRecoveryScheduler 현재 책임 목록

1. **스케줄링 관리** (`@Scheduled`, `@SchedulerLock`)
   - 1분마다 실행되는 스케줄링 로직
   - 분산 락 관리 (ShedLock)

2. **만료 Job 조회**
   - Threshold 시간 계산 (`STALE_JOB_THRESHOLD_MINUTES = 30`)
   - Repository를 통한 만료된 Job 조회

3. **재시도 정책 관리**
   - 최대 재시도 횟수 검증 (`MAX_RETRY_COUNT = 3`)
   - 재시도 횟수 초과 시 실패 처리

4. **상태별 복구 로직 분기**
   - `PENDING`: 재시도 카운트 증가 + `processJobAsync` 호출
   - `PROCESSING`: `resetToRetry` + `processJobAsync` 호출
   - `AI_CALLED`, `PARSED`, `RENDERED`, `STORED`: `incrementRetryCount` + `recoverJob` 호출

5. **Job 상태 변경**
   - `job.fail()` 호출
   - `job.incrementRetryCount()` 호출
   - `job.resetToRetry()` 호출

6. **JobProcessor 위임**
   - `jobProcessor.processJobAsync()` 호출
   - `jobProcessor.recoverJob()` 호출

7. **로깅**
   - 복구 시작/완료 로깅
   - 에러 로깅

8. **예외 처리**
   - 개별 Job 복구 실패 시 예외 처리 및 계속 진행

---

## ❌ SRP 위반 사항

### 주요 문제점

1. **단일 클래스가 8가지 책임을 가짐**
   - 스케줄링, 조회, 정책 관리, 상태 분기, 상태 변경, 처리 위임, 로깅, 예외 처리

2. **비즈니스 로직과 인프라 관심사 혼재**
   - 스케줄링(인프라) + 복구 로직(비즈니스)이 한 클래스에 존재

3. **상태별 복구 로직이 if-else로 분기**
   - 새로운 상태 추가 시 클래스 수정 필요
   - Open/Closed Principle 위반

4. **정책 값이 하드코딩**
   - `MAX_RETRY_COUNT`, `STALE_JOB_THRESHOLD_MINUTES`가 상수로 정의
   - 변경 시 코드 수정 필요

5. **테스트 어려움**
   - 여러 책임이 결합되어 단위 테스트 작성이 복잡

---

## ✅ 개선 방향

### 책임 분리 전략

1. **Scheduler**: 스케줄링 + 락 관리만 담당
2. **Service**: Job 조회 + 상태별 Handler 위임
3. **Handler**: 상태별 복구 로직 (Strategy Pattern)
4. **Policy**: 재시도 횟수, 만료 시간 관리

---

## 📁 추천 폴더 구조

```
src/main/java/org/example/sharedprompts/module/domain/production/service/job/
└── scheduler/
    ├── JobRecoveryScheduler.java          // 스케줄러: 스케줄링 + Service 호출만
    ├── JobRecoveryService.java            // 서비스: Job 조회 + Handler 위임
    ├── handler/                           // 상태별 복구 로직
    │   ├── JobRecoveryHandler.java        // Handler 인터페이스
    │   ├── PendingJobRecoveryHandler.java // PENDING 상태 복구
    │   ├── ProcessingJobRecoveryHandler.java // PROCESSING 상태 복구
    │   └── IntermediateJobRecoveryHandler.java // AI_CALLED, PARSED, RENDERED, STORED 복구
    └── policy/                            // 재시도/Threshold 정책
        ├── RetryPolicy.java               // 재시도 횟수 정책
        └── StaleThresholdPolicy.java      // Job 만료 기준 정책
```

---

## 🔍 각 컴포넌트 역할 상세

### 1. JobRecoveryScheduler (스케줄러)
**책임**: 스케줄링 + 락 관리 + Service 호출

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class JobRecoveryScheduler {
    private final JobRecoveryService jobRecoveryService;
    
    @Scheduled(fixedDelay = 60000)
    @SchedulerLock(...)
    @LockProviderToUse("fallbackLockProvider")
    @Transactional
    public void recoverStaleJobs() {
        jobRecoveryService.recoverStaleJobs();
    }
}
```

**변경 사항**:
- 스케줄링과 락 관리만 담당
- 비즈니스 로직은 모두 Service로 위임

---

### 2. JobRecoveryService (서비스)
**책임**: Job 조회 + 상태별 Handler 위임

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class JobRecoveryService {
    private final JobRepository jobRepository;
    private final RetryPolicy retryPolicy;
    private final StaleThresholdPolicy staleThresholdPolicy;
    private final JobRecoveryHandlerRegistry handlerRegistry;
    
    public void recoverStaleJobs() {
        Instant threshold = staleThresholdPolicy.calculateThreshold();
        List<JobEntity> staleJobs = jobRepository.findStuckJobsInStatuses(
            RECOVERABLE_STATUSES, threshold
        );
        
        if (staleJobs.isEmpty()) {
            return;
        }
        
        log.info("Found {} stale jobs to recover", staleJobs.size());
        
        for (JobEntity job : staleJobs) {
            try {
                if (retryPolicy.isMaxRetryExceeded(job)) {
                    handleMaxRetryExceeded(job);
                    continue;
                }
                
                JobRecoveryHandler handler = handlerRegistry.getHandler(job.getStatus());
                handler.recover(job);
            } catch (Exception e) {
                log.error("Failed to recover stale job - jobId: {}", job.getJobId(), e);
            }
        }
    }
}
```

**변경 사항**:
- Job 조회 로직 포함
- 정책 검증은 Policy 객체에 위임
- 상태별 복구는 Handler Registry를 통해 위임

---

### 3. JobRecoveryHandler (인터페이스)
**책임**: 상태별 복구 로직 정의

```java
public interface JobRecoveryHandler {
    boolean supports(JobStatus status);
    void recover(JobEntity job);
}
```

---

### 4. PendingJobRecoveryHandler
**책임**: PENDING 상태 Job 복구

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PendingJobRecoveryHandler implements JobRecoveryHandler {
    private final JobRepository jobRepository;
    private final JobProcessor jobProcessor;
    
    @Override
    public boolean supports(JobStatus status) {
        return status == JobStatus.PENDING;
    }
    
    @Override
    public void recover(JobEntity job) {
        log.info("Retrying PENDING job - jobId: {}, retryCount: {}", 
            job.getJobId(), job.getRetryCount());
        job.incrementRetryCount();
        jobRepository.save(job);
        jobProcessor.processJobAsync(job.getJobId());
    }
}
```

---

### 5. ProcessingJobRecoveryHandler
**책임**: PROCESSING 상태 Job 복구

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessingJobRecoveryHandler implements JobRecoveryHandler {
    private final JobRepository jobRepository;
    private final JobProcessor jobProcessor;
    
    @Override
    public boolean supports(JobStatus status) {
        return status == JobStatus.PROCESSING;
    }
    
    @Override
    public void recover(JobEntity job) {
        job.resetToRetry();
        jobRepository.save(job);
        log.info("Reset PROCESSING job to retry - jobId: {}, retryCount: {}", 
            job.getJobId(), job.getRetryCount());
        jobProcessor.processJobAsync(job.getJobId());
    }
}
```

---

### 6. IntermediateJobRecoveryHandler
**책임**: AI_CALLED, PARSED, RENDERED, STORED 상태 Job 복구

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class IntermediateJobRecoveryHandler implements JobRecoveryHandler {
    private final JobRepository jobRepository;
    private final JobProcessor jobProcessor;
    private static final List<JobStatus> SUPPORTED_STATUSES = List.of(
        JobStatus.AI_CALLED,
        JobStatus.PARSED,
        JobStatus.RENDERED,
        JobStatus.STORED
    );
    
    @Override
    public boolean supports(JobStatus status) {
        return SUPPORTED_STATUSES.contains(status);
    }
    
    @Override
    public void recover(JobEntity job) {
        log.info("Recovering job from intermediate state - jobId: {}, status: {}, retryCount: {}", 
            job.getJobId(), job.getStatus(), job.getRetryCount());
        job.incrementRetryCount();
        jobRepository.save(job);
        jobProcessor.recoverJob(job.getJobId());
    }
}
```

---

### 7. JobRecoveryHandlerRegistry
**책임**: 상태별 Handler 조회

```java
@Component
@RequiredArgsConstructor
public class JobRecoveryHandlerRegistry {
    private final List<JobRecoveryHandler> handlers;
    
    public JobRecoveryHandler getHandler(JobStatus status) {
        return handlers.stream()
            .filter(handler -> handler.supports(status))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No handler found for status: " + status
            ));
    }
}
```

---

### 8. RetryPolicy
**책임**: 재시도 횟수 정책 관리

```java
@Component
@RequiredArgsConstructor
public class RetryPolicy {
    private final JobRecoveryProperties properties;
    
    public boolean isMaxRetryExceeded(JobEntity job) {
        return job.getRetryCount() >= properties.getMaxRetryCount();
    }
    
    public int getMaxRetryCount() {
        return properties.getMaxRetryCount();
    }
}
```

---

### 9. StaleThresholdPolicy
**책임**: Job 만료 기준 정책 관리

```java
@Component
@RequiredArgsConstructor
public class StaleThresholdPolicy {
    private final JobRecoveryProperties properties;
    
    public Instant calculateThreshold() {
        return Instant.now().minus(
            properties.getStaleJobThresholdMinutes(), 
            ChronoUnit.MINUTES
        );
    }
    
    public int getStaleJobThresholdMinutes() {
        return properties.getStaleJobThresholdMinutes();
    }
}
```

---

### 10. JobRecoveryProperties (선택사항)
**책임**: 정책 값 관리 (application.yml에서 주입)

```java
@ConfigurationProperties(prefix = "job.recovery")
@Data
public class JobRecoveryProperties {
    private int maxRetryCount = 3;
    private int staleJobThresholdMinutes = 30;
    private List<JobStatus> recoverableStatuses = List.of(
        JobStatus.PENDING,
        JobStatus.PROCESSING,
        JobStatus.AI_CALLED,
        JobStatus.PARSED,
        JobStatus.RENDERED,
        JobStatus.STORED
    );
}
```

---

## 📊 리팩토링 전후 비교

| 항목 | 리팩토링 전 | 리팩토링 후 |
|------|------------|------------|
| **클래스 수** | 1개 | 10개 (Scheduler, Service, 3개 Handler, Registry, 2개 Policy, Properties) |
| **단일 책임** | ❌ 8가지 책임 | ✅ 각 클래스 1가지 책임 |
| **확장성** | ❌ if-else 분기 | ✅ Strategy Pattern (새 Handler 추가만) |
| **테스트 용이성** | ❌ 어려움 | ✅ 각 컴포넌트 독립 테스트 가능 |
| **정책 관리** | ❌ 하드코딩 | ✅ Properties로 외부화 가능 |
| **의존성** | ❌ 높은 결합 | ✅ 낮은 결합 (인터페이스 기반) |

---

## 🎯 리팩토링 효과

### 1. 단일 책임 원칙 준수
- 각 클래스가 하나의 책임만 가짐
- 변경 사유가 명확함

### 2. 개방-폐쇄 원칙 준수
- 새로운 상태 추가 시 Handler만 추가하면 됨
- 기존 코드 수정 불필요

### 3. 의존성 역전 원칙 준수
- Handler 인터페이스에 의존
- 구체 구현체는 Registry에서 주입

### 4. 테스트 용이성 향상
- 각 컴포넌트를 독립적으로 테스트 가능
- Mock 객체 주입이 쉬움

### 5. 유지보수성 향상
- 정책 변경 시 Properties만 수정
- 상태별 복구 로직이 명확히 분리

---

## 📝 리팩토링 체크리스트

- [ ] `JobRecoveryScheduler`를 스케줄링만 담당하도록 수정
- [ ] `JobRecoveryService` 생성 (Job 조회 + Handler 위임)
- [ ] `JobRecoveryHandler` 인터페이스 생성
- [ ] `PendingJobRecoveryHandler` 구현
- [ ] `ProcessingJobRecoveryHandler` 구현
- [ ] `IntermediateJobRecoveryHandler` 구현
- [ ] `JobRecoveryHandlerRegistry` 구현
- [ ] `RetryPolicy` 생성
- [ ] `StaleThresholdPolicy` 생성
- [ ] `JobRecoveryProperties` 생성 (선택사항)
- [ ] 기존 테스트 수정 또는 새 테스트 작성
- [ ] 통합 테스트 작성

---

## 💡 추가 개선 제안

### 1. 메트릭 수집
- 복구 성공/실패 횟수
- 상태별 복구 시간
- 재시도 횟수 분포

### 2. 알림 기능
- 최대 재시도 초과 Job 알림
- 복구 실패 Job 알림

### 3. 배치 처리 최적화
- 대량 Job 처리 시 배치 단위로 나누어 처리
- 트랜잭션 범위 최적화

### 4. 복구 전략 확장
- 상태별로 다른 복구 전략 적용 가능
- 복구 실패 시 Dead Letter Queue 전송

---

**작성일**: 2024년
**분석 대상**: `JobRecoveryScheduler.java`
**분석 기준**: 단일 책임 원칙(SRP) 준수


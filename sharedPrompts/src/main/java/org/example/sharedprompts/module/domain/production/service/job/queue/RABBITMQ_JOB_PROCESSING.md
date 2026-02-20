# RabbitMQ 기반 비동기 Job 처리 솔루션

## 개요

Job 처리에서 발생하는 비동기/스레드 풀 문제를 RabbitMQ 기반으로 해결한 솔루션입니다.

## 해결된 문제

1. **Thread.sleep() 블로킹 문제**
   - `RetryExecutor`, `LeonardoImageAiClient` 등에서 `Thread.sleep()` 사용으로 스레드 블로킹 발생
   - **해결**: `Mono.delay()` 기반 non-blocking 재시도로 전환

2. **@Async void 예외 처리 부족**
   - `@Async void` 사용으로 비동기 예외 처리 부족, Job 실패 알림 누락 가능
   - **해결**: RabbitMQ Consumer에서 모든 예외 처리 및 DLQ 처리

3. **장시간 AI 호출로 인한 스레드 점유**
   - AI 호출 등 장시간 작업으로 스레드 점유 장기화
   - **해결**: RabbitMQ Worker 전용 스레드 풀 및 Prefetch 최적화

## 아키텍처

### 1. 메시지 흐름

```
Job 생성 → JobQueuePublisher → RabbitMQ Queue → JobQueueConsumer (Worker) → Job 처리
                                                      ↓
                                              실패 시 재시도 또는 DLQ
```

### 2. 주요 컴포넌트

#### 2.1 RabbitMQ 설정 (`RabbitMQConfig`)
- **Job Exchange**: `job.exchange` (Topic Exchange)
- **Job Queue**: `job.queue` (Durable, DLX 설정)
- **Job DLQ**: `job.dlq` (Dead Letter Queue)
- **Worker Container Factory**: `jobWorkerContainerFactory`
  - Prefetch: 1 (동시 처리량 최적화)
  - Concurrent Consumers: 3~10 (동적 확장)
  - Acknowledge Mode: MANUAL (트랜잭션 범위 최소화)

#### 2.2 Job 메시지 DTO (`JobMessage`)
- Job ID, 생성 시각, 재시도 횟수, 최대 재시도 횟수 포함
- 재시도용 메시지 생성 메서드 제공

#### 2.3 Job Publisher (`JobQueuePublisher`)
- Job 요청을 RabbitMQ 메시지 큐에 발행
- 스레드 블로킹 없이 즉시 반환
- 재시도 메시지 발행 지원

#### 2.4 Job Consumer (`JobQueueConsumer`)
- RabbitMQ에서 Job 메시지 소비
- Job 처리 실행
- 예외 처리 및 재시도/DLQ 처리
- TenantContext 설정 (JobEntity의 tenantId 사용)

#### 2.5 Reactive 재시도 서비스 (`ReactiveJobRetryService`)
- `Mono.delay()` 기반 non-blocking 재시도
- 지수 백오프(Exponential Backoff) 적용
- 스레드 블로킹 없이 재시도 스케줄링

#### 2.6 DLQ Consumer (`JobDlqConsumer`)
- DLQ로 이동한 실패한 Job 메시지 처리
- Job 상태를 FAILED로 업데이트
- DLQ 메시지 로깅 및 모니터링

## 재시도 로직

### 1. 재시도 전략

- **지수 백오프**: `delay = INITIAL_RETRY_DELAY_MS * 2^retryCount`
  - retryCount 0: 500ms
  - retryCount 1: 1000ms (1초)
  - retryCount 2: 2000ms (2초)
  - retryCount 3: 4000ms (4초)
  - 최대 지연 시간: 30초

### 2. 재시도 흐름

```
Job 처리 실패
    ↓
최대 재시도 횟수 확인
    ↓
미초과 → ReactiveJobRetryService.scheduleRetry()
    ↓
Mono.delay()로 non-blocking 지연
    ↓
재시도 메시지 발행
    ↓
초과 → DLQ로 이동
```

## 예외 처리

### 1. 예외 처리 전략

1. **재시도 가능한 오류**
   - 최대 재시도 횟수 미초과: `ReactiveJobRetryService`로 재시도 스케줄링
   - 메시지 nack (requeue=false) → 재시도 메시지가 새로 발행됨

2. **최대 재시도 초과 또는 영구적 오류**
   - DLQ로 이동 (nack, requeue=false)
   - Job 상태를 FAILED로 업데이트

3. **Job 상태 업데이트**
   - 실패 시 PENDING 상태에 남지 않도록 FAILED 상태로 변경

### 2. 예외 처리 흐름

```
Job 처리 중 예외 발생
    ↓
handleJobProcessingException()
    ↓
최대 재시도 횟수 확인
    ↓
미초과 → scheduleRetry() → 재시도 메시지 발행
초과 → updateJobToFailed() → DLQ로 이동
```

## Worker 스레드 풀 관리

### 1. 스레드 풀 설정

- **Container Factory**: `jobWorkerContainerFactory`
- **Prefetch**: 1
  - 한 번에 하나씩만 가져와서 Worker가 처리 중인 동안 다른 Worker가 메시지를 가져갈 수 있음
  - 장시간 AI 호출 시 여러 Worker가 동시에 작업할 수 있도록 함
- **Concurrent Consumers**: 3~10
  - 최소 3개, 최대 10개로 동적 확장
  - 부하에 따라 자동으로 Worker 수 조절
- **Acknowledge Mode**: MANUAL
  - 트랜잭션 범위 최소화
  - AI 호출 등 장시간 I/O는 트랜잭션 밖에서 수행

### 2. 트랜잭션 범위 최소화

- AI 호출 등 장시간 I/O는 트랜잭션 밖에서 수행
- DB 업데이트만 트랜잭션 내에서 수행
- `JobUpdateHelper`가 트랜잭션 관리

## 사용 방법

### 1. Job 생성 및 발행

```java
@Autowired
private JobQueueService jobQueueService;

// Job 생성 및 RabbitMQ 큐에 발행
String jobId = jobQueueService.enqueueJob(promptId, userId, command, userInput);
```

### 2. Job 복구

```java
@Autowired
private JobProcessor jobProcessor;

// FAILED 상태 Job 복구
jobProcessor.recoverJob(jobId);
```

## 변경 사항

### 1. 기존 코드 변경

- `JobQueueService`: `JobAsyncScheduler` 대신 `JobQueuePublisher` 사용
- `JobProcessor`: `@Async` 제거, RabbitMQ Publisher 사용
- `JobProcessorDelegate`: `@Async` 제거, 동기 메서드로 변경
- `PendingJobRecoveryHandler`: RabbitMQ Publisher 사용
- `ProcessingJobRecoveryHandler`: RabbitMQ Publisher 사용

### 2. 새로운 컴포넌트

- `JobMessage`: Job 메시지 DTO
- `JobQueuePublisher`: RabbitMQ 메시지 발행
- `JobQueueConsumer`: RabbitMQ 메시지 소비 (Worker)
- `ReactiveJobRetryService`: Reactor 기반 재시도
- `JobDlqConsumer`: DLQ 메시지 처리

## 모니터링

### 1. 로깅

- Job 메시지 발행/소비 로그
- 재시도 스케줄링 로그
- DLQ 이동 로그
- 예외 처리 로그

### 2. 메트릭

- Job 처리 성공/실패 메트릭
- 재시도 횟수 메트릭
- DLQ 메시지 수 메트릭
- Worker 스레드 풀 사용률

## 주의 사항

1. **TenantContext 설정**
   - JobEntity의 tenantId를 사용하여 TenantContext 설정
   - 멀티 테넌트 환경에서 필수

2. **트랜잭션 범위**
   - AI 호출 등 장시간 I/O는 트랜잭션 밖에서 수행
   - DB 업데이트만 트랜잭션 내에서 수행

3. **메시지 순서**
   - RabbitMQ는 메시지 순서를 보장하지 않음
   - 동일 Job의 재시도 메시지는 순서대로 처리되지 않을 수 있음
   - JobLockService가 동시 처리 방지

4. **DLQ 처리**
   - DLQ로 이동한 메시지는 수동으로 처리해야 함
   - Job 상태는 FAILED로 업데이트됨

## 향후 개선 사항

1. **메시지 순서 보장**
   - 단일 큐 대신 Job별 큐 사용 고려
   - 또는 메시지 순서 보장 메커니즘 추가

2. **재시도 정책 커스터마이징**
   - Job 타입별 재시도 정책 설정
   - 오류 타입별 재시도 정책 설정

3. **모니터링 강화**
   - Prometheus 메트릭 추가
   - Grafana 대시보드 구성

4. **DLQ 자동 복구**
   - DLQ 메시지 자동 재처리 스케줄러
   - DLQ 메시지 분석 및 알림


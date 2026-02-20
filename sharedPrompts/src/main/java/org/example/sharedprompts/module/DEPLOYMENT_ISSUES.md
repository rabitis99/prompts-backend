# 배포 시 문제점 및 장기적 이슈 분석

## 📋 개요

이 문서는 `module` 패키지의 코드를 분석하여 배포 시 발생할 수 있는 문제점과 장기적으로 해결해야 할 이슈를 정리합니다.

---

## 🚨 배포 시 즉시 발생 가능한 문제점

### 0. ✅ 해결됨: JobProcessor의 중복 완료 처리 버그

**위치**: `JobProcessorDelegate.processJobAsync()` (116번 라인)

**문제점** (이미 해결됨):
```java
jobStateService.markStored(job.getJobId(), s3Key);  // 이미 내부에서 complete() 호출
// jobStateService.markCompleted(job.getJobId());  // ❌ 중복 호출! (이미 제거됨)
```

- `markStored()` 내부에서 이미 `jobEntity.complete(artifactId)`를 호출함
- 이전에는 바로 다음에 `markCompleted()`를 또 호출하여 중복 완료 처리 버그 발생
- `markCompleted()`는 `PROCESSING` 상태를 기대하지만, 이미 `SUCCEEDED` 상태가 되어 `IllegalStateException` 발생 가능

**영향** (해결 전):
- **즉시 발생**: 모든 Job 완료 시 예외 발생
- Job이 완료되지 않고 실패 상태로 남을 수 있음
- 사용자 요청이 실패로 처리됨

**해결 상태**:
- ✅ **수정 완료**: `JobProcessorDelegate.java` 116번 라인에서 `markStored()`만 호출하고, 118번 라인에 주석으로 중복 호출이 제거되었음을 명시
- `markCompleted()` 호출이 제거되어 중복 완료 처리 문제 해결됨

### 1. 트랜잭션 관리 문제

#### 1.1 REQUIRES_NEW 전파로 인한 커넥션 풀 고갈 위험

**위치**: `JobStateService`, `JobLockService` 등

**문제점**:
- `@Transactional(propagation = Propagation.REQUIRES_NEW)` 사용 시 각 메서드마다 새로운 DB 커넥션을 획득
- 동시에 많은 Job이 처리되면 커넥션 풀 고갈 가능
- 특히 `JobStateService`의 여러 메서드가 모두 `REQUIRES_NEW` 사용

**영향**:
- 배포 직후 트래픽 증가 시 DB 커넥션 부족으로 전체 서비스 장애 가능
- HikariCP 기본 설정(10개)으로는 부족할 수 있음

**권장 조치**:
```yaml
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50  # 충분한 크기로 설정
      minimum-idle: 10
      connection-timeout: 30000
      leak-detection-threshold: 60000
```

**코드 개선**:
- `REQUIRES_NEW` 사용을 최소화하고, 필요한 경우에만 사용
- 트랜잭션 범위를 재검토하여 불필요한 분리 제거

#### 1.2 중복 트랜잭션 전파로 인한 불필요한 커넥션 사용

**위치**: `JobStateService` + `JobUpdateHelper`

**문제점**:
- `JobStateService`의 메서드들이 이미 `@Transactional(propagation = Propagation.REQUIRES_NEW)`를 가지고 있음
- `JobUpdateHelper.updateJob()`도 `@Transactional(propagation = Propagation.REQUIRES_NEW)`를 가지고 있음
- 중복된 트랜잭션 전파로 불필요한 커넥션 사용

**예시**:
```java
// JobStateService.java
@Transactional(propagation = Propagation.REQUIRES_NEW)  // 트랜잭션 1 시작
public void updateJobPromptVersion(String jobId, String promptVersion) {
    jobUpdateHelper.updateJob(jobId, job -> job.setPromptVersion(promptVersion));
    // JobUpdateHelper.updateJob() 내부에서 또 REQUIRES_NEW로 트랜잭션 2 시작
    // 트랜잭션 1은 실제로 사용되지 않음
}
```

**영향**:
- 불필요한 DB 커넥션 사용
- 트랜잭션 오버헤드 증가
- 커넥션 풀 고갈 위험 증가

**권장 조치**:
- `JobUpdateHelper`의 `@Transactional` 제거 (이미 상위에서 트랜잭션 관리)
- 또는 `JobStateService`의 `@Transactional` 제거하고 `JobUpdateHelper`에서만 관리

#### 1.3 Optimistic Locking 누락으로 인한 동시성 문제

**위치**: `JobUpdateHelper.updateJob()`

**문제점**:
- `JobEntity`에 `@Version` 필드가 있지만, `JobUpdateHelper`에서 Optimistic Locking을 활용하지 않음
- `findByJobId()`는 Lock 없이 조회
- 동시 업데이트 시 `ObjectOptimisticLockingFailureException`이 발생할 수 있지만, 이를 처리하는 로직이 없음

**시나리오**:
1. Thread A: `updateJobPromptVersion()` 호출 → Job 조회 (version=1)
2. Thread B: `setModelInfo()` 호출 → Job 조회 (version=1)
3. Thread A: Job 저장 → version=2로 업데이트
4. Thread B: Job 저장 → `ObjectOptimisticLockingFailureException` 발생 (version이 이미 2로 변경됨)

**영향**:
- 동시 업데이트 시 예외 발생
- 예외 처리 로직이 없어 Job 업데이트 실패
- 재시도 로직 없음

**권장 조치**:
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void updateJob(String jobId, Consumer<JobEntity> updater) {
    int maxRetries = 3;
    for (int i = 0; i < maxRetries; i++) {
        try {
            JobEntity job = jobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
            updater.accept(job);
            jobRepository.save(job);  // Optimistic Locking 자동 적용
            return;
        } catch (ObjectOptimisticLockingFailureException e) {
            if (i == maxRetries - 1) {
                throw new IllegalStateException("Failed to update job after " + maxRetries + " retries", e);
            }
            log.warn("Optimistic lock conflict, retrying - jobId: {}, attempt: {}", jobId, i + 1);
            // 짧은 지연 후 재시도
            try {
                Thread.sleep(10 + (i * 10));  // 10ms, 20ms, 30ms
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted during retry", ie);
            }
        }
    }
}
```

#### 1.4 findLockedJob()에 Lock 없음

**위치**: `JobRepository.findLockedJob()`

**문제점**:
- `JobLockService.acquireJobLock()`에서 `acquireJobLock()` native query로 Lock을 획득
- 하지만 `findLockedJob()`에는 `@Lock` 어노테이션이 없음
- Lock이 해제되기 전에 다른 트랜잭션에서 업데이트 가능

**영향**:
- Lock 획득 후에도 동시 업데이트 가능
- Race condition 발생 가능

**권장 조치**:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT j FROM JobEntity j WHERE j.jobId = :jobId AND j.status = 'PROCESSING'")
Optional<JobEntity> findLockedJob(@Param("jobId") String jobId);
```

#### 1.5 트랜잭션 타임아웃 미설정

**문제점**:
- 대부분의 `@Transactional`에 타임아웃 설정 없음
- AI 호출 등 장시간 작업 시 트랜잭션이 무한정 유지될 수 있음

**영향**:
- DB 커넥션 장기 점유로 커넥션 풀 고갈
- 데드락 발생 가능성 증가

**권장 조치**:
```java
@Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
public void markStored(String jobId, String filePath) {
    // ...
}
```

### 2. 비동기 처리 및 스레드 풀 관리

#### 2.1 Thread.sleep() 사용으로 인한 스레드 풀 고갈

**위치**: `RetryExecutor`, `LeonardoImageAiClient` 등

**문제점**:
- `Thread.sleep()` 사용으로 스레드가 블로킹됨
- 재시도 로직에서 스레드가 장시간 점유
- AI 호출 대기 중에도 스레드가 점유됨

**영향**:
- 스레드 풀 고갈로 새로운 요청 처리 불가
- 동시 처리량 급격히 감소

**권장 조치**:
- 비동기 재시도 패턴으로 전환 (예: `Mono.delay()` 기반)
- 또는 별도의 스레드 풀에서 재시도 처리

#### 2.2 비동기 작업 예외 처리 부족

**위치**: `JobAsyncScheduler`, `JobProcessor`

**문제점**:
- `@Async` 메서드에서 발생한 예외가 제대로 처리되지 않을 수 있음
- `CustomAsyncUncaughtExceptionHandler`가 있지만, 모든 예외를 잡지 못할 수 있음

**영향**:
- Job 실패 시 사용자에게 알림이 가지 않을 수 있음
- 실패한 Job이 영구적으로 PENDING 상태로 남을 수 있음

**권장 조치**:
- `JobRecoveryScheduler`가 제대로 동작하는지 확인
- 비동기 작업의 예외 처리 로직 강화

### 3. 외부 의존성 및 리소스 관리

#### 3.1 S3 클라이언트 리소스 관리

**위치**: `S3Config`

**문제점**:
- `@Bean(destroyMethod = "close")`로 설정되어 있지만, 애플리케이션 종료 시 제대로 정리되지 않을 수 있음
- S3 클라이언트가 제대로 닫히지 않으면 리소스 누수 가능

**영향**:
- 장기 운영 시 리소스 누수
- 애플리케이션 재시작 시 이전 연결이 남아있을 수 있음

**권장 조치**:
- `@PreDestroy` 메서드 추가하여 명시적 정리
- Health check를 통한 연결 상태 모니터링

#### 3.2 AI 서비스 타임아웃 설정

**위치**: `application.yml`의 AI Provider 설정

**문제점**:
- 타임아웃이 설정되어 있지만, 실제 클라이언트에서 제대로 적용되는지 확인 필요
- 타임아웃 발생 시 재시도 로직과의 조합이 복잡함

**영향**:
- AI 서비스 장애 시 요청이 무한정 대기할 수 있음
- 스레드 풀 고갈 가능

**권장 조치**:
- Circuit Breaker와 타임아웃의 조합 검증
- 타임아웃 발생 시 즉시 실패 처리

### 4. 동시성 제어 문제

#### 4.1 Job Lock 메커니즘의 Race Condition

**위치**: `JobLockService.acquireJobLock()`

**문제점**:
- Optimistic Locking을 사용하지만, 동시에 여러 스레드가 같은 Job을 처리하려 할 때 경쟁 가능
- `acquireJobLock()`과 실제 처리 사이에 시간차가 있음

**영향**:
- 동일 Job이 중복 처리될 수 있음
- 데이터 일관성 문제

**권장 조치**:
- 분산 락(Redis) 추가 고려
- 또는 Pessimistic Locking으로 전환 검토

#### 4.2 Version 필드 업데이트 누락 가능성

**위치**: `JobEntity`

**문제점**:
- `@Version` 필드가 있지만, 모든 업데이트에서 버전이 증가하는지 확인 필요
- Optimistic Locking이 제대로 동작하지 않을 수 있음

**영향**:
- 동시성 제어 실패
- 데이터 무결성 문제

**권장 조치**:
- 모든 업데이트 쿼리에서 `version = version + 1` 확인
- 테스트를 통한 동시성 검증

### 5. 에러 처리 및 복구

#### 5.1 Job 실패 시 복구 메커니즘 부족

**위치**: `JobStateService.saveJobFailure()`

**문제점**:
- Job 실패 시 단순히 상태만 변경
- 재시도 로직이 있지만, 최대 재시도 횟수 초과 시 영구 실패
- 실패한 Job에 대한 모니터링/알림 부족

**영향**:
- 사용자 요청이 영구적으로 실패할 수 있음
- 문제 발생 시 빠른 대응 어려움

**권장 조치**:
- 실패한 Job에 대한 알림 시스템 구축
- Dead Letter Queue 패턴 적용 검토
- 관리자 대시보드를 통한 실패 Job 모니터링

#### 5.2 예외 처리 일관성 부족

**위치**: 전역

**문제점**:
- `BaseException`과 `ApiException`이 혼재
- 일부는 `IllegalStateException` 등 일반 예외 사용
- 예외 처리 전략이 일관되지 않음

**영향**:
- 예외 처리 로직이 복잡해짐
- 디버깅 어려움

**권장 조치**:
- 예외 처리 전략 통일
- 모든 비즈니스 예외는 `BaseException` 사용
- `IllegalStateException` 등은 내부 로직 검증용으로만 사용

### 6. 설정 및 환경 변수

#### 6.1 필수 환경 변수 누락 가능성

**위치**: `application.yml`

**문제점**:
- 많은 설정이 환경 변수로 관리되지만, 필수 여부가 명확하지 않음
- 일부는 기본값이 있지만, 프로덕션에서 적절하지 않을 수 있음

**영향**:
- 배포 시 설정 누락으로 인한 런타임 에러
- 잘못된 기본값으로 인한 성능 문제

**권장 조치**:
- 필수 환경 변수 목록 문서화
- 애플리케이션 시작 시 필수 설정 검증
- 설정 검증 로직 추가

#### 6.2 프로덕션 환경별 설정 차이

**위치**: `application-prod.yml`

**문제점**:
- 개발/프로덕션 환경별 설정이 충분히 분리되지 않았을 수 있음
- 프로덕션 환경에서만 필요한 설정이 개발 환경에 노출될 수 있음

**영향**:
- 보안 문제
- 성능 문제

**권장 조치**:
- 환경별 설정 파일 명확히 분리
- 프로덕션 전용 설정 검증

---

## ⚠️ 장기적으로 해결해야 할 문제점

### 1. 코드 품질 및 유지보수성

#### 1.1 코드 중복

**위치**: 전역

**문제점**:
- `extractS3Key()`, `extractFileName()`, `parseS3Path()` 등 S3 경로 파싱 로직이 여러 곳에 중복
- Presigned URL 생성 로직이 여러 곳에 분산되어 있음
- 소유권 검증 로직이 중복되어 있음

**영향**:
- 버그 수정 시 여러 곳 수정 필요
- 일관성 유지 어려움
- 코드 복잡도 증가

**권장 조치**:
- [IMPROVEMENT_PLAN.md](./IMPROVEMENT_PLAN.md)의 Phase 1 실행
  - `S3PathUtils` 유틸리티 클래스 생성
  - `PresignedStrategy` 패턴을 통한 Presigned URL 생성 로직 통합
  - 소유권 검증 로직 통합
- 코드 리뷰 시 중복 체크

**상세 내용**: [IMPROVEMENT_PLAN.md](./IMPROVEMENT_PLAN.md)의 "4. S3 경로 파싱 로직 중복", "2. Presigned URL 생성 로직 중복 및 불일치", "3. 소유권 검증 중복" 섹션 참조

#### 1.2 복잡한 의존성 구조

**위치**: Service 계층

**문제점**:
- Service 간 의존성이 복잡함
- 순환 의존성 가능성
- 테스트 어려움

**영향**:
- 코드 변경 시 사이드 이펙트 발생 가능
- 단위 테스트 작성 어려움
- 리팩토링 어려움

**권장 조치**:
- 의존성 그래프 분석
- 순환 의존성 제거
- Facade 패턴 활용

### 2. 성능 및 확장성

#### 2.1 DTO 매핑 시 S3 I/O 발생 ✅ 해결됨

**위치**: `ImageArtifactHandler.toDto()`

**문제점** (해결됨):
- ~~DTO 매핑 시점에 S3에서 HTML 파일을 다운로드하여 이미지 경로 추출~~
- ~~응답 지연 및 장애 전파 위험~~
- ~~코드 주석에 이미 언급됨~~

**영향** (해결됨):
- ~~API 응답 시간 증가~~
- ~~S3 장애 시 전체 API 장애로 전파~~
- ~~동시 요청 시 S3 부하 증가~~

**해결 조치** (완료):
- ✅ 엔티티 생성 시점(`createDetail()`)에 이미지 경로 미리 추출하여 `actualImagePath` 필드에 저장
- ✅ DTO 매핑(`toDto()`)은 메모리 기반 연산만 수행 (엔티티의 `actualImagePath` 필드 사용)
- ✅ `ProductionArtifactDetailEntity`에 `actualImagePath` 필드 추가
- ✅ `ProductionArtifactDetailEntityFactory`에 `actualImagePath` 파라미터 추가

#### 2.2 N+1 쿼리 문제

**위치**: Repository 계층

**문제점**:
- `findByIdWithArtifacts()` 등이 있지만, 모든 곳에서 사용되는지 확인 필요
- Lazy Loading으로 인한 N+1 쿼리 가능성

**영향**:
- DB 부하 증가
- 응답 시간 증가

**권장 조치**:
- JPA 쿼리 로그 모니터링
- Fetch Join 활용
- `@EntityGraph` 활용

#### 2.3 대용량 데이터 처리 미고려

**위치**: 전역

**문제점**:
- 페이징 처리가 일부만 구현됨
- 대용량 파일 업로드/다운로드 처리 미고려
- 배치 처리 로직 부족

**영향**:
- 메모리 부족
- 처리 시간 증가
- 사용자 경험 저하

**권장 조치**:
- 스트리밍 방식으로 대용량 파일 처리
- 배치 처리 로직 추가
- 페이징 처리 일관성 확보

### 3. 모니터링 및 관찰성

#### 3.1 로깅 일관성 부족

**위치**: 전역

**문제점**:
- 로그 레벨이 일관되지 않음
- 구조화된 로깅 부족
- 중요한 이벤트 로깅 누락 가능

**영향**:
- 문제 진단 어려움
- 모니터링 어려움

**권장 조치**:
- 로깅 전략 수립
- 구조화된 로깅 도입 (예: JSON 로그)
- 중요 이벤트 로깅 강화

#### 3.2 메트릭 수집 부족

**위치**: 전역

**문제점**:
- `JobMetrics` 등 일부 메트릭이 있지만, 전반적인 메트릭 수집 부족
- 비즈니스 메트릭 부족

**영향**:
- 성능 모니터링 어려움
- 문제 예측 어려움

**권장 조치**:
- Micrometer를 활용한 메트릭 수집 강화
- 비즈니스 메트릭 추가
- 대시보드 구축

### 4. 보안

#### 4.1 Deprecated 메서드의 보안 취약점

**위치**: `StorageCommandService`

**문제점**:
- `generateDownloadPresignedUrl(String s3Key)` 등 Deprecated 메서드가 여전히 존재
- 소유권 검증 없이 Presigned URL 생성 가능
- 보안 취약점으로 인한 무단 접근 가능

**영향**:
- 보안 취약점
- 무단 접근 가능

**권장 조치**:
- Deprecated 메서드 제거 또는 완전히 차단
- [IMPROVEMENT_PLAN.md](./IMPROVEMENT_PLAN.md)의 Phase 1에서 보안 검증이 포함된 메서드로 대체

**상세 내용**: [IMPROVEMENT_PLAN.md](./IMPROVEMENT_PLAN.md)의 "5. Deprecated 메서드" 섹션 참조

#### 4.2 입력 검증 부족

**위치**: Controller 계층

**문제점**:
- 일부 DTO에 `@Valid`가 있지만, 모든 입력에 적용되는지 확인 필요
- SQL Injection, XSS 등에 대한 방어 로직 확인 필요

**영향**:
- 보안 취약점
- 데이터 무결성 문제

**권장 조치**:
- 모든 입력에 대한 검증 강화
- 보안 테스트 수행

### 5. 테스트

#### 5.1 통합 테스트 부족

**문제점**:
- 단위 테스트는 일부 있지만, 통합 테스트 부족
- 실제 환경과 유사한 테스트 환경 부족

**영향**:
- 배포 시 예상치 못한 문제 발생
- 회귀 버그 발생 가능

**권장 조치**:
- 통합 테스트 추가
- 테스트 환경 구축
- CI/CD 파이프라인에 통합

#### 5.2 부하 테스트 부족

**문제점**:
- 부하 테스트가 수행되지 않았을 가능성
- 실제 트래픽에 대한 준비 부족

**영향**:
- 트래픽 증가 시 서비스 장애
- 성능 병목 미발견

**권장 조치**:
- 부하 테스트 수행
- 성능 기준 설정
- 정기적인 부하 테스트

### 6. 문서화

#### 6.1 API 문서화 부족

**문제점**:
- Swagger/OpenAPI 문서가 있는지 확인 필요
- API 변경 시 문서 업데이트 누락 가능

**영향**:
- 클라이언트 개발 어려움
- API 사용 오류 발생

**권장 조치**:
- Swagger/OpenAPI 문서화
- API 버전 관리
- 문서 자동 생성

#### 6.2 아키텍처 문서 부족

**문제점**:
- `IMPROVEMENT_PLAN.md`는 있지만, 전체 아키텍처 문서 부족
- 의사결정 기록 부족

**영향**:
- 신규 개발자 온보딩 어려움
- 기술 부채 증가

**권장 조치**:
- 아키텍처 문서 작성
- ADR (Architecture Decision Records) 도입
- 정기적인 문서 업데이트

---

## 📊 우선순위별 조치 사항

### 🔴 긴급 (배포 전 반드시 해결)

1. **JobProcessor의 중복 완료 처리 버그 수정** ⚠️ 최우선
   - `JobProcessor.processJobAsync()`에서 `markCompleted()` 호출 제거
   - `markStored()`가 이미 완료 처리를 수행함

2. **DB 커넥션 풀 크기 조정**
   - 프로덕션 환경에서 충분한 크기로 설정
   - 모니터링 설정

2. **트랜잭션 타임아웃 설정**
   - 모든 `@Transactional`에 타임아웃 추가
   - 특히 `REQUIRES_NEW` 사용하는 곳

3. **필수 환경 변수 검증**
   - 애플리케이션 시작 시 필수 설정 검증
   - 누락 시 명확한 에러 메시지

4. **Deprecated 메서드 제거 또는 차단**
   - 보안 취약점 제거

### 🟡 중요 (배포 후 1개월 내)

1. **Thread.sleep() 제거**
   - 비동기 재시도 패턴으로 전환
   - 스레드 풀 고갈 방지

2. **Job 실패 모니터링 강화**
   - 실패 Job 알림 시스템
   - 관리자 대시보드

3. **코드 중복 제거**
   - [IMPROVEMENT_PLAN.md](./IMPROVEMENT_PLAN.md) Phase 1 실행
   - 공통 유틸리티 클래스 생성

4. **DTO 매핑 시 S3 I/O 제거**
   - 엔티티 생성 시점에 데이터 추출

### 🟢 개선 (3개월 내)

1. **모니터링 및 메트릭 강화**
   - 구조화된 로깅
   - 메트릭 수집 강화
   - 대시보드 구축

2. **테스트 강화**
   - 통합 테스트 추가
   - 부하 테스트 수행

3. **문서화**
   - API 문서화
   - 아키텍처 문서 작성

4. **성능 최적화**
   - N+1 쿼리 해결
   - 대용량 데이터 처리 개선

---

## 🔍 체크리스트

### 배포 전 체크리스트

- [ ] ⚠️ **JobProcessor 중복 완료 처리 버그 수정** (최우선)
- [ ] DB 커넥션 풀 크기 확인 및 조정
- [ ] JobUpdateHelper의 중복 트랜잭션 전파 제거
- [ ] Optimistic Locking 재시도 로직 추가
- [ ] 트랜잭션 타임아웃 설정 확인
- [ ] 필수 환경 변수 목록 작성 및 검증
- [ ] Deprecated 메서드 제거 또는 차단
- [ ] 로그 레벨 및 출력 형식 확인
- [ ] Health check 엔드포인트 확인
- [ ] 모니터링 설정 확인
- [ ] 백업 및 복구 계획 수립

### 배포 후 체크리스트

- [ ] 애플리케이션 로그 모니터링
- [ ] DB 커넥션 풀 사용률 모니터링
- [ ] 스레드 풀 사용률 모니터링
- [ ] API 응답 시간 모니터링
- [ ] 에러율 모니터링
- [ ] 리소스 사용률 모니터링

---

## 📚 참고 자료

- [IMPROVEMENT_PLAN.md](./IMPROVEMENT_PLAN.md) - API 개선 계획
- Spring Boot Best Practices
- Database Connection Pool Tuning Guide
- AWS S3 Best Practices


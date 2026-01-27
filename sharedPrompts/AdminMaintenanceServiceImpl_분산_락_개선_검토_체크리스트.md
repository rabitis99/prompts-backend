# AdminMaintenanceServiceImpl 분산 락 개선 검토 체크리스트

본 문서는 `AdminMaintenanceServiceImpl`의 분산 락 개선 사항을 체크리스트 형태로 정리한 검토용 문서입니다.
팀 리뷰용으로 바로 활용 가능하도록 항목별 개선 포인트와 분리 포인트를 명확히 구분했습니다.

---

## 1. 동시성/분산 락

| 항목 | 현재 상태 | 개선 포인트 / 확인 사항 |
|------|-----------|------------------------|
| **동시성 보장** | `synchronized(this)` → 로컬 인스턴스 수준 | 분산 환경에서 단일 인스턴스 실행 보장 필요 → `@SchedulerLock` 적용 ✅ |
| **ShedLock 설정** | `lockAtMostFor = 1h`, `lockAtLeastFor = 30m` | - `lockAtMostFor`/`lockAtLeastFor` 값 적절성 확인<br>- 장시간 작업 시 lock 만료/중복 실행 방지 |
| **실패 시 행동** | 없음 | - ShedLock 실패 시 로그/알람 필요 여부<br>- 재시도 큐 고려 가능 |

### 확인 사항
- [x] `lockAtMostFor` 시간이 실제 작업 시간보다 충분히 긴지 확인 (1h로 설정, 작업 소요 시간 메트릭으로 추적) ✅
- [x] `lockAtLeastFor` 시간이 너무 길어서 다음 스케줄 실행을 방해하지 않는지 확인 (30m로 설정) ✅
- [x] ShedLock 실패 시 모니터링/알람 연동 여부 결정 (메트릭 추가 완료: `admin.maintenance.rebuild.shedlock.failure`) ✅
- [x] 재시도 메커니즘 필요 여부 검토 ✅ (`RetryableRebuildService` 구현 완료, 설정으로 활성화/비활성화 가능)

---

## 2. 상태 관리

| 항목 | 현재 상태 | 개선 포인트 / 확인 사항 |
|------|-----------|------------------------|
| **상태 변수** | `rebuildStatus`, `rebuildStartedAt`, `rebuildFinishedAt`, `rebuildErrorMessage` | `volatile` 사용으로 스레드 읽기/쓰기 안전성 확보 ✅ |
| **글로벌 공유** | 없음, 로컬 인스턴스만 관리 | 필요 시 Redis/DB 기반 글로벌 상태 관리 가능 여부 검토 |
| **상태 전이** | `RUNNING` → `COMPLETED`/`FAILED` | 정상 전이 여부 테스트 필요 |

### 확인 사항
- [x] 모든 상태 변수에 `volatile` 키워드 적용 확인 ✅ (`LocalRebuildStatusManager`에서 관리)
- [x] 멀티 인스턴스 환경에서 상태 조회 시 글로벌 상태 관리 필요 여부 검토 ✅ (`GlobalRebuildStatusService` 구현 완료, Redis 기반)
- [x] 상태 전이 로직이 모든 케이스를 커버하는지 확인 (RUNNING → COMPLETED/FAILED 구현 완료) ✅
- [x] 예외 발생 시 상태가 `FAILED`로 정상 전이되는지 확인 (구현 완료) ✅

---

## 3. 메서드 구조 / 분리

| 항목 | 현재 상태 | 개선 포인트 / 분리 제안 |
|------|-----------|------------------------|
| **rebuildLikeCountsFromDbAsync** | 락 관리 + 상태 관리 + 작업 수행 모두 포함 | - 락 관리 + 상태 관리 → 별도 private 메서드 분리 가능<br>- 프롬프트 재빌드 / 댓글 재빌드 → 별도 메서드 분리 가능<br>- 테스트 용이성 증가, 코드 가독성 향상 |

### 분리 제안 구조

```java
// 현재 구조
@SchedulerLock(...)
public void rebuildLikeCountsFromDbAsync() {
    // 락 관리
    // 상태 관리
    // 프롬프트 재빌드
    // 댓글 재빌드
}

// 개선 제안 구조
@SchedulerLock(...)
public void rebuildLikeCountsFromDbAsync() {
    if (!acquireLockAndUpdateStatus()) {
        return;
    }
    
    try {
        rebuildPromptLikeCounts();
        rebuildCommentLikeCounts();
        updateStatusToCompleted();
    } catch (Exception e) {
        updateStatusToFailed(e);
        throw e;
    }
}

private boolean acquireLockAndUpdateStatus() { ... }
private void rebuildPromptLikeCounts() { ... }
private void rebuildCommentLikeCounts() { ... }
private void updateStatusToCompleted() { ... }
private void updateStatusToFailed(Exception e) { ... }
```

### 확인 사항
- [x] 락 관리 로직을 별도 메서드로 분리 ✅ (`LocalRebuildStatusManager.acquireLockAndUpdateStatus()`)
- [x] 상태 관리 로직을 별도 메서드로 분리 ✅ (`LocalRebuildStatusManager` 클래스로 완전 분리)
- [x] 프롬프트 재빌드 로직을 별도 메서드로 분리 ✅ (`PromptRebuildExecutor` 클래스로 완전 분리)
- [x] 댓글 재빌드 로직을 별도 메서드로 분리 ✅ (`CommentRebuildExecutor` 클래스로 완전 분리)
- [x] 각 메서드가 단일 책임을 가지도록 설계 ✅ (각 클래스가 단일 책임 원칙 준수)

---

## 4. 예외 처리

| 항목 | 현재 상태 | 개선 포인트 / 확인 사항 |
|------|-----------|------------------------|
| **예외 처리** | `try-catch`로 상태 업데이트 후 로그 | - Sentry/모니터링 연동 가능<br>- 실패 시 재시도 전략 검토<br>- 실패 메시지 상세화 (예: 어느 페이지에서 실패) |

### 확인 사항
- [x] 예외 발생 시 Sentry/모니터링 시스템에 자동 전송 여부 확인 (메트릭 추가 완료, Sentry 연동은 별도 구성 필요) ✅
- [x] 실패한 작업에 대한 재시도 메커니즘 필요 여부 검토 ✅ (`RetryableRebuildService` 구현 완료, 설정 가능)
- [x] 실패 메시지에 구체적인 실패 지점 정보 포함 (페이지 번호, 데이터 ID 등) ✅
- [x] 예외 타입별 처리 전략 수립 (일시적 오류 vs 영구적 오류) ✅ (재시도 메커니즘으로 일시적 오류 처리)

---

## 5. 테스트 / 검증

| 항목 | 체크 포인트 |
|------|-------------|
| **단일 스레드 실행** | `synchronized(this)`로 중복 실행 방지 확인 |
| **멀티 스레드 실행** | 단일 인스턴스 내 중복 실행 방지 확인 |
| **멀티 인스턴스 실행** | ShedLock 적용으로 단일 서버만 실행 확인 |
| **상태 전이** | `RUNNING` → `COMPLETED` / `FAILED` 정상 전이 확인 |
| **Like count 재빌드** | 프롬프트/댓글의 Redis 카운트 정상 반영 확인 |
| **장기 실행** | `lockAtMostFor` 시간보다 오래 걸리는 작업 처리 검증 |

### 테스트 시나리오

#### 5.1 단일 인스턴스 테스트
- [ ] 동일 스레드에서 연속 호출 시 한 번만 실행되는지 확인
- [ ] 여러 스레드에서 동시 호출 시 한 번만 실행되는지 확인

#### 5.2 멀티 인스턴스 테스트
- [ ] 여러 서버 인스턴스에서 동시 실행 시도 시 단일 서버만 실행되는지 확인
- [ ] ShedLock이 정상적으로 작동하는지 확인

#### 5.3 상태 전이 테스트
- [ ] 정상 완료 시 `RUNNING` → `COMPLETED` 전이 확인
- [ ] 예외 발생 시 `RUNNING` → `FAILED` 전이 확인
- [ ] 상태 변수들이 정상적으로 업데이트되는지 확인

#### 5.4 데이터 정합성 테스트
- [ ] 프롬프트 Like count가 DB와 Redis에서 일치하는지 확인
- [ ] 댓글 Like count가 DB와 Redis에서 일치하는지 확인
- [ ] 대량 데이터 처리 시 성능 및 정확성 확인

#### 5.5 장기 실행 테스트
- [ ] `lockAtMostFor` 시간보다 오래 걸리는 작업 시 lock 만료 방지 확인
- [ ] 작업이 완료되기 전에 lock이 해제되지 않는지 확인

---

## 6. 추가 개선 포인트

### 6.1 로그 개선
- **목적**: 실패 시 어떤 데이터에서 오류 발생했는지 파악 용이
- **제안**: 페이지별 진행 로그 추가
  ```java
  log.info("프롬프트 Like count 재빌드 시작 - 페이지: {}/{}", currentPage, totalPages);
  log.info("댓글 Like count 재빌드 시작 - 페이지: {}/{}", currentPage, totalPages);
  ```

### 6.2 성능 최적화
- **목적**: 대규모 데이터 처리 시 효율성 향상
- **제안**: 
  - PageSize / ChunkSize 조정
  - 배치 처리 최적화
  - 병렬 처리 고려 (단, 동시성 제어 필요)

### 6.3 분리된 유틸/서비스 ✅
- **목적**: 락 + 상태 관리 + 재빌드 메서드 각각 독립적으로 테스트 가능하도록 설계
- **구현 완료**:
  - `LocalRebuildStatusManager`: 로컬 상태 관리 및 락 획득 로직
  - `GlobalRebuildStatusService`: Redis 기반 글로벌 상태 관리
  - `PromptRebuildExecutor`: 프롬프트 재빌드 로직
  - `CommentRebuildExecutor`: 댓글 재빌드 로직
  - `RebuildBatchProcessor`: 배치 처리 유틸리티
  - `RebuildMetrics`: 메트릭 관리
  - `RetryableRebuildService`: 재시도 메커니즘
  - `RebuildNotificationService`: 알림 서비스
  - 각 컴포넌트를 독립적으로 테스트 가능하도록 설계 완료

### 6.4 모니터링 및 알람
- **제안**:
  - 작업 시작/완료/실패 시 메트릭 수집 ✅ (Micrometer 메트릭 추가 완료)
  - 실패 시 알람 발송 (Slack, Email 등) - 메트릭 기반 알람 설정 필요
  - 작업 소요 시간 추적 ✅ (`admin.maintenance.rebuild.duration` 타이머 추가)
  - 처리된 데이터 건수 추적 ✅ (`totalProcessedPrompts`, `totalProcessedComments` 추가)

### 6.5 설정 외부화
- **제안**:
  - `lockAtMostFor`, `lockAtLeastFor` 값을 `application.yml`로 외부화 ✅ (Properties 클래스 생성 완료)
  - PageSize, ChunkSize 등도 설정 파일로 관리 ✅ (`admin.maintenance.rebuild.page-size` 설정 추가)
  - 환경별로 다른 값 적용 가능하도록 구성 ✅ (환경 변수 지원)

---

## 7. 우선순위별 작업 계획

### 높은 우선순위 (즉시 적용)
1. ✅ `@SchedulerLock` 적용 (완료)
2. ✅ `volatile` 키워드 적용 (완료)
3. [ ] 상태 전이 테스트 작성 및 검증 (테스트 코드 작성 필요)
4. ✅ 예외 처리 개선 (상세 메시지, 모니터링 연동) - 완료

### 중간 우선순위 (단기)
1. ✅ 메서드 분리 (락 관리, 상태 관리, 재빌드 로직) - 완료
2. ✅ 로그 개선 (페이지별 진행 로그) - 완료
3. [ ] 멀티 인스턴스 테스트 작성 (테스트 코드 작성 필요)

### 낮은 우선순위 (중장기)
1. [x] 글로벌 상태 관리 (Redis/DB 기반) ✅ (`GlobalRebuildStatusService` 구현 완료)
2. [x] 재시도 메커니즘 구현 ✅ (`RetryableRebuildService` 구현 완료)
3. [x] 성능 최적화 (배치 처리, 병렬 처리) ✅ (배치 처리 구현 완료, 병렬 처리는 선택적)
4. [x] 유틸/서비스 분리 (독립 테스트 가능 구조) ✅ (완전히 분리 완료)

---

## 8. 체크리스트 요약

### 필수 확인 사항
- [x] ShedLock 설정 값 적절성 검토 ✅ (1h/30m 설정, 메트릭으로 추적)
- [x] 모든 상태 변수에 `volatile` 적용 확인 ✅
- [ ] 상태 전이 로직 테스트 완료 (테스트 코드 작성 필요)
- [x] 멀티 인스턴스 환경에서 단일 실행 보장 확인 ✅ (`@SchedulerLock` 적용)
- [x] 예외 처리 및 로깅 개선 ✅

### 권장 개선 사항
- [x] 메서드 분리로 코드 가독성 향상 ✅
- [x] 페이지별 진행 로그 추가 ✅
- [x] 모니터링/알람 연동 ✅ (메트릭 추가 완료, 알람 설정은 별도 구성 필요)
- [x] 설정 값 외부화 ✅

### 선택적 개선 사항
- [x] 글로벌 상태 관리 구현 ✅ (`GlobalRebuildStatusService`, 설정으로 활성화/비활성화)
- [x] 재시도 메커니즘 구현 ✅ (`RetryableRebuildService`, 설정으로 활성화/비활성화)
- [x] 성능 최적화 (배치/병렬 처리) ✅ (배치 처리 구현 완료, `RebuildBatchProcessor`)
- [x] 유틸/서비스 분리 ✅ (완전히 분리 완료, 각 컴포넌트 독립 테스트 가능)

---

## 9. 참고 자료

- ShedLock 공식 문서: https://github.com/lukas-krecan/ShedLock
- Spring @SchedulerLock 사용법
- 분산 락 패턴 및 모범 사례
- 트랜잭션 동기화 및 상태 관리 패턴

---

## 10. 최종 구현 현황 및 아키텍처 (2024년 업데이트)

### 현재 아키텍처 구조

```
domain/admin/maintenance/
├── config/
│   ├── AdminMaintenanceProperties.java          # 설정 Properties
│   └── RebuildStatusServiceConfig.java          # 상태 서비스 설정
├── metrics/
│   └── RebuildMetrics.java                      # 메트릭 관리
├── rebuild/
│   ├── global/
│   │   ├── PromptRebuildExecutor.java           # 프롬프트 재빌드 실행기
│   │   ├── CommentRebuildExecutor.java          # 댓글 재빌드 실행기
│   │   ├── GlobalRebuildStatusService.java      # Redis 기반 글로벌 상태 관리
│   │   └── StatusData.java                      # 상태 데이터 모델
│   ├── util/
│   │   ├── LocalRebuildStatusManager.java      # 로컬 상태 관리
│   │   └── RebuildBatchProcessor.java           # 배치 처리 유틸리티
│   ├── RebuildNotificationService.java          # 알림 서비스
│   ├── RetryableRebuildService.java             # 재시도 메커니즘
│   └── LocalRebuildStatusService.java           # 로컬 상태 서비스 구현
└── service/
    ├── AdminMaintenanceService.java              # 인터페이스
    ├── AdminMaintenanceServiceImpl.java          # 오케스트레이션 (약 180줄)
    └── RebuildStatusService.java                 # 상태 서비스 인터페이스
```

### 컴포넌트 역할

| 컴포넌트 | 역할 | 책임 |
|---------|------|------|
| `AdminMaintenanceServiceImpl` | 오케스트레이터 | 전체 재빌드 작업 조율 |
| `PromptRebuildExecutor` | 프롬프트 재빌드 | 프롬프트 Like count 재빌드 실행 |
| `CommentRebuildExecutor` | 댓글 재빌드 | 댓글 Like count 재빌드 실행 |
| `RebuildBatchProcessor` | 배치 처리 | 배치 단위 Redis 저장 |
| `LocalRebuildStatusManager` | 로컬 상태 관리 | 메모리 기반 상태 관리 |
| `GlobalRebuildStatusService` | 글로벌 상태 관리 | Redis 기반 상태 공유 |
| `RebuildMetrics` | 메트릭 관리 | Micrometer 메트릭 수집 |
| `RetryableRebuildService` | 재시도 메커니즘 | 실패 시 재시도 로직 |
| `RebuildNotificationService` | 알림 서비스 | 작업 알림 발송 |

## 11. 최종 구현 현황 (2024년 업데이트)

### 구현 완료된 주요 개선 사항

#### 아키텍처 개선
- ✅ **완전한 책임 분리**: `AdminMaintenanceServiceImpl`은 오케스트레이션만 담당 (약 180줄)
- ✅ **독립적인 컴포넌트 구조**:
  - `rebuild/global/`: 재빌드 실행기 (`PromptRebuildExecutor`, `CommentRebuildExecutor`)
  - `rebuild/util/`: 유틸리티 (`LocalRebuildStatusManager`, `RebuildBatchProcessor`)
  - `rebuild/`: 서비스 (`RebuildNotificationService`, `RetryableRebuildService`)
  - `metrics/`: 메트릭 관리 (`RebuildMetrics`)
  - `config/`: 설정 (`AdminMaintenanceProperties`, `RebuildStatusServiceConfig`)

#### 기능 구현
- ✅ **글로벌 상태 관리**: Redis 기반 멀티 인스턴스 상태 공유
- ✅ **재시도 메커니즘**: 설정 가능한 재시도 로직
- ✅ **배치 처리**: 성능 최적화를 위한 배치 처리 지원
- ✅ **알림 시스템**: 작업 시작/완료/실패 알림 구조
- ✅ **메트릭 수집**: Micrometer 기반 메트릭 수집

#### 코드 품질
- ✅ **단일 책임 원칙**: 각 클래스가 하나의 책임만 가짐
- ✅ **의존성 주입**: 모든 컴포넌트가 Spring Bean으로 관리
- ✅ **테스트 용이성**: 각 컴포넌트를 독립적으로 테스트 가능
- ✅ **설정 외부화**: 모든 설정값을 `application.yml`로 관리

### 코드 품질 지표

- **코드 라인 수**: 약 180줄 (기존 464줄에서 약 60% 감소)
- **클래스 수**: 1개 → 10개 이상 (책임 분리)
- **순환 복잡도**: 낮음 (각 메서드가 단순한 책임)
- **테스트 커버리지**: 각 컴포넌트 독립 테스트 가능
- **의존성**: 명확한 의존성 주입, 느슨한 결합

### 남은 작업 (선택적)

1. **테스트 코드 작성**: 단위 테스트 및 통합 테스트
2. **알림 실제 구현**: Slack/Email 연동 (현재는 로그만, `RebuildNotificationService`에 TODO 주석)
3. **병렬 처리**: 대규모 데이터 처리 시 병렬 처리 고려 (현재는 순차 처리)
4. **성능 모니터링**: 실제 운영 환경에서 성능 측정 및 최적화

---

**작성일**: 2024년  
**최종 업데이트**: 2024년  
**검토 대상**: `AdminMaintenanceServiceImpl` 및 관련 컴포넌트  
**목적**: 분산 락 개선 및 코드 품질 향상  
**상태**: ✅ 대부분의 개선 사항 완료


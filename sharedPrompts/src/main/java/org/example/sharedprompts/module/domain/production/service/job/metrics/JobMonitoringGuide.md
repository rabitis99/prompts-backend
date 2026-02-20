# Job 운영 모니터링 가이드

## 📊 주요 메트릭

### 1. Optimistic Lock 재시도 메트릭

**메트릭명**: `job.update.optimistic_lock.retry`
- **타입**: Counter
- **태그**: `attempt` (1, 2, 3)
- **설명**: Optimistic Lock 충돌 시 재시도 횟수
- **모니터링 포인트**: 
  - 재시도 빈도가 높으면 동시성 문제 발생 가능
  - attempt=3이 많으면 최대 재시도 초과로 실패 증가

**메트릭명**: `job.update.optimistic_lock.failure`
- **타입**: Counter
- **설명**: 최대 재시도 횟수 초과로 인한 실패
- **모니터링 포인트**: 
  - 이 메트릭이 증가하면 즉시 조사 필요
  - Job 업데이트 실패로 이어질 수 있음

### 2. 트랜잭션 타임아웃 메트릭

**메트릭명**: `job.transaction.timeout`
- **타입**: Counter
- **태그**: `operation` (acquireJobLock, updateJobInTransaction, updateJobAndReturnInTransaction)
- **설명**: 트랜잭션 타임아웃 발생 횟수
- **모니터링 포인트**:
  - 타임아웃 발생 시 커넥션 풀 고갈 가능성
  - DB 성능 저하 또는 데드락 가능성

### 3. Job 업데이트 소요 시간

**메트릭명**: `job.update.duration`
- **타입**: Timer
- **태그**: 
  - `operation` (updateJob, updateJobAndReturn)
  - `status` (success, failure)
- **설명**: Job 업데이트 소요 시간
- **모니터링 포인트**:
  - 평균/최대 소요 시간 모니터링
  - 느린 업데이트는 DB 성능 문제 가능성

### 4. HikariCP 커넥션 풀 메트릭 (자동 노출)

**메트릭명**: `hikaricp.connections.*`
- **주요 메트릭**:
  - `hikaricp.connections.active`: 활성 커넥션 수
  - `hikaricp.connections.idle`: 유휴 커넥션 수
  - `hikaricp.connections.pending`: 대기 중인 스레드 수
  - `hikaricp.connections.acquisition`: 커넥션 획득 시간
- **모니터링 포인트**:
  - `active`가 `maximum-pool-size`에 근접하면 커넥션 풀 부족
  - `pending`이 증가하면 커넥션 획득 대기 중인 요청 증가
  - `acquisition` 시간이 길면 DB 성능 저하

## 🔍 Prometheus 쿼리 예시

### Optimistic Lock 재시도율
```promql
rate(job_update_optimistic_lock_retry_total[5m])
```

### 트랜잭션 타임아웃 발생률
```promql
rate(job_transaction_timeout_total[5m])
```

### 커넥션 풀 사용률
```promql
hikaricp_connections_active / hikaricp_connections_max * 100
```

### Job 업데이트 평균 소요 시간
```promql
rate(job_update_duration_seconds_sum[5m]) / rate(job_update_duration_seconds_count[5m])
```

## ⚠️ 알람 임계값 권장사항

1. **Optimistic Lock 재시도 실패**: 1분에 10건 이상
2. **트랜잭션 타임아웃**: 1분에 5건 이상
3. **커넥션 풀 사용률**: 80% 이상 지속
4. **커넥션 획득 대기 스레드**: 5개 이상 지속
5. **Job 업데이트 평균 소요 시간**: 1초 이상

## 📍 메트릭 확인 방법

### Actuator 엔드포인트
```
GET /api/actuator/metrics/job.update.optimistic_lock.retry
GET /api/actuator/metrics/job.transaction.timeout
GET /api/actuator/metrics/hikaricp.connections.active
```

### Prometheus 엔드포인트
```
GET /api/actuator/prometheus
```


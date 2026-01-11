# 성능 개선 방안 (4/5 → 5/5)

## 현재 상태

- 리액티브 프로그래밍 적용
- Redis 캐싱 활용 (카운터)
- N+1 쿼리 최적화
- 블로킹 호출 제거

## 5점 달성 방안

### 1. 캐싱 전략 확대 🟠 **중요**

**현재**: Redis는 카운터에만 사용

**개선**:
- 응답 캐싱 추가 (자주 조회되는 프롬프트 목록)
- `@Cacheable` 어노테이션 활용
- TTL 적절히 설정
  - 인기 프롬프트: 5분
  - 일반 프롬프트: 1분
  - 사용자 정보: 10분

### 2. Connection Pool 최적화 🟡

**HikariCP 설정 튜닝**:
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=3000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

- DB 커넥션 모니터링 도구 도입
- 실제 트래픽에 맞춰 최적값 조정

### 3. DB 인덱스 최적화 🟡

- 쿼리 성능 분석 (EXPLAIN 실행 계획)
- 복합 인덱스 추가 검토 (자주 함께 사용되는 컬럼)
- 불필요한 인덱스 제거

### 4. 비동기 처리 범위 확대 🟡

- 이메일 발송, 알림 전송 등 외부 API 호출 비동기화
- `@Async` 또는 `CompletableFuture` 활용
- 스레드 풀 크기 적절히 설정

### 5. 쿼리 결과 최적화 🟢

- DTO Projection 활용 (엔티티 전체 조회 대신 필요한 필드만)
- `@EntityGraph` 활용 범위 확대

**우선순위**: 캐싱 전략 확대와 Connection Pool 최적화가 가장 중요 (즉각적인 성능 향상)

[← 목차로 돌아가기](../CODE_IMPROVEMENT_GUIDE.md)


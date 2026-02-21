# 배포 전 체크리스트 (4가지)

배포 전에 아래 4가지를 반드시 확인하세요.

---

## 1. Outbox 테이블

**목표**: `production_job_outbox` 테이블이 배포 대상 DB에 존재해야 합니다.

### 확인 방법

- **Flyway 사용 시**: `db/migration/` 경로가 Flyway `locations`에 포함되어 있고, Outbox 마이그레이션이 버전 순서에 맞게 적용되도록 파일명 규칙을 지킨다.  
  - 현재 스크립트: `src/main/resources/db/migration/outbox.sql`
- **Flyway 미사용 시**: 배포 전에 **수동으로** 아래 SQL을 대상 DB에서 1회 실행한다.

### 적용할 SQL

```sql
-- 파일: src/main/resources/db/migration/outbox.sql
CREATE TABLE IF NOT EXISTS production_job_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_id VARCHAR(36) NOT NULL COMMENT 'Job ID (production_jobs.job_id)',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '메시지 maxRetryCount',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, SENT, FAILED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sent_at DATETIME(6) NULL COMMENT '발행 완료 시각',
    error_message VARCHAR(500) NULL COMMENT '발행 실패 시 오류 메시지',
    PRIMARY KEY (id),
    INDEX idx_outbox_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Job 큐 발행용 Transactional Outbox - 동일 TX 기록 후 비동기 발행';
```

### 체크

- [ ] Outbox 테이블이 배포 대상 DB에 적용됨 (Flyway 또는 수동 실행)

---

## 2. 환경 변수

**목표**: 프로덕션에서 `EnvironmentValidator`가 요구하는 필수 환경 변수가 모두 설정되어 있어야 합니다.

### 공통 필수

| 변수명 | 설명 |
|--------|------|
| `JWT_SECRET` | JWT 서명용 시크릿 |
| `SPRING_DATASOURCE_URL` | DB URL |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자명 |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 |
| `CORS_ALLOWED_ORIGINS` | CORS 허용 오리진 |
| `OAUTH2_REDIRECT_FRONT_URL` | OAuth2 리다이렉트 URL |
| `OAUTH2_FAILURE_REDIRECT_URL` | OAuth2 실패 시 리다이렉트 URL |
| `OAUTH2_SALT` | OAuth2 Salt |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth2 |
| `NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET` | Naver OAuth2 |
| `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET` | Kakao OAuth2 |

### 프로덕션(prod 프로필) 추가 필수

| 변수명 | 설명 |
|--------|------|
| `SPRING_PROFILES_ACTIVE` | 반드시 `prod` (또는 `DEPLOYMENT_ENV=production` 사용 시 prod 프로필 필수) |
| `GOOGLE_GEMINI_API_KEY` | Gemini API 키 |
| `SPRING_AI_OPENAI_API_KEY` | OpenAI API 키 (더미 값 `dummy-openai-api-key` 금지) |

### 프로덕션 권장·경고

- `REDIS_HOST` / `RABBITMQ_HOST`: localhost면 경고 로그 (실제 서버 주소 권장).
- `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD`: guest/guest면 경고 (강한 자격 증명 권장).
- `HIKARI_MAXIMUM_POOL_SIZE`: 미설정 시 기본값 사용 경고 (예: 50으로 설정 권장).

**검증**: 앱 기동 시 `EnvironmentValidator`가 실행되며, 누락 시 **기동이 실패**합니다.

### 체크

- [ ] 위 공통 + 프로덕션 필수 환경 변수 모두 설정됨
- [ ] 프로덕션 배포 시 `SPRING_PROFILES_ACTIVE=prod` (또는 동등 설정) 적용됨
- [ ] (선택) Redis/RabbitMQ/Hikari 프로덕션 권장값 적용 여부 확인

---

## 3. OPERATIONS §1~§3 (로그 / 모니터링 / 백업)

**기준 문서**: `src/main/java/org/example/sharedprompts/module/OPERATIONS.md`

### §1 로깅 기준

- **로그 레벨**: ERROR(예외·실패), WARN(재시도 가능 실패), INFO(주요 이벤트), DEBUG(기본 비활성).
- **출력 형식**: 로그백 JSON 또는 단일 라인, `timestamp`, `level`, `logger`, `message`, `thread`, `exception` 권장.
- **민감 정보**: 토큰·비밀번호·전체 요청 본문 로그 금지.
- **MDC**: `requestId`, `userId`, `jobId` 등 요청/Job 단위 문맥 설정 권장.

### §2 모니터링

- **Actuator**: `/actuator/health`, `/actuator/metrics` (필요 시 `/actuator/prometheus`).
- **Health**: DB, Redis, RabbitMQ, S3(production.storage.type=S3일 때) 포함.
- **보안**: 운영에서는 health만 외부 노출, metrics/prometheus는 내부망·인증 제한 권장.
- **메트릭 예시**: Job 성공/실패 수, 처리 지연(p99), DB 커넥션 풀 사용률, RabbitMQ 대기 메시지, **Outbox PENDING 건수**.
- **알림 권장**: Health down, Job 실패율 급증, Outbox PENDING 누적, 에러 로그 급증.

### §3 백업 및 복구

- **DB**: 일일 전체 백업 + 트랜잭션 로그(또는 binlog) 보존, 보존 기간 최소 7일(30일 권장).
- **복구 절차**: 전체 백업 복원 → 필요 시 PITR → 앱 헬스 체크 후 트래픽 복구.
- **S3/로컬**: 버전 관리 또는 복제/백업 정책, 설정·배포 아티팩트 버전 보존(롤백용).
- **복구 검증**: 분기별 복구 드릴 권장.

### 체크

- [ ] §1: 로그 레벨·출력 형식·민감정보·MDC 설정 확인
- [ ] §2: Actuator/Health/메트릭/알림 설정 확인
- [ ] §3: DB·스토리지·설정 백업 및 복구 계획 수립 여부 확인

---

## 4. Outbox 설정

**목표**: Job 큐 발행을 Transactional Outbox로 사용할지, 그리고 발행 주기를 확인합니다.

### 설정 위치

`application.yml` (및 환경 변수 오버라이드):

```yaml
production:
  job:
    outbox:
      enabled: ${PRODUCTION_JOB_OUTBOX_ENABLED:true}   # 기본값: 사용
      publisher-interval-ms: ${PRODUCTION_JOB_OUTBOX_PUBLISHER_INTERVAL_MS:2000}  # 2초마다 PENDING 발행
```

### 동작

- **enabled=true (기본)**: Job 생성과 동일 트랜잭션에서 `production_job_outbox`에 기록 후, `OutboxPublisher`가 주기적으로 PENDING 행을 RabbitMQ로 발행. 발행 실패 시 재시도 가능.
- **enabled=false**: 기존처럼 Job 생성 후 즉시 `JobQueuePublisher.publishJob()` 호출 (Outbox 미사용).

### 체크

- [ ] `PRODUCTION_JOB_OUTBOX_ENABLED` 미설정 또는 `true` → Outbox 사용 (권장)
- [ ] `PRODUCTION_JOB_OUTBOX_PUBLISHER_INTERVAL_MS` 미설정 시 2000ms(2초) 적용 확인
- [ ] Outbox 비활성화 시: `PRODUCTION_JOB_OUTBOX_ENABLED=false`로 명시했는지 확인

---

## 요약

| # | 항목 | 확인 내용 |
|---|------|-----------|
| 1 | Outbox 테이블 | `production_job_outbox` Flyway 또는 수동 적용 |
| 2 | 환경 변수 | 공통 + prod 필수 변수 설정, 앱 기동 시 검증 통과 |
| 3 | OPERATIONS §1~§3 | 로깅·모니터링·백업 계획 점검 |
| 4 | Outbox 설정 | enabled(true 권장), publisher-interval-ms(기본 2000) 확인 |

위 4가지를 모두 체크한 뒤 배포하면 됩니다.

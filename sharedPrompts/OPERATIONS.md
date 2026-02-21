# 운영 가이드: 로그 / 모니터링 / 백업

이 문서는 DEPLOYMENT_ISSUES.md의 「로그/모니터링/백업 계획」 실행을 위한 기준과 절차를 정리합니다.

---

## 1. 로깅 기준

### 1.1 로그 레벨

- **ERROR**: 예외·실패(재시도 불가·사용자 영향)
- **WARN**: 재시도 가능 실패, 비정상이지만 복구 가능한 상태
- **INFO**: 주요 비즈니스 이벤트(Job 생성/완료/실패, 발행 성공 등)
- **DEBUG**: 개발/디버깅용 상세(기본 비활성)

### 1.2 출력 형식

- 포맷: 로그백 JSON 또는 단일 라인 텍스트(운영 환경에 맞게 설정)
- 권장 필드: `timestamp`, `level`, `logger`, `message`, `thread`, `exception`(있을 때)
- 민감 정보: 토큰·비밀번호·전체 요청 본문 로그 금지

### 1.3 MDC(문맥) 활용

- 요청 단위: `requestId`, `userId`(가능한 경우), `jobId`(Job 처리 시)
- 설정 예: `logging.pattern` 또는 Logback `%X{requestId}`

### 1.4 적용 위치

- Job 생성/발행/처리/완료/실패: INFO 이상
- Outbox 발행 실패: WARN
- 외부 호출(AI, S3) 실패: WARN + 상세는 DEBUG
- 트랜잭션/락 타임아웃: ERROR

---

## 2. 모니터링

### 2.1 Spring Boot Actuator

- **엔드포인트**: `/actuator/health`, `/actuator/metrics` (필요 시 `/actuator/prometheus`)
- **Health**: DB, Redis, RabbitMQ, S3(production.storage.type=S3일 때) 포함
- **보안**: 운영에서는 health만 노출하고, metrics/prometheus는 내부망·인증으로 제한

### 2.2 주요 메트릭(SLI 후보)

| 메트릭 | 설명 | 목표 예시 |
|--------|------|-----------|
| Job 처리 성공/실패 수 | 카운터 | 실패율 &lt; 1% |
| Job 처리 지연 | 히스토그램(예: p99) | p99 &lt; 120s |
| DB 커넥션 풀 사용률 | gauge | &lt; 80% |
| RabbitMQ consumer 수/대기 메시지 | gauge | 대기 메시지 정상 범위 유지 |
| Outbox PENDING 건수 | gauge | 증가 추이 모니터링(발행 지연 탐지) |

### 2.3 알림 권장

- Health down (DB, Redis, RabbitMQ)
- Job 실패율 급증
- Outbox PENDING 누적
- 에러 로그 급증

---

## 3. 백업 및 복구 계획

### 3.1 데이터베이스

- **백업**: 일일 전체 백업 + 트랜잭션 로그(또는 binlog) 보존
- **보존 기간**: 최소 7일(운영 정책에 따라 30일 권장)
- **복구 절차**: 
  1) 최신 전체 백업 복원  
  2) 필요 시 point-in-time recovery(트랜잭션 로그 적용)  
  3) 애플리케이션 헬스 체크 후 트래픽 복구

### 3.2 스토리지(Production Artifact)

- **S3**: 버전 관리 또는 크로스 리전/크로스 계정 복제 정책 수립
- **로컬 스토리지**: 정기 파일 시스템 백업

### 3.3 설정·아티팩트

- **설정**: 버전 관리(환경별 application-*.yml, 시크릿은 vault 등)
- **배포 아티팩트**: 빌드 버전·이미지 태그 보존(롤백용)

### 3.4 복구 검증

- 분기별 복구 드릴 권장(백업에서 복원 후 서비스 검증)

---

## 4. DEPLOYMENT_ISSUES 연동

- 배포 전 체크리스트: 「로그 레벨 및 출력 형식 확인」「모니터링 설정 확인」「백업 및 복구 계획 수립」 항목은 본 문서 1~3절 기준으로 점검
- 배포 후: 「애플리케이션 로그 모니터링」「DB/스레드 풀/API/에러율/리소스 모니터링」 및 Job 복구·AI 타임아웃 검증 수행

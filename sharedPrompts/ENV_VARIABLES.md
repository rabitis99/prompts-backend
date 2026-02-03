# 환경 변수(.env) 정의 목록

> 이 문서는 코드베이스 분석을 통해 식별된 모든 환경 변수 및 환경별로 분리되어야 할 설정값 목록입니다.

---

## 1. 데이터베이스 (필수)

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| SPRING_DATASOURCE_URL | 데이터베이스 JDBC URL | {DB_JDBC_URL} |
| SPRING_DATASOURCE_USERNAME | 데이터베이스 사용자명 | {DB_USERNAME} |
| SPRING_DATASOURCE_PASSWORD | 데이터베이스 비밀번호 | {DB_PASSWORD} |
| HIKARI_MAXIMUM_POOL_SIZE | HikariCP 커넥션 풀 최대 크기 | {POOL_SIZE} |
| JPA_DDL_AUTO | JPA DDL 자동 생성 모드 (prod: none 필수) | none |

---

## 2. Redis

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| REDIS_HOST | Redis 서버 호스트 | {REDIS_HOST} |
| REDIS_PORT | Redis 서버 포트 | 6379 |
| REDIS_PASSWORD | Redis 비밀번호 | {REDIS_PASSWORD} |
| app.redis.hot-ttl-seconds | Hot 데이터 TTL(초) | 86400 |

---

## 3. RabbitMQ

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| RABBITMQ_HOST | RabbitMQ 서버 호스트 | {RABBITMQ_HOST} |
| RABBITMQ_PORT | RabbitMQ 서버 포트 | 5672 |
| RABBITMQ_USERNAME | RabbitMQ 사용자명 | {RABBITMQ_USERNAME} |
| RABBITMQ_PASSWORD | RabbitMQ 비밀번호 | {RABBITMQ_PASSWORD} |

---

## 4. JWT / 인증 (필수)

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| JWT_SECRET | JWT 서명 비밀키 (필수) | {JWT_SECRET_KEY} |
| jwt.access.expiration | Access Token 유효기간(분) | 30 |
| jwt.refresh.expiration | Refresh Token 유효기간(일) | 7 |
| jwt.refresh.rotation-threshold-days | Refresh Token 갱신 임계값(일) | 1 |

---

## 5. OAuth2 설정 (필수)

### 5.1 OAuth2 공통

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| OAUTH2_REDIRECT_FRONT_URL | OAuth2 성공 후 프론트엔드 리다이렉트 URL | {FRONT_URL}/oauth/callback |
| OAUTH2_FAILURE_REDIRECT_URL | OAuth2 실패 시 리다이렉트 URL | {FRONT_URL}/oauth/error |
| OAUTH2_SALT | OAuth2 State HMAC 서명용 Salt | {OAUTH2_HMAC_SECRET} |
| oauth2.state.validity-minutes | OAuth2 State 유효기간(분) | 10 |

### 5.2 Google OAuth2

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| GOOGLE_CLIENT_ID | Google OAuth2 클라이언트 ID | {GOOGLE_CLIENT_ID} |
| GOOGLE_CLIENT_SECRET | Google OAuth2 클라이언트 Secret | {GOOGLE_CLIENT_SECRET} |

### 5.3 Naver OAuth2

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| NAVER_CLIENT_ID | Naver OAuth2 클라이언트 ID | {NAVER_CLIENT_ID} |
| NAVER_CLIENT_SECRET | Naver OAuth2 클라이언트 Secret | {NAVER_CLIENT_SECRET} |

### 5.4 Kakao OAuth2

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| KAKAO_CLIENT_ID | Kakao OAuth2 클라이언트 ID | {KAKAO_CLIENT_ID} |
| KAKAO_CLIENT_SECRET | Kakao OAuth2 클라이언트 Secret | {KAKAO_CLIENT_SECRET} |

---

## 6. CORS 설정 (필수)

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| CORS_ALLOWED_ORIGINS | 허용할 Origin 목록 (쉼표 구분) | {ALLOWED_ORIGINS} |

---

## 7. AI / LLM API

### 7.1 Google Gemini

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| GOOGLE_GEMINI_API_KEY | Gemini API 키 (prod 필수) | {GEMINI_API_KEY} |
| google.gemini.model | 사용할 Gemini 모델명 | gemini-2.5-flash-lite |
| google.gemini.base-url | Gemini API Base URL | https://generativelanguage.googleapis.com/v1 |
| google.gemini.timeout-seconds | Gemini API 타임아웃(초) | 30 |

### 7.2 OpenAI

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| SPRING_AI_OPENAI_API_KEY | OpenAI API 키 (prod 필수) | {OPENAI_API_KEY} |

---

## 8. 결제사 - 토스페이먼츠

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| payment.toss.api-key | 토스 API 키 | {TOSS_API_KEY} |
| payment.toss.secret-key | 토스 Secret 키 | {TOSS_SECRET_KEY} |
| payment.toss.base-url | 토스 API Base URL | https://api.tosspayments.com/v1/payments |
| payment.toss.confirm-endpoint | 토스 결제 승인 Endpoint | /confirm |

---

## 9. 결제사 - 카카오페이

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| payment.kakao.secret | 카카오페이 Admin 키 | {KAKAO_PAY_SECRET} |
| payment.kakao.cid | 카카오페이 가맹점 CID | TC0ONETIME |

---

## 10. 결제사 - PayPal

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| payment.paypal.client-id | PayPal Client ID | {PAYPAL_CLIENT_ID} |
| payment.paypal.client-secret | PayPal Client Secret | {PAYPAL_CLIENT_SECRET} |
| payment.paypal.webhook-id | PayPal Webhook ID | {PAYPAL_WEBHOOK_ID} |

---

## 11. 결제 공통 설정

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| payment.webhook.secret | Webhook 서명 검증 Secret | {WEBHOOK_SECRET} |
| payment.retry.max-attempts | 결제 재시도 최대 횟수 | 3 |
| payment.retry.delay-ms | 결제 재시도 간격(ms) | 1000 |
| payment.cashback.rate | 캐시백 적립률 | 0.01 |
| payment.point.rate | 포인트 적립률 | 0.005 |

---

## 12. 환율 API

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| payment.exchange-rate.api-key | 환율 API 키 | {EXCHANGE_RATE_API_KEY} |
| payment.exchange-rate.api-url | 환율 API URL | https://api.exchangerate-api.com/v4/latest/ |
| payment.exchange-rate.schedule | 환율 업데이트 스케줄 (cron) | 0 0 2 * * ? |

---

## 13. FCM (Firebase Cloud Messaging)

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| payment.fcm.project-id | Firebase 프로젝트 ID | {FCM_PROJECT_ID} |
| payment.fcm.credentials-path | Firebase 인증 파일 경로 | {FCM_CREDENTIALS_PATH} |
| payment.fcm.classpath-resource | Firebase 인증 classpath 리소스 | firebase/firebase-adminsdk.json |
| payment.fcm.enabled | FCM 활성화 여부 | true |

---

## 14. Rate Limit 설정

### 14.1 Rate Limit 규칙 (요청 횟수 제한)

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| rate-limit.rules.login | 로그인 시도 횟수 제한 | 5 |
| rate-limit.rules.signup | 회원가입 시도 횟수 제한 | 3 |
| rate-limit.rules.confirm | OAuth2 확정 시도 횟수 제한 | 5 |
| rate-limit.rules.prompt-create | 프롬프트 생성 횟수 제한 | 10 |
| rate-limit.rules.general | 일반 API 요청 횟수 제한 | 100 |

### 14.2 Rate Limit 윈도우 (시간 창, 초 단위)

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| rate-limit.windows.default-seconds | 기본 윈도우(초) | 60 |
| rate-limit.windows.login-seconds | 로그인 윈도우(초) | 900 |
| rate-limit.windows.signup-seconds | 회원가입 윈도우(초) | 60 |
| rate-limit.windows.confirm-seconds | OAuth2 확정 윈도우(초) | 60 |
| rate-limit.windows.prompt-create-seconds | 프롬프트 생성 윈도우(초) | 60 |
| rate-limit.windows.general-seconds | 일반 API 윈도우(초) | 60 |

### 14.3 Rate Limit 장애 정책

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| rate-limit.failure-policy.fail-open | 장애 시 요청 허용 여부 | true |
| rate-limit.failure-policy.log-failure | 장애 로깅 활성화 | true |

### 14.4 Rate Limit 로그 정리

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| rate-limit.log.cleanup.enabled | 로그 정리 활성화 | true |
| rate-limit.log.cleanup.retention-days | 로그 보관일(일) | 90 |
| rate-limit.log.cleanup.schedule | 정리 스케줄 (cron) | 0 0 3 * * ? |

---

## 15. Redis Health Check

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| redis.health-check.enabled | Health Check 활성화 | true |
| redis.health-check.interval-ms | Health Check 주기(ms) | 5000 |
| redis.health-check.max-consecutive-failures | 연속 실패 임계값 | 3 |
| redis.health-check.fixed-delay | 스케줄러 실행 주기(ms) | 5000 |
| redis.health-check.initial-delay | 스케줄러 초기 지연(ms) | 10000 |

---

## 16. 관리자 계정

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| ADMIN_EMAIL | 관리자 이메일 | admin@sharedprompts.com |
| ADMIN_PASSWORD | 관리자 비밀번호 (prod 필수) | {ADMIN_PASSWORD} |
| ADMIN_NICKNAME | 관리자 닉네임 | 관리자 |

---

## 17. 관리자 유지보수 (Like Count 재빌드)

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| admin.maintenance.rebuild.page-size | 페이지 크기 | 1000 |
| admin.maintenance.rebuild.use-global-status | 글로벌 상태 관리 사용 | false |
| admin.maintenance.rebuild.lock.name | ShedLock 이름 | AdminMaintenanceService_rebuildLikeCounts |
| admin.maintenance.rebuild.lock.lock-at-most-for | 최대 Lock 유지 시간 | 1h |
| admin.maintenance.rebuild.lock.lock-at-least-for | 최소 Lock 유지 시간 | 30m |
| admin.maintenance.rebuild.retry.max-retries | 최대 재시도 횟수 | 3 |
| admin.maintenance.rebuild.retry.retry-delay-ms | 재시도 간격(ms) | 1000 |
| admin.maintenance.rebuild.retry.enabled | 재시도 활성화 | false |
| admin.maintenance.rebuild.batch.size | 배치 크기 | 100 |
| admin.maintenance.rebuild.batch.enabled | 배치 처리 활성화 | true |

---

## 18. 알림 (Notification)

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| notification.sse.timeout | SSE 연결 타임아웃(ms) | 1800000 |
| notification.sse.cleanup-interval | SSE 정리 주기(ms) | 300000 |
| notification.cache.unread-count-ttl | 읽지 않은 알림 캐시 TTL(초) | 60 |
| notification.cleanup.enabled | 알림 정리 활성화 | true |
| notification.cleanup.retention-days | 알림 보관일(일) | 90 |
| notification.cleanup.schedule | 알림 정리 스케줄 (cron) | 0 0 2 * * ? |
| notification.duplicate.check-window-minutes | 중복 알림 확인 윈도우(분) | 5 |

---

## 19. 통계 캐시

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| statistics.cache.ttl | 통계 캐시 TTL(초) | 300 |

---

## 20. 프롬프트 생성

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| app.prompt.creation.timeout-ms | 프롬프트 생성 타임아웃(ms) | 40000 |

---

## 21. 태그 카운트

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| tag.count.update.sync-fallback | 동기 폴백 사용 | false |
| tag.count.update.enable-monitoring | 모니터링 활성화 | true |
| tag.count.update.enable-dlq | DLQ 활성화 | true |
| tag.count.dlq.process-interval | DLQ 처리 주기(ms) | 3600000 |
| tag.count.retry.process-interval | 재시도 큐 처리 주기(ms) | 5000 |

---

## 22. ShedLock (분산 스케줄러 Lock)

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| shedlock.fallback.enabled | DB Fallback 활성화 | false |
| shedlock.table.auto-create | ShedLock 테이블 자동 생성 | false |

---

## 23. 쿠키 보안

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| auth.cookie.secure | HTTPS 전용 쿠키 (prod: true 권장) | false |

---

## 24. 비밀번호 정책

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| password.policy.min-length | 최소 비밀번호 길이 | 10 |
| password.policy.max-length | 최대 비밀번호 길이 | 100 |
| password.policy.require-upper-case | 대문자 필수 | true |
| password.policy.require-lower-case | 소문자 필수 | true |
| password.policy.require-digit | 숫자 필수 | true |
| password.policy.require-special-char | 특수문자 필수 | true |
| password.policy.special-chars | 허용 특수문자 목록 | `!@#$%^&*()_+-=[]{}\|;:,.<>?` |

---

## 25. Async 예외 처리

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| async.exception.critical-method-patterns | 중요 메서드 패턴 목록 | AuthEventListener,Payment,Audit,RateLimitLogBatchService |

---

## 26. Circuit Breaker (application.yml 설정)

> Resilience4j Circuit Breaker 설정은 application.yml에서 관리됩니다.

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| resilience4j.circuitbreaker.instances.tokenRedis.failure-rate-threshold | 실패율 임계값(%) | 50 |
| resilience4j.circuitbreaker.instances.tokenRedis.wait-duration-in-open-state | Open 상태 유지 시간 | 30s |
| resilience4j.circuitbreaker.instances.tokenRedis.permitted-number-of-calls-in-half-open-state | Half-Open 허용 호출 수 | 5 |
| resilience4j.circuitbreaker.instances.tokenRedis.sliding-window-size | 슬라이딩 윈도우 크기 | 20 |
| resilience4j.circuitbreaker.instances.tokenRedis.slow-call-rate-threshold | 느린 호출 비율 임계값(%) | 80 |
| resilience4j.circuitbreaker.instances.tokenRedis.slow-call-duration-threshold | 느린 호출 임계 시간 | 2s |
| resilience4j.circuitbreaker.instances.googleGemini.* | Gemini Circuit Breaker 설정 | - |

---

## 27. 하드코딩된 값 (환경 변수화 권장)

> 현재 코드에 하드코딩되어 있으나, 환경 변수로 분리하는 것이 권장되는 값들입니다.

### 27.1 RestTemplate 타임아웃

| 위치 | 현재 값 | 권장 변수명 |
|------|---------|-------------|
| RestTemplateConfig.java:17 | 5000ms (Connect) | rest-template.connect-timeout |
| RestTemplateConfig.java:18 | 5000ms (Read) | rest-template.read-timeout |

### 27.2 RabbitMQ 메시지 TTL

| 위치 | 현재 값 | 권장 변수명 |
|------|---------|-------------|
| RabbitMQConfig.java:109 | 86400000ms (24시간) | rabbitmq.message.ttl |
| RabbitMQConfig.java:195 | 86400000ms (24시간) | rabbitmq.message.ttl |

### 27.3 RabbitMQ Consumer 설정

| 위치 | 현재 값 | 권장 변수명 |
|------|---------|-------------|
| RabbitMQConfig.java:240 | 3 (Concurrent) | rabbitmq.listener.concurrent-consumers |
| RabbitMQConfig.java:241 | 10 (Max Concurrent) | rabbitmq.listener.max-concurrent-consumers |

### 27.4 Async Executor 설정

| Executor | Core | Max | Queue | 권장 변수명 Prefix |
|----------|------|-----|-------|-------------------|
| sseTaskExecutor | 5 | 20 | 100 | async.executor.sse.* |
| rateLimitLogTaskExecutor | 3 | 10 | 500 | async.executor.rate-limit-log.* |
| aiCallTaskExecutor | 10 | 50 | 100 | async.executor.ai-call.* |
| tagCountUpdateExecutor | 2 | 5 | 100 | async.executor.tag-count.* |
| taskExecutor (default) | 5 | 20 | 100 | async.executor.default.* |

### 27.5 스케줄러 주기 (하드코딩)

| 스케줄러 | 현재 값 | 권장 변수명 |
|----------|---------|-------------|
| StatisticsCacheScheduler | 5분 (300000ms) | scheduler.statistics-cache.fixed-rate |
| PromptCountSyncScheduler | 10분 (600000ms) | scheduler.prompt-count-sync.fixed-delay |
| PromptUsageCountSyncScheduler | 10분 (600000ms) | scheduler.prompt-usage-sync.fixed-delay |
| PromptLikeCountSyncScheduler | 10분 (600000ms) | scheduler.like-count-sync.fixed-delay |
| CommentLikeCountSyncScheduler | 10분 (600000ms) | scheduler.comment-like-sync.fixed-delay |
| PromptFavoriteCountSyncScheduler | 10분 (600000ms) | scheduler.favorite-count-sync.fixed-delay |
| CommentCountSyncScheduler | 10분 (600000ms) | scheduler.comment-count-sync.fixed-delay |
| PaymentRetryScheduler | 5분 (300000ms) | scheduler.payment-retry.fixed-delay |

---

## 28. 배포 환경 설정

| 변수명 | 용도 설명 | 예시 값 |
|--------|----------|---------|
| DEPLOYMENT_ENV | 배포 환경 식별 (prod/dev) | production |
| SPRING_PROFILES_ACTIVE | 활성 프로필 | prod |
| logging.level.root | 루트 로깅 레벨 | INFO |

---

## 프로덕션 환경 필수 체크리스트

### 필수 환경 변수 (누락 시 기동 실패)
- [ ] JWT_SECRET
- [ ] SPRING_DATASOURCE_URL
- [ ] SPRING_DATASOURCE_USERNAME
- [ ] SPRING_DATASOURCE_PASSWORD
- [ ] CORS_ALLOWED_ORIGINS
- [ ] OAUTH2_REDIRECT_FRONT_URL
- [ ] OAUTH2_FAILURE_REDIRECT_URL
- [ ] OAUTH2_SALT
- [ ] GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET
- [ ] NAVER_CLIENT_ID / NAVER_CLIENT_SECRET
- [ ] KAKAO_CLIENT_ID / KAKAO_CLIENT_SECRET
- [ ] GOOGLE_GEMINI_API_KEY (prod 환경)
- [ ] SPRING_AI_OPENAI_API_KEY (prod 환경)
- [ ] ADMIN_PASSWORD (prod 환경에서 신규 어드민 생성 시)

### 보안 권장 설정 (prod)
- [ ] auth.cookie.secure = true
- [ ] JPA_DDL_AUTO = none
- [ ] rate-limit.failure-policy.fail-open = false (보안 우선 시)
- [ ] REDIS_HOST != localhost
- [ ] RABBITMQ_USERNAME/PASSWORD != guest

---

*문서 생성일: 2026-02-02*
*분석 대상: sharedPrompts 프로젝트*

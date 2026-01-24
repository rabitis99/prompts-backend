# 브랜치 전략 및 커밋 계획

## 📋 변경사항 분석 요약

현재 dev 브랜치에서 대규모 리팩토링 작업이 진행되었습니다. 변경사항을 논리적 단위로 분리하여 브랜치 전략을 수립했습니다.

---

## 🌿 브랜치 전략

### 1. 인증/보안 리팩토링 (가장 큰 변경)
**브랜치**: `feature/auth-security-refactoring`

**변경 내용**:
- `global/jwt` → `auth` 패키지로 재구성
- Response 클래스 이동 (`global/response` → `dto/common`)
- OAuth2 구조 개선
- JWT 처리 로직 재구성
- Security 설정 개선

**커밋 전략**:
```bash
# 1단계: Response 클래스 이동 (의존성 최소화)
git checkout -b feature/auth-security-refactoring
git restore --staged AUTH_SECURITY_REFACTORING_PLAN.md
git add src/main/java/org/example/sharedprompts/global/response/
git add src/main/java/org/example/sharedprompts/dto/common/
git commit -m "Move response classes from global/response to dto/common"

# 2단계: 기존 JWT 패키지 삭제 (staged 변경사항 활용)
git add src/main/java/org/example/sharedprompts/global/jwt/
git commit -m "Remove old global/jwt package (moved to auth package)"

# 3단계: ValidReportProcessStatus 이동
git add src/main/java/org/example/sharedprompts/dto/report/validator/ValidReportProcessStatus.java
git commit -m "Move ValidReportProcessStatus to validator package"

# 4단계: 새로운 auth 패키지 추가
git add src/main/java/org/example/sharedprompts/auth/
git commit -m "Add new auth package with JWT, OAuth2, and security components"

# 5단계: 컨트롤러 및 서비스 업데이트
git add src/main/java/org/example/sharedprompts/controller/
git add src/main/java/org/example/sharedprompts/domain/auth/
git add src/main/java/org/example/sharedprompts/global/exception/
git commit -m "Update controllers and auth services to use new auth package structure"
```

---

### 2. 인프라 설정 재구성
**브랜치**: `refactor/infra-config-reorganization`

**변경 내용**:
- `global/config`의 일부를 `infra` 패키지로 이동
- QuerydslConfig, RabbitMQConfig, RedisConfig 이동
- 불필요한 설정 파일 정리

**커밋 전략**:
```bash
git checkout dev
git pull origin dev
git checkout -b refactor/infra-config-reorganization
git add src/main/java/org/example/sharedprompts/infra/
git add src/main/java/org/example/sharedprompts/global/config/
git commit -m "Reorganize infrastructure configs from global/config to infra package"
```

---

### 3. Rate Limiting 기능 추가
**브랜치**: `feature/rate-limiting`

**변경 내용**:
- Rate limiting 시스템 추가
- Rate limit 로깅 및 통계 기능
- Filter 및 정책 시스템

**커밋 전략**:
```bash
git checkout dev
git pull origin dev
git checkout -b feature/rate-limiting
git add src/main/java/org/example/sharedprompts/domain/rate/
git add src/main/java/org/example/sharedprompts/auth/rate/
git add src/main/java/org/example/sharedprompts/scheduler/ratelimit/
git add src/main/java/org/example/sharedprompts/dto/admin/request/RateLimit*
git add src/main/java/org/example/sharedprompts/dto/admin/response/RateLimit*
git commit -m "Add rate limiting system with logging and statistics"
```

---

### 4. Admin 서비스 리팩토링
**브랜치**: `refactor/admin-service-consolidation`

**변경 내용**:
- 여러 Admin 서비스를 하나로 통합
- Admin 도메인 구조 개선

**커밋 전략**:
```bash
git checkout dev
git pull origin dev
git checkout -b refactor/admin-service-consolidation
git add src/main/java/org/example/sharedprompts/domain/admin/
git add src/main/java/org/example/sharedprompts/controller/admin/
git add src/main/java/org/example/sharedprompts/dto/admin/
git commit -m "Consolidate admin services into unified AdminService"
```

---

### 5. Audit 로깅 기능
**브랜치**: `feature/audit-logging`

**변경 내용**:
- Auth audit 로깅
- Admin audit 로깅
- Audit 도메인 구조

**커밋 전략**:
```bash
git checkout dev
git pull origin dev
git checkout -b feature/audit-logging
git add src/main/java/org/example/sharedprompts/domain/audit/
git add src/main/java/org/example/sharedprompts/domain/admin/audit/
git add src/main/java/org/example/sharedprompts/auth/audit/
git add src/main/java/org/example/sharedprompts/dto/audit/
git commit -m "Add comprehensive audit logging for auth and admin operations"
```

---

### 6. 도메인 엔티티 개선
**브랜치**: `refactor/domain-entities-improvement`

**변경 내용**:
- 여러 엔티티에 필드 추가/수정
- 도메인 로직 개선

**커밋 전략**:
```bash
git checkout dev
git pull origin dev
git checkout -b refactor/domain-entities-improvement
git add src/main/java/org/example/sharedprompts/domain/comment/Comment.java
git add src/main/java/org/example/sharedprompts/domain/favorite/Favorite.java
git add src/main/java/org/example/sharedprompts/domain/follow/Follow.java
git add src/main/java/org/example/sharedprompts/domain/like/
git add src/main/java/org/example/sharedprompts/domain/notification/
git add src/main/java/org/example/sharedprompts/domain/prompt/Prompt.java
git add src/main/java/org/example/sharedprompts/domain/report/Report.java
git add src/main/java/org/example/sharedprompts/domain/tag/
git add src/main/java/org/example/sharedprompts/domain/user/User.java
git commit -m "Improve domain entities with additional fields and logic"
```

---

### 7. 도메인 서비스 개선
**브랜치**: `refactor/domain-services-improvement`

**변경 내용**:
- 도메인 서비스 로직 개선
- 새로운 서비스 클래스 추가

**커밋 전략**:
```bash
git checkout dev
git pull origin dev
git checkout -b refactor/domain-services-improvement
git add src/main/java/org/example/sharedprompts/domain/comment/service/
git add src/main/java/org/example/sharedprompts/domain/favorite/service/
git add src/main/java/org/example/sharedprompts/domain/follow/service/
git add src/main/java/org/example/sharedprompts/domain/prompt/service/
git add src/main/java/org/example/sharedprompts/domain/user/service/
git commit -m "Improve domain services with better separation of concerns"
```

---

### 8. 설정 및 의존성 업데이트
**브랜치**: `chore/config-and-dependencies-update`

**변경 내용**:
- build.gradle 업데이트
- application.yml 수정
- 기타 설정 파일 수정

**커밋 전략**:
```bash
git checkout dev
git pull origin dev
git checkout -b chore/config-and-dependencies-update
git add build.gradle
git add src/main/resources/application.yml
git add src/main/java/org/example/sharedprompts/global/config/
git add src/main/java/org/example/sharedprompts/global/util/
git commit -m "Update build configuration and application properties"
```

---

## 📅 실행 순서

### 권장 실행 순서:
1. ✅ `feature/auth-security-refactoring` (가장 큰 변경, 다른 브랜치의 기반)
2. ✅ `refactor/infra-config-reorganization` (인프라 설정)
3. ✅ `feature/rate-limiting` (독립적 기능)
4. ✅ `refactor/admin-service-consolidation` (Admin 리팩토링)
5. ✅ `feature/audit-logging` (Audit 기능)
6. ✅ `refactor/domain-entities-improvement` (엔티티 개선)
7. ✅ `refactor/domain-services-improvement` (서비스 개선)
8. ✅ `chore/config-and-dependencies-update` (설정 업데이트)

---

## ⚠️ 충돌 예방 전략

### 1. 브랜치 생성 전략
- 각 브랜치는 **최신 dev 브랜치**에서 생성
- 이전 브랜치가 머지된 후 다음 브랜치 생성 시 `git pull origin dev` 실행

### 2. 커밋 분할 전략
- 큰 변경사항은 작은 논리적 단위로 분할
- 각 커밋은 독립적으로 동작 가능하도록 구성
- 의존성이 있는 변경사항은 순서대로 커밋

### 3. PR 및 머지 전략
- 각 브랜치별로 PR 생성
- 순차적으로 머지하여 충돌 최소화
- 머지 후 다음 브랜치 생성 전에 dev 동기화

### 4. 현재 Staged 변경사항 처리
현재 staged된 변경사항은 인증/보안 리팩토링의 일부입니다:
- `AUTH_SECURITY_REFACTORING_PLAN.md` (문서, 제거 권장)
- `global/jwt/*` (삭제)
- `global/response/*` (삭제)
- `dto/report/validator/ValidReportProcessStatus.java` (이동)

---

## 🚀 즉시 실행 가능한 명령어

### 현재 상태 확인
```bash
git status
git diff --cached --name-only
```

### 첫 번째 브랜치 생성 및 커밋
```bash
# 브랜치 생성
git checkout -b feature/auth-security-refactoring

# 문서 파일은 제외 (필요시 나중에 별도 커밋)
git restore --staged AUTH_SECURITY_REFACTORING_PLAN.md

# 첫 번째 커밋: Response 클래스 이동
git add src/main/java/org/example/sharedprompts/global/response/
git add src/main/java/org/example/sharedprompts/dto/common/
git commit -m "Move response classes from global/response to dto/common"

# 두 번째 커밋: 기존 JWT 패키지 삭제
git add src/main/java/org/example/sharedprompts/global/jwt/
git commit -m "Remove old global/jwt package (moved to auth package)"

# 세 번째 커밋: Validator 이동
git add src/main/java/org/example/sharedprompts/dto/report/validator/ValidReportProcessStatus.java
git commit -m "Move ValidReportProcessStatus to validator package"

# 나머지 변경사항은 위의 단계별 커밋 전략을 따르세요
```

---

## 📝 커밋 메시지 규칙

### 형식
- **명령형** 사용 (예: "Add", "Remove", "Update", "Refactor")
- **간결하게** 작성 (50자 이내 권장)
- **변경 목적** 명확히 표현

### 예시
- ✅ `Refactor JWT payload to minimal claims (userId, role, tokenVersion)`
- ✅ `Move response classes from global/response to dto/common`
- ✅ `Add rate limiting system with logging and statistics`
- ❌ `fix: jwt 관련 수정` (너무 모호함)
- ❌ `update` (너무 짧고 불명확)

---

## 🔄 브랜치별 작업 체크리스트

### feature/auth-security-refactoring
- [ ] Response 클래스 이동 커밋
- [ ] 기존 JWT 패키지 삭제 커밋
- [ ] Validator 이동 커밋
- [ ] 새로운 auth 패키지 추가 커밋
- [ ] 컨트롤러 및 서비스 업데이트 커밋
- [ ] 테스트 및 검증
- [ ] PR 생성 및 머지

### refactor/infra-config-reorganization
- [ ] infra 패키지 추가 커밋
- [ ] 설정 파일 이동 커밋
- [ ] 테스트 및 검증
- [ ] PR 생성 및 머지

### feature/rate-limiting
- [ ] Rate limiting 시스템 추가 커밋
- [ ] 로깅 및 통계 기능 커밋
- [ ] 테스트 및 검증
- [ ] PR 생성 및 머지

### refactor/admin-service-consolidation
- [ ] Admin 서비스 통합 커밋
- [ ] 테스트 및 검증
- [ ] PR 생성 및 머지

### feature/audit-logging
- [ ] Audit 로깅 시스템 추가 커밋
- [ ] 테스트 및 검증
- [ ] PR 생성 및 머지

### refactor/domain-entities-improvement
- [ ] 엔티티 개선 커밋
- [ ] 테스트 및 검증
- [ ] PR 생성 및 머지

### refactor/domain-services-improvement
- [ ] 서비스 개선 커밋
- [ ] 테스트 및 검증
- [ ] PR 생성 및 머지

### chore/config-and-dependencies-update
- [ ] 설정 파일 업데이트 커밋
- [ ] 테스트 및 검증
- [ ] PR 생성 및 머지

---

## 💡 참고사항

1. **Push는 직접 수행**: 각 브랜치 작업 완료 후 `git push origin <branch-name>` 실행
2. **PR 생성**: 각 브랜치별로 PR을 생성하여 코드 리뷰 후 머지
3. **충돌 해결**: 충돌 발생 시 최신 dev 브랜치와 동기화 후 해결
4. **테스트**: 각 브랜치 머지 전에 충분한 테스트 수행

---

**작성일**: 2024년
**작성자**: AI 개발 도우미
**목적**: dev 브랜치 변경사항을 논리적 단위로 분리하여 브랜치 전략 수립




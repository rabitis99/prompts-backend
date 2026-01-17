# SharedPrompts 백엔드 배포 가이드

> **프론트엔드 배포 가이드는** `../front/sharedPrompts/my-app/FREE_DEPLOYMENT_GUIDE.md`를 참고하세요.

## 📋 개요

이 문서는 **SharedPrompts 백엔드 (Spring Boot)**를 무료로 배포하는 방법을 설명합니다.

**주요 기술 스택**:
- Spring Boot 3.5.8
- Java 17
- MySQL
- Redis
- RabbitMQ (선택적)
- Spring AI (OpenAI/Gemini)
- OAuth2 (Google/Naver/Kakao)
- JWT

---

## 🚀 Railway 배포 (권장)

### 1. 프로젝트 준비

```bash
# 빌드 테스트
./gradlew clean build

# JAR 파일 확인
ls -la build/libs/
# sharedPrompts-0.0.1-SNAPSHOT.jar
```

### 2. Railway 계정 생성

1. [Railway](https://railway.app) 접속
2. GitHub 계정으로 로그인
3. 무료 플랜 선택

### 3. 프로젝트 생성

1. Railway 대시보드 → "New Project" → "Deploy from GitHub repo"
2. 백엔드 저장소 선택
3. 자동으로 빌드 시작

### 4. 서비스 추가

#### 4.1 MySQL 데이터베이스

1. 프로젝트 → "New" → "Database" → "Add MySQL"
2. 자동으로 환경 변수 설정됨

#### 4.2 Redis

1. 프로젝트 → "New" → "Database" → "Add Redis"
2. 자동으로 환경 변수 설정됨

#### 4.3 RabbitMQ (선택적)

Railway는 RabbitMQ를 직접 지원하지 않으므로:

**옵션 A: CloudAMQP 사용 (무료)**

1. [CloudAMQP](https://www.cloudamqp.com) 계정 생성
2. "Create Instance" → "Lemur" 플랜 (무료)
3. 연결 정보 복사

**옵션 B: RabbitMQ 생략**

알림 기능을 동기식으로 변경

### 5. 환경 변수 설정

Railway 대시보드 → 프로젝트 → Variables:

#### 필수 환경 변수

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=production

# 포트 (Railway가 자동 제공)
PORT=8080
SERVER_PORT=${PORT}

# MySQL (Railway MySQL 서비스)
SPRING_DATASOURCE_URL=${MYSQL_URL}
SPRING_DATASOURCE_USERNAME=${MYSQL_USER}
SPRING_DATASOURCE_PASSWORD=${MYSQL_PASSWORD}

# Redis (Railway Redis 서비스)
REDIS_HOST=${REDIS_HOST}
REDIS_PORT=${REDIS_PORT}

# RabbitMQ (CloudAMQP 사용 시)
RABBITMQ_HOST=your-host.cloudamqp.com
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=your-username
RABBITMQ_PASSWORD=your-password

# JWT
JWT_SECRET=your-strong-secret-key-min-32-characters
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# OAuth2 - Google
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# OAuth2 - Naver
NAVER_CLIENT_ID=your-naver-client-id
NAVER_CLIENT_SECRET=your-naver-client-secret

# OAuth2 - Kakao
KAKAO_CLIENT_ID=your-kakao-client-id
KAKAO_CLIENT_SECRET=your-kakao-client-secret

# OAuth2 리다이렉트 URL
OAUTH2_REDIRECT_FRONT_URL=https://your-frontend.vercel.app
OAUTH2_FAILURE_REDIRECT_URL=https://your-frontend.vercel.app/auth/oauth-failure
OAUTH2_SALT=your-random-salt

# CORS
CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app

# AI - OpenAI
SPRING_AI_OPENAI_API_KEY=your-openai-api-key

# AI - Google Gemini
GOOGLE_GEMINI_API_KEY=your-gemini-api-key
GOOGLE_GEMINI_MODEL=gemini-1.5-pro
GOOGLE_GEMINI_BASE_URL=https://generativelanguage.googleapis.com/v1beta
GOOGLE_GEMINI_TIMEOUT_SECONDS=30

# JPA (선택적)
JPA_SHOW_SQL=false
JPA_DDL_AUTO=validate
HIBERNATE_DIALECT=org.hibernate.dialect.MySQL8Dialect
```

### 6. application-production.yml 생성

`src/main/resources/application-production.yml` 파일 생성:

```yaml
server:
  port: ${PORT:8080}
  servlet:
    context-path: /api

spring:
  profiles:
    active: production

  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: ${HIBERNATE_DIALECT:org.hibernate.dialect.MySQL8Dialect}

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}

  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}

jwt:
  secret: ${JWT_SECRET}
  access:
    expiration: ${JWT_ACCESS_EXPIRATION:3600000}
  refresh:
    expiration: ${JWT_REFRESH_EXPIRATION:604800000}

oauth2:
  failure-redirect-url: ${OAUTH2_FAILURE_REDIRECT_URL}
  salt: ${OAUTH2_SALT}
  redirect:
    front-url: ${OAUTH2_REDIRECT_FRONT_URL}

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS}

logging:
  level:
    root: INFO
```

### 7. 빌드 설정

Railway는 Gradle을 자동 감지합니다. 필요시 `railway.toml` 생성:

```toml
[build]
builder = "NIXPACKS"

[deploy]
startCommand = "java -Xmx384m -Xms128m -jar build/libs/sharedPrompts-0.0.1-SNAPSHOT.jar"
healthcheckPath = "/api/actuator/health"
healthcheckTimeout = 300
restartPolicyType = "ON_FAILURE"
```

**중요**: JVM 메모리 제한 (`-Xmx384m`)은 Railway 무료 플랜(512MB RAM)에 맞춘 설정입니다.

### 8. OAuth2 제공자 설정

각 OAuth2 제공자의 개발자 콘솔에서 리다이렉트 URL을 등록해야 합니다:

#### Google

1. [Google Cloud Console](https://console.cloud.google.com)
2. APIs & Services → Credentials → OAuth 2.0 Client IDs
3. Authorized redirect URIs 추가:
   ```
   https://your-backend.railway.app/api/login/oauth2/code/google
   ```

#### Naver

1. [Naver Developers](https://developers.naver.com)
2. 애플리케이션 → API 설정 → 서비스 URL
3. Callback URL 추가:
   ```
   https://your-backend.railway.app/api/login/oauth2/code/naver
   ```

#### Kakao

1. [Kakao Developers](https://developers.kakao.com)
2. 애플리케이션 → 플랫폼 → Web 플랫폼 등록
3. Redirect URI 추가:
   ```
   https://your-backend.railway.app/api/login/oauth2/code/kakao
   ```

### 9. 배포 완료

배포 후 확인:

1. **헬스 체크**: `https://your-backend.railway.app/api/actuator/health`
2. **로그 확인**: Railway 대시보드 → Logs
3. **API 테스트**: 프론트엔드에서 API 호출 테스트

---

## 📝 배포 체크리스트

- [ ] `application-production.yml` 생성
- [ ] MySQL 데이터베이스 추가
- [ ] Redis 추가
- [ ] RabbitMQ 설정 (선택적)
- [ ] 환경 변수 설정:
  - [ ] Spring Profile
  - [ ] 데이터베이스 연결 정보
  - [ ] Redis 연결 정보
  - [ ] RabbitMQ 연결 정보 (선택적)
  - [ ] JWT 설정
  - [ ] OAuth2 클라이언트 정보
  - [ ] OAuth2 리다이렉트 URL
  - [ ] CORS 설정
  - [ ] AI API 키
- [ ] OAuth2 제공자에 리다이렉트 URL 등록
- [ ] 빌드 및 배포 성공 확인
- [ ] 헬스 체크 엔드포인트 테스트
- [ ] API 엔드포인트 테스트

---

## 🔧 트러블슈팅

### 빌드 실패

**원인**: Gradle 빌드 오류

**해결**:
```bash
# 로컬에서 빌드 테스트
./gradlew clean build

# 의존성 문제 시
./gradlew clean build --refresh-dependencies
```

### 데이터베이스 연결 실패

**원인**: MySQL 연결 정보 오류

**해결**:
1. Railway 대시보드에서 MySQL 서비스 확인
2. 환경 변수 `SPRING_DATASOURCE_URL` 확인
3. 연결 문자열 형식: `jdbc:mysql://host:port/database?sslMode=REQUIRED`

### Redis 연결 실패

**원인**: Redis 연결 정보 오류

**해결**:
1. Railway 대시보드에서 Redis 서비스 확인
2. 환경 변수 `REDIS_HOST`, `REDIS_PORT` 확인

### OAuth2 리다이렉트 오류

**원인**: OAuth2 제공자에 리다이렉트 URL 미등록

**해결**:
1. 각 OAuth2 제공자 콘솔에서 리다이렉트 URL 등록
2. 백엔드 URL 정확히 확인: `https://your-backend.railway.app/api/login/oauth2/code/{provider}`

### 메모리 부족 (OOM)

**원인**: JVM 힙 메모리 초과

**해결**:
- Start Command에 메모리 제한 추가:
  ```bash
  java -Xmx384m -Xms128m -jar build/libs/sharedPrompts-0.0.1-SNAPSHOT.jar
  ```

---

## 📚 참고 자료

- [Railway 문서](https://docs.railway.app)
- [Spring Boot 배포 가이드](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)
- [MySQL 연결 가이드](https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-usagenotes-connect-driver-manager.html)

---

**프론트엔드 배포 가이드**: `../front/sharedPrompts/my-app/FREE_DEPLOYMENT_GUIDE.md`


# Docker 배포 가이드

## 개요

이 프로젝트는 Docker를 사용하여 단일 배포 환경에서 애플리케이션을 관리합니다. 

**단일 배포 모드**: 모든 Production 모듈을 하나의 컨테이너에서 실행 (`docker-compose.yml`)
**다중 모듈 배포 모드**: 각 Production 모듈을 독립적인 컨테이너로 실행 (`docker-compose.modules.yml`)

데이터베이스는 외부 서버에 별도로 배치됩니다.

## 아키텍처

### 단일 배포 모드 (docker-compose.yml)

- **애플리케이션 컨테이너**: Spring Boot 애플리케이션 (모든 Production 모듈 포함)
  - BlogProductionModule
  - EmailProductionModule
  - TextProductionModule
  - ImageProductionModule
  - DocumentProductionModule
- **Redis 컨테이너**: 캐시 및 세션 저장소
- **RabbitMQ 컨테이너**: 메시지 큐
- **외부 데이터베이스**: MySQL (Docker 컨테이너 외부)

### 다중 모듈 배포 모드 (docker-compose.modules.yml)

각 Production 모듈이 독립적인 컨테이너로 실행됩니다. 자세한 내용은 [README.MODULES.md](./README.MODULES.md)를 참조하세요.

## 사전 요구사항

- Docker 20.10 이상
- Docker Compose 2.0 이상
- 외부 MySQL 데이터베이스 서버 접근 권한

## 빠른 시작

### 1. 환경 변수 설정

`.env.example` 파일을 복사하여 `.env` 파일을 생성하고 필요한 값들을 설정합니다:

```bash
cp .env.example .env
```

`.env` 파일에서 다음 항목들을 반드시 설정해야 합니다:

- `SPRING_DATASOURCE_URL`: 외부 MySQL 데이터베이스 연결 URL
- `SPRING_DATASOURCE_USERNAME`: 데이터베이스 사용자명
- `SPRING_DATASOURCE_PASSWORD`: 데이터베이스 비밀번호
- `JWT_SECRET`: JWT 토큰 서명용 비밀키
- OAuth2 클라이언트 정보 (Google, Naver, Kakao)

### 2. 프로덕션 환경 실행

```bash
docker-compose up -d
```

### 3. 개발 환경 실행

```bash
docker-compose -f docker-compose.dev.yml up -d
```

## 서비스 구성

### 애플리케이션 서비스 (app)

- **포트**: 8080 (기본값, `SERVER_PORT` 환경변수로 변경 가능)
- **의존성**: Redis, RabbitMQ
- **헬스체크**: `/api/actuator/health` 엔드포인트

### Redis 서비스

- **포트**: 6379
- **데이터 영속성**: `redis_data` 볼륨에 저장
- **네트워크**: `sharedprompts_network`

### RabbitMQ 서비스

- **AMQP 포트**: 5672
- **관리 UI 포트**: 15672
- **데이터 영속성**: `rabbitmq_data` 볼륨에 저장
- **네트워크**: `sharedprompts_network`

## 네트워크 구성

모든 서비스는 `sharedprompts_network`라는 브리지 네트워크에 연결되어 있습니다. 이를 통해:

- 컨테이너 간 통신이 가능합니다
- 서비스 이름으로 서로를 참조할 수 있습니다 (예: `redis`, `rabbitmq`)
- 외부 데이터베이스는 호스트 네트워크나 외부 IP를 통해 접근합니다

## 데이터베이스 연결

데이터베이스는 Docker 컨테이너 외부의 별도 서버에 배치됩니다. 연결 설정은 `.env` 파일의 다음 환경변수를 통해 구성됩니다:

```env
SPRING_DATASOURCE_URL=jdbc:mysql://your-db-host:3306/sharedprompts?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=your_db_username
SPRING_DATASOURCE_PASSWORD=your_db_password
```

**중요**: 
- 데이터베이스 서버가 Docker 컨테이너에서 접근 가능한 네트워크에 있어야 합니다
- 방화벽 설정에서 데이터베이스 포트(3306)가 열려있어야 합니다
- SSL 연결을 사용하는 경우 인증서 설정이 필요할 수 있습니다

## 환경 변수

주요 환경 변수는 `.env.example` 파일에 정의되어 있습니다. 프로덕션 환경에서는 다음을 반드시 설정해야 합니다:

### 필수 설정

- `SPRING_DATASOURCE_URL`: 데이터베이스 연결 URL
- `SPRING_DATASOURCE_USERNAME`: 데이터베이스 사용자명
- `SPRING_DATASOURCE_PASSWORD`: 데이터베이스 비밀번호
- `JWT_SECRET`: JWT 토큰 서명용 비밀키 (최소 256비트)
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`: Google OAuth2 설정
- `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`: Naver OAuth2 설정
- `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`: Kakao OAuth2 설정

### 선택적 설정

- `SPRING_AI_OPENAI_API_KEY`: OpenAI API 키
- `GOOGLE_GEMINI_API_KEY`: Google Gemini API 키
- `PAYMENT_*`: 결제 관련 설정

## 명령어

### 서비스 시작

```bash
docker-compose up -d
```

### 서비스 중지

```bash
docker-compose down
```

### 로그 확인

```bash
docker-compose logs -f app
```

### 특정 서비스 재시작

```bash
docker-compose restart app
```

### 이미지 재빌드

```bash
docker-compose build --no-cache app
docker-compose up -d
```

## 모니터링

### 헬스체크

애플리케이션 헬스체크 엔드포인트:

```bash
curl http://localhost:8080/api/actuator/health
```

### RabbitMQ 관리 UI

브라우저에서 접근:

```
http://localhost:15672
```

기본 로그인 정보:
- Username: `guest` (또는 `RABBITMQ_USERNAME` 환경변수 값)
- Password: `guest` (또는 `RABBITMQ_PASSWORD` 환경변수 값)

## 문제 해결

### 데이터베이스 연결 실패

1. 데이터베이스 서버가 실행 중인지 확인
2. 네트워크 연결 확인: `docker exec -it sharedprompts_app ping your-db-host`
3. 방화벽 설정 확인
4. 데이터베이스 사용자 권한 확인

### 컨테이너 간 통신 실패

1. 네트워크 확인: `docker network inspect sharedprompts_sharedprompts_network`
2. 서비스 이름으로 접근하는지 확인 (IP 주소가 아닌)
3. `depends_on` 설정 확인

### 포트 충돌

다른 서비스가 이미 포트를 사용 중인 경우:

1. `.env` 파일에서 포트 번호 변경
2. 또는 실행 중인 서비스 중지

## 보안 고려사항

1. **환경 변수**: `.env` 파일은 절대 버전 관리에 포함하지 마세요
2. **비밀번호**: 프로덕션 환경에서는 강력한 비밀번호 사용
3. **네트워크**: 필요한 포트만 외부에 노출
4. **데이터베이스**: SSL 연결 사용 권장
5. **JWT Secret**: 최소 256비트 길이의 랜덤 문자열 사용

## 확장성

향후 여러 모듈을 독립적인 컨테이너로 분리하려면:

1. 각 모듈별로 별도의 Dockerfile 생성
2. `docker-compose.yml`에 각 모듈 서비스 추가
3. 모듈 간 통신을 위해 네트워크 설정 유지
4. 공통 서비스(Redis, RabbitMQ)는 공유

## 참고 자료

- [Docker 공식 문서](https://docs.docker.com/)
- [Docker Compose 공식 문서](https://docs.docker.com/compose/)


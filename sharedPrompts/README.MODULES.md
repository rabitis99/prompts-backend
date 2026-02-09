# Production 모듈 독립 배포 가이드

## 개요

각 Production 모듈(Blog, Email, Text, Image, Document)을 독립적인 Docker 컨테이너로 배포할 수 있습니다. 모든 모듈은 동일한 외부 데이터베이스를 공유하며, 생성된 결과물은 DB에 저장됩니다.

## 아키텍처

```
┌─────────────────┐
│  Blog Module    │ :8081
│  (Container)    │
└────────┬────────┘
         │
┌────────┴────────┐
│  Email Module   │ :8082
│  (Container)    │
└────────┬────────┘
         │
┌────────┴────────┐
│  Text Module    │ :8083
│  (Container)    │
└────────┬────────┘
         │
┌────────┴────────┐
│  Image Module   │ :8084
│  (Container)    │
└────────┬────────┘
         │
┌────────┴────────┐
│ Document Module │ :8085
│  (Container)    │
└────────┬────────┘
         │
    ┌────┴────┐
    │        │
┌───▼───┐ ┌─▼────┐
│ Redis │ │RabbitMQ│
└───────┘ └──────┘
    │        │
    └───┬────┘
        │
┌───────▼────────┐
│ External MySQL │
│   (Database)   │
└────────────────┘
```

## 모듈 포트

| 모듈 | 포트 | 환경 변수 |
|------|------|-----------|
| Blog Module | 8081 | `BLOG_MODULE_PORT` |
| Email Module | 8082 | `EMAIL_MODULE_PORT` |
| Text Module | 8083 | `TEXT_MODULE_PORT` |
| Image Module | 8084 | `IMAGE_MODULE_PORT` |
| Document Module | 8085 | `DOCUMENT_MODULE_PORT` |

## 빠른 시작

### 1. 환경 변수 설정

`.env` 파일에 다음 설정을 추가합니다:

```env
# 데이터베이스 연결 (모든 모듈이 공유)
SPRING_DATASOURCE_URL=jdbc:mysql://your-db-host:3306/sharedprompts?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=your_db_username
SPRING_DATASOURCE_PASSWORD=your_db_password

# 모듈별 포트 설정
BLOG_MODULE_PORT=8081
EMAIL_MODULE_PORT=8082
TEXT_MODULE_PORT=8083
IMAGE_MODULE_PORT=8084
DOCUMENT_MODULE_PORT=8085
```

### 2. 모든 모듈 실행

```bash
docker-compose -f docker-compose.modules.yml up -d
```

### 3. 특정 모듈만 실행

특정 모듈만 실행하려면:

```bash
# Blog 모듈만 실행
docker-compose -f docker-compose.modules.yml up -d blog-module redis rabbitmq

# Email과 Text 모듈만 실행
docker-compose -f docker-compose.modules.yml up -d email-module text-module redis rabbitmq
```

## 모듈 활성화

각 컨테이너는 `ACTIVE_MODULES` 환경 변수를 통해 활성화할 모듈을 지정합니다. 각 컨테이너에서는 하나의 모듈만 활성화됩니다.

예시:
- Blog Module: `ACTIVE_MODULES=blog`
- Email Module: `ACTIVE_MODULES=email`
- Text Module: `ACTIVE_MODULES=text`
- Image Module: `ACTIVE_MODULES=image`
- Document Module: `ACTIVE_MODULES=document`

## 데이터베이스 공유

모든 모듈은 동일한 외부 MySQL 데이터베이스를 사용합니다:

- **연결 정보**: `.env` 파일의 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`로 설정
- **저장 위치**: 각 모듈이 생성한 결과물은 동일한 DB의 `production_artifacts` 테이블에 저장됩니다
- **트랜잭션**: 각 모듈은 독립적으로 DB에 접근하며, ProductionCoordinator가 결과를 자동으로 저장합니다

## 모듈 간 통신

모듈 간 통신은 다음을 통해 이루어집니다:

1. **RabbitMQ**: 비동기 메시징을 통한 모듈 간 통신
2. **Redis**: 캐시 및 세션 공유
3. **데이터베이스**: 생성된 결과물을 DB에 저장하여 다른 모듈이 참조 가능

## 네트워크 구성

모든 모듈과 서비스는 `sharedprompts_network` 브리지 네트워크에 연결되어 있습니다:

- 모듈 간 서비스 이름으로 통신 가능
- Redis: `redis:6379`
- RabbitMQ: `rabbitmq:5672`
- 외부 DB: 환경 변수로 지정된 호스트

## 모니터링

### 각 모듈의 헬스체크

```bash
# Blog Module
curl http://localhost:8081/api/actuator/health

# Email Module
curl http://localhost:8082/api/actuator/health

# Text Module
curl http://localhost:8083/api/actuator/health

# Image Module
curl http://localhost:8084/api/actuator/health

# Document Module
curl http://localhost:8085/api/actuator/health
```

### 로그 확인

```bash
# 특정 모듈 로그
docker-compose -f docker-compose.modules.yml logs -f blog-module

# 모든 모듈 로그
docker-compose -f docker-compose.modules.yml logs -f
```

## 스케일링

각 모듈을 독립적으로 스케일링할 수 있습니다:

```bash
# Blog 모듈을 3개 인스턴스로 실행
docker-compose -f docker-compose.modules.yml up -d --scale blog-module=3
```

포트 충돌을 피하기 위해 각 인스턴스는 다른 포트를 사용해야 합니다. 이 경우 로드 밸런서나 리버스 프록시를 사용하는 것을 권장합니다.

## 문제 해결

### 모듈이 시작되지 않음

1. 환경 변수 확인: `ACTIVE_MODULES`가 올바르게 설정되었는지 확인
2. 데이터베이스 연결 확인: 외부 DB에 접근 가능한지 확인
3. 로그 확인: `docker-compose logs <module-name>`

### 데이터베이스 연결 실패

1. 외부 DB 서버가 실행 중인지 확인
2. 네트워크 연결 확인: 컨테이너에서 DB 호스트에 ping 가능한지 확인
3. 방화벽 설정 확인: DB 포트(3306)가 열려있는지 확인
4. DB 사용자 권한 확인: 모든 모듈이 동일한 DB에 접근할 수 있는지 확인

### 포트 충돌

다른 서비스가 이미 포트를 사용 중인 경우:

1. `.env` 파일에서 포트 번호 변경
2. 또는 실행 중인 서비스 중지

## 보안 고려사항

1. **데이터베이스 접근**: 모든 모듈이 동일한 DB 자격 증명을 사용하므로, 각 모듈의 접근 권한을 제한하는 것을 고려하세요
2. **네트워크 격리**: 필요시 모듈별로 별도의 네트워크를 구성할 수 있습니다
3. **환경 변수**: 민감한 정보는 Docker Secrets나 외부 설정 관리 도구를 사용하세요

## 성능 최적화

1. **리소스 제한**: 각 모듈에 적절한 CPU/메모리 제한 설정
2. **커넥션 풀**: 각 모듈이 독립적인 커넥션 풀을 사용하므로, 전체 커넥션 수를 고려하여 설정
3. **캐싱**: Redis를 활용한 공통 데이터 캐싱

## 참고

- 단일 애플리케이션으로 실행하려면 `docker-compose.yml` 사용
- 개발 환경에서는 `docker-compose.dev.yml` 사용


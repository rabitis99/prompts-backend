# 결제 모듈 연동 및 구현 가이드

## 📋 목차
1. [아키텍처 개요](#아키텍처-개요)
2. [연동 필요 사항](#연동-필요-사항)
3. [미구현 기능](#미구현-기능)
4. [데이터베이스 마이그레이션](#데이터베이스-마이그레이션)
5. [환경 변수 설정](#환경-변수-설정)
6. [테스트](#테스트)

---

## 🏗️ 아키텍처 개요

### 디자인 패턴

#### 1. 파사드 패턴 (Facade Pattern)
결제 서비스의 복잡한 로직을 책임별로 분리하여 관리합니다.

**파사드 클래스**:
- `PaymentValidationFacade`: 검증 로직 (티어 체크, 권한 체크, 상태 체크)
- `PaymentAmountFacade`: 금액 처리 (환율 변환, 포인트 사용, 실제 결제 금액 계산)
- `PaymentProviderFacade`: 결제사별 처리 (승인, 취소, 환불, 상태 조회)
- `PaymentPostProcessFacade`: 후처리 (포인트/캐시백 적립, 이벤트 발행, 메트릭, 로깅)

**위치**: `src/main/java/org/example/sharedprompts/domain/payment/service/facade/`

#### 2. DTO Mapper 패턴
RequestDto와 ResponseDto에 mapper 메서드를 제공하여 엔티티 변환을 깔끔하게 처리합니다.

**RequestDto Mapper 메서드**:
- `PaymentRequestDto.toPaymentBuilder()`: Payment 엔티티 빌더 생성
- `PointUseRequestDto.toPointBuilder()`: Point 엔티티 빌더 생성
- `PaymentCancelRequestDto.getPaymentIdAsLong()`, `getReasonOrDefault()`: 헬퍼 메서드
- `PaymentRefundRequestDto.getPaymentIdAsLong()`, `getAmountOrNull()`, `getReasonOrDefault()`: 헬퍼 메서드
- `TierChangeRequestDto.getReasonOrDefault()`: 헬퍼 메서드

**ResponseDto Mapper 메서드**:
- 모든 ResponseDto에 `public static from()` 메서드 제공
  - `PaymentResponseDto.from(Payment)`
  - `PointResponseDto.from(Point)`
  - `PaymentStatusResponseDto.from(Payment)`
  - `PointBalanceResponseDto.from(Long, BigDecimal, BigDecimal, BigDecimal)`
  - `TierInfoResponseDto.from(User, int, int, int)`

### 패키지 구조

```
domain/payment/
├── config/              # 설정 클래스
├── dto/                 # 결제사별 DTO (KakaoPay 등)
├── enums/               # 열거형 (PaymentMethod, PaymentStatus 등)
├── event/               # 이벤트 정의
├── logging/             # 로깅 서비스
├── metrics/             # 메트릭 수집
├── repository/          # 데이터 접근 계층
├── service/             # 서비스 계층
│   ├── cashback/        # 캐시백 서비스
│   ├── core/            # 핵심 결제 서비스
│   ├── evnet/           # 이벤트 발행 (PaymentEventPublisher)
│   ├── exchange/        # 환율 서비스
│   ├── facade/          # 파사드 패턴 구현
│   ├── notification/    # 알림 서비스
│   ├── payment/         # 결제사 인터페이스
│   ├── point/           # 포인트 서비스
│   └── provider/        # 결제사 구현체
└── statistics/          # 통계 클래스
```

### 통계 클래스 분리

결제 관련 통계 클래스는 별도 패키지로 분리되었습니다.

**위치**: `src/main/java/org/example/sharedprompts/domain/payment/statistics/`

**클래스**:
- `PaymentFailureStatistics`: 결제 실패 통계

### 모니터링 및 메트릭

결제 모니터링과 메트릭 수집 기능이 구현되어 있습니다.

**모니터링 서비스**:
- `PaymentMonitoringService`: 결제 실패/성공 이벤트 리스너, 실패 통계 조회, 실패율 임계값 체크
- `PaymentMetrics`: Prometheus 메트릭 수집 (성공/실패 카운터, 처리 시간, 금액 게이지 등)
- `PaymentFailureStatistics`: 일일 결제 실패 통계 데이터 클래스

**위치**: 
- `src/main/java/org/example/sharedprompts/domain/payment/service/PaymentMonitoringService.java`
- `src/main/java/org/example/sharedprompts/domain/payment/metrics/PaymentMetrics.java`

---

## 🔗 연동 필요 사항

### 1. 결제사 API 연동

#### 1.1 카카오페이
- **필요 항목**: API 키, Secret 키
- **설정 위치**: `application.yml` 또는 환경 변수
- **환경 변수**:
  ```bash
  KAKAO_API_KEY=your_kakao_api_key
  KAKAO_SECRET=your_kakao_secret
  ```
- **연동 문서**: [카카오페이 개발자센터](https://developers.kakao.com/docs/latest/ko/kakaopay/common)
- **구현 상태**: ✅ 기본 구조 완료 (실제 API 호출 로직 포함)

#### 1.2 토스페이먼츠
- **필요 항목**: API 키, Secret 키
- **환경 변수**:
  ```bash
  TOSS_API_KEY=your_toss_api_key
  TOSS_SECRET=your_toss_secret
  ```
- **연동 문서**: [토스페이먼츠 개발자센터](https://developers.tosspayments.com/)
- **구현 상태**: ✅ 기본 구조 완료 (실제 API 호출 로직 포함)
- **주의사항**: Webhook 서명 검증 로직 구현 완료 (검증 필요)

#### 1.3 페이팔
- **필요 항목**: Client ID, Client Secret
- **환경 변수**:
  ```bash
  PAYPAL_CLIENT_ID=your_paypal_client_id
  PAYPAL_CLIENT_SECRET=your_paypal_client_secret
  ```
- **연동 문서**: [PayPal API 문서](https://developer.paypal.com/docs/api/overview/)
- **구현 상태**: ✅ 기본 구조 완료 (실제 API 호출 로직 포함)
- **주의사항**: Webhook 서명 검증 로직 수정 완료

### 2. 이메일 발송 (SMTP)

- **필요 항목**: SMTP 서버 정보
- **환경 변수**:
  ```bash
  MAIL_HOST=smtp.gmail.com
  MAIL_PORT=587
  MAIL_USERNAME=your_email@gmail.com
  MAIL_PASSWORD=your_app_password
  MAIL_SMTP_AUTH=true
  MAIL_SMTP_STARTTLS_ENABLE=true
  ```
- **구현 상태**: ✅ Spring Mail 통합 완료
- **주의사항**: Gmail 사용 시 앱 비밀번호 필요

### 3. 푸시 알림 (FCM)

- **필요 항목**: Firebase 프로젝트 ID, 서비스 계정 키 파일
- **구현 상태**: ✅ FCM HTTP v1 API 연동 완료
- **구현 내용**:
  - `PushNotificationService`: FCM HTTP v1 API를 사용한 푸시 알림 발송
  - `FcmTokenService`: OAuth2 액세스 토큰 자동 발급 및 갱신
  - 결제 성공/실패 알림 발송 지원
- **환경 변수**:
  ```bash
  PAYMENT_FCM_ENABLED=true
  PAYMENT_FCM_PROJECT_ID=your_firebase_project_id
  PAYMENT_FCM_CREDENTIALS_PATH=/path/to/firebase-credentials.json
  # 또는 환경 변수로 설정
  GOOGLE_APPLICATION_CREDENTIALS=/path/to/firebase-credentials.json
  ```
- **주의사항**: 
  - Firebase 서비스 계정 키 파일 필요 (JSON 형식)
  - 클래스 경로에 `firebase/sharedprompt-8ed9d-firebase-adminsdk-fbsvc-601af9062b.json` 또는 `fcm-credentials.json` 파일 배치 가능
  - FCM이 비활성화되어 있으면 로그만 남기고 알림 발송 건너뜀

### 4. 환율 API

- **필요 항목**: 환율 API URL (API 키는 선택사항 - 무료 플랜은 불필요)
- **환경 변수**:
  ```bash
  # ExchangeRate-API 무료 플랜 사용 시 (API 키 불필요)
  EXCHANGE_RATE_API_URL=https://api.exchangerate-api.com/v4/latest/
  PAYMENT_EXCHANGE_RATE_SCHEDULE=0 0 * * * ?  # 매 시간 정각 (1시간 간격, 기본값)
  
  # 유료 플랜 또는 다른 API 사용 시에만 필요
  EXCHANGE_RATE_API_KEY=your_exchange_rate_api_key  # 선택사항
  ```
- **구현 상태**: ✅ 엔티티 기반 구조 완료, 스케줄러 및 Webhook 구현 완료
- **구현 내용**:
  - `ExchangeRate` 엔티티로 환율 정보 관리
  - `ExchangeRateScheduler`로 주기적으로 환율 업데이트 (기본: 1시간 간격)
    - ShedLock을 사용한 분산 락 처리
    - 지원 통화: KRW, EUR, JPY, CNY, GBP (기준 통화: USD)
    - 애플리케이션 시작 시 초기 로드 (`@PostConstruct`)
  - `ExchangeRateServiceImpl`은 엔티티에서 환율 조회
  - 역방향 환율 자동 계산 지원 (USD→KRW가 없으면 KRW→USD 역수 사용)
  - `ExchangeRateWebhookController`: 환율 업데이트 Webhook 엔드포인트 (`POST /webhooks/exchange-rates`)
- **추천 서비스**:
  
  **1. ExchangeRate-API** (현재 구현 기준) ⭐
  - **URL**: `https://api.exchangerate-api.com/v4/latest/`
  - **무료 플랜**: 월 1,500회 요청, 업데이트 주기: 24시간, **API 키 불필요** ✅
  - **유료 플랜**: $9.99/월부터 (무제한 요청, 실시간 업데이트, API 키 필요)
  - **장점**: 
    - 무료 플랜이 충분히 넉넉함
    - **API 키 없이 바로 사용 가능** (무료 플랜)
    - 간단한 사용법, 코드에서 API 키 체크 로직이 있어 유료 전환 시에도 호환
  - **단점**: 무료 플랜은 업데이트 주기가 24시간
  - **사용법**: 
    ```bash
    # 무료 플랜: API 키 없이 URL만 설정
    EXCHANGE_RATE_API_URL=https://api.exchangerate-api.com/v4/latest/USD
    
    # 유료 플랜: API 키 추가
    EXCHANGE_RATE_API_KEY=your_api_key
    ```
  - **문서**: https://www.exchangerate-api.com/docs
  
  **2. 한국은행 환율 API** (국내 공식) 🇰🇷
  - **URL**: `https://www.koreaexim.go.kr/site/program/financial/exchangeJSON`
  - **무료**: 완전 무료, API 키 불필요
  - **장점**: 한국 공식 데이터, 신뢰성 높음, 한국 원화 중심
  - **단점**: 한국 원화 기준만 제공, 일부 통화만 지원
  - **문서**: https://www.koreaexim.go.kr/ir/HPHKIR020M01
  
  **3. exchangerate.host** (간단한 무료 API)
  - **URL**: `https://api.exchangerate.host/latest`
  - **무료**: 완전 무료, API 키 불필요, 제한 없음
  - **장점**: 매우 간단한 사용법, 제한 없음, 실시간 데이터
  - **단점**: 상업적 사용 시 제한 가능성
  - **문서**: https://exchangerate.host/
  
  **4. Fixer.io** (유료 중심)
  - **URL**: `https://api.fixer.io/latest` (구버전) 또는 `https://api.apilayer.com/fixer/latest` (신버전)
  - **무료 플랜**: 월 100회 요청
  - **유료 플랜**: $10/월부터
  - **장점**: 정확한 데이터, 다양한 통화 지원, 실시간 업데이트
  - **단점**: 무료 플랜 제한이 많음
  - **문서**: https://fixer.io/
  
  **5. CurrencyLayer** (유료 중심)
  - **URL**: `https://api.currencylayer.com/live`
  - **무료 플랜**: 월 1,000회 요청
  - **유료 플랜**: $9.99/월부터
  - **장점**: 안정적인 서비스, 다양한 통화 지원
  - **단점**: 무료 플랜 제한
  - **문서**: https://currencylayer.com/
  
  **6. Open Exchange Rates** (무료/유료)
  - **URL**: `https://openexchangerates.org/api/latest.json`
  - **무료 플랜**: 월 1,000회 요청, USD 기준만
  - **유료 플랜**: $12/월부터
  - **장점**: 다양한 통화 지원, 안정적
  - **단점**: 무료 플랜은 USD 기준만
  - **문서**: https://openexchangerates.org/
  
  **추천 순서**:
  1. **개발/테스트**: exchangerate.host (가장 간단, 제한 없음)
  2. **프로덕션 (소규모)**: ExchangeRate-API (현재 사용 중, 무료 플랜 충분)
  3. **프로덕션 (대규모/실시간)**: Fixer.io 또는 CurrencyLayer (유료 플랜)
  4. **한국 원화 중심**: 한국은행 환율 API (무료, 공식 데이터)

### 5. Webhook 시크릿

- **필요 항목**: 각 결제사별 Webhook 시크릿
- **환경 변수**:
  ```bash
  PAYMENT_WEBHOOK_SECRET=your_webhook_secret
  ```
- **구현 상태**: ✅ 서명 검증 로직 완료 (수정 완료)

---

## ⚠️ 미구현 기능

### 1. 푸시 알림 설정 완료

**파일**: 
- `src/main/java/org/example/sharedprompts/domain/payment/service/notification/PushNotificationService.java`
- `src/main/java/org/example/sharedprompts/domain/payment/service/notification/FcmTokenService.java`

**현재 상태**: ✅ FCM HTTP v1 API 연동 완료

**구현 내용**:
- FCM HTTP v1 API를 사용한 푸시 알림 발송
- OAuth2 액세스 토큰 자동 발급 및 갱신 (`FcmTokenService`)
- 결제 성공/실패 알림 발송 지원
- FCM 비활성화 시 로그만 남기고 알림 발송 건너뜀

**필요 작업**:
- Firebase 프로젝트 생성 및 서비스 계정 키 파일 다운로드
- 환경 변수 또는 설정 파일에 Firebase 프로젝트 ID 및 credentials 경로 설정
- 테스트를 통한 실제 알림 발송 검증

### 2. 환율 API 실제 연동 테스트

**파일**: 
- `src/main/java/org/example/sharedprompts/domain/payment/ExchangeRate.java` (엔티티)
- `src/main/java/org/example/sharedprompts/domain/payment/repository/ExchangeRateRepository.java`
- `src/main/java/org/example/sharedprompts/domain/payment/service/exchange/ExchangeRateServiceImpl.java`
- `src/main/java/org/example/sharedprompts/scheduler/payment/ExchangeRateScheduler.java`

**현재 상태**: ✅ 엔티티 기반 구조 완료, 스케줄러 구현 완료

**구현 내용**:
- 환율을 엔티티로 관리 (`ExchangeRate`)
- `ExchangeRateScheduler`로 주기적으로 환율 API 호출하여 업데이트
  - ShedLock을 사용한 분산 환경에서의 중복 실행 방지
  - ExchangeRate-API 형식의 응답 파싱 구현
  - 지원 통화: KRW, EUR, JPY, CNY, GBP (기준 통화: USD)
- `ExchangeRateServiceImpl`은 엔티티에서 환율 조회
  - 역방향 환율 자동 계산 지원

**필요 작업**:
- 실제 환율 API 연동 테스트 및 검증
- API 응답 파싱 로직 검증 및 에러 처리 강화
- 지원 통화 목록 확장 (현재: KRW, EUR, JPY, CNY, GBP)
- 환율 API 장애 시 대체 API 연동 로직 추가
- Webhook 엔드포인트 보안 강화 (서명 검증 등)

**참고**: 환율 스케줄러 및 Webhook은 구현 완료되었으나, 실제 API 연동 테스트가 필요합니다.

### 3. Webhook 이벤트 처리 로직

**파일**: `src/main/java/org/example/sharedprompts/domain/payment/service/provider/*PaymentService.java`

**현재 상태**: 기본 구조 완료, 실제 이벤트 처리 로직 미구현

**구현 내용**:
- 모든 결제사 서비스에 `processWebhook()` 메서드 구현됨
- Webhook 서명 검증 로직 구현 완료
- Webhook 수신 로깅 처리 완료

**필요 작업**:
- Webhook 이벤트 타입별 처리 로직 구현 (현재는 로깅만 수행)
- 결제 상태 업데이트 로직 구현
- `PaymentEventPublisher`를 통한 이벤트 발행 연동
- 알림 처리 연동

### 4. 데이터베이스 마이그레이션

**필요 작업**: Flyway 또는 Liquibase 마이그레이션 스크립트 작성

**변경 사항**:
- `payments` 테이블: `user_id` 컬럼이 `@ManyToOne` 관계로 변경됨
- `points` 테이블: `user_id` 컬럼이 `@ManyToOne` 관계로 변경됨
- `cashbacks` 테이블: `user_id` 컬럼이 `@ManyToOne` 관계로 변경됨
- `user_tier_history` 테이블: `user_id` 컬럼이 `@ManyToOne` 관계로 변경됨
- `exchange_rates` 테이블: **신규 생성** (환율 정보 저장)

**마이그레이션 예시**:
```sql
-- 기존 외래키 제약조건 제거 (필요시)
ALTER TABLE payments DROP FOREIGN KEY IF EXISTS fk_payment_user;
ALTER TABLE points DROP FOREIGN KEY IF EXISTS fk_point_user;
ALTER TABLE cashbacks DROP FOREIGN KEY IF EXISTS fk_cashback_user;
ALTER TABLE user_tier_history DROP FOREIGN KEY IF EXISTS fk_tier_history_user;

-- 외래키 재생성 (JPA가 자동 생성하지만 명시적으로 생성 가능)
ALTER TABLE payments ADD CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE points ADD CONSTRAINT fk_point_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE cashbacks ADD CONSTRAINT fk_cashback_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_tier_history ADD CONSTRAINT fk_tier_history_user FOREIGN KEY (user_id) REFERENCES users(id);

-- 환율 테이블 생성
CREATE TABLE exchange_rates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(19, 6) NOT NULL,
    last_fetched_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY idx_exchange_rates_from_to (from_currency, to_currency),
    KEY idx_exchange_rates_updated_at (updated_at)
);
```

### 5. 통합 테스트

**필요 작업**:
- 결제 플로우 통합 테스트 작성
- Webhook 처리 통합 테스트 작성
- 포인트 사용/적립 통합 테스트 작성
- 이메일 알림 발송 통합 테스트 작성

**테스트 파일 위치**:
- `src/test/java/org/example/sharedprompts/domain/payment/`

### 6. 결제 실패 모니터링 대시보드

**필요 작업**:
- Prometheus 메트릭을 활용한 대시보드 구성
- Grafana 대시보드 설정
- 알림 규칙 설정 (예: 실패율 5% 초과 시 알림)

---

## 🗄️ 데이터베이스 마이그레이션

### 엔티티 관계 변경

모든 엔티티에서 `Long userId` → `@ManyToOne User user`로 변경되었습니다.

**영향받는 테이블**:
1. `payments`
2. `points`
3. `cashbacks`
4. `user_tier_history`
5. `exchange_rates` (신규)

**마이그레이션 전략**:
1. 기존 데이터 백업
2. 외래키 제약조건 확인 및 제거 (필요시)
3. JPA가 자동으로 외래키 생성하거나 수동으로 생성
4. 데이터 무결성 검증

---

## 🔧 환경 변수 설정

### 필수 환경 변수

`.env` 파일 또는 환경 변수에 다음 값들을 설정해야 합니다:

```bash
# 결제사 API 키
KAKAO_API_KEY=your_kakao_api_key
KAKAO_SECRET=your_kakao_secret
TOSS_API_KEY=your_toss_api_key
TOSS_SECRET=your_toss_secret
PAYPAL_CLIENT_ID=your_paypal_client_id
PAYPAL_CLIENT_SECRET=your_paypal_client_secret

# Webhook
PAYMENT_WEBHOOK_SECRET=your_webhook_secret

# 이메일 설정
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true

# 환율 API
# ExchangeRate-API 무료 플랜 사용 시 API 키 불필요
EXCHANGE_RATE_API_URL=https://api.exchangerate-api.com/v4/latest/
PAYMENT_EXCHANGE_RATE_SCHEDULE=0 0 * * * ?  # 매 시간 정각 (1시간 간격, 기본값)
# 유료 플랜 또는 다른 API 사용 시에만 필요
EXCHANGE_RATE_API_KEY=your_exchange_rate_api_key  # 선택사항

# 결제 설정
PAYMENT_RETRY_MAX_ATTEMPTS=3
PAYMENT_RETRY_DELAY_MS=1000
PAYMENT_CASHBACK_RATE=0.01
PAYMENT_POINT_RATE=0.005

# FCM (Firebase Cloud Messaging)
PAYMENT_FCM_ENABLED=true
PAYMENT_FCM_PROJECT_ID=your_firebase_project_id
PAYMENT_FCM_CREDENTIALS_PATH=/path/to/firebase-credentials.json
# 또는 환경 변수로 설정
GOOGLE_APPLICATION_CREDENTIALS=/path/to/firebase-credentials.json
```

---

## 🧪 테스트

### 단위 테스트

**구현 완료**:
- ✅ `PaymentServiceTest.java` (`src/test/java/org/example/sharedprompts/domain/payment/service/PaymentServiceTest.java`)
- ✅ `UserTierServiceTest.java` (`src/test/java/org/example/sharedprompts/domain/payment/service/UserTierServiceTest.java`)

**필요 작업**:
- PointService 테스트 작성
- CashbackService 테스트 작성
- PaymentProviderService 테스트 작성
- PaymentFacade 테스트 작성

### 통합 테스트

**구현 완료**:
- ✅ `PaymentControllerIntegrationTest.java` (`src/test/java/org/example/sharedprompts/controller/payment/PaymentControllerIntegrationTest.java`)

**필요 작업**:
- Webhook 처리 통합 테스트 (`PaymentWebhookController` 테스트)
- 포인트 사용/적립 통합 테스트
- 알림 발송 통합 테스트 (이메일, 푸시)
- 환율 스케줄러 통합 테스트

---

## 📝 체크리스트

### 연동 필수 사항
- [ ] 카카오페이 API 키 발급 및 설정
- [ ] 토스페이먼츠 API 키 발급 및 설정
- [ ] 페이팔 Client ID/Secret 발급 및 설정
- [ ] 이메일 SMTP 설정
- [ ] Webhook 시크릿 설정
- [ ] 데이터베이스 마이그레이션 실행

### 구현 필요 사항
- [ ] 푸시 알림 (FCM) 실제 설정 및 테스트 (구조는 완료, Firebase 프로젝트 설정 필요)
- [ ] 환율 API 실제 연동 테스트 및 검증 (구조 및 스케줄러는 완료)
- [ ] Webhook 이벤트 처리 로직 구현 (현재는 로깅만 수행)
- [ ] 통합 테스트 작성 (Webhook, 포인트, 알림, 환율 스케줄러)
- [ ] 결제 실패 모니터링 대시보드 구성

### ✅ 완료된 사항
- [x] 파사드 패턴 도입 (PaymentValidationFacade, PaymentAmountFacade, PaymentProviderFacade, PaymentPostProcessFacade)
- [x] RequestDto/ResponseDto mapper 메서드 추가
  - `PaymentRequestDto.toPaymentBuilder()`
  - `PointUseRequestDto.toPointBuilder()`
  - `PaymentCancelRequestDto.getPaymentIdAsLong()`, `getReasonOrDefault()`
  - `PaymentRefundRequestDto.getPaymentIdAsLong()`, `getAmountOrNull()`, `getReasonOrDefault()`
  - `TierChangeRequestDto.getReasonOrDefault()`
  - 모든 ResponseDto에 `from()` 메서드 구현
- [x] 통계 클래스 분리 (statistics 패키지)
- [x] Webhook 서명 검증 로직 구현 완료 (모든 결제사)
- [x] Webhook 엔드포인트 구현 완료 (`PaymentWebhookController`)
- [x] Import 정리 완료 (전체 패키지 경로 → import 문)
- [x] 환율 엔티티 기반 구조 구현 (ExchangeRate 엔티티, Repository, 스케줄러)
- [x] 환율 스케줄러 구현 완료 (ShedLock 분산 락 포함, 초기 로드 포함)
- [x] 환율 Webhook 엔드포인트 구현 완료 (`ExchangeRateWebhookController`)
- [x] 결제사별 서비스 구현 완료 (카카오페이, 토스, 페이팔)
- [x] 결제 컨트롤러 구현 완료 (`PaymentController`, `PaymentWebhookController`, `PointController`)
- [x] 결제 모니터링 서비스 구현 완료 (`PaymentMonitoringService`)
- [x] Prometheus 메트릭 수집 구현 완료 (`PaymentMetrics`)
- [x] FCM HTTP v1 API 연동 완료 (`PushNotificationService`, `FcmTokenService`)

---

## 🔍 참고 사항

### Webhook 엔드포인트

**결제사별 Webhook** (`PaymentWebhookController`):
- 카카오페이: `POST /webhooks/payments/kakao` (헤더: `X-Kakao-Signature`)
- 토스: `POST /webhooks/payments/toss` (헤더: `X-Toss-Signature`)
- 페이팔: `POST /webhooks/payments/paypal` (헤더: `PayPal-Signature`)

**환율 Webhook** (`ExchangeRateWebhookController`):
- 환율 업데이트: `POST /webhooks/exchange-rates`

**구현 내용**:
- 모든 Webhook 엔드포인트에서 서명 검증 수행 (결제사 Webhook)
- Webhook 수신 시 로깅 처리
- 이벤트 타입 추출 및 처리 (현재는 로깅만 수행)
- 환율 Webhook 수신 시 스케줄러 실행하여 환율 업데이트

### 메트릭 엔드포인트

Prometheus 메트릭: `GET /actuator/prometheus`

주요 메트릭:
- `payment.success`: 결제 성공 횟수
- `payment.failure`: 결제 실패 횟수
- `payment.processing.time`: 결제 처리 시간
- `payment.amount`: 결제 금액
- `payment.daily_limit_exceeded`: 일일 제한 초과 횟수

---

## 📚 추가 리소스

- [카카오페이 개발자센터](https://developers.kakao.com/docs/latest/ko/kakaopay/common)
- [토스페이먼츠 개발자센터](https://developers.tosspayments.com/)
- [Stripe API 문서](https://stripe.com/docs/api)
- [PayPal API 문서](https://developer.paypal.com/docs/api/overview/)
- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)
- [FCM HTTP v1 API](https://firebase.google.com/docs/cloud-messaging/migrate-v1)
- [Spring Mail 문서](https://spring.io/guides/gs/sending-email/)


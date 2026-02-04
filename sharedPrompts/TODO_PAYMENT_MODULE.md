# 결제 모듈 완성 체크리스트

## 🚀 최우선 작업 (프로덕션 배포 전 필수)

### 1. 환경 변수 설정 및 API 키 발급
```bash
# .env 파일 또는 환경 변수에 설정 필요
KAKAO_API_KEY=your_kakao_api_key
KAKAO_SECRET=your_kakao_secret
TOSS_API_KEY=your_toss_api_key
TOSS_SECRET=your_toss_secret
STRIPE_API_KEY=your_stripe_api_key
STRIPE_SECRET=your_stripe_secret
PAYPAL_CLIENT_ID=your_paypal_client_id
PAYPAL_CLIENT_SECRET=your_paypal_client_secret
PAYMENT_WEBHOOK_SECRET=your_webhook_secret

# 이메일 설정
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true

# 환율 API
EXCHANGE_RATE_API_KEY=your_exchange_rate_api_key
EXCHANGE_RATE_API_URL=https://api.exchangerate-api.com/v4/latest/
PAYMENT_EXCHANGE_RATE_SCHEDULE=0 0 * * * ?  # 매 시간 정각 (1시간 간격, 기본값)
```

**작업 내용**:
- [ ] 각 결제사 개발자 센터에서 API 키 발급
- [ ] 환경 변수 파일에 설정 추가
- [ ] 이메일 SMTP 서버 설정 (Gmail 사용 시 앱 비밀번호 필요)

---

### 2. 데이터베이스 마이그레이션 실행

**작업 내용**:
- [ ] Flyway 또는 Liquibase 마이그레이션 스크립트 작성
- [ ] `exchange_rates` 테이블 생성
- [ ] 기존 테이블 외래키 관계 확인 및 업데이트
- [ ] 마이그레이션 실행 및 검증

**마이그레이션 SQL 예시** (문서의 234-260줄 참고)

---

## 🔧 핵심 기능 구현 (중요도 높음)

### 3. Webhook 이벤트 처리 로직 구현

**파일**: `src/main/java/org/example/sharedprompts/domain/payment/service/provider/*PaymentService.java`

**현재 상태**: `processWebhook()` 메서드는 있지만 로깅만 수행

**구현 필요**:
- [ ] Webhook 이벤트 타입별 처리 로직 구현
  - 결제 완료 이벤트 → 결제 상태 `SUCCESS`로 업데이트
  - 결제 실패 이벤트 → 결제 상태 `FAILED`로 업데이트
  - 결제 취소 이벤트 → 결제 상태 `CANCELED`로 업데이트
- [ ] `PaymentEventPublisher`를 통한 이벤트 발행 연동
- [ ] 결제 상태 업데이트 로직 구현
- [ ] 알림 처리 연동 (이메일, 푸시)

**예상 작업 시간**: 4-6시간

---

### 4. 푸시 알림 (FCM) 실제 연동

**파일**: `src/main/java/org/example/sharedprompts/domain/payment/service/notification/PushNotificationService.java`

**현재 상태**: 시뮬레이션만 구현됨 (67-85줄 주석 처리됨)

**구현 필요**:
- [ ] Firebase 프로젝트 생성 및 설정
- [ ] `build.gradle`에 Firebase Admin SDK 의존성 추가
  ```gradle
  implementation 'com.google.firebase:firebase-admin:9.2.0'
  ```
- [ ] FCM 서버 키 환경 변수 추가
  ```bash
  FCM_SERVER_KEY=your_fcm_server_key
  ```
- [ ] `sendPushNotification()` 메서드 실제 구현
- [ ] Firebase Admin SDK 초기화 설정 클래스 생성

**예상 작업 시간**: 2-3시간

---

## 🧪 테스트 작성 (품질 보장)

### 5. 단위 테스트 작성

**필요 작업**:
- [ ] `PointServiceTest.java` 작성
- [ ] `CashbackServiceTest.java` 작성
- [ ] `PaymentProviderServiceTest.java` 작성 (각 결제사별)
- [ ] `PaymentFacadeTest.java` 작성 (4개 파사드 클래스)

**예상 작업 시간**: 6-8시간

### 6. 통합 테스트 작성

**필요 작업**:
- [ ] `PaymentWebhookControllerIntegrationTest.java` 작성
- [ ] 포인트 사용/적립 통합 테스트 작성
- [ ] 알림 발송 통합 테스트 작성 (이메일, 푸시)
- [ ] 환율 스케줄러 통합 테스트 작성

**예상 작업 시간**: 4-6시간

---

## 🔍 검증 및 최적화 (중요도 중간)

### 7. 환율 API 실제 연동 테스트

**작업 내용**:
- [ ] 실제 환율 API 연동 테스트 (ExchangeRate-API 등)
- [ ] API 응답 파싱 로직 검증
- [ ] 에러 처리 강화 (API 장애 시 대체 로직)
- [ ] 지원 통화 목록 확장 (필요시)

**예상 작업 시간**: 2-3시간

---

## 📊 모니터링 및 운영 (중요도 낮음, 운영 후)

### 8. 결제 실패 모니터링 대시보드 구성

**작업 내용**:
- [ ] Prometheus 메트릭을 활용한 대시보드 구성
- [ ] Grafana 대시보드 설정
- [ ] 알림 규칙 설정 (예: 실패율 5% 초과 시 알림)

**예상 작업 시간**: 3-4시간

---

## 📋 작업 우선순위 요약

### Phase 1: 필수 인프라 설정 (1-2일)
1. ✅ 환경 변수 설정 및 API 키 발급
2. ✅ 데이터베이스 마이그레이션 실행

### Phase 2: 핵심 기능 구현 (2-3일)
3. ✅ Webhook 이벤트 처리 로직 구현
4. ✅ 푸시 알림 (FCM) 실제 연동

### Phase 3: 테스트 및 검증 (2-3일)
5. ✅ 단위 테스트 작성
6. ✅ 통합 테스트 작성
7. ✅ 환율 API 실제 연동 테스트

### Phase 4: 모니터링 (운영 후)
8. ✅ 결제 실패 모니터링 대시보드 구성

---

## 💡 빠른 시작 가이드

### 가장 먼저 해야 할 것:
1. **환경 변수 설정** - API 키 발급 및 `.env` 파일 설정
2. **데이터베이스 마이그레이션** - 테이블 생성 및 관계 설정
3. **Webhook 이벤트 처리** - 실제 결제 상태 업데이트 로직 구현

### 가장 중요한 것:
- **Webhook 이벤트 처리 로직** - 결제 완료/실패 상태를 실제로 반영하는 핵심 로직
- **환경 변수 설정** - 모든 외부 서비스 연동의 기반

---

## 📝 참고사항

- 모든 결제사 API 키는 **테스트/개발 환경**에서 먼저 발급받아 테스트
- Webhook 엔드포인트는 각 결제사 개발자 센터에서 등록 필요
- 이메일 SMTP는 Gmail 사용 시 **앱 비밀번호** 필요 (일반 비밀번호 불가)
- 환율 API는 무료 플랜도 있으니 먼저 테스트 후 유료 플랜 고려


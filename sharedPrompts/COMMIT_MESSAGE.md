# Git Commit Message

```
fix: 결제 시스템 안정성 및 보안 개선

- 동시성 문제 해결
  * CashbackServiceImpl: ShedLock을 사용한 캐시백 지급 동시성 보호
  * AdminAccountInitializer: 멀티 인스턴스 기동 시 중복 생성 레이스 컨디션 처리

- 결제 프로세스 개선
  * PaymentServiceImpl: 승인 성공과 후처리 실패 분리 (후처리 실패 시에도 결제 상태는 SUCCESS 유지)
  * PaymentServiceImpl: 외부 결제사 상태 동기화 로직 추가 (SUCCESS, FAILED, CANCELED, REFUNDED, PARTIALLY_REFUNDED 모두 처리)
  * CashbackServiceImpl: 중복 캐시백 적립 방지 로직 추가

- 리소스 관리 및 보안 강화
  * DefaultFcmCredentialsProvider: InputStream 리소스 누수 수정 (try-with-resources 적용)
  * FcmTokenService: Double-checked locking을 위한 volatile 키워드 추가
  * FcmTokenService: GoogleCredentials refresh() 호출 추가 (토큰 초기화 보장)
  * PushNotificationService: 모든 로그에서 deviceToken 마스킹 적용

- 입력 검증 강화
  * PointServiceImpl: 포인트 사용 금액이 0 이하인 경우 검증 추가
  * PaymentRefundRequestDto, PaymentCancelRequestDto: NumberFormatException 처리 추가

- 보안 개선
  * AdminAccountInitializer: ADMIN_PASSWORD 없이 Role 자동 승격 방지

- 레거시 코드 정리
  * RateLimitConstants: 사용되지 않는 deprecated 상수 삭제 (Capacities, Windows)
```

---

## 짧은 버전 (한 줄)

```
fix: 결제 시스템 안정성 및 보안 개선
```

---

## 상세 버전 (여러 커밋으로 나눌 경우)

### 1. 동시성 문제 해결
```
fix: 결제 시스템 동시성 문제 해결

- CashbackServiceImpl: ShedLock을 사용한 캐시백 지급 동시성 보호
- AdminAccountInitializer: 멀티 인스턴스 기동 시 중복 생성 레이스 컨디션 처리
```

### 2. 결제 프로세스 개선
```
fix: 결제 프로세스 안정성 개선

- PaymentServiceImpl: 승인 성공과 후처리 실패 분리
- PaymentServiceImpl: 외부 결제사 상태 동기화 로직 추가
- CashbackServiceImpl: 중복 캐시백 적립 방지
```

### 3. 리소스 관리 및 보안 강화
```
fix: FCM 서비스 리소스 관리 및 보안 개선

- DefaultFcmCredentialsProvider: InputStream 리소스 누수 수정
- FcmTokenService: volatile 키워드 추가 및 refresh() 호출 추가
- PushNotificationService: deviceToken 마스킹 적용
```

### 4. 입력 검증 강화
```
fix: 입력 검증 강화

- PointServiceImpl: 포인트 사용 금액 검증 추가
- PaymentRefundRequestDto, PaymentCancelRequestDto: NumberFormatException 처리
```

### 5. 보안 및 레거시 코드 정리
```
fix: 보안 강화 및 레거시 코드 정리

- AdminAccountInitializer: Role 자동 승격 방지
- RateLimitConstants: deprecated 상수 삭제
```

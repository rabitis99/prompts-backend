# Git Commit Message

```
feat: 관리자용 결제/정산 API 추가 및 USER/ADMIN API 분리

- PaymentController 리팩토링
  * 티어 관련 API를 /me 패턴으로 변경 (userId path variable 제거)
  * GET /payments/users/{userId}/tier → GET /payments/me/tier
  * GET /payments/users/{userId}/tier-info → GET /payments/me/tier-info
  * GET /payments/users/{userId}/tier-history → GET /payments/me/tier-history

- 관리자용 컨트롤러 추가
  * AdminPaymentController: 결제 상태 조회, 내역 조회, 취소, 환불, 티어 관리
  * AdminPointController: 포인트 잔액/내역 조회, 포인트 차감, 결제별 포인트 조회
  * AdminCashbackController: 캐시백 내역 조회, 미지급 조회, 캐시백 지급

- 서비스 레이어 확장
  * PaymentService: 관리자용 메서드 추가 (소유권 검증 없음)
  * PointService: 관리자용 포인트 조회 메서드 추가
  * CashbackService: 관리자용 캐시백 조회/지급 메서드 추가

- Repository 레이어 확장
  * CustomPaymentRepository: 전체 결제 내역 조회 메서드 추가
  * CustomPointRepository: 결제별 포인트 조회 (소유권 검증 없음) 추가
  * CustomCashbackRepository: 전체 미지급 캐시백 조회 메서드 추가

- 아키텍처 개선
  * 컨트롤러에서 Repository 의존성 제거
  * 사용자 존재 여부 검증을 서비스 레이어로 이동
  * SPR 준수 및 레이어 분리 강화

BREAKING CHANGE: 
- GET /payments/users/{userId}/tier → GET /payments/me/tier
- GET /payments/users/{userId}/tier-info → GET /payments/me/tier-info  
- GET /payments/users/{userId}/tier-history → GET /payments/me/tier-history
```

---

## 짧은 버전 (한 줄)

```
feat: 관리자용 결제/정산 API 추가 및 USER/ADMIN API 분리
```

---

## 상세 버전 (여러 커밋으로 나눌 경우)

### 1. PaymentController 리팩토링
```
refactor: PaymentController 티어 API를 /me 패턴으로 변경

- userId path variable 제거
- @CurrentUser만 사용하도록 변경
- 소유권 검증 로직 제거 (서비스 레이어에서 처리)

BREAKING CHANGE:
- GET /payments/users/{userId}/tier → GET /payments/me/tier
- GET /payments/users/{userId}/tier-info → GET /payments/me/tier-info
- GET /payments/users/{userId}/tier-history → GET /payments/me/tier-history
```

### 2. 관리자용 컨트롤러 추가
```
feat: 관리자용 결제/정산 컨트롤러 추가

- AdminPaymentController: 결제 관리 API
- AdminPointController: 포인트 관리 API
- AdminCashbackController: 캐시백 관리 API
- 모든 관리자 API는 /admin/* 경로 사용
```

### 3. 서비스 및 Repository 레이어 확장
```
feat: 관리자용 서비스 및 Repository 메서드 추가

- PaymentService: 관리자용 메서드 (소유권 검증 없음)
- PointService: 관리자용 포인트 조회
- CashbackService: 관리자용 캐시백 조회/지급
- Repository: 관리자용 조회 메서드 추가
```

### 4. 아키텍처 개선
```
refactor: 컨트롤러에서 Repository 의존성 제거

- 모든 컨트롤러에서 Repository 제거
- 사용자 존재 여부 검증을 서비스 레이어로 이동
- SPR 준수 및 레이어 분리 강화
```


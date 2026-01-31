# 결제/정산 도메인 컨트롤러 API 분석 결과

## 1. PointController

### 엔드포인트 분석

#### GET /points/balance
- **현재 성격**: USER 전용
- **관리자 필요 여부**: O
- **관리자용 권장 엔드포인트**: `GET /admin/points/users/{userId}/balance`
- **리팩토링 시 주의사항**:
  - USER API는 그대로 유지 (`/points/balance`)
  - ADMIN API는 `/admin/points/users/{userId}/balance`로 분리
  - ADMIN API에서는 userId 존재 여부 검증 필수
  - 소유권 검증 없음 (관리자는 모든 사용자 조회 가능)

#### GET /points/history
- **현재 성격**: USER 전용
- **관리자 필요 여부**: O
- **관리자용 권장 엔드포인트**: `GET /admin/points/users/{userId}/history`
- **리팩토링 시 주의사항**:
  - USER API는 그대로 유지 (`/points/history`)
  - ADMIN API는 `/admin/points/users/{userId}/history`로 분리
  - userId 존재 여부 검증 필수

#### POST /points/use
- **현재 성격**: USER 전용
- **관리자 필요 여부**: O (관리자가 사용자 포인트를 직접 차감/사용하는 경우)
- **관리자용 권장 엔드포인트**: `POST /admin/points/users/{userId}/use`
- **리팩토링 시 주의사항**:
  - USER API는 그대로 유지
  - ADMIN API는 관리자 권한으로 사용자 포인트 차감 가능
  - 차감 사유(description)에 관리자 정보 포함 권장

#### GET /points/payment/{paymentId}
- **현재 성격**: USER 전용
- **관리자 필요 여부**: O
- **관리자용 권장 엔드포인트**: `GET /admin/points/payments/{paymentId}`
- **리팩토링 시 주의사항**:
  - USER API는 그대로 유지 (본인 결제의 포인트만 조회)
  - ADMIN API는 결제 ID만으로 조회 가능 (소유권 검증 없음)
  - paymentId 존재 여부 검증 필수

---

## 2. CashbackController

### 엔드포인트 분석

#### GET /cashbacks/history
- **현재 성격**: USER 전용
- **관리자 필요 여부**: O
- **관리자용 권장 엔드포인트**: `GET /admin/cashbacks/users/{userId}/history`
- **리팩토링 시 주의사항**:
  - USER API는 그대로 유지
  - ADMIN API는 userId로 특정 사용자의 캐시백 내역 조회
  - userId 존재 여부 검증 필수

#### GET /cashbacks/unpaid-total
- **현재 성격**: USER 전용
- **관리자 필요 여부**: O
- **관리자용 권장 엔드포인트**: `GET /admin/cashbacks/users/{userId}/unpaid-total`
- **리팩토링 시 주의사항**:
  - USER API는 그대로 유지
  - ADMIN API는 특정 사용자의 미지급 캐시백 총액 조회
  - 추가로 전체 미지급 캐시백 총액 조회 API도 고려: `GET /admin/cashbacks/unpaid-total`

#### GET /cashbacks/unpaid
- **현재 성격**: USER 전용
- **관리자 필요 여부**: O
- **관리자용 권장 엔드포인트**: 
  - `GET /admin/cashbacks/users/{userId}/unpaid` (특정 사용자)
  - `GET /admin/cashbacks/unpaid` (전체 미지급 목록)
- **리팩토링 시 주의사항**:
  - USER API는 그대로 유지
  - ADMIN API는 특정 사용자 또는 전체 미지급 캐시백 조회 가능
  - 필터링 옵션 추가 고려 (상태, 기간 등)

#### POST /cashbacks/{cashbackId}/pay
- **현재 성격**: USER 전용
- **관리자 필요 여부**: O
- **관리자용 권장 엔드포인트**: `POST /admin/cashbacks/{cashbackId}/pay`
- **리팩토링 시 주의사항**:
  - USER API는 그대로 유지 (본인 캐시백만 지급 가능)
  - ADMIN API는 모든 캐시백 지급 가능
  - cashbackId 존재 여부 및 소유권 검증 없음
  - 일괄 지급 API도 고려: `POST /admin/cashbacks/batch-pay`

---

## 3. ExchangeRateWebhookController

### 엔드포인트 분석

#### POST /webhooks/exchange-rates
- **현재 성격**: ADMIN 전용 (이미 `@AdminOnly` 적용됨)
- **관리자 필요 여부**: N/A (이미 ADMIN 전용)
- **관리자용 권장 엔드포인트**: 현재 엔드포인트 유지
- **리팩토링 시 주의사항**:
  - Webhook은 일반 사용자 접근 대상이 아님
  - 현재 구조 적절함
  - 추가로 환율 수동 업데이트 API 고려: `POST /admin/exchange-rates/update`
  - 환율 조회 API 고려: `GET /admin/exchange-rates` (현재 환율 정보 조회)

---

## 4. PaymentController

### 엔드포인트 분석

#### POST /payments
- **현재 성격**: USER 전용
- **관리자 필요 여부**: X (일반적으로 관리자가 결제를 대신 요청하지 않음)
- **관리자용 권장 엔드포인트**: 없음
- **리팩토링 시 주의사항**:
  - USER 전용으로 유지

#### GET /payments/{paymentId}/status
- **현재 성격**: USER 전용
- **관리자 필요 여부**: O
- **관리자용 권장 엔드포인트**: `GET /admin/payments/{paymentId}/status`
- **리팩토링 시 주의사항**:
  - USER API는 그대로 유지 (본인 결제만 조회)
  - ADMIN API는 모든 결제 상태 조회 가능
  - paymentId 존재 여부 검증 필수

#### POST /payments/cancel
- **현재 성격**: USER 전용
- **관리자 필요 여부**: O
- **관리자용 권장 엔드포인트**: `POST /admin/payments/{paymentId}/cancel`
- **리팩토링 시 주의사항**:
  - USER API는 그대로 유지 (본인 결제만 취소)
  - ADMIN API는 모든 결제 취소 가능
  - 취소 사유에 관리자 정보 포함 권장

#### POST /payments/refund
- **현재 성격**: USER 전용
- **관리자 필요 여부**: O
- **관리자용 권장 엔드포인트**: `POST /admin/payments/{paymentId}/refund`
- **리팩토링 시 주의사항**:
  - USER API는 그대로 유지 (본인 결제만 환불)
  - ADMIN API는 모든 결제 환불 가능
  - 환불 사유에 관리자 정보 포함 권장

#### GET /payments/users/{userId}/tier
- **현재 성격**: 혼합 (현재는 누구나 조회 가능하지만, 실제로는 USER/ADMIN 분리 필요)
- **관리자 필요 여부**: O
- **관리자용 권장 엔드포인트**: `GET /admin/payments/users/{userId}/tier`
- **리팩토링 시 주의사항**:
  - **중요**: 현재 userId를 path variable로 받지만 소유권 검증이 없음
  - USER API는 `/payments/me/tier` 또는 `/payments/my/tier`로 변경 (userId 제거)
  - ADMIN API는 `/admin/payments/users/{userId}/tier`로 분리
  - USER API에서는 `@CurrentUser`만 사용

#### GET /payments/users/{userId}/tier-info
- **현재 성격**: USER 전용 (소유권 검증 있음)
- **관리자 필요 여부**: O
- **관리자용 권장 엔드포인트**: `GET /admin/payments/users/{userId}/tier-info`
- **리팩토링 시 주의사항**:
  - **중요**: 현재 userId를 path variable로 받지만 소유권 검증으로 본인만 가능
  - USER API는 `/payments/me/tier-info` 또는 `/payments/my/tier-info`로 변경 (userId 제거)
  - ADMIN API는 `/admin/payments/users/{userId}/tier-info`로 분리
  - 소유권 검증 제거 (ADMIN은 모든 사용자 조회 가능)

#### GET /payments/history
- **현재 성격**: USER 전용
- **관리자 필요 여부**: O
- **관리자용 권장 엔드포인트**: 
  - `GET /admin/payments/users/{userId}/history` (특정 사용자)
  - `GET /admin/payments/history` (전체 결제 내역)
- **리팩토링 시 주의사항**:
  - USER API는 그대로 유지
  - ADMIN API는 특정 사용자 또는 전체 결제 내역 조회 가능
  - 필터링 옵션 추가 고려 (상태, 결제 수단, 기간 등)

#### GET /payments/users/{userId}/tier-history
- **현재 성격**: USER 전용 (소유권 검증 있음)
- **관리자 필요 여부**: O
- **관리자용 권장 엔드포인트**: `GET /admin/payments/users/{userId}/tier-history`
- **리팩토링 시 주의사항**:
  - **중요**: 현재 userId를 path variable로 받지만 소유권 검증으로 본인만 가능
  - USER API는 `/payments/me/tier-history` 또는 `/payments/my/tier-history`로 변경 (userId 제거)
  - ADMIN API는 `/admin/payments/users/{userId}/tier-history`로 분리
  - 소유권 검증 제거

#### POST /payments/confirm
- **현재 성격**: USER 전용
- **관리자 필요 여부**: X (일반적으로 사용자가 직접 승인)
- **관리자용 권장 엔드포인트**: 없음
- **리팩토링 시 주의사항**:
  - USER 전용으로 유지

---

## 5. PaymentWebhookController

### 엔드포인트 분석

#### POST /webhooks/payments/kakao
#### POST /webhooks/payments/toss
#### POST /webhooks/payments/paypal
- **현재 성격**: ADMIN 전용 (Webhook은 일반 사용자 접근 대상이 아님)
- **관리자 필요 여부**: N/A (이미 외부 시스템 전용)
- **관리자용 권장 엔드포인트**: 현재 엔드포인트 유지
- **리팩토링 시 주의사항**:
  - Webhook은 외부 결제사에서 호출하는 엔드포인트
  - 일반 사용자 접근 대상이 아님
  - 현재 구조 적절함
  - 추가로 웹훅 로그 조회 API 고려: `GET /admin/webhooks/payments/logs`
  - 웹훅 재처리 API 고려: `POST /admin/webhooks/payments/{webhookId}/retry`

---

## 요약: 관리자용 추가 API 목록

### 필수 관리자 API

1. **포인트 관리**
   - `GET /admin/points/users/{userId}/balance` - 사용자 포인트 잔액 조회
   - `GET /admin/points/users/{userId}/history` - 사용자 포인트 내역 조회
   - `POST /admin/points/users/{userId}/use` - 사용자 포인트 차감
   - `GET /admin/points/payments/{paymentId}` - 결제별 포인트 조회

2. **캐시백 관리**
   - `GET /admin/cashbacks/users/{userId}/history` - 사용자 캐시백 내역 조회
   - `GET /admin/cashbacks/users/{userId}/unpaid-total` - 사용자 미지급 총액
   - `GET /admin/cashbacks/users/{userId}/unpaid` - 사용자 미지급 목록
   - `GET /admin/cashbacks/unpaid-total` - 전체 미지급 총액
   - `GET /admin/cashbacks/unpaid` - 전체 미지급 목록
   - `POST /admin/cashbacks/{cashbackId}/pay` - 캐시백 지급
   - `POST /admin/cashbacks/batch-pay` - 일괄 지급

3. **결제 관리**
   - `GET /admin/payments/{paymentId}/status` - 결제 상태 조회
   - `GET /admin/payments/users/{userId}/history` - 사용자 결제 내역
   - `GET /admin/payments/history` - 전체 결제 내역
   - `POST /admin/payments/{paymentId}/cancel` - 결제 취소
   - `POST /admin/payments/{paymentId}/refund` - 결제 환불
   - `GET /admin/payments/users/{userId}/tier` - 사용자 티어 조회
   - `GET /admin/payments/users/{userId}/tier-info` - 사용자 티어 정보
   - `GET /admin/payments/users/{userId}/tier-history` - 사용자 티어 이력

### 권장 관리자 API (추가 기능)

1. **환율 관리**
   - `GET /admin/exchange-rates` - 현재 환율 조회
   - `POST /admin/exchange-rates/update` - 환율 수동 업데이트

2. **웹훅 관리**
   - `GET /admin/webhooks/payments/logs` - 웹훅 로그 조회
   - `POST /admin/webhooks/payments/{webhookId}/retry` - 웹훅 재처리

3. **통계/대시보드**
   - `GET /admin/payments/statistics` - 결제 통계
   - `GET /admin/points/statistics` - 포인트 통계
   - `GET /admin/cashbacks/statistics` - 캐시백 통계

---

## 리팩토링 우선순위

### 높음 (보안/일관성 문제)
1. `GET /payments/users/{userId}/tier` - 소유권 검증 없음, USER/ADMIN 분리 필요
2. `GET /payments/users/{userId}/tier-info` - userId 제거, `/me` 또는 `/my` 사용
3. `GET /payments/users/{userId}/tier-history` - userId 제거, `/me` 또는 `/my` 사용

### 중간 (기능 확장)
1. 포인트 관리자 API 추가
2. 캐시백 관리자 API 추가
3. 결제 관리자 API 추가

### 낮음 (편의 기능)
1. 통계 API 추가
2. 일괄 처리 API 추가

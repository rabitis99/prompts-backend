# OAuth 인증 플로우 변경 사항

## 변경 내용

### 이전 방식 (Deprecated)
- **엔드포인트**: `/auth/callback`
- **동작**: OAuth 인증 완료 후 콜백에서 **실제 토큰 값(access_token, refresh_token)을 직접 반환**
- **문제점**: 
  - 보안 취약점: URL에 토큰이 노출될 수 있음
  - CSRF 공격에 취약
  - 토큰이 브라우저 히스토리에 남을 수 있음

### 현재 방식 (Current)
- **엔드포인트**: `/auth/confirm`
- **동작**: 
  1. OAuth 인증 완료 후 임시 키(`tempKey`)와 상태(`state`)를 받음
  2. 클라이언트는 `/auth/confirm` 엔드포인트로 `tempKey`와 `state`를 전송
  3. 서버에서 검증 후 실제 토큰을 반환
- **장점**:
  - 보안 강화: 토큰이 URL에 노출되지 않음
  - CSRF 방지: state 파라미터로 검증
  - 안전한 토큰 전달

## API 사용법

### 1. OAuth 로그인 시작
클라이언트는 OAuth 제공자(Google, Naver, Kakao)의 인증 페이지로 리다이렉트합니다.

### 2. OAuth 콜백 처리
OAuth 제공자가 `/login/oauth2/code/{provider}`로 리다이렉트합니다.
- 이 엔드포인트는 **더 이상 토큰을 반환하지 않습니다**
- 대신 임시 키(`tempKey`)와 상태(`state`)를 포함한 정보를 반환합니다

### 3. 토큰 발급 요청
클라이언트는 `/auth/confirm` 엔드포인트로 POST 요청을 보냅니다:

```http
POST /api/auth/confirm
Content-Type: application/json

{
  "temp_key": "임시_키_값",
  "state": "상태_값",
  "device_token": "푸시_알림_디바이스_토큰" // 선택사항
}
```

### 4. 토큰 응답
서버는 검증 후 토큰을 반환합니다:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "success": true,
  "data": {
    "access_token": "액세스_토큰",
    "refresh_token": "리프레시_토큰"
  }
}
```

## 주의사항

1. **`/auth/callback` 엔드포인트는 더 이상 사용되지 않습니다**
2. **`tempKey`는 일회용이며, 사용 후 즉시 만료됩니다**
3. **`state` 파라미터는 CSRF 공격 방지를 위해 반드시 검증해야 합니다**
4. **토큰은 HTTPS를 통해서만 전송되어야 합니다**

## 마이그레이션 가이드

기존 `/auth/callback`을 사용하던 클라이언트는 다음 단계로 마이그레이션해야 합니다:

1. OAuth 콜백에서 `tempKey`와 `state` 추출
2. `/auth/confirm` 엔드포인트로 POST 요청
3. 응답에서 토큰 추출 및 저장

## 관련 파일

- `AuthController.java`: `/auth/confirm` 엔드포인트 구현
- `AuthServiceImpl.java`: `confirm()` 메서드에서 토큰 발급 처리
- `ConfirmRequestDto.java`: 요청 DTO
- `TokenResponseDto.java`: 응답 DTO


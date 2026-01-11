# 보안 개선 방안 (4/5 → 5/5)

## 현재 상태

- 기본적인 보안 구현 (JWT, OAuth2, PasswordEncoder)
- 로그아웃 보안 취약점 해결됨
- CORS 설정 보안 개선됨

## 5점 달성 방안

### 1. Rate Limiting 구현 🟠 **중요**

**목적**: DDoS 공격 및 API 남용 방지

**적용 방법**:
- Spring Boot Starter for Resilience4j 또는 Bucket4j 도입
- API 엔드포인트별 Rate Limit 설정
  - 로그인: 5회/분
  - 회원가입: 3회/분
  - 프롬프트 생성: 10회/분
- Redis 기반 분산 Rate Limiting 구현

### 2. 입력값 Sanitization 🟠 **중요**

**목적**: XSS 공격 방지

**적용 방법**:
- HTML 태그 필터링 (프롬프트, 댓글 내용)
- OWASP Java HTML Sanitizer 라이브러리 활용
- 사용자 입력값 검증 및 이스케이프

### 3. 보안 헤더 추가 🟡

**Security Filter Chain에서 보안 헤더 설정**:
```java
http.headers()
    .frameOptions().deny()
    .contentTypeOptions().and()
    .httpStrictTransportSecurity(hstsConfig -> hstsConfig
        .maxAgeInSeconds(31536000)
        .includeSubdomains(true))
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"));
```

- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `Strict-Transport-Security` (HTTPS 환경)
- `Content-Security-Policy` 설정

### 4. 인증/인가 로깅 🟡

- 실패한 로그인 시도 로깅
- 권한 부족 접근 시도 로깅
- 비정상적인 패턴 탐지 (예: 짧은 시간 내 다수 실패)

### 5. 토큰 보안 강화 🟢

- Refresh Token Rotation 구현
- 토큰 저장소 보안 강화 (HttpOnly Cookie 고려)
- 토큰 탈취 감지 메커니즘

**우선순위**: Rate Limiting과 입력값 Sanitization이 가장 중요 (공격 방어)

[← 목차로 돌아가기](../CODE_IMPROVEMENT_GUIDE.md)



# 코드 품질 개선 및 AI 호출 안정성 설계 가이드

## 📋 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [현재 상태 종합 평가](#2-현재-상태-종합-평가)
3. [5점 달성을 위한 개선 방안](#3-5점-달성을-위한-개선-방안)
4. [AI 호출 안정성 설계](#4-ai-호출-안정성-설계)
5. [개선 로드맵](#5-개선-로드맵)

---

## 1. 프로젝트 개요

**프로젝트명**: SharedPrompts  
**기술 스택**: Spring Boot 3.5.8, Java 17, JPA, QueryDSL, Redis, MySQL, JWT, OAuth2, WebFlux, WebClient  
**주요 기능**: 프롬프트 공유 플랫폼 (AI 기반 프롬프트 생성, 댓글, 좋아요, 태그)  
**AI 연동**: Google Gemini API (WebFlux + WebClient 기반)

---

## 2. 현재 상태 종합 평가

### 2.1 종합 점수

| 항목 | 점수 | 비고 |
|------|------|------|
| **아키텍처** | ⭐⭐⭐⭐ (4/5) | 계층 분리 명확, BaseCountService 리팩토링 우수 |
| **보안** | ⭐⭐⭐⭐ (4/5) | 기본적인 보안 구현, 로그아웃 보안 취약점 해결됨 |
| **성능** | ⭐⭐⭐⭐ (4/5) | 리액티브 프로그래밍 적용, 최적화 잘 구현됨 |
| **코드 품질** | ⭐⭐⭐⭐ (4/5) | 전반적으로 양호, 중복 코드 리팩토링 완료 |
| **유지보수성** | ⭐⭐⭐ (3/5) | 구조는 좋으나 테스트 부족 |
| **예외 처리** | ⭐⭐⭐⭐ (4/5) | 전역 핸들러 잘 구현, 구체적인 에러 코드 사용 |
| **AI 호출 안정성** | ⭐⭐⭐ (3/5) | 기본 timeout/retry만 존재, 서킷브레이커/fallback 부재 |

**종합 점수: 3.7/5.0** ⭐⭐⭐⭐

### 2.2 주요 강점

- ✅ 계층형 아키텍처: Controller → Service → Repository 명확한 분리
- ✅ QueryDSL 활용: 동적 쿼리 처리
- ✅ 이벤트 기반 아키텍처: Spring Events로 댓글/좋아요 카운트 비동기 처리
- ✅ Redis 최적화: Lua Script 활용한 원자적 연산
- ✅ BaseCountService 리팩토링: 중복 코드 제거
- ✅ N+1 쿼리 최적화: LEFT JOIN FETCH 적절히 사용
- ✅ 리액티브 프로그래밍: WebFlux + WebClient로 AI 호출

### 2.3 개선 필요 사항

- ⚠️ **AI 호출 안정성**: 서킷브레이커, fallback 전략 부재
- ⚠️ **테스트 코드**: 단위/통합 테스트 거의 없음
- ⚠️ **서비스 인터페이스**: 일부는 인터페이스 분리, 일부는 직접 구현 (일관성 부족)
- ⚠️ **Controller Mono 노출**: `PromptController.createPrompt`에서 Mono 반환 (일관성 저하)

---

## 3. 5점 달성을 위한 개선 방안

### 3.1 아키텍처 (4/5 → 5/5)

**현재 상태**:
- 계층 분리 명확
- BaseCountService 리팩토링 우수
- 서비스 인터페이스 일관성 부족 (일부는 인터페이스 분리, 일부는 직접 구현)
- Controller에서 Mono 노출 (일관성 저하)

**5점 달성 방안**:

#### 1. 서비스 인터페이스 일관성 확보 🟠 **중요**

**목표**: 모든 Service 클래스에 인터페이스 분리

**이유**:
- 테스트 용이성 향상 (Mock 객체 생성)
- 의존성 주입 명확화
- 코드 가독성 및 유지보수성 향상

**적용 대상**:
- 인터페이스가 없는 Service 구현체 확인
- 모든 Service에 인터페이스 추가

#### 2. Controller Mono 노출 제거 🟠 **중요** (AI 안정성과 연계)

**현재 문제점**:
```java
// PromptController.java
@PostMapping
public Mono<ResponseEntity<CustomResponse<PromptResponseDto>>> createPrompt(...) {
    return promptService.createPrompt(request, authUser.getId())
        .map(result -> CustomResponseHelper.created(result));
}
```

**문제점**:
- MVC 스타일 API와 혼용 시 일관성 저하
- 공통 응답 래핑/필터/인터셉터 적용 난이도 증가
- 팀 내 개발자 숙련도에 따라 유지보수 비용 증가

**개선 방안**: Facade 계층 도입 (AI 안정성 설계 섹션 참조)

#### 3. 도메인 이벤트 패턴 고도화 🟡

- 현재 Spring Events 사용 중이지만, 도메인 이벤트와 인프라 이벤트 명확히 구분
- 도메인 레이어에 이벤트 정의, 인프라 레이어에서 처리
- 예: `CommentCreatedEvent`, `LikeCreatedEvent`를 도메인 이벤트로 정의

#### 4. DDD 패턴 적용 심화 🟡

- Aggregate Root 명시적 정의
- Value Object 도입 (예: `Email`, `Tag` 등)
- Domain Service 패턴 적용 (복잡한 도메인 로직 분리)

**우선순위**: 서비스 인터페이스 일관성 확보 + Controller Mono 노출 제거가 가장 중요

---

### 3.2 보안 (4/5 → 5/5)

**현재 상태**:
- 기본적인 보안 구현 (JWT, OAuth2, PasswordEncoder)
- 로그아웃 보안 취약점 해결됨
- CORS 설정 보안 개선됨

**5점 달성 방안**:

#### 1. Rate Limiting 구현 🟠 **중요**

**목적**: DDoS 공격 및 API 남용 방지

**적용 방법**:
- Spring Boot Starter for Resilience4j 또는 Bucket4j 도입
- API 엔드포인트별 Rate Limit 설정
  - 로그인: 5회/분
  - 회원가입: 3회/분
  - 프롬프트 생성: 10회/분
- Redis 기반 분산 Rate Limiting 구현

#### 2. 입력값 Sanitization 🟠 **중요**

**목적**: XSS 공격 방지

**적용 방법**:
- HTML 태그 필터링 (프롬프트, 댓글 내용)
- OWASP Java HTML Sanitizer 라이브러리 활용
- 사용자 입력값 검증 및 이스케이프

#### 3. 보안 헤더 추가 🟡

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

#### 4. 인증/인가 로깅 🟡

- 실패한 로그인 시도 로깅
- 권한 부족 접근 시도 로깅
- 비정상적인 패턴 탐지 (예: 짧은 시간 내 다수 실패)

#### 5. 토큰 보안 강화 🟢

- Refresh Token Rotation 구현
- 토큰 저장소 보안 강화 (HttpOnly Cookie 고려)
- 토큰 탈취 감지 메커니즘

**우선순위**: Rate Limiting과 입력값 Sanitization이 가장 중요 (공격 방어)

---

### 3.3 성능 (4/5 → 5/5)

**현재 상태**:
- 리액티브 프로그래밍 적용
- Redis 캐싱 활용 (카운터)
- N+1 쿼리 최적화
- 블로킹 호출 제거

**5점 달성 방안**:

#### 1. 캐싱 전략 확대 🟠 **중요**

**현재**: Redis는 카운터에만 사용

**개선**:
- 응답 캐싱 추가 (자주 조회되는 프롬프트 목록)
- `@Cacheable` 어노테이션 활용
- TTL 적절히 설정
  - 인기 프롬프트: 5분
  - 일반 프롬프트: 1분
  - 사용자 정보: 10분

#### 2. Connection Pool 최적화 🟡

**HikariCP 설정 튜닝**:
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=3000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

- DB 커넥션 모니터링 도구 도입
- 실제 트래픽에 맞춰 최적값 조정

#### 3. DB 인덱스 최적화 🟡

- 쿼리 성능 분석 (EXPLAIN 실행 계획)
- 복합 인덱스 추가 검토 (자주 함께 사용되는 컬럼)
- 불필요한 인덱스 제거

#### 4. 비동기 처리 범위 확대 🟡

- 이메일 발송, 알림 전송 등 외부 API 호출 비동기화
- `@Async` 또는 `CompletableFuture` 활용
- 스레드 풀 크기 적절히 설정

#### 5. 쿼리 결과 최적화 🟢

- DTO Projection 활용 (엔티티 전체 조회 대신 필요한 필드만)
- `@EntityGraph` 활용 범위 확대

**우선순위**: 캐싱 전략 확대와 Connection Pool 최적화가 가장 중요 (즉각적인 성능 향상)

---

### 3.4 코드 품질 (4/5 → 5/5)

**현재 상태**:
- 전반적으로 양호한 코드 구조
- 중복 코드 리팩토링 완료
- Validation 메시지 통일 완료

**5점 달성 방안**:

#### 1. JavaDoc 추가 🟡

- 모든 public 클래스, 메서드에 JavaDoc 작성
- 파라미터, 반환값, 예외 설명
- `@param`, `@return`, `@throws` 태그 활용

#### 2. 코드 복잡도 감소 🟡

- 순환 복잡도(Cyclomatic Complexity) 높은 메서드 리팩토링
- 메서드 분리, Early Return 패턴 활용
- 복잡한 조건문을 전략 패턴 또는 명령 패턴으로 전환

#### 3. 매직 넘버/문자열 상수화 🟢

- 하드코딩된 숫자, 문자열을 상수로 추출
- 예: `@Size(max = 200)` → `MAX_TITLE_LENGTH = 200`
- Configuration Properties 활용

#### 4. 일관된 코딩 컨벤션 🟢

- Google Java Style Guide 또는 회사 코딩 컨벤션 적용
- Checkstyle, SpotBugs 도구 도입
- CI/CD 파이프라인에서 코드 품질 검사

#### 5. 불변성(Immutability) 강화 🟢

- DTO 클래스를 불변 객체로 설계 (`final` 필드, Builder 패턴)
- 값 변경이 필요없는 Entity 필드는 `final` 선언

**우선순위**: JavaDoc 추가와 코드 복잡도 감소가 중요 (가독성 및 유지보수성 향상)

---

### 3.5 유지보수성 (3/5 → 5/5)

**현재 상태**:
- 구조는 좋으나 테스트 부족

**5점 달성 방안**:

#### 1. 테스트 코드 작성 🟠 **가장 중요**

**단위 테스트** (Service 계층):
- 테스트 커버리지 80% 이상 목표
- Mockito 활용한 의존성 모킹
- Given-When-Then 패턴 적용

**통합 테스트** (Repository, Controller):
- `@DataJpaTest`, `@WebMvcTest` 활용
- TestContainers 또는 H2 인메모리 DB 사용

**테스트 코드 구조**:
- 테스트 클래스명: `{ClassName}Test`
- 테스트 메서드명: `메서드명_조건_예상결과` 패턴

#### 2. 테스트 커버리지 측정 🟡

- JaCoCo 도구 도입
- CI/CD 파이프라인에서 커버리지 측정 및 리포트 생성
- 커버리지 임계값 설정 (예: 80% 미만 시 빌드 실패)

#### 3. 테스트 데이터 관리 🟡

- 테스트 픽스처 생성 유틸리티 (Builder 패턴)
- 테스트 데이터베이스 초기화 전략 (Flyway/Liquibase)

#### 4. 문서화 강화 🟢

- README.md에 프로젝트 구조, 실행 방법, 테스트 방법 설명
- 주요 설계 결정 사항 문서화 (ADR: Architecture Decision Records)
- API 문서화 (Spring REST Docs 또는 Swagger)

#### 5. 로깅 전략 수립 🟡

- 로깅 레벨 가이드라인 (ERROR, WARN, INFO, DEBUG)
- 구조화된 로깅 (JSON 형식)
- 주요 비즈니스 로직에 로깅 추가

**우선순위**: 테스트 코드 작성이 가장 중요 (유지보수성의 핵심)

---

### 3.6 예외 처리 (4/5 → 5/5)

**현재 상태**:
- 전역 핸들러 잘 구현
- 구체적인 에러 코드 사용
- 일관된 Response 구조

**5점 달성 방안**:

#### 1. 에러 코드 체계 고도화 🟡

- 에러 코드 분류 체계 명확화 (현재 숫자 기반 코드 사용 중)
- 에러 코드 문서화 (각 에러 코드의 의미, 발생 조건, 해결 방법)
- 에러 코드별 HTTP Status Code 매핑 검토

#### 2. 예외 로깅 강화 🟡

- 예외 발생 시 스택 트레이스 로깅 (개발 환경)
- 운영 환경에서는 민감 정보 제거하여 로깅
- 예외별 로그 레벨 설정 (예: 비즈니스 예외는 WARN, 시스템 예외는 ERROR)

#### 3. 예외 계층 구조 명확화 🟢

- 도메인 예외와 인프라 예외 분리
- 예외 변환 전략 수립 (인프라 예외 → 도메인 예외)

#### 4. 예외 처리 테스트 🟡

- GlobalExceptionHandler 테스트
- 각 예외 케이스에 대한 테스트 코드 작성

#### 5. 에러 응답 개선 🟢

- 개발 환경에서는 상세한 에러 정보 제공
- 운영 환경에서는 일반적인 메시지만 제공
- `ErrorDetails` DTO에 타임스탬프, 요청 ID 등 추가

**우선순위**: 에러 코드 체계 고도화와 예외 로깅 강화가 중요 (운영 안정성)

---

## 4. AI 호출 안정성 설계

### 4.1 적용 배경

#### 4.1.1 현재 구조상의 문제점

본 시스템은 **WebFlux 기반 서비스 레이어(Mono)**를 사용하지만, Controller 레이어에서는 다음과 같은 제약을 고려해야 한다.

**현재 코드**:
```java
// PromptController.java
@PostMapping
public Mono<ResponseEntity<CustomResponse<PromptResponseDto>>> createPrompt(...) {
    return promptService.createPrompt(request, authUser.getId())
        .map(result -> CustomResponseHelper.created(result));
}
```

**문제점**:
- Controller에 `Mono<T>`가 직접 노출될 경우:
  - MVC 스타일 API와 혼용 시 일관성 저하
  - 공통 응답 래핑/필터/인터셉터 적용 난이도 증가
  - 팀 내 개발자 숙련도에 따라 유지보수 비용 증가

#### 4.1.2 AI API의 특성

**외부 AI API는 다음 문제가 빈번함**:
- 지연 (Latency): 3~10초 응답 지연
- 실패 (Failure): 5xx 에러, 네트워크 오류
- 쿼터 제한 (Rate Limit): API 사용량 초과

**단순 retry/timeout만으로는 장애 전파를 막기 어려움**

#### 4.1.3 설계 목표

- Controller 계층: **동기식, 명확한 흐름**
- Service 계층: **비동기/리액티브 내부 처리**
- 외부 AI 장애가 Controller/API 계약에 전파되지 않도록 차단
- 목표:
  - 장애 격리 (fail-fast)
  - 사용자 경험 보호 (fallback)
  - 운영 가시성 확보 (metrics)

---

### 4.2 Resilience4j 서킷브레이커 설계

#### 4.2.1 도입 목적

- 연속 실패 시 외부 AI 호출 차단
- 불필요한 재시도 및 스레드 점유 방지
- AI 장애가 전체 시스템으로 확산되는 것 방지

#### 4.2.2 서킷브레이커 상태

- **CLOSED**: 정상 호출
- **OPEN**: 실패율 초과 → 즉시 실패 (fallback으로 전환)
- **HALF_OPEN**: 제한적 재시도로 회복 여부 판단

#### 4.2.3 권장 설정값 (AI API 기준)

```yaml
resilience4j:
  circuitbreaker:
    instances:
      googleGemini:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 20
        failureRateThreshold: 50
        slowCallRateThreshold: 50
        slowCallDurationThreshold: 5s
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
        minimumNumberOfCalls: 10
```

**설정 설명**:
- `slidingWindowSize: 20`: 최근 20개 요청 기준
- `failureRateThreshold: 50%`: 실패율 50% 초과 시 OPEN
- `slowCallRateThreshold: 50%`: 느린 호출(5초 초과) 50% 시 OPEN
- `waitDurationInOpenState: 30s`: OPEN 상태에서 30초 후 HALF_OPEN으로 전환
- `permittedNumberOfCallsInHalfOpenState: 3`: HALF_OPEN에서 3개 요청으로 회복 여부 판단

#### 4.2.4 WebFlux 적용 패턴

**위치**: `GoogleGeminiService.chat()`

**적용 방식**: Reactor 연동 (resilience4j-reactor)

**적용 순서**: timeout/retry **이후**에 circuit breaker 적용

```java
@Service
@RequiredArgsConstructor
public class GoogleGeminiService {
    
    private final CircuitBreaker circuitBreaker;
    private final WebClient webClient;
    
    public Mono<String> chat(String prompt) {
        Mono<String> chatCall = webClient.post()
            .uri(...)
            .retrieve()
            .bodyToMono(ChatResponse.class)
            .map(this::extractFirstCandidate)
            .timeout(Duration.ofSeconds(30))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
        
        return CircuitBreakerOperator.of(circuitBreaker)
            .apply(chatCall);
    }
}
```

---

### 4.3 AI 실패 Fallback 전략

#### 4.3.1 실패 유형 분류

1. **네트워크 오류 / 5xx**: 서버 오류, 연결 실패
2. **Timeout**: 응답 시간 초과
3. **서킷 OPEN 상태**: CircuitBreakerOpenException
4. **응답 파싱 실패**: JSON 파싱 오류
5. **의미 없는 결과**: empty 응답

#### 4.3.2 Fallback 전략 유형

##### 3.2.1 사용자 가시적 Fallback (권장)

**목적**: 서비스 중단 대신 최소 기능 제공

**예시**:
- "AI 응답 생성에 실패했습니다. 아래 기본 가이드를 참고해주세요."
- 사전 정의된 템플릿 프롬프트 반환

**구현 예시**:
```java
public Mono<String> chat(String prompt) {
    return webClient.post()
        .uri(...)
        .retrieve()
        .bodyToMono(ChatResponse.class)
        .map(this::extractFirstCandidate)
        .timeout(Duration.ofSeconds(30))
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
        .onErrorResume(e -> {
            log.warn("AI 호출 실패, fallback 사용", e);
            return Mono.just(getFallbackPrompt());
        })
        .switchIfEmpty(Mono.just(getFallbackPrompt()));
}

private String getFallbackPrompt() {
    return "AI 응답 생성에 실패했습니다. 기본 프롬프트를 사용합니다.";
}
```

##### 3.2.2 내부 대체 로직

- 이전 성공 응답 캐시 사용
- 언어별 기본 Prompt Skeleton 제공

##### 3.2.3 명시적 실패 (비권장, 제한적 사용)

- 관리자/내부 API에서만 허용
- 사용자 요청에는 가급적 사용하지 않음

#### 4.3.3 Fallback 트리거 조건

- CircuitBreakerOpenException
- RetryExhausted
- TimeoutException
- 5xx 서버 오류 (재시도 후 실패)

---

### 4.4 응답 시간 / 실패율 메트릭

#### 4.4.1 왜 필요한가

- AI 장애는 **로그만으로 감지 불가**
- 지연 증가 → UX 저하
- 장애 전조를 조기에 탐지 필요

#### 4.4.2 핵심 지표 (필수)

##### Latency (응답 시간)

- 평균 응답 시간
- p95 / p99
- slow call 비율

##### Error Rate (실패율)

- 전체 요청 대비 실패 비율
- 에러 유형별 비율:
  - timeout
  - 4xx
  - 5xx
  - circuit open

##### Throughput (처리량)

- 초당 요청 수
- 성공/실패 요청 수

#### 4.4.3 수집 위치

- `GoogleGeminiService.chat()`
- CircuitBreaker 이벤트 리스너
- WebClient filter

#### 4.4.4 태깅 전략

```java
Timer.Sample sample = Timer.start(meterRegistry);

return googleGeminiService.chat(promptText)
    .doOnSuccess(result -> {
        sample.stop(Timer.builder("ai.call")
            .tag("provider", "gemini")
            .tag("model", properties.getModel())
            .tag("result", "success")
            .register(meterRegistry));
    })
    .doOnError(error -> {
        sample.stop(Timer.builder("ai.call")
            .tag("provider", "gemini")
            .tag("model", properties.getModel())
            .tag("result", "error")
            .tag("error.type", error.getClass().getSimpleName())
            .register(meterRegistry));
    });
```

**태그**:
- `ai.provider = gemini`
- `ai.model`
- `language`
- `result = success | fallback | error`

---

### 4.5 권장 결합 구조 (Controller Mono 노출 방지)

#### 4.5.1 최종 권장 아키텍처

```
Controller (동기)
  ↓
Facade / Application Service (동기)
  ↓
Reactive AI Service (Mono 내부 사용)
  ├─ WebClient
  ├─ Timeout
  ├─ Retry
  ├─ CircuitBreaker
  ├─ Metrics
  └─ Fallback
  ↓
Persistence Service (JPA, @Transactional)
```

#### 4.5.2 계층 분리 핵심 원칙

- **Controller는 Mono를 알지 않는다**
- 리액티브는 외부 API 경계에서만 사용
- `.block()`은 Facade/Application Service 계층에서만 허용
- DB 트랜잭션은 항상 blocking 영역에서 시작

#### 4.5.3 구현 예시

**1. Facade 계층 도입**

```java
@Service
@RequiredArgsConstructor
public class PromptFacade {
    
    private final PromptService promptService;
    
    @Transactional
    public PromptResponseDto createPrompt(PromptRequestDto request, Long userId) {
        // 리액티브 → 동기 변환 (block 허용)
        return promptService.createPrompt(request, userId)
            .block(Duration.ofSeconds(60));
    }
}
```

**2. Controller 수정**

```java
@RestController
@RequestMapping("/prompts")
@RequiredArgsConstructor
public class PromptController {
    
    private final PromptFacade promptFacade; // Service 대신 Facade 사용
    
    @PostMapping
    public ResponseEntity<CustomResponse<PromptResponseDto>> createPrompt(
            @Valid @RequestBody PromptRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        // 동기 방식으로 변경
        PromptResponseDto result = promptFacade.createPrompt(request, authUser.getId());
        return CustomResponseHelper.created(result);
    }
}
```

**3. Service 계층은 리액티브 유지**

```java
public interface PromptService {
    Mono<PromptResponseDto> createPrompt(PromptRequestDto request, Long userId);
}
```

---

### 4.6 Controller 구조를 고려한 Fallback / 장애 전파 기준

#### 4.6.1 Controller 계층으로 전파되는 경우

다음 경우에만 Controller까지 예외를 전파한다:

- 사용자 입력 오류 (4xx)
- 권한/인증 오류
- 비즈니스 규칙 위반

#### 4.6.2 Controller에서 절대 보지 말아야 할 것

- WebClient 예외
- TimeoutException
- CircuitBreakerOpenException
- RetryExhaustedException

위 예외들은 **모두 Service 내부에서 Fallback 또는 도메인 예외로 변환**되어야 한다.

#### 4.6.3 Controller 관점의 응답 정책

| 상황 | Controller 응답 |
|------|----------------|
| AI 정상 | 생성된 Prompt 반환 |
| AI 장애 | Fallback Prompt 반환 |
| DB 오류 | 500 (서버 오류) |

---

### 4.7 Controller 친화적 Fallback 전략

#### 4.7.1 동기 Controller용 Fallback 예시

- "AI 생성이 지연되어 기본 프롬프트를 제공합니다"
- 언어별 기본 Prompt Template
- 마지막 성공 결과 캐시

#### 4.7.2 금지 패턴

- ❌ Controller에서 `.block()` 남용
- ❌ Controller에서 Retry/Timeout 제어
- ❌ Controller에서 서킷 상태 분기

**Controller는 결과만 받고 판단하지 않는다.**

---

### 4.8 추가 개선 포인트 (운영 단계에서 차이를 만드는 부분)

#### 4.8.1 Facade 계층 명확화 (Application Service)

**개선 이유**:
- Controller ↔ Service 사이 책임 분리가 애매해지기 쉬움
- `.block()` 위치가 흐려지면 다시 안티패턴으로 회귀

**개선 방안**:
- `PromptFacade` 또는 `PromptApplicationService` 도입
- Facade에서만 다음 책임 수행:
  - 리액티브 → 동기 변환 (`block()`)
  - Fallback 결과 최종 결정
  - 트랜잭션 경계 진입

#### 4.8.2 AI 호출 Idempotency 고려

**문제**:
- 네트워크 타임아웃 이후 재시도 시
- 실제 AI는 이미 응답을 생성했을 수 있음

**개선 방안**:
- `requestId` / `promptHash` 생성
- 동일 요청에 대해:
  - 중복 AI 호출 방지
  - 결과 캐싱 가능

#### 4.8.3 결과 캐싱 전략 (Selective Cache)

**적용 대상**:
- 동일 InputRequestDto
- 언어 + 목적 + 옵션이 동일한 경우

**전략**:
- 성공 결과만 캐싱
- TTL 짧게 (예: 5~30분)
- Fallback 결과는 캐싱 ❌

#### 4.8.4 서킷브레이커 + 알림 연계

**개선 이유**:
- 서킷 OPEN은 **운영 이벤트**
- 로그만으로는 대응 불가

**개선 방안**:
- CircuitBreaker EventListener 활용
- OPEN 전환 시:
  - Slack / Discord / Email 알림
  - 알림 빈도 제한

#### 4.8.5 Fallback 품질 관리

**문제**:
- Fallback이 잦아지면 서비스 품질 저하

**개선 방안**:
- fallback 응답에 메타 정보 포함
  - `generatedBy = AI | FALLBACK`
- 관리자 화면에서 fallback 비율 확인

#### 4.8.6 SLA 기준 명문화

**예시**:
- AI 성공률: ≥ 95%
- p95 응답 시간: ≤ 3s
- fallback 비율: ≤ 5%

**SLA 초과 시**:
- 자동 알림
- 서킷 파라미터 재조정 검토

#### 4.8.7 테스트 전략 보강

**필수 테스트**:
- AI Timeout 시 fallback 반환
- Circuit OPEN 상태에서 즉시 fallback
- Retry Exhaust 후 fallback
- DB 저장 실패 시 전체 롤백

**추천 방식**:
- Gemini API Mock Server
- CircuitBreaker 상태 강제 전환 테스트

---

### 4.9 최종 결론 (권장 아키텍처 요약)

#### 4.9.1 핵심 원칙

- Controller는 **끝까지 동기**
- Mono는 **외부 API 경계 내부에서만**
- `.block()`은 Facade에서만 허용
- 장애는 예외가 아닌 **대체 경로로 흡수**
- Metrics 없는 안정성은 존재하지 않음

#### 4.9.2 구조 요약

이 구조는 "돌아가는 코드"가 아니라 **"운영을 견디는 코드"**를 목표로 한다.

- Controller는 **동기 구조 유지**
- AI 연동의 복잡성은 Service 내부로 격리
- 서킷브레이커/Retry/Timeout은 외부 API 경계에서만 사용
- Fallback은 사용자 경험 보호 수단이지 예외 처리가 아님

이 구조는 다음을 동시에 만족한다:

- WebFlux의 장점 활용
- 기존 MVC 스타일과의 공존
- 운영 안정성 및 디버깅 용이성

즉, **리액티브를 쓰되 노출하지 않는 구조**가 최종 목표다.

#### 4.9.3 안정성의 핵심 요소

- **Retry**: 일시적 실패 대응
- **Circuit Breaker**: 지속적 장애 격리
- **Fallback**: 사용자 경험 보호
- **Metrics**: 운영 안정성의 핵심

이 네 가지가 결합되어야 AI 연동 서비스가 운영 환경에서 안전하게 동작한다.

---

## 5. 개선 로드맵

### Phase 1: 즉시 개선 (1-2주) 🟠

**우선순위**: 높음 (안정성 및 보안)

1. **AI 호출 안정성 강화**
   - Resilience4j CircuitBreaker 도입
   - Fallback 전략 구현
   - Facade 계층 도입 (Controller Mono 제거)

2. **보안 강화**
   - Rate Limiting 구현
   - 입력값 Sanitization (XSS 방지)
   - 보안 헤더 추가

3. **서비스 인터페이스 일관성 확보**
   - 모든 Service에 인터페이스 추가

### Phase 2: 단기 개선 (1-2개월) 🟡

**우선순위**: 중간 (성능 및 코드 품질)

1. **성능 최적화**
   - 캐싱 전략 확대 (응답 캐싱)
   - Connection Pool 최적화
   - DB 인덱스 최적화

2. **코드 품질 개선**
   - JavaDoc 추가
   - 코드 복잡도 감소

3. **테스트 코드 작성**
   - 단위 테스트 작성 (Service 계층 우선)
   - 통합 테스트 작성

4. **메트릭 수집**
   - AI 호출 메트릭 (응답 시간, 실패율)
   - Micrometer + Prometheus 연동

5. **예외 처리 개선**
   - 예외 로깅 강화
   - 에러 코드 체계 고도화

### Phase 3: 중장기 개선 (3-6개월) 🟢

**우선순위**: 낮음 (장기적 가치)

1. **아키텍처 고도화**
   - DDD 패턴 적용 심화
   - 도메인 이벤트 패턴 고도화
   - Port and Adapter 패턴 적용 검토

2. **테스트 커버리지 80% 달성**
   - JaCoCo 도입
   - CI/CD 파이프라인에서 커버리지 검사

3. **문서화 강화**
   - README.md 보완
   - API 문서화 (Swagger/Spring REST Docs)
   - ADR (Architecture Decision Records)

4. **운영 도구 도입**
   - 로깅 중앙화 (ELK Stack)
   - 모니터링 대시보드 (Grafana)
   - CI/CD 파이프라인 구축

5. **추가 개선**
   - AI 호출 Idempotency
   - 결과 캐싱 전략
   - 서킷브레이커 알림 연계
   - SLA 기준 명문화

---

## 6. 체크리스트

### 6.1 AI 호출 안정성

- [ ] Resilience4j CircuitBreaker 도입
- [ ] Fallback 전략 구현
- [ ] Facade 계층 도입 (Controller Mono 제거)
- [ ] 메트릭 수집 (응답 시간, 실패율)
- [ ] 서킷브레이커 알림 연계
- [ ] Fallback 품질 관리

### 6.2 보안

- [ ] Rate Limiting 구현
- [ ] 입력값 Sanitization
- [ ] 보안 헤더 추가
- [ ] 인증/인가 로깅
- [ ] 토큰 보안 강화

### 6.3 아키텍처

- [ ] 서비스 인터페이스 일관성 확보
- [ ] Controller Mono 노출 제거
- [ ] 도메인 이벤트 패턴 고도화
- [ ] DDD 패턴 적용 심화

### 6.4 성능

- [ ] 캐싱 전략 확대
- [ ] Connection Pool 최적화
- [ ] DB 인덱스 최적화
- [ ] 비동기 처리 범위 확대

### 6.5 코드 품질

- [ ] JavaDoc 추가
- [ ] 코드 복잡도 감소
- [ ] 매직 넘버/문자열 상수화
- [ ] 일관된 코딩 컨벤션
- [ ] 불변성 강화

### 6.6 유지보수성

- [ ] 테스트 코드 작성 (단위/통합)
- [ ] 테스트 커버리지 80% 달성
- [ ] 테스트 커버리지 측정 도구 도입
- [ ] 문서화 강화
- [ ] 로깅 전략 수립

### 6.7 예외 처리

- [ ] 에러 코드 체계 고도화
- [ ] 예외 로깅 강화
- [ ] 예외 계층 구조 명확화
- [ ] 예외 처리 테스트
- [ ] 에러 응답 개선

---

**작성일**: 2024년  
**문서 목적**: 코드 품질 개선 및 AI 호출 안정성 확보를 위한 종합 가이드  
**대상 독자**: 개발팀 전원, 아키텍트, 기술 리더


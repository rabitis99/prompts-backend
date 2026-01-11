# AI 호출 안정성 설계

## 4.1 적용 배경

### 4.1.1 현재 구조상의 문제점

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

### 4.1.2 AI API의 특성

**외부 AI API는 다음 문제가 빈번함**:
- 지연 (Latency): 3~10초 응답 지연
- 실패 (Failure): 5xx 에러, 네트워크 오류
- 쿼터 제한 (Rate Limit): API 사용량 초과

**단순 retry/timeout만으로는 장애 전파를 막기 어려움**

### 4.1.3 설계 목표

- Controller 계층: **동기식, 명확한 흐름**
- Service 계층: **비동기/리액티브 내부 처리**
- 외부 AI 장애가 Controller/API 계약에 전파되지 않도록 차단
- 목표:
  - 장애 격리 (fail-fast)
  - 사용자 경험 보호 (fallback)
  - 운영 가시성 확보 (metrics)

---

## 4.2 Resilience4j 서킷브레이커 설계

### 4.2.1 도입 목적

- 연속 실패 시 외부 AI 호출 차단
- 불필요한 재시도 및 스레드 점유 방지
- AI 장애가 전체 시스템으로 확산되는 것 방지

### 4.2.2 서킷브레이커 상태

- **CLOSED**: 정상 호출
- **OPEN**: 실패율 초과 → 즉시 실패 (fallback으로 전환)
- **HALF_OPEN**: 제한적 재시도로 회복 여부 판단

### 4.2.3 권장 설정값 (AI API 기준)

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

### 4.2.4 WebFlux 적용 패턴

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

## 4.3 AI 실패 Fallback 전략

### 4.3.1 실패 유형 분류

1. **네트워크 오류 / 5xx**: 서버 오류, 연결 실패
2. **Timeout**: 응답 시간 초과
3. **서킷 OPEN 상태**: CircuitBreakerOpenException
4. **응답 파싱 실패**: JSON 파싱 오류
5. **의미 없는 결과**: empty 응답

### 4.3.2 Fallback 전략 유형

#### 4.3.2.1 사용자 가시적 Fallback (권장)

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

#### 4.3.2.2 내부 대체 로직

- 이전 성공 응답 캐시 사용
- 언어별 기본 Prompt Skeleton 제공

#### 4.3.2.3 명시적 실패 (비권장, 제한적 사용)

- 관리자/내부 API에서만 허용
- 사용자 요청에는 가급적 사용하지 않음

### 4.3.3 Fallback 트리거 조건

- CircuitBreakerOpenException
- RetryExhausted
- TimeoutException
- 5xx 서버 오류 (재시도 후 실패)

---

## 4.4 응답 시간 / 실패율 메트릭

### 4.4.1 왜 필요한가

- AI 장애는 **로그만으로 감지 불가**
- 지연 증가 → UX 저하
- 장애 전조를 조기에 탐지 필요

### 4.4.2 핵심 지표 (필수)

#### Latency (응답 시간)

- 평균 응답 시간
- p95 / p99
- slow call 비율

#### Error Rate (실패율)

- 전체 요청 대비 실패 비율
- 에러 유형별 비율:
  - timeout
  - 4xx
  - 5xx
  - circuit open

#### Throughput (처리량)

- 초당 요청 수
- 성공/실패 요청 수

### 4.4.3 수집 위치

- `GoogleGeminiService.chat()`
- CircuitBreaker 이벤트 리스너
- WebClient filter

### 4.4.4 태깅 전략

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

## 4.5 권장 결합 구조 (Controller Mono 노출 방지)

### 4.5.1 최종 권장 아키텍처

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

### 4.5.2 계층 분리 핵심 원칙

- **Controller는 Mono를 알지 않는다**
- 리액티브는 외부 API 경계에서만 사용
- `.block()`은 Facade/Application Service 계층에서만 허용
- DB 트랜잭션은 항상 blocking 영역에서 시작

### 4.5.3 구현 예시

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

## 4.6 Controller 구조를 고려한 Fallback / 장애 전파 기준

### 4.6.1 Controller 계층으로 전파되는 경우

다음 경우에만 Controller까지 예외를 전파한다:

- 사용자 입력 오류 (4xx)
- 권한/인증 오류
- 비즈니스 규칙 위반

### 4.6.2 Controller에서 절대 보지 말아야 할 것

- WebClient 예외
- TimeoutException
- CircuitBreakerOpenException
- RetryExhaustedException

위 예외들은 **모두 Service 내부에서 Fallback 또는 도메인 예외로 변환**되어야 한다.

### 4.6.3 Controller 관점의 응답 정책

| 상황 | Controller 응답 |
|------|----------------|
| AI 정상 | 생성된 Prompt 반환 |
| AI 장애 | Fallback Prompt 반환 |
| DB 오류 | 500 (서버 오류) |

---

## 4.7 Controller 친화적 Fallback 전략

### 4.7.1 동기 Controller용 Fallback 예시

- "AI 생성이 지연되어 기본 프롬프트를 제공합니다"
- 언어별 기본 Prompt Template
- 마지막 성공 결과 캐시

### 4.7.2 금지 패턴

- ❌ Controller에서 `.block()` 남용
- ❌ Controller에서 Retry/Timeout 제어
- ❌ Controller에서 서킷 상태 분기

**Controller는 결과만 받고 판단하지 않는다.**

---

## 4.8 추가 개선 포인트 (운영 단계에서 차이를 만드는 부분)

### 4.8.1 Facade 계층 명확화 (Application Service)

**개선 이유**:
- Controller ↔ Service 사이 책임 분리가 애매해지기 쉬움
- `.block()` 위치가 흐려지면 다시 안티패턴으로 회귀

**개선 방안**:
- `PromptFacade` 또는 `PromptApplicationService` 도입
- Facade에서만 다음 책임 수행:
  - 리액티브 → 동기 변환 (`block()`)
  - Fallback 결과 최종 결정
  - 트랜잭션 경계 진입

### 4.8.2 AI 호출 Idempotency 고려

**문제**:
- 네트워크 타임아웃 이후 재시도 시
- 실제 AI는 이미 응답을 생성했을 수 있음

**개선 방안**:
- `requestId` / `promptHash` 생성
- 동일 요청에 대해:
  - 중복 AI 호출 방지
  - 결과 캐싱 가능

### 4.8.3 결과 캐싱 전략 (Selective Cache)

**적용 대상**:
- 동일 InputRequestDto
- 언어 + 목적 + 옵션이 동일한 경우

**전략**:
- 성공 결과만 캐싱
- TTL 짧게 (예: 5~30분)
- Fallback 결과는 캐싱 ❌

### 4.8.4 서킷브레이커 + 알림 연계

**개선 이유**:
- 서킷 OPEN은 **운영 이벤트**
- 로그만으로는 대응 불가

**개선 방안**:
- CircuitBreaker EventListener 활용
- OPEN 전환 시:
  - Slack / Discord / Email 알림
  - 알림 빈도 제한

### 4.8.5 Fallback 품질 관리

**문제**:
- Fallback이 잦아지면 서비스 품질 저하

**개선 방안**:
- fallback 응답에 메타 정보 포함
  - `generatedBy = AI | FALLBACK`
- 관리자 화면에서 fallback 비율 확인

### 4.8.6 SLA 기준 명문화

**예시**:
- AI 성공률: ≥ 95%
- p95 응답 시간: ≤ 3s
- fallback 비율: ≤ 5%

**SLA 초과 시**:
- 자동 알림
- 서킷 파라미터 재조정 검토

### 4.8.7 테스트 전략 보강

**필수 테스트**:
- AI Timeout 시 fallback 반환
- Circuit OPEN 상태에서 즉시 fallback
- Retry Exhaust 후 fallback
- DB 저장 실패 시 전체 롤백

**추천 방식**:
- Gemini API Mock Server
- CircuitBreaker 상태 강제 전환 테스트

---

## 4.9 최종 결론 (권장 아키텍처 요약)

### 4.9.1 핵심 원칙

- Controller는 **끝까지 동기**
- Mono는 **외부 API 경계 내부에서만**
- `.block()`은 Facade에서만 허용
- 장애는 예외가 아닌 **대체 경로로 흡수**
- Metrics 없는 안정성은 존재하지 않음

### 4.9.2 구조 요약

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

### 4.9.3 안정성의 핵심 요소

- **Retry**: 일시적 실패 대응
- **Circuit Breaker**: 지속적 장애 격리
- **Fallback**: 사용자 경험 보호
- **Metrics**: 운영 안정성의 핵심

이 네 가지가 결합되어야 AI 연동 서비스가 운영 환경에서 안전하게 동작한다.

[← 목차로 돌아가기](../CODE_IMPROVEMENT_GUIDE.md)


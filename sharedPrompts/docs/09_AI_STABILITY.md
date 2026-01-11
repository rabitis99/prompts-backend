# AI 호출 안정성 설계

## 4.1 적용 배경

### 4.1.1 현재 구조상의 문제점

본 시스템은 **WebFlux 기반 서비스 레이어(Mono)**를 사용하지만, Controller 레이어에서는 다음과 같은 제약을 고려해야 한다.

**이전 코드** (문제가 있던 구조):
```java
// PromptController.java (과거)
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

**현재 구현** (✅ 완료):
- Facade 계층을 도입하여 Controller에서 Mono 노출을 제거
- 실제 구현: [`PromptFacade`](../../src/main/java/org/example/sharedprompts/domain/prompt/facade/PromptFacade.java)
- 관련 문서: [아키텍처 개선 방안](./03_ARCHITECTURE.md#2-controller-mono-노출-제거-완료-ai-안정성과-연계)

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

### 4.2.4 WebFlux 적용 패턴 ✅ **구현 완료**

**위치**: `GoogleGeminiService.chat()`

**현재 구현 상태**: 
- ✅ Timeout 적용 완료
- ✅ Retry 적용 완료
- ✅ CircuitBreaker 적용 완료

**실제 구현 파일**: [`GoogleGeminiServiceImpl.java`](../../src/main/java/org/example/sharedprompts/global/google/gemini/GoogleGeminiServiceImpl.java)

**적용 방식**: Reactor 연동 (resilience4j-reactor)

**적용 순서**: timeout/retry **이후**에 circuit breaker 적용

**실제 구현 코드**:
```java
// GoogleGeminiServiceImpl.java
@Override
public Mono<String> chat(String prompt) {
    GeminiRequest request = GeminiRequest.fromUserPrompt(prompt);

    Mono<String> chatCall = webClient.post()
        .uri(...)
        .retrieve()
        .bodyToMono(ChatResponse.class)
        .map(this::extractFirstCandidate)
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
            .filter(e -> {
                // 5xx 서버 오류 및 네트워크 오류만 재시도
                if (e instanceof WebClientResponseException wcre) {
                    if (wcre.getStatusCode().is4xxClientError()) {
                        return false;
                    }
                }
                return true;
            }))
        .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
        .doOnError(e -> log.error("GoogleGemini API error", e));

    // CircuitBreaker 적용 (timeout/retry 이후)
    return CircuitBreakerOperator.of(circuitBreaker).apply(chatCall);
}
```

**설정 파일**:
- CircuitBreaker 설정: [`application.yml`](../../src/main/resources/application.yml)
- CircuitBreaker Bean: [`GoogleGeminiConfig.java`](../../src/main/java/org/example/sharedprompts/global/config/GoogleGeminiConfig.java)

---

## 4.3 AI 실패 Fallback 전략

### 4.3.1 실패 유형 분류

1. **네트워크 오류 / 5xx**: 서버 오류, 연결 실패
2. **Timeout**: 응답 시간 초과
3. **서킷 OPEN 상태**: CircuitBreakerOpenException
4. **응답 파싱 실패**: JSON 파싱 오류
5. **의미 없는 결과**: empty 응답

### 4.3.2 Fallback 전략 유형

#### 4.3.2.1 사용자 가시적 Fallback ✅ **구현 완료**

**목적**: 서비스 중단 대신 최소 기능 제공

**현재 구현 상태**: ✅ Fallback 구현 완료

**실제 구현 파일**: [`GoogleGeminiServiceImpl.java`](../../src/main/java/org/example/sharedprompts/global/google/gemini/GoogleGeminiServiceImpl.java)

**구현 내용**:
```java
@Override
public Mono<String> chat(String prompt) {
    // ... WebClient 호출 및 CircuitBreaker 적용 ...
    
    Mono<String> protectedCall = CircuitBreakerOperator.of(circuitBreaker).apply(chatCall);

    // Fallback 적용: 모든 에러와 빈 응답에 대해 Fallback 반환
    return protectedCall
        .onErrorResume(e -> {
            log.warn("AI 호출 실패, fallback 사용. Error: {}", e.getClass().getSimpleName(), e);
            return Mono.just(getFallbackPrompt(prompt));
        })
        .switchIfEmpty(Mono.defer(() -> {
            log.warn("AI 응답이 비어있음, fallback 사용");
            return Mono.just(getFallbackPrompt(prompt));
        }));
}

private String getFallbackPrompt(String originalPrompt) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    
    return String.format("""
        [AI 응답 생성에 실패했습니다 - %s]
        
        시스템이 일시적으로 응답을 생성할 수 없습니다. 아래 기본 가이드를 참고해주세요.
        
        원본 요청:
        %s
        
        참고사항:
        - 네트워크 연결을 확인해주세요
        - 잠시 후 다시 시도해주세요
        - 문제가 지속되면 관리자에게 문의해주세요
        """, timestamp, originalPrompt);
}
```

**Fallback 트리거 조건**:
- ✅ CircuitBreaker OPEN 상태 (CallNotPermittedException)
- ✅ Retry Exhausted
- ✅ TimeoutException
- ✅ 5xx 서버 오류 (재시도 후 실패)
- ✅ 빈 응답 (empty response)

**참고**: `PromptServiceImpl`에서 Fallback 결과를 그대로 사용하도록 수정되었습니다. Fallback은 Service 내부에서 처리되며, Controller까지 예외가 전파되지 않습니다.

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

**현재 구현 상태**: 🟡 메트릭 수집 미구현 (향후 구현 예정)

**수집 대상 위치**:
- `GoogleGeminiService.chat()` - AI 호출 지표
- CircuitBreaker 이벤트 리스너 (CircuitBreaker 구현 시)
- WebClient filter - 네트워크 레벨 지표

### 4.4.4 태깅 전략 ✅ **구현 완료**

**실제 구현 코드**:
```java
// GoogleGeminiServiceImpl.java
@Override
public Mono<String> chat(String prompt) {
    Timer.Sample sample = Timer.start(meterRegistry);
    
    // ... WebClient 호출 및 CircuitBreaker 적용 ...
    
    return protectedCall
        .onErrorResume(e -> {
            log.warn("AI 호출 실패, fallback 사용. Error: {}", e.getClass().getSimpleName(), e);
            String fallbackPrompt = getFallbackPrompt(prompt);
            recordMetrics(sample, "fallback", e.getClass().getSimpleName());
            return Mono.just(fallbackPrompt);
        })
        .switchIfEmpty(Mono.defer(() -> {
            log.warn("AI 응답이 비어있음, fallback 사용");
            String fallbackPrompt = getFallbackPrompt(prompt);
            recordMetrics(sample, "fallback", "EmptyResponse");
            return Mono.just(fallbackPrompt);
        }))
        .doOnSuccess(result -> {
            // Fallback이 아닌 정상 응답인 경우에만 메트릭 기록
            if (!result.startsWith(FALLBACK_INDICATOR)) {
                recordMetrics(sample, "success", null);
            }
        });
}

private void recordMetrics(Timer.Sample sample, String result, String errorType) {
    Timer.Builder timerBuilder = Timer.builder("ai.call")
            .tag("provider", "gemini")
            .tag("model", properties.getModel())
            .tag("result", result);

    if (errorType != null) {
        timerBuilder.tag("error.type", errorType);
    }

    sample.stop(timerBuilder.register(meterRegistry));
}
```

**태그**:
- ✅ `provider = gemini`
- ✅ `model` (properties.getModel())
- ✅ `result = success | fallback | error`
- ✅ `error.type` (에러 발생 시만)

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

### 4.5.3 구현 상태

#### ✅ 구현 완료 항목

**1. Facade 계층 도입** (✅ 구현 완료)

실제 구현 파일: [`PromptFacade.java`](../../src/main/java/org/example/sharedprompts/domain/prompt/facade/PromptFacade.java)

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

**2. Controller 수정** (✅ 구현 완료)

실제 구현 파일: [`PromptController.java`](../../src/main/java/org/example/sharedprompts/controller/prompt/PromptController.java)

```java
@RestController
@RequestMapping("/prompts")
@RequiredArgsConstructor
public class PromptController {
    
    private final PromptService promptService;  // 다른 메서드용
    private final PromptFacade promptFacade;    // createPrompt용
    
    @PostMapping
    public ResponseEntity<CustomResponse<PromptResponseDto>> createPrompt(
            @Valid @RequestBody PromptRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        // 동기 방식으로 변경 - Facade 사용
        PromptResponseDto result = promptFacade.createPrompt(request, authUser.getId());
        return CustomResponseHelper.created(result);
    }
    
    // 다른 메서드들은 동기 Service 직접 사용 (이미 Mono 노출 없음)
    @GetMapping
    public ResponseEntity<CustomResponse<PageResponse<PromptResponseDto>>> getPrompts(...) {
        PageResponse<PromptResponseDto> response = promptService.getPrompts(condition);
        return CustomResponseHelper.ok(response);
    }
}
```

**3. Service 계층은 리액티브 유지**

Service 계층은 리액티브(`Mono`)를 유지하며, Facade 계층에서만 동기 변환이 이루어집니다.

#### ✅ 구현 완료 항목

- ✅ Facade 계층 도입 (4.5.3 섹션 참조)
- ✅ CircuitBreaker 적용 (4.2.4 섹션 참조)
- ✅ Fallback 전략 적용 (4.3.2.1 섹션 참조)
- ✅ 메트릭 수집 (4.4 섹션 참조)

**AI 호출 안정성 설계의 모든 핵심 요소 구현 완료!** ✅

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

### 4.8.1 Facade 계층 명확화 (Application Service) ✅ **완료**

**구현 완료**:
- `PromptFacade` 도입 완료 (실제 파일: [`PromptFacade.java`](../../src/main/java/org/example/sharedprompts/domain/prompt/facade/PromptFacade.java))
- Controller에서 Mono 노출 제거 완료
- 관련 문서: [아키텍처 개선 방안](./03_ARCHITECTURE.md#2-controller-mono-노출-제거-완료-ai-안정성과-연계)

**구현 내용**:
- Facade에서만 다음 책임 수행:
  - 리액티브 → 동기 변환 (`block()`)
  - 트랜잭션 경계 진입
  - (향후) Fallback 결과 최종 결정

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



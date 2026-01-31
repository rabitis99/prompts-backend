# StackOverflowError 분석: DefaultedRedisConnection.pExpire 무한 재귀

## 문제 요약

`java.lang.StackOverflowError`가 `DefaultedRedisConnection.pExpire`에서 발생하며, `redisTemplate.getExpire()` 호출 시 무한 재귀가 발생합니다.

## 호출 경로 분석

### 1. getExpire 호출 지점

코드베이스에서 `redisTemplate.getExpire()` 호출은 다음 위치에서 발생합니다:

#### 1.1 RateLimitHeaderUtil.calculateResetTimestamp()
```java:106:src/main/java/org/example/sharedprompts/auth/rate/filter/util/RateLimitHeaderUtil.java
Long ttl = redisTemplate.getExpire(rateLimitKey, TimeUnit.SECONDS);
```

**호출 컨텍스트:**
- `AbstractRateLimitFilter.addRateLimitHeaders()` → 성공 응답 시 헤더 추가
- `RateLimitExceededFacade.handle()` → 429 응답 작성 시 헤더 추가

#### 1.2 RateLimitResponseWriter.calculateResetTimestamp()
```java:119:src/main/java/org/example/sharedprompts/auth/rate/filter/builder/writer/RateLimitResponseWriter.java
Long ttl = redisTemplate.getExpire(rateLimitKey, java.util.concurrent.TimeUnit.SECONDS);
```

**호출 컨텍스트:**
- `RateLimitResponseWriter.writeTooManyRequests()` → 429 응답 본문 작성 시 reset timestamp 계산

#### 1.3 FixedWindowRateLimiter.consume()
```java:66:src/main/java/org/example/sharedprompts/auth/rate/FixedWindowRateLimiter.java
Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
```

**호출 컨텍스트:**
- Rate Limit 초과 시 retryAfter 계산

### 2. Spring Data Redis 내부 호출 경로

#### 2.1 RedisTemplate.getExpire() → RedisConnection.getExpire()

```
RedisTemplate.getExpire(key, timeUnit)
  → AbstractOperations.getExpire(key, timeUnit)
    → RedisTemplate.execute(connection -> connection.getExpire(key))
      → RedisConnectionFactory.getConnection()
        → [RedisConnection 래퍼 체인]
          → DefaultedRedisConnection.getExpire(key)
```

#### 2.2 DefaultedRedisConnection.getExpire() → pExpire() 재귀 경로

**문제의 핵심:**
`DefaultedRedisConnection.getExpire()`는 내부적으로 `pExpire()`를 호출하여 밀리초 단위 TTL을 조회한 후 초 단위로 변환합니다.

```java
// Spring Data Redis 내부 구현 (추정)
public Long getExpire(byte[] key) {
    Long pttl = pExpire(key);  // 밀리초 단위 TTL 조회
    return pttl != null ? pttl / 1000 : null;  // 초 단위로 변환
}
```

**재귀 발생 시나리오:**
1. `DefaultedRedisConnection.getExpire(key)` 호출
2. 내부에서 `this.pExpire(key)` 호출
3. `DefaultedRedisConnection.pExpire()`가 delegate를 통해 다시 자기 자신을 호출
4. **무한 재귀 발생**

### 3. RedisConnection 래퍼 체인 구조

Spring Boot 3.x에서는 다음 순서로 RedisConnection이 래핑됩니다:

```
LettuceConnection (실제 Redis 연결)
  ↓
ObservationRedisConnection (Micrometer 관찰성)
  ↓
DefaultedRedisConnection (기본 구현 제공)
  ↓
[기타 래퍼: TransactionalRedisConnection, LazyConnection 등]
```

#### 3.1 ObservationRedisConnection (Spring Boot 3.x 자동 활성화)

**설정 확인:**
- `spring-boot-starter-actuator` 의존성 존재 (build.gradle:67)
- `management.endpoints.web.exposure.include` 설정 존재 (application.yml:364)
- **기본적으로 Observation이 활성화되어 있음**

**문제 가능성:**
`ObservationRedisConnection`이 delegate를 잘못 설정하거나, `DefaultedRedisConnection`과의 조합에서 delegate 체인이 깨질 수 있습니다.

#### 3.2 DefaultedRedisConnection의 delegate 설정

`DefaultedRedisConnection`은 다른 `RedisConnection`을 래핑하는데, 만약:
- delegate가 자기 자신을 가리키거나
- delegate가 null이거나
- delegate 체인이 순환 참조를 형성하면

재귀 호출이 발생합니다.

### 4. 재현 조건 분석

#### 4.1 필터 체인 실행 순서

```
1. IpRateLimitFilter.doFilterInternal()
   → performRateLimitCheck()
   → FixedWindowRateLimiter.consume() [Lua 스크립트 실행]
   → 성공 시: addRateLimitHeaders() → getExpire() 호출 ⚠️
   → 초과 시: handleRateLimitExceeded() → getExpire() 호출 ⚠️

2. UserRateLimitFilter.doFilterInternal()
   → 동일한 흐름
```

#### 4.2 Error Dispatch 경로

**주의:** `OncePerRequestFilter`는 기본적으로 동일 요청에 대해 한 번만 실행되지만, **에러 디스패치 시 재실행될 수 있습니다.**

```java
// OncePerRequestFilter 내부 (Spring Framework)
if (isAsyncDispatch(request)) {
    // 비동기 디스패치: 필터 재실행 가능
}
if (isAsyncStarted(request)) {
    // 비동기 요청: 필터 재실행 가능
}
```

**재현 시나리오:**
1. Rate Limit 체크 중 예외 발생
2. Error dispatch로 필터 재실행
3. `getExpire()` 호출 시 RedisConnection 래퍼 체인 문제로 재귀 발생

#### 4.3 응답 커밋 직전 호출

`RateLimitResponseWriter.writeTooManyRequests()`는:
- `response.getWriter()` 호출 전에 `getExpire()` 호출
- 응답이 이미 커밋된 상태에서 호출되면 문제 발생 가능

### 5. 문제가 되는 RedisConnection 래퍼 또는 설정 지점

#### 5.1 확인 필요 사항

1. **RedisConnectionFactory 설정**
   - `RedisConfig.java`에서 `RedisConnectionFactory` Bean 확인 필요
   - Redisson과 Spring Data Redis의 혼용 가능성

2. **Redisson 통합**
   - `RedissonConfig.java` 존재 확인 필요
   - Redisson이 RedisConnectionFactory를 래핑하는 경우 충돌 가능

3. **Transaction 설정**
   - `@Transactional`이 Redis에 적용되는 경우 `TransactionalRedisConnection` 래핑 가능

4. **LazyConnectionFactory**
   - 지연 연결 팩토리 사용 시 프록시 래핑 가능

## 정확한 호출 스택 (추정)

```
java.lang.StackOverflowError
  at org.springframework.data.redis.connection.DefaultedRedisConnection.pExpire(DefaultedRedisConnection.java:XXX)
  at org.springframework.data.redis.connection.DefaultedRedisConnection.getExpire(DefaultedRedisConnection.java:XXX)
  at org.springframework.data.redis.connection.DefaultedRedisConnection.pExpire(DefaultedRedisConnection.java:XXX)
  at org.springframework.data.redis.connection.DefaultedRedisConnection.getExpire(DefaultedRedisConnection.java:XXX)
  ... (무한 반복)
```

## 해결 방안

### 방안 1: getExpire() 호출 제거 또는 예외 처리 강화

**RateLimitHeaderUtil.calculateResetTimestamp() 수정:**
```java
private static long calculateResetTimestamp(...) {
    long now = Instant.now().getEpochSecond();
    
    if (result != null && result.retryAfterSeconds() > 0) {
        return now + result.retryAfterSeconds();
    }
    
    // getExpire() 호출 제거 - windowSeconds 사용
    // Redis TTL 조회는 Lua 스크립트에서 이미 수행되므로 불필요
    return now + rule.getWindowSeconds();
}
```

**장점:**
- 재귀 문제 완전 회피
- 성능 향상 (Redis 추가 호출 제거)

**단점:**
- TTL 정확도 약간 감소 (windowSeconds 사용)

### 방안 2: RedisConnection 래퍼 체인 수정

**RedisConfig에 명시적 ConnectionFactory 설정:**
```java
@Bean
public RedisConnectionFactory redisConnectionFactory() {
    LettuceConnectionFactory factory = new LettuceConnectionFactory();
    // Observation 비활성화 (필요 시)
    // 또는 delegate 체인 명시적 설정
    return factory;
}
```

### 방안 3: Observation 비활성화

**application.yml:**
```yaml
management:
  tracing:
    enabled: false
  metrics:
    export:
      prometheus:
        enabled: false
```

**주의:** 이 방법은 관찰성 기능을 완전히 비활성화하므로 권장하지 않음

### 방안 4: getExpire() 호출을 try-catch로 보호

현재 코드에 이미 try-catch가 있지만, StackOverflowError는 catch되지 않을 수 있습니다.

```java
try {
    Long ttl = redisTemplate.getExpire(rateLimitKey, TimeUnit.SECONDS);
    if (ttl != null && ttl > 0) {
        return now + ttl;
    }
} catch (StackOverflowError e) {
    // 재귀 감지 시 폴백
    logger.error("Redis getExpire() 재귀 오류 감지", e);
    return now + rule.getWindowSeconds();
} catch (Exception e) {
    // 기존 예외 처리
}
```

## 권장 해결책

**방안 1 (getExpire() 호출 제거)을 권장합니다.**

**이유:**
1. Rate Limit 로직은 이미 Lua 스크립트로 TTL을 설정하므로, Java에서 TTL을 다시 조회할 필요가 없습니다.
2. `retryAfterSeconds`는 이미 `FixedWindowRateLimiter`에서 계산되므로 중복 조회입니다.
3. 재귀 문제를 근본적으로 해결합니다.

## 확인된 설정 사항

### RedisConnectionFactory 설정
- `RedisConfig.java`: `RedisConnectionFactory`를 직접 Bean으로 등록하지 않음
- Spring Boot 자동 설정에 의존 (LettuceConnectionFactory 자동 생성)
- `RedissonConfig.java`: `RedissonClient`만 별도로 생성 (RedisConnectionFactory와 독립적)

### Redisson 통합
- Redisson은 분산 락 전용으로 사용되며, Spring Data Redis와 별도로 동작
- RedisConnectionFactory 래핑 없음 (충돌 가능성 낮음)

### Observation 설정
- `spring-boot-starter-actuator` 의존성 존재
- `management.endpoints.web.exposure.include` 설정 존재
- **Spring Boot 3.x 기본값으로 Observation 자동 활성화**

## 추가 조사 필요 사항

1. **실제 StackTrace 확인**
   - 재현 시 정확한 호출 스택 확인
   - delegate 체인의 실제 구조 확인
   - `DefaultedRedisConnection`의 `delegate` 필드 값 확인

2. **Spring Boot 버전 확인**
   - `build.gradle`에서 Spring Boot 3.5.8 사용
   - 해당 버전의 알려진 버그 확인 필요
   - Spring Data Redis 버전 확인 필요

3. **환경별 설정 확인**
   - `application-dev.yml`, `application-prod.yml`의 Redis 설정 확인
   - Observation 관련 설정 확인

4. **디버깅 방법**
   ```java
   // RedisConnection 체인 확인용 디버그 코드
   RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
   RedisConnection actual = connection;
   int depth = 0;
   while (actual != null) {
       logger.info("RedisConnection depth {}: {}", depth++, actual.getClass().getName());
       if (actual instanceof DefaultedRedisConnection) {
           Field delegateField = DefaultedRedisConnection.class.getDeclaredField("delegate");
           delegateField.setAccessible(true);
           actual = (RedisConnection) delegateField.get(actual);
       } else {
           break;
       }
   }
   ```


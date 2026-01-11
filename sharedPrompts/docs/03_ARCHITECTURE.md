# 아키텍처 개선 방안 (4/5 → 5/5)

## 현재 상태

- 계층 분리 명확
- BaseCountService 리팩토링 우수
- 서비스 인터페이스 일관성 부족 (일부는 인터페이스 분리, 일부는 직접 구현)
- Controller에서 Mono 노출 (일관성 저하)

## 5점 달성 방안

### 1. 서비스 인터페이스 일관성 확보 🟠 **중요**

**목표**: 모든 Service 클래스에 인터페이스 분리

**이유**:
- 테스트 용이성 향상 (Mock 객체 생성)
- 의존성 주입 명확화
- 코드 가독성 및 유지보수성 향상
- 구현체 변경 시 클라이언트 코드 영향 최소화

**Service 인터페이스 현황**:
- 인터페이스가 있는 Service: `PromptService`, `UserService`, `CommentService`, `LikeService`, `AuthService`, `PromptTagService`
- 인터페이스가 없는 Service: `BaseCountService`, `GoogleGeminiService`, `TokenRedisService`, `CustomOAuth2UserService` 등

**적용 대상**:
- 인터페이스가 없는 Service 구현체 확인
- 모든 Service에 인터페이스 추가

**구현 방법**:

1. **인터페이스 생성** (예: `GoogleGeminiService`)
```java
// 기존: 인터페이스 없음
@Service
public class GoogleGeminiService {
    public Mono<String> chat(String prompt) { ... }
}

// 개선: 인터페이스 분리
public interface GoogleGeminiService {
    Mono<String> chat(String prompt);
}

@Service
public class GoogleGeminiServiceImpl implements GoogleGeminiService {
    @Override
    public Mono<String> chat(String prompt) { ... }
}
```

2. **의존성 주입 수정** (Controller, 다른 Service 등)
```java
// 개선 전
@RequiredArgsConstructor
public class PromptServiceImpl {
    private final GoogleGeminiService googleGeminiService; // 구현체 직접 참조
}

// 개선 후 (변경 불필요 - 인터페이스로 이미 주입됨)
@RequiredArgsConstructor
public class PromptServiceImpl {
    private final GoogleGeminiService googleGeminiService; // 인터페이스로 주입
}
```

**예외 사항**:
- `BaseCountService`는 내부 유틸리티 Service로 인터페이스 분리 불필요 (다른 Service에서만 사용)
- 또는 모든 Service에 인터페이스를 적용하는 원칙에 따라 분리 가능

### 2. Controller Mono 노출 제거 🟠 **중요** (AI 안정성과 연계)

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

**개선 방안**: Facade 계층 도입 ([AI 호출 안정성 설계](./09_AI_STABILITY.md) 섹션 참조)

### 3. 도메인 이벤트 패턴 고도화 🟡

- 현재 Spring Events 사용 중이지만, 도메인 이벤트와 인프라 이벤트 명확히 구분
- 도메인 레이어에 이벤트 정의, 인프라 레이어에서 처리
- 예: `CommentCreatedEvent`, `LikeCreatedEvent`를 도메인 이벤트로 정의

### 4. DDD 패턴 적용 심화 🟡

- Aggregate Root 명시적 정의
- Value Object 도입 (예: `Email`, `Tag` 등)
- Domain Service 패턴 적용 (복잡한 도메인 로직 분리)

**우선순위**: 서비스 인터페이스 일관성 확보 + Controller Mono 노출 제거가 가장 중요

[← 목차로 돌아가기](../CODE_IMPROVEMENT_GUIDE.md)


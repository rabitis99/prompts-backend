# 아키텍처 개선 방안 (4/5 → 5/5)

## 현재 상태

- 계층 분리 명확
- BaseCountService 리팩토링 우수
- **서비스 인터페이스 일관성 확보 완료** ✅ (모든 주요 비즈니스 로직 Service에 인터페이스 분리됨)
- **Controller Mono 노출 제거 완료** ✅ (Facade 계층 도입으로 일관성 확보)

## 5점 달성 방안

### 1. 서비스 인터페이스 일관성 확보 ✅ **완료**

**현황**: 모든 주요 비즈니스 로직 Service에 인터페이스가 분리되어 있습니다.

**Service 인터페이스 목록**:
- ✅ 인터페이스가 있는 Service: 
  - `PromptService`, `UserService`, `CommentService`, `LikeService`, 
  - `AuthService`, `PromptTagService`, `BaseCountService`, 
  - `GoogleGeminiService`, `TokenRedisService`, `CustomOAuth2UserService`,
  - `CommentCountService`, `LikeCountService`

**예외 사항** (인터페이스 분리 불필요):
- 스케줄러 서비스: `PromptBatchService`, `PromptLikeBatchService`, `CommentLikeBatchService`, `PromptCountSyncScheduler`
- Builder 패턴 서비스: `KoreanGuidelineBuilder`, `JapaneseGuidelineBuilder`, `EnglishGuidelineBuilder`
  - 이러한 서비스들은 특수한 용도(스케줄링, Builder 패턴)로 인터페이스 분리 불필요

**결론**: 주요 비즈니스 로직을 담당하는 모든 Service에 인터페이스가 적용되어 있어, 아키텍처 관점에서 인터페이스 일관성이 확보되었습니다.

### 2. Controller Mono 노출 제거 ✅ **완료** (AI 안정성과 연계)

**관련 문서**: [AI 호출 안정성 설계](./09_AI_STABILITY.md) (4.5.3 섹션 참조)

**완료 내역**:
- `PromptFacade` 클래스 생성 (`domain/prompt/facade/PromptFacade.java`)
- Controller의 `createPrompt` 메서드에서 Mono 반환 타입 제거
- 리액티브 → 동기 변환을 Facade 계층에서 처리

**구현 내용**:
```java
// PromptFacade.java
@Service
@RequiredArgsConstructor
public class PromptFacade {
    private final PromptService promptService;
    
    @Transactional
    public PromptResponseDto createPrompt(PromptRequestDto request, Long userId) {
        return promptService.createPrompt(request, userId)
            .block(Duration.ofSeconds(60));
    }
}

// PromptController.java
@PostMapping
public ResponseEntity<CustomResponse<PromptResponseDto>> createPrompt(...) {
    PromptResponseDto result = promptFacade.createPrompt(request, authUser.getId());
    return CustomResponseHelper.created(result);
}
```

**개선 효과**:
- ✅ MVC 스타일 API와 일관성 확보
- ✅ 공통 응답 래핑/필터/인터셉터 적용 용이
- ✅ Controller 계층의 복잡도 감소 및 유지보수성 향상

### 3. 도메인 이벤트 패턴 고도화 🟡

- 현재 Spring Events 사용 중이지만, 도메인 이벤트와 인프라 이벤트 명확히 구분
- 도메인 레이어에 이벤트 정의, 인프라 레이어에서 처리
- 예: `CommentCreatedEvent`, `LikeCreatedEvent`를 도메인 이벤트로 정의

### 4. DDD 패턴 적용 심화 🟡

- Aggregate Root 명시적 정의
- Value Object 도입 (예: `Email`, `Tag` 등)
- Domain Service 패턴 적용 (복잡한 도메인 로직 분리)

**우선순위**: ✅ Controller Mono 노출 제거 완료, ✅ 서비스 인터페이스 일관성 확보 완료

[← 목차로 돌아가기](../CODE_IMPROVEMENT_GUIDE.md)



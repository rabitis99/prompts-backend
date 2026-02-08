# Prompt 도메인 개선 계획

## 개요

실서비스 운영 기준으로 Prompt 도메인 코드를 분석하고 개선 계획을 수립한다.

---

## 1. 도메인 엔티티 개선

### 1.1 Prompt 엔티티 - 도메인 로직 강화

**현재 문제점:**
- DTO가 엔티티를 직접 수정 (`PromptUpdateDto.applyTo()`)
- 도메인 로직이 서비스 계층에 분산되어 있음
- 상태 변경 로직이 명확하지 않음

**개선 방안:**

```java
// Prompt.java 개선
public class Prompt extends BaseEntity {
    // ... 필드 ...
    
    // 상태 변경 도메인 로직 (검증은 서비스 계층에서 수행)
    public void update(PromptUpdateCommand command) {
        if (command.hasTitle()) {
            this.title = command.getTitle();
        }
        if (command.hasDescription()) {
            this.description = command.getDescription();
        }
        if (command.hasIsPublic()) {
            this.isPublic = command.getIsPublic();
        }
        if (command.hasCategory()) {
            this.promptCategory = command.getCategory();
        }
    }
    
    // 통계 관련 도메인 로직
    public void incrementViewCount() {
        this.viewCount++;
    }
    
    public void incrementCommentCount() {
        this.commentCount++;
    }
    
    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }
}
```

**판단 근거:**
- 도메인 엔티티는 순수한 비즈니스 로직(상태 변경)만 담당
- 검증 로직은 서비스 계층에서 수행
- 엔티티의 책임을 명확히 분리하여 테스트 용이성 향상

---

## 2. 서비스 계층 개선

### 2.1 PromptServiceImpl - 책임 분리

**현재 문제점:**
- DTO가 엔티티를 직접 수정
- 트랜잭션 경계가 명확하지 않음
- 검증 로직이 분산되어 있음

**개선 방안:**

```java
@Service
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptService {
    
    // ... 의존성 ...
    
    @Override
    @Transactional(readOnly = true)
    public PromptResponseDto getPromptDetail(Long promptId, Long viewerId) {
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));
        
        // 서비스 계층에서 권한 검증 수행
        if (viewerId != null && 
                followBlockPolicy.isBlocked(viewerId, prompt.getAuthor().getId())) {
            throw new ApiException(ErrorCode.PROMPT_BLOCKED_VIEW);
        }
        
        // 조회 이벤트 발행
        promptEventPublisher.publishPromptViewed(promptId, viewerId);
        
        List<Tag> tags = promptTagService.getTags(prompt);
        Long likeCount = likeCountService
                .getPromptLikeCounts(List.of(promptId))
                .getOrDefault(promptId, prompt.getLikeCount());
        
        return PromptResponseDto.from(prompt, tags, likeCount);
    }
    
    @Override
    @Transactional(timeout = 10)
    public PromptResponseDto updatePrompt(Long promptId, PromptUpdateDto dto, Long userId) {
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));
        
        // 서비스 계층에서 권한 검증 수행
        if (!prompt.getAuthor().getId().equals(userId)) {
            throw new ApiException(ErrorCode.PROMPT_FORBIDDEN);
        }
        
        // 서비스 계층에서 데이터 검증 수행
        validateUpdateData(dto);
        
        // Sanitization 수행
        PromptUpdateDto sanitizedDto = promptSanitizationService.sanitize(dto);
        
        // Command 객체로 변환하여 도메인 메서드 호출
        PromptUpdateCommand command = PromptUpdateCommand.from(sanitizedDto);
        prompt.update(command);
        
        // 태그 업데이트
        if (sanitizedDto.getTags() != null) {
            promptTagService.updateTags(prompt, sanitizedDto.getTags());
        }
        
        List<Tag> tags = promptTagService.getTags(prompt);
        return PromptResponseDto.from(prompt, tags);
    }
    
    @Override
    @Transactional(timeout = 10)
    public void deletePrompt(Long promptId, Long userId) {
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));
        
        // 서비스 계층에서 권한 검증 수행
        if (!prompt.getAuthor().getId().equals(userId)) {
            throw new ApiException(ErrorCode.PROMPT_FORBIDDEN);
        }
        
        Long authorId = prompt.getAuthor().getId();
        
        promptTagService.updateTags(prompt, List.of());
        promptRepository.delete(prompt);
        
        promptEventPublisher.publishPromptDeleted(promptId, authorId);
    }
    
    // 서비스 계층 검증 메서드
    private void validateUpdateData(PromptUpdateDto dto) {
        if (dto.getTitle() != null) {
            String title = dto.getTitle().trim();
            if (title.isEmpty() || title.length() > 200) {
                throw new ApiException(ErrorCode.INVALID_INPUT, 
                    "제목은 1자 이상 200자 이하여야 합니다.");
            }
        }
        
        if (dto.getDescription() != null) {
            String desc = dto.getDescription().trim();
            if (desc.length() > 5000) {
                throw new ApiException(ErrorCode.INVALID_INPUT,
                    "설명은 5000자 이하여야 합니다.");
            }
        }
    }
}
```

**판단 근거:**
- 서비스 계층에서 검증 및 권한 체크를 수행
- 도메인 엔티티는 순수한 상태 변경 로직만 담당
- 검증 로직을 서비스에 집중하여 일관성 유지

---

## 3. Command 객체 도입

### 3.1 PromptUpdateCommand 생성

**현재 문제점:**
- DTO가 도메인 엔티티를 직접 수정 (`applyTo()`)
- 도메인과 인프라 계층의 결합도가 높음

**개선 방안:**

```java
// domain/prompt/command/PromptUpdateCommand.java
public class PromptUpdateCommand {
    private final String title;
    private final String description;
    private final Boolean isPublic;
    private final PromptCategory category;
    
    private PromptUpdateCommand(Builder builder) {
        this.title = builder.title;
        this.description = builder.description;
        this.isPublic = builder.isPublic;
        this.category = builder.category;
    }
    
    public static PromptUpdateCommand from(PromptUpdateDto dto) {
        return PromptUpdateCommand.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .isPublic(dto.getIsPublic())
                .category(dto.getPromptCategory())
                .build();
    }
    
    // null 체크 헬퍼 메서드
    public boolean hasTitle() { return title != null; }
    public boolean hasDescription() { return description != null; }
    public boolean hasIsPublic() { return isPublic != null; }
    public boolean hasCategory() { return category != null; }
    
    // getters...
}
```

**판단 근거:**
- DTO와 도메인 엔티티 간 결합도 감소
- 도메인 계층이 인프라 계층(DTO)에 의존하지 않음
- 명시적인 Command 패턴으로 의도 명확화

---

## 4. 트랜잭션 경계 개선

### 4.1 트랜잭션 타임아웃 명시

**현재 문제점:**
- `PromptCreationFlow`에만 타임아웃 설정 (30초)
- 다른 트랜잭션에는 타임아웃이 없음
- AI 호출이 트랜잭션 내부에 포함되어 있음 (실제로는 NOT_SUPPORTED)

**개선 방안:**

```java
// PromptCreationFlow.java
@Transactional(timeout = 30) // 유지
public PromptResponseDto create(PromptRequestDto request, Long userId) {
    // AI 호출은 트랜잭션 외부에서 수행 (현재 구조 유지)
    String aiContent = promptAIService.generateContentSync(sanitizedRequest);
    
    // DB 저장만 트랜잭션 내부
    return promptPersistenceService.savePrompt(sanitizedRequest, userId, aiContent);
}

// PromptPersistenceService.java
@Transactional(timeout = 30) // 명시적 타임아웃 추가
public PromptResponseDto savePrompt(...) {
    // ...
}

// PromptServiceImpl.java
@Transactional(timeout = 10) // 수정/삭제는 짧은 타임아웃
public PromptResponseDto updatePrompt(...) {
    // ...
}

@Transactional(timeout = 10)
public void deletePrompt(...) {
    // ...
}
```

**판단 근거:**
- 실서비스에서 트랜잭션 타임아웃은 필수
- 작업 유형별로 적절한 타임아웃 설정
- 데드락 방지 및 리소스 해제 보장

---

## 5. 이벤트 발행 시점 개선

### 5.1 트랜잭션 커밋 후 이벤트 발행 보장

**현재 문제점:**
- `@TransactionalEventListener` 사용 여부 불명확
- 트랜잭션 커밋 전 이벤트 발행 시 사이드 이펙트 가능성

**개선 방안:**

```java
// PromptEventPublisher.java - 변경 없음 (현재 구조 유지)

// PromptViewedEventListener.java 확인 필요
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handlePromptViewed(PromptEvent.Viewed event) {
    // Redis에 조회수 증가 (트랜잭션 커밋 후 수행)
    // ...
}

// PromptCreatedEventListener.java 확인 필요
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handlePromptCreated(PromptEvent.Created event) {
    // 통계 캐시 무효화 (트랜잭션 커밋 후 수행)
    // ...
}
```

**판단 근거:**
- 트랜잭션 커밋 후 이벤트 처리로 데이터 일관성 보장
- 롤백 시 이벤트 미발행으로 불일치 방지

---

## 6. 성능 최적화

### 6.1 N+1 쿼리 방지

**현재 상태:**
- `CustomPromptRepositoryImpl`에서 fetch join 사용 중 (양호)
- `getPromptDetail`에서 추가 조회 발생 가능성

**개선 방안:**

```java
// PromptRepository.java
@Query("""
    SELECT p FROM Prompt p
    LEFT JOIN FETCH p.promptTags pt
    LEFT JOIN FETCH pt.tag
    WHERE p.id = :promptId
""")
Optional<Prompt> findByIdWithTags(@Param("promptId") Long promptId);

// PromptServiceImpl.java
@Override
@Transactional(readOnly = true)
public PromptResponseDto getPromptDetail(Long promptId, Long viewerId) {
    Prompt prompt = promptRepository.findByIdWithTags(promptId)
            .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));
    
    // fetch join으로 태그를 이미 로드했으므로 추가 조회 불필요
    // 단, promptTagService.getTags()가 캐시를 사용한다면 유지 가능
    // ...
}
```

**판단 근거:**
- 조회 성능 최적화
- 불필요한 쿼리 감소

### 6.2 페이징 최적화

**현재 상태:**
- 2-step 페이징 사용 중 (양호)
- `searchInternal` 메서드에서 fetch join 사용 (양호)

**개선 검토 사항:**
- 대용량 데이터 처리 시 커서 기반 페이징 고려
- 현재 구조 유지 (offset 기반 페이징으로 충분)

---

## 7. 예외 처리 개선

### 7.1 도메인 예외 명확화

**현재 상태:**
- `ApiException` 사용 중 (양호)
- 도메인별 예외 코드 사용 중 (양호)

**개선 방안:**
- 도메인 예외를 도메인 패키지 내부로 이동 검토
- 현재 구조 유지 가능 (전역 예외 처리 구조가 잘 되어 있다면)

---

## 8. 검증 로직 개선

### 8.1 서비스 계층 검증 강화

**현재 문제점:**
- DTO 레벨 검증만 존재
- 서비스 계층 검증 로직이 분산되어 있음

**개선 방안:**

```java
// PromptServiceImpl.java
@Service
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptService {
    
    // ... 의존성 ...
    
    // 서비스 계층에서 검증 수행
    private void validateUpdateData(PromptUpdateDto dto) {
        if (dto.getTitle() != null) {
            String title = dto.getTitle().trim();
            if (title.isEmpty() || title.length() > 200) {
                throw new ApiException(ErrorCode.INVALID_INPUT, 
                    "제목은 1자 이상 200자 이하여야 합니다.");
            }
        }
        
        if (dto.getDescription() != null) {
            String desc = dto.getDescription().trim();
            if (desc.length() > 5000) {
                throw new ApiException(ErrorCode.INVALID_INPUT,
                    "설명은 5000자 이하여야 합니다.");
            }
        }
    }
    
    private void validateUpdatePermission(Prompt prompt, Long userId) {
        if (!prompt.getAuthor().getId().equals(userId)) {
            throw new ApiException(ErrorCode.PROMPT_FORBIDDEN);
        }
    }
}
```

**판단 근거:**
- DTO 검증은 입력 형식 검증 (Bean Validation)
- 서비스 계층 검증은 비즈니스 규칙 검증
- 검증 로직을 서비스 계층에 집중하여 일관성 유지

---

## 9. 보안 개선

### 9.1 권한 검증 일관성

**현재 상태:**
- 서비스 계층에서 권한 검증 수행 (적절함)

**개선 방안:**
- 권한 검증 로직을 서비스 계층에 유지
- 검증 메서드를 서비스 내부에 명시적으로 분리하여 가독성 향상
- `validateUpdatePermission`, `validateDeletePermission` 등 헬퍼 메서드 추가

---

## 10. 테스트 가능성 개선

### 10.1 도메인 로직 단위 테스트

**개선 방안:**
- 도메인 엔티티의 비즈니스 로직을 독립적으로 테스트 가능하도록 구조 개선
- Command 객체 도입으로 테스트 데이터 생성 용이

---

## 구현 우선순위

1. **High Priority**
   - Command 객체 도입 (DTO-도메인 결합도 감소)
   - Prompt 엔티티에 도메인 로직 이동
   - 트랜잭션 타임아웃 명시

2. **Medium Priority**
   - 서비스 계층 검증 로직 강화 및 정리
   - N+1 쿼리 최적화

3. **Low Priority**
   - 이벤트 리스너 확인 및 개선
   - 예외 처리 구조 개선

---

## 참고사항

- 기존 동작을 변경하지 않는 범위에서 개선
- 점진적 리팩토링으로 사이드 이펙트 최소화
- 각 개선 사항은 독립적으로 적용 가능하도록 설계


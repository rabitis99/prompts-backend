# 프롬프트 가이드라인 시스템 도메인별 분화 리팩토링

## Context

현재 프롬프트 설계에서 역할/원칙/출력 규칙을 모든 작업에 일괄 적용하고 있어,
작업 유형에 따라 출력 품질과 적합성이 저하되는 문제가 발생함.

특히 창작/카피라이팅/시 창작 같은 감성/비정형 작업에서
"정확성 우선", "모호한 표현 금지", "인사말 금지" 등 기술 문서 중심의 원칙이 강제되어
결과물이 무력화되는 현상이 확인됨.

**목표**: TaskDomain을 도입하여 PromptCategory별로 적합한 원칙/규칙/품질기준/출력제약을 적용하고,
기존 하드코딩된 가이드라인을 도메인 특화 가이드라인으로 교체한다.

---

## 핵심 설계 결정

### 1. TaskDomain enum (6개 도메인)

| TaskDomain | 대상 PromptCategory | 성격 |
|---|---|---|
| TECHNICAL | DEVELOPMENT, CODING, PROGRAMMING | 정확성/실용성 중심 기술 작업 |
| CREATIVE | CREATIVE, CONTENT, WRITING, DESIGN | 독창성/감성 중심 창작 작업 |
| ANALYTICAL | ANALYSIS, RESEARCH | 증거 기반 체계적 분석 |
| PRACTICAL | PRODUCTIVITY, BUSINESS, MARKETING | 즉시 활용 가능한 실무 |
| EDUCATIONAL | STUDY, EDUCATION | 이해 촉진/단계적 학습 |
| GENERAL | ETC | **보수적 안전 모드** (아래 참고) |

### 2. GENERAL = "보수적 안전 모드"

GENERAL은 "무색무취 임시 수용소"가 아니라 **안전한 기본값**:
- 구조는 허용하되 강제하지 않음
- 감성 표현 허용하되 과도한 것은 지양
- 설명/창작 모두 완전히 금지하지 않음
- 어떤 작업이든 최소한 합리적인 결과를 보장하는 "세이프 모드"

### 3. RuleLevel 도입

```java
enum RuleLevel { HARD, SOFT }
```

- **HARD**: 위반 시 품질 실패로 간주 (예: TECHNICAL에서 "추측 금지")
- **SOFT**: 가능하면 준수 (예: "서론 최소화")

### 4. Domain은 "기본값", Override 가능 구조

TaskDomain이 또 다른 족쇄가 되지 않도록 **확장 포인트를 열어둔다**.

```java
// 확장 포인트 인터페이스 — 지금은 TaskDomain이 직접 구현
interface GuidelinePolicy {
    List<GuidelineRule> principles();
    List<GuidelineRule> structuringRules();
    List<GuidelineRule> qualityStandards();
    List<GuidelineRule> outputConstraints();
}

// TaskDomain은 GuidelinePolicy를 구현
enum TaskDomain implements GuidelinePolicy { ... }

// 향후 Override 가능 (지금은 구현 안 함, 구조만 확보)
// GuidelinePolicy finalPolicy =
//     PolicyOverride.apply(basePolicy, roleType, actionType, toneType);
```

**핵심**: 지금은 TaskDomain = GuidelinePolicy 직접 구현.
나중에 "CREATIVE인데 분석적인 글", "TECHNICAL인데 감성적 설명" 같은 요구가 오면
PolicyOverride 레이어만 끼워넣으면 됨. 구조 변경 없이 확장 가능.

### 5. Category는 힌트, ActionType이 최종 결정권

PromptCategory → TaskDomain 매핑은 **"대표값(defaultDomain)"**이지 고정이 아님.

실제 예시:
- WRITING(기술 문서 작성) → ActionType이 TECHNICAL로 override
- CONTENT(SEO 분석 보고서) → ActionType이 ANALYTICAL로 override

```java
// 도메인 결정 우선순위 (상세 구현은 PromptGuidelineBuilder.resolveDomain() 참고)
// 1순위: ActionType.getTaskDomain() — Optional.of(X)이면 명시적 지정
// 2순위: PromptCategory.getDefaultDomain() — 대표값 힌트
// 3순위: GENERAL 폴백 (의도적 vs 미매핑 구분 — Optional.empty() = 미매핑)
```

**Category는 힌트, Action은 의도** — 이렇게 안 하면 카테고리 재분류 지옥이 온다.

### 6. 미매핑 ActionType은 "의도적 폴백" + 로깅

침묵 실패 금지. **의도적 GENERAL vs 매핑 누락을 `Optional<TaskDomain>`으로 구분**한다.

- `Optional.of(GENERAL)` → 의도적 범용 (로그 불필요)
- `Optional.empty()` → 매핑 누락 가능 (warn 로그 + isFallback=true)

상세 구현은 **설계 결정 10번** 및 **Phase 4의 `resolveDomain()` 참고**.

빌드 타임 강제:
```java
// ActionTypeTaskDomainTest
// 모든 ActionType이 getTaskDomain()을 오버라이드했는지 검증
// Optional.empty()인 ActionType이 0개여야 함 → 새 ActionType 추가 시 미구현이면 테스트 실패
```

### 8. GuidelineRule에 id 필드 추가 — 규칙 추적/분석 기반 확보

RuleLevel만으로는 "어떤 규칙이 얼마나 적용/무시되었는지" 추적이 불가능함.
규칙에 고유 식별자를 부여하면 향후 **규칙별 override, A/B 테스트, 적용 통계** 기반이 된다.

```java
record GuidelineRule(
    String id,           // e.g. "TECH.PRINCIPLE.NO_GUESS", "CREATIVE.ANTI.NO_FORCED_CONCLUSION"
    I18nText title,
    I18nText description,
    RuleLevel level,
    RuleType type        // 아래 9번 참고
) {}
```

**id 네이밍 컨벤션**: `{DOMAIN}.{SECTION}.{RULE_NAME}`
- `TECH.PRINCIPLE.ACCURACY` — TECHNICAL 도메인, 핵심 원칙, 정확성
- `CREATIVE.ANTI.NO_FORCED_COMPLETION` — CREATIVE 도메인, 반-규칙, 완전성 강제 금지
- `GENERAL.OUTPUT.MINIMAL_GREETING` — GENERAL 도메인, 출력 제약, 인사말 최소화

**지금은 로깅/디버깅용**, 나중에 per-rule override나 사용자 커스터마이징 기반이 됨.

### 9. RuleType enum — REQUIRE / FORBID / ALLOW

RuleLevel(HARD/SOFT)은 **강도**만 표현, **방향**을 표현 못 함.
CREATIVE의 "완전성 요구 금지"가 `HARD`인데 이게 "반드시 지켜라"인지 "반드시 하지 마라"인지 혼동.

```java
enum RuleType {
    REQUIRE,  // ~해야 한다  (TECHNICAL: "정확성 우선")
    FORBID,   // ~하지 마라  (CREATIVE: "완전성 강제 금지", "구조 강제 금지")
    ALLOW     // ~해도 된다  (CREATIVE: "모호한 표현 허용", GENERAL: "짧은 인사 허용")
}
```

**RuleLevel × RuleType 조합 해석:**

| RuleType | HARD | SOFT |
|---|---|---|
| REQUIRE | 반드시 해야 함 (위반 = 품질 실패) | 가능하면 해야 함 |
| FORBID | 절대 하면 안 됨 | 가능하면 하지 않는 게 좋음 |
| ALLOW | 명시적으로 허용 (다른 규칙이 금지해도 이 도메인에서는 허용) | 허용하되 남용 지양 |

**CREATIVE 반-규칙 재해석 (Before → After):**
- Before: `완전성 요구 금지 | HARD` — "HARD인데 금지?" 혼란
- After: `완전성 강제 금지 | FORBID | HARD` — "완전성 강제를 절대 하지 마라" 명확

### 10. ActionType GENERAL 반환의 의미 모호성 해소

현재 `getDefaultTaskDomain()`이 GENERAL을 반환하면 두 가지 의미가 섞임:
- **의도적 GENERAL**: EtcActionType처럼 진짜 범용 작업
- **미지정**: 새 ActionType 추가 시 매핑을 깜빡한 경우

`Optional<TaskDomain>`으로 구분한다:

```java
// ActionTypeInterface
default Optional<TaskDomain> getTaskDomain() {
    return Optional.empty();  // 미지정 (= 매핑 누락 가능)
}

// EtcActionType — 의도적 GENERAL
@Override
public Optional<TaskDomain> getTaskDomain() {
    return Optional.of(TaskDomain.GENERAL);  // 명시적
}

// DevelopmentActionType — 명시적 TECHNICAL
@Override
public Optional<TaskDomain> getTaskDomain() {
    return Optional.of(TaskDomain.TECHNICAL);
}
```

**resolveDomain() 업데이트:**
```java
TaskDomain resolveDomain(InputRequestDto request) {
    Optional<TaskDomain> fromAction = request.getActionType().getTaskDomain();

    // 1순위: ActionType이 명시적으로 지정한 도메인
    if (fromAction.isPresent() && fromAction.get() != TaskDomain.GENERAL) {
        return fromAction.get();
    }

    // 2순위: PromptCategory 기본 도메인
    TaskDomain fromCategory = request.getPromptCategory().getDefaultDomain();
    if (fromCategory != TaskDomain.GENERAL) {
        return fromCategory;
    }

    // 3순위: ActionType이 명시적 GENERAL인 경우
    if (fromAction.isPresent()) {
        return TaskDomain.GENERAL;  // 의도적 — 로그 불필요
    }

    // 폴백: 진짜 미매핑 — 경고 로그
    log.warn("Unmapped ActionType: {} (category={}) — GENERAL fallback. 매핑 추가 필요.",
             request.getActionType().name(), request.getPromptCategory());
    return TaskDomain.GENERAL;
}
```

**빌드 타임 테스트도 강화:**
```java
// ActionTypeTaskDomainTest
// Optional.empty()인 ActionType이 0개인지 검증
// → 새 ActionType 추가 시 getTaskDomain() 미구현이면 테스트 실패
```

### 11. GuidelineRenderer 분리 — 정책과 포매팅의 책임 분리

현재 `KoreanGuidelineBuilder`가 **두 가지 책임**을 동시에 짊어짐:
1. 어떤 규칙을 적용할지 결정 (정책)
2. 규칙을 텍스트로 포매팅 (렌더링)

이를 분리한다:

```
[현재]
PromptGuidelineBuilder (resolveDomain + 조합)
  └─ KoreanGuidelineBuilder (규칙 선택 + 한국어 포매팅)

[변경 후]
PromptGuidelineBuilder (resolveDomain + 조합)
  └─ GuidelineRenderer (규칙 → 텍스트 변환만 담당)
       └─ KoreanGuidelineRenderer
       └─ EnglishGuidelineRenderer
       └─ JapaneseGuidelineRenderer
```

```java
// 렌더링만 담당 — "어떤 규칙"은 모름, "어떻게 표현"만 앎
interface GuidelineRenderer {
    String renderPrinciples(List<GuidelineRule> rules);
    String renderStructuringRules(List<GuidelineRule> rules);
    String renderQualityStandards(List<GuidelineRule> rules);
    String renderOutputConstraints(List<GuidelineRule> rules);
}

// PromptGuidelineBuilder — 정책 결정 + 렌더러 위임
public final String build(String basePrompt, InputRequestDto request) {
    TaskDomain domain = resolveDomain(request);
    GuidelineRenderer renderer = rendererFactory.getRenderer(request.getLanguage());

    return renderer.renderPrinciples(domain.principles())
            + "\n\n" + basePrompt
            + "\n\n" + renderer.renderStructuringRules(domain.structuringRules())
            + "\n\n" + renderer.renderQualityStandards(domain.qualityStandards())
            + "\n\n" + renderer.renderOutputConstraints(domain.outputConstraints());
}
```

**효과:**
- `PromptGuidelineBuilder`가 더 이상 abstract class가 아님 → 3개 언어 서브클래스 제거 가능
- 새 언어 추가 시 Renderer만 추가 (정책 로직 무관)
- RuleType/RuleLevel에 따른 렌더링 차이도 Renderer 내부에서 처리
- 테스트 용이: 정책 테스트와 렌더링 테스트 독립 수행

### 12. GENERAL 도메인의 한계 명시적 인정

GENERAL이 "보수적 안전 모드"라는 것만으로는 부족.
**"나는 전문가가 아니다"를 명시적으로 인정**하는 메타-규칙이 필요.

GENERAL 도메인 가이드라인에 다음을 추가:

```java
// GeneralGuidelines.java — 도메인 한계 인정 원칙
GuidelineRule.of(
    "GENERAL.META.LIMITATION_ACK",
    I18nText.of(
        "도메인 비특화 안내",
        "Domain Non-Specialization Notice",
        "ドメイン非特化案内"
    ),
    I18nText.of(
        "이 응답은 범용 모드로 생성되었습니다. 특정 분야의 전문적 깊이가 필요한 경우, "
        + "보다 적합한 카테고리와 작업 유형을 선택하면 더 나은 결과를 얻을 수 있습니다.",
        "This response was generated in general-purpose mode. For domain-specific depth, "
        + "selecting a more appropriate category and action type may yield better results.",
        "この応答は汎用モードで生成されました。特定分野の専門的な深さが必要な場合、"
        + "より適切なカテゴリとアクションタイプを選択するとより良い結果が得られます。"
    ),
    RuleLevel.SOFT,
    RuleType.ALLOW  // 렌더링 시 포함 여부는 Renderer가 판단
)
```

**렌더링 조건:** GENERAL 폴백으로 떨어진 경우에만 출력 (ActionType/Category가 명시적으로 GENERAL인 경우는 출력하지 않음 — 사용자가 의도한 것이므로).
이 조건은 `resolveDomain()`에서 반환하는 정보에 "폴백 여부" 플래그를 포함시켜 처리.

```java
// DomainResolution — resolveDomain()의 반환 타입 확장
record DomainResolution(TaskDomain domain, boolean isFallback) {}
```

---

### 7. 도메인별 가이드라인은 별도 클래스로 분리

TaskDomain enum 비대화 방지. Git diff 지옥과 협업 충돌을 막는다.

```
domain/prompt/enums/
    TaskDomain.java              ← enum 자체는 가볍게
    guideline/
        GuidelinePolicy.java     ← 인터페이스
        GuidelineRule.java       ← record
        I18nText.java            ← record
        RuleLevel.java           ← enum
        TechnicalGuidelines.java ← TECHNICAL 도메인 데이터
        CreativeGuidelines.java  ← CREATIVE 도메인 데이터
        AnalyticalGuidelines.java
        PracticalGuidelines.java
        EducationalGuidelines.java
        GeneralGuidelines.java
```

TaskDomain은 참조만:
```java
enum TaskDomain implements GuidelinePolicy {
    TECHNICAL(TechnicalGuidelines.INSTANCE),
    CREATIVE(CreativeGuidelines.INSTANCE),
    ...;
    private final GuidelinePolicy delegate;
}
```

---

## 파일 변경 목록

### Phase 1: 가이드라인 인프라 신규 생성 (14개 파일)

| 파일 | 설명 |
|---|---|
| `enums/guideline/GuidelinePolicy.java` | 정책 인터페이스 (확장 포인트) |
| `enums/guideline/GuidelineRule.java` | 규칙 record (String id, I18nText title, I18nText desc, RuleLevel level, RuleType type) |
| `enums/guideline/I18nText.java` | 다국어 텍스트 record (ko, en, ja) |
| `enums/guideline/RuleLevel.java` | HARD / SOFT enum |
| `enums/guideline/RuleType.java` | REQUIRE / FORBID / ALLOW enum |
| `enums/guideline/DomainResolution.java` | record (TaskDomain domain, boolean isFallback) |
| `enums/guideline/TechnicalGuidelines.java` | TECHNICAL 도메인 가이드라인 데이터 |
| `enums/guideline/CreativeGuidelines.java` | CREATIVE 도메인 가이드라인 데이터 |
| `enums/guideline/AnalyticalGuidelines.java` | ANALYTICAL 도메인 가이드라인 데이터 |
| `enums/guideline/PracticalGuidelines.java` | PRACTICAL 도메인 가이드라인 데이터 |
| `enums/guideline/EducationalGuidelines.java` | EDUCATIONAL 도메인 가이드라인 데이터 |
| `enums/guideline/GeneralGuidelines.java` | GENERAL 도메인 가이드라인 데이터 (한계 인정 메타-규칙 포함) |
| `enums/TaskDomain.java` | 6개 도메인 enum, GuidelinePolicy 구현 (delegate) |
| `service/guideline/GuidelineRenderer.java` | 규칙 → 텍스트 변환 인터페이스 |
| `service/guideline/KoreanGuidelineRenderer.java` | 한국어 렌더링 구현 |
| `service/guideline/EnglishGuidelineRenderer.java` | 영어 렌더링 구현 |
| `service/guideline/JapaneseGuidelineRenderer.java` | 일본어 렌더링 구현 |
| `service/guideline/GuidelineRendererFactory.java` | LanguageType → Renderer 매핑 팩토리 |

### Phase 2: 기존 enum 수정 (5개 파일)

| 파일 | 변경 |
|---|---|
| `PromptCategory.java` | `defaultDomain` 필드 추가 (TaskDomain, **대표값**) |
| `ActionTypeInterface.java` | `getTaskDomain()` → `Optional<TaskDomain>` 디폴트 메서드 추가 (empty = 미지정) |
| `SortType.java` | `name` → `displayName` (Enum.name() shadowing 해소, 즉시 수정) |
| `ToneType.java` | 필드 순서 `En→Ko→Ja` → `Ko→En→Ja` 통일 |
| `StyleType.java` | 필드 순서 `En→Ko→Ja` → `Ko→En→Ja` 통일 |

### Phase 3: ActionType 28개에 `getTaskDomain()` 오버라이드

**PromptCategory 매핑 없는 13개:**

| ActionType | getTaskDomain() |
|---|---|
| AiMlActionType | TECHNICAL |
| CloudServicesActionType | TECHNICAL |
| DevOpsActionType | TECHNICAL |
| CybersecurityActionType | TECHNICAL |
| CustomerSupportActionType | PRACTICAL |
| EmailActionType | PRACTICAL |
| CareerActionType | PRACTICAL |
| SocialActionType | PRACTICAL |
| PersonalDevelopmentActionType | EDUCATIONAL |
| HealthFitnessActionType | GENERAL |
| LifestyleActionType | GENERAL |
| RecommendationActionType | GENERAL |
| ShoppingActionType | GENERAL |

**PromptCategory 매핑 있는 15개 (일관성 위해 추가):**

| ActionType | getTaskDomain() |
|---|---|
| ProductivityActionType | PRACTICAL |
| DevelopmentActionType | TECHNICAL |
| CodingActionType | TECHNICAL |
| ProgrammingActionType | TECHNICAL |
| AnalysisActionType | ANALYTICAL |
| MarketingActionType | PRACTICAL |
| ContentActionType | CREATIVE |
| CreativeActionType | CREATIVE |
| StudyActionType | EDUCATIONAL |
| EducationActionType | EDUCATIONAL |
| ResearchActionType | ANALYTICAL |
| BusinessActionType | PRACTICAL |
| DesignActionType | CREATIVE |
| WritingActionType | CREATIVE |
| EtcActionType | GENERAL |

### Phase 4: GuidelineBuilder → Renderer 리팩토링 (기존 4개 파일 수정 + 4개 신규)

**`PromptGuidelineBuilder.java`** — abstract class → 일반 class로 변경, Renderer에 위임

```java
// 더 이상 abstract class가 아님 — 3개 언어 서브클래스 제거
@RequiredArgsConstructor
public class PromptGuidelineBuilder {
    private final GuidelineRendererFactory rendererFactory;

    public String build(String basePrompt, InputRequestDto request) {
        DomainResolution resolution = resolveDomain(request);
        GuidelineRenderer renderer = rendererFactory.getRenderer(request.getLanguage());
        TaskDomain domain = resolution.domain();

        String guidelines = renderer.renderPrinciples(domain.principles())
                + "\n\n" + basePrompt
                + "\n\n" + renderer.renderStructuringRules(domain.structuringRules())
                + "\n\n" + renderer.renderQualityStandards(domain.qualityStandards())
                + "\n\n" + renderer.renderOutputConstraints(domain.outputConstraints());

        // GENERAL 폴백인 경우 한계 인정 문구 추가
        if (resolution.isFallback()) {
            guidelines += "\n\n" + renderer.renderFallbackNotice(domain);
        }
        return guidelines;
    }

    /**
     * 도메인 결정 우선순위 (Optional 기반):
     * 1순위 — ActionType이 명시적으로 지정한 도메인 (GENERAL이 아닌 경우)
     * 2순위 — PromptCategory의 기본 도메인
     * 3순위 — ActionType이 명시적 GENERAL (의도적 — 로그 불필요)
     * 폴백  — 진짜 미매핑 → GENERAL + 경고 로그 + isFallback=true
     */
    private DomainResolution resolveDomain(InputRequestDto request) {
        Optional<TaskDomain> fromAction = request.getActionType().getTaskDomain();

        if (fromAction.isPresent() && fromAction.get() != TaskDomain.GENERAL) {
            return new DomainResolution(fromAction.get(), false);
        }

        TaskDomain fromCategory = request.getPromptCategory().getDefaultDomain();
        if (fromCategory != TaskDomain.GENERAL) {
            return new DomainResolution(fromCategory, false);
        }

        if (fromAction.isPresent()) {
            return new DomainResolution(TaskDomain.GENERAL, false); // 의도적
        }

        log.warn("Unmapped ActionType: {} (category={}) — GENERAL fallback. 매핑 추가 필요.",
                 request.getActionType().name(), request.getPromptCategory());
        return new DomainResolution(TaskDomain.GENERAL, true); // 미매핑 폴백
    }
}
```

**신규 `GuidelineRenderer.java`** — 규칙 → 텍스트 변환 인터페이스
```java
interface GuidelineRenderer {
    String renderPrinciples(List<GuidelineRule> rules);
    String renderStructuringRules(List<GuidelineRule> rules);
    String renderQualityStandards(List<GuidelineRule> rules);
    String renderOutputConstraints(List<GuidelineRule> rules);
    String renderFallbackNotice(TaskDomain domain);
}
```

**신규 `Korean/English/JapaneseGuidelineRenderer.java`** — 언어별 렌더링
- RuleLevel.HARD → "절대 위반 불가" / "Must never violate" / "絶対に違反不可"
- RuleLevel.SOFT → "가능하면 준수" / "Follow when possible" / "可能な限り遵守"
- RuleType.REQUIRE → "~해야 한다" / RuleType.FORBID → "~하지 마라" / RuleType.ALLOW → "~해도 된다"

**신규 `GuidelineRendererFactory.java`** — LanguageType → Renderer 매핑

**삭제:** `KoreanGuidelineBuilder.java`, `EnglishGuidelineBuilder.java`, `JapaneseGuidelineBuilder.java`
(각 Renderer로 대체됨. `GuidelineBuilderFactory.java`는 `GuidelineRendererFactory`로 대체.)

### Phase 5: 테스트 (신규 2개)

| 파일 | 설명 |
|---|---|
| `TaskDomainTest.java` | 각 도메인 가이드라인 비어있지 않음 + I18nText null 검증 |
| `ActionTypeTaskDomainTest.java` | 리플렉션으로 모든 ActionType 스캔, getDefaultTaskDomain() != null 강제 |

---

## 도메인별 가이드라인 상세 (Ko 기준, En/Ja는 번역)

### TECHNICAL

**핵심 원칙**
| # | 이름 | 설명 | Level |
|---|---|---|---|
| 1 | 정확성 우선 | 추측·가정·불확실한 정보 제공 금지. 검증된 사실과 즉시 활용 가능한 정보만 제공 | HARD |
| 2 | 맥락 준수 | 사용자 요청 범위를 정확히 이해하고 그 범위 내에서만 응답. 불필요한 확장 금지 | HARD |
| 3 | 실용성 중심 | 이론보다 실전 적용 가능한 내용 우선. 구체적이고 실행 가능한 지침 제공 | SOFT |

**응답 구조화 규칙**
| # | 규칙 | Level |
|---|---|---|
| 1 | 논리적 구성: 명확한 제목, 하위 섹션, 단계별 구분 사용 | HARD |
| 2 | 가독성 최우선: 목록, 표, 코드 블록 등 적절한 형식 활용 | SOFT |
| 3 | 정보 계층화: 중요도에 따른 정보 배치 (핵심 → 세부사항) | SOFT |
| 4 | 간결성: 불필요한 반복이나 장황한 설명 지양 | SOFT |

**응답 품질 기준**
| # | 이름 | 설명 | Level |
|---|---|---|---|
| 1 | 명확성 | 모호한 표현·추상적 설명 금지. 구체적이고 명확한 용어 사용 | HARD |
| 2 | 실용성 | 실전 적용 방법 우선. 단계별 실행 가능한 지침, 샘플 코드/템플릿 포함 권장 | SOFT |
| 3 | 완전성 | 후속 질문 없이 자급자족하는 완전한 응답 제공 | SOFT |

**출력 형식 제약**
| # | 제약 | Level |
|---|---|---|
| 1 | 인사말 금지: 불필요한 인사말 사용 금지 | HARD |
| 2 | 서론 최소화: 핵심 내용으로 바로 진입 | SOFT |
| 3 | 마무리 간결: 불필요한 마무리 문구나 요약 지양 | SOFT |
| 4 | 직접적 표현: 간접적 표현보다 직접적 지시 사용 | SOFT |

---

### CREATIVE

**핵심 원칙**
| # | 이름 | 설명 | Level |
|---|---|---|---|
| 1 | 독창성 추구 | 기존 틀에 얽매이지 않는 새로운 관점과 표현을 적극 탐색. 창의적 도약과 예상 밖의 연결 장려 | HARD |
| 2 | 감성적 공명 | 독자/사용자의 감정과 공감을 불러일으키는 표현 지향. 논리적 설명보다 감각적 체험 우선 | HARD |
| 3 | 맥락적 자유 | 작품 의도와 분위기에 맞는 표현이라면 모호함·은유·비유 적극 허용. 주제와 분위기에 자연스럽게 부합하는 흐름 추구 | SOFT |

**의도적 불완전성 허용 (CREATIVE 전용 반-규칙)**

CREATIVE 도메인에서는 다음 항목을 **명시적으로 금지하지 않는다** — TECHNICAL과의 철저한 분리.
`RuleType.FORBID`로 표기하여 "이것을 강제하는 행위를 금지"함을 명확히 함:

| # | 반-규칙 (id) | 설명 | RuleType | Level |
|---|---|---|---|---|
| 1 | `CREATIVE.ANTI.NO_FORCED_COMPLETION` | 명확한 결론이 없더라도 감정 전달이 충분하면 허용 | FORBID | HARD |
| 2 | `CREATIVE.ANTI.NO_FORCED_CONCLUSION` | 질문으로 끝나는 응답, 열린 결말 허용 | FORBID | HARD |
| 3 | `CREATIVE.ANTI.NO_FORCED_MESSAGE` | 의미가 열려 있는 표현, 다중 해석 가능한 표현 허용 | FORBID | HARD |
| 4 | `CREATIVE.ANTI.NO_FORCED_STRUCTURE` | 목록·표·단계별 구분 등 기술적 구조화를 강요하지 않음 | FORBID | HARD |

**응답 구조화 규칙**
| # | 규칙 | Level |
|---|---|---|
| 1 | 자연스러운 흐름: 기계적 단계 구분보다 내용의 자연스러운 전개와 호흡 우선 | HARD |
| 2 | 감각적 표현 장려: 추상적 개념도 구체적 이미지·비유·감각적 묘사로 전달 | SOFT |
| 3 | 분위기 일관성: 작품 전체의 톤·분위기·정서적 색채의 통일감 유지 | HARD |
| 4 | 여백의 활용: 모든 것을 설명하지 않고, 독자의 상상력이 채울 공간을 남김 | SOFT |

**응답 품질 기준**
| # | 이름 | 설명 | Level |
|---|---|---|---|
| 1 | 몰입감 | 독자가 작품 세계에 자연스럽게 빠져들 수 있는 생생한 장면과 감정 묘사 | HARD |
| 2 | 독창성 | 진부한 표현·클리셰 회피. 새로운 시각, 예상 밖의 전개, 신선한 비유 활용 | HARD |
| 3 | 정서적 울림 | 단순한 정보 전달을 넘어 감동·여운·성찰을 불러일으키는 깊이 | SOFT |

**출력 형식 제약**
| # | 제약 | Level |
|---|---|---|
| 1 | 형식 자유: 창작물의 장르와 의도에 따라 자유로운 형식 허용. 강제 템플릿 없음 | HARD |
| 2 | 도입부 자유: 서정적·묘사적·대화적 시작 모두 허용 (작품 분위기에 맞다면) | SOFT |
| 3 | 마무리 여운: 깔끔한 종결보다 독자에게 여운과 생각거리를 남기는 열린 마무리 권장 | SOFT |

---

### ANALYTICAL

**핵심 원칙**
| # | 이름 | 설명 | Level |
|---|---|---|---|
| 1 | 증거 기반 | 모든 주장과 결론은 데이터·사례·논거로 뒷받침. 근거 없는 주장이나 개인적 의견 배제 | HARD |
| 2 | 다각적 관점 | 단일 시각에 치우치지 않고 복수의 관점에서 현상 분석. 상반된 의견이나 대안적 해석도 함께 제시 | HARD |
| 3 | 체계적 사고 | 분석 과정을 명확한 프레임워크와 방법론에 따라 수행. 가설→검증→결론의 논리적 흐름 유지 | SOFT |

**응답 구조화 규칙**
| # | 규칙 | Level |
|---|---|---|
| 1 | 논리적 구조: 서론(배경/목적) → 본론(분석/근거) → 결론(종합/제언)의 명확한 구조 | HARD |
| 2 | 데이터 시각화: 수치·비교·추이는 표·차트 설명·요약 통계를 활용하여 제시 | SOFT |
| 3 | 인과관계 명시: 원인과 결과의 관계를 명확히 구분, 상관관계와 혼동 금지 | HARD |
| 4 | 한계 인정: 분석의 제한사항, 데이터의 한계, 추가 검증 필요 영역을 솔직히 명시 | SOFT |

**응답 품질 기준**
| # | 이름 | 설명 | Level |
|---|---|---|---|
| 1 | 논리적 엄밀성 | 추론 과정에 논리적 비약이나 오류가 없어야 함. 전제→결론의 논증이 타당해야 함 | HARD |
| 2 | 객관성 | 편향 없는 중립적 분석 수행. 사실과 해석을 명확히 구분 | HARD |
| 3 | 실행 가능성 | 분석 결과에서 구체적 행동 방안이나 의사결정 근거를 도출. 단순 현황 기술에 그치지 않음 | SOFT |

**출력 형식 제약**
| # | 제약 | Level |
|---|---|---|
| 1 | 인사말 금지 | HARD |
| 2 | 출처 명시: 참조 데이터·이론·프레임워크의 출처를 가능한 한 명시 | SOFT |
| 3 | 핵심 요약 선행: 결론/핵심 인사이트를 서두에 배치, 상세 분석은 이후 전개 | HARD |
| 4 | 정량/정성 구분: 정량적 데이터와 정성적 해석을 명확히 분리 표기 | SOFT |

---

### PRACTICAL

**핵심 원칙**
| # | 이름 | 설명 | Level |
|---|---|---|---|
| 1 | 즉시 활용 가능 | 바로 업무에 적용할 수 있는 실행 방안 제공. 복사하여 바로 사용 가능한 수준의 구체성 | HARD |
| 2 | 효율성 지향 | 시간과 자원의 최적 활용 지향. 불필요한 절차나 과도한 부연 설명 지양 | SOFT |
| 3 | 결과 중심 | 과정보다 성과와 결과물에 초점. 구체적 산출물·지표·달성 목표 명시 | HARD |

**응답 구조화 규칙**
| # | 규칙 | Level |
|---|---|---|
| 1 | 체크리스트 형식: 실행 항목을 체크리스트나 단계별 목록으로 구조화 | SOFT |
| 2 | 우선순위 표기: 작업의 중요도·긴급도를 명시하여 실행 순서 안내 | SOFT |
| 3 | 템플릿 제공: 문서·이메일·보고서는 바로 사용 가능한 템플릿 형태로 제시 | SOFT |
| 4 | 기대 효과 명시: 각 행동 방안의 예상 결과나 기대 효과를 구체적으로 제시 | SOFT |

**응답 품질 기준**
| # | 이름 | 설명 | Level |
|---|---|---|---|
| 1 | 실행 가능성 | 현실적 제약(예산·인력·시간) 고려한 실현 가능한 방안 제시 | HARD |
| 2 | 명확성 | 실행 주체·시기·방법이 모호하지 않게 구체적 서술. "적절히" 같은 모호한 수식어 최소화 | HARD |
| 3 | 완결성 | 추가 확인 없이 실행에 옮길 수 있는 자급자족적 응답 | SOFT |

**출력 형식 제약**
| # | 제약 | Level |
|---|---|---|
| 1 | 인사말 금지 | HARD |
| 2 | 서론 최소화: 핵심 실행 사항으로 바로 진입 | SOFT |
| 3 | 마무리 → Next Steps: 불필요한 마무리 대신 다음 단계 명시 | SOFT |
| 4 | 직접적 표현 | SOFT |

---

### EDUCATIONAL

**핵심 원칙**
| # | 이름 | 설명 | Level |
|---|---|---|---|
| 1 | 이해 촉진 | 새로운 개념을 학습자의 기존 지식과 연결. 유추·비유·실생활 예시 적극 활용 | HARD |
| 2 | 단계적 심화 | 기초→심화로 자연스럽게 이행하는 점진적 학습 경로. 각 단계에서 선행 지식 확인 | HARD |
| 3 | 능동적 참여 유도 | 단순 정보 전달보다 학습자의 사고를 자극하는 질문·연습·자기 점검 요소 포함 | SOFT |

**응답 구조화 규칙**
| # | 규칙 | Level |
|---|---|---|
| 1 | 개념 선행: 핵심 개념/용어를 먼저 정의한 후 상세 설명 진입 | HARD |
| 2 | 예시 풍부: 각 개념마다 구체적 예시·비유·실습 과제 병기 | SOFT |
| 3 | 요약 및 복습: 주요 섹션 끝에 Key Takeaways 배치 | SOFT |
| 4 | 난이도 표기: 내용의 난이도(기초/중급/심화) 명시 | SOFT |

**응답 품질 기준**
| # | 이름 | 설명 | Level |
|---|---|---|---|
| 1 | 정확성 | 교육 내용의 사실적 정확성 보장. 오개념으로 학습자 오도 금지 | HARD |
| 2 | 접근성 | 학습자 수준에 맞는 언어와 설명 깊이. 전문 용어는 반드시 풀어서 설명 | HARD |
| 3 | 동기 부여 | 학습의 실용적 가치와 활용 방안을 제시하여 "왜 배우는가" 동기 부여 | SOFT |

**출력 형식 제약**
| # | 제약 | Level |
|---|---|---|
| 1 | 친근한 도입 허용: 학습 동기를 유발하는 간결한 도입부 허용 (과도한 인사말은 지양) | SOFT |
| 2 | 격려의 마무리: 학습자를 격려하는 간결한 마무리 문구 허용 | SOFT |
| 3 | 학습 행동 유도: "~해보세요", "~를 확인하세요" 등 직접적 학습 지시 사용 | SOFT |

---

### GENERAL (보수적 안전 모드)

**핵심 원칙**
| # | 이름 | 설명 | Level |
|---|---|---|---|
| 1 | 균형 잡힌 정확성 | 검증된 정보를 우선하되, 맥락에 따라 합리적 추론과 유연한 표현도 허용 | SOFT |
| 2 | 적응적 맥락 파악 | 사용자 요청의 성격(정보형/감성형/실무형)을 파악하여 그에 맞는 접근 방식 선택 | HARD |
| 3 | 보편적 유용성 | 특정 도메인에 치우치지 않는 범용적이고 이해하기 쉬운 응답 제공 | SOFT |

**응답 구조화 규칙**
| # | 규칙 | Level |
|---|---|---|
| 1 | 유연한 구성: 내용에 적합한 구조를 자율적으로 선택 (강제 포맷 없음) | SOFT |
| 2 | 가독성 확보: 적절한 단락 구분과 형식 활용으로 읽기 쉬운 응답 | SOFT |
| 3 | 핵심 우선: 가장 중요한 정보를 먼저 제시하되, 부가 정보도 필요시 포함 | SOFT |
| 4 | 적절한 분량: 요청의 복잡도에 비례하는 분량. 과하지도 부족하지도 않게 | SOFT |

**응답 품질 기준**
| # | 이름 | 설명 | Level |
|---|---|---|---|
| 1 | 이해 용이성 | 전문 지식 없이도 이해할 수 있는 쉬운 표현 사용 | SOFT |
| 2 | 실용성 | 가능한 한 실생활에 활용할 수 있는 구체적 정보 포함 | SOFT |
| 3 | 신뢰성 | 불확실한 정보는 그 불확실성을 명시하고, 확인된 사실과 구분 | HARD |

**출력 형식 제약**
| # | 제약 | Level |
|---|---|---|
| 1 | 인사말 최소화: 짧은 인사는 허용하되 과도한 형식적 인사말 지양 | SOFT |
| 2 | 자연스러운 도입: 맥락에 맞는 자연스러운 시작 (강제 서론 최소화 없음) | SOFT |
| 3 | 적절한 마무리: 요약이나 다음 단계 안내가 도움이 되면 포함 | SOFT |

---

## 추가 정리 사항

### ToneType/StyleType 필드 순서 통일 (Ko→En→Ja)

**ToneType.java**: 필드 + 13개 상수의 인자 순서 `En,Ko,Ja` → `Ko,En,Ja`
**StyleType.java**: 필드 + 14개 상수의 인자 순서 `En,Ko,Ja` → `Ko,En,Ja`

### SortType name → displayName (즉시 수정)

`name` 필드를 `displayName`으로 변경.
- `getName()` 호출부 없음 확인됨 → 안전하게 변경 가능
- Enum.name()과 의미 충돌, Jackson/JPA/Reflection 전부 위험 — 토론 대상 아님

---

## 구현 순서

```
1. GuidelinePolicy 인터페이스 + GuidelineRule record + I18nText record + RuleLevel + RuleType + DomainResolution 생성
2. 도메인별 가이드라인 클래스 6개 생성 (GeneralGuidelines에 한계 인정 메타-규칙 포함)
3. TaskDomain enum 생성 (GuidelinePolicy 구현, delegate 패턴)
4. PromptCategory에 defaultDomain 필드 추가
5. ActionTypeInterface에 getTaskDomain() → Optional<TaskDomain> 추가
6. 28개 ActionType enum에 getTaskDomain() 오버라이드 (empty 허용 안 함 — 테스트가 강제)
7. GuidelineRenderer 인터페이스 + Korean/English/JapaneseGuidelineRenderer + Factory 생성
8. PromptGuidelineBuilder → 일반 class로 변경 (resolveDomain() + DomainResolution + Renderer 위임)
9. 기존 Korean/English/JapaneseGuidelineBuilder + GuidelineBuilderFactory 삭제
10. SortType name→displayName (독립, 즉시)
11. ToneType/StyleType 필드 순서 통일 (독립)
12. 테스트 작성 (TaskDomainTest, ActionTypeTaskDomainTest, GuidelineRendererTest)
```

Step 10-11은 Step 1-9와 독립적이므로 병렬 가능.

## 검증 방법

1. `./gradlew compileJava` — 컴파일 확인
2. `./gradlew test` — 기존 테스트 통과 + 신규 테스트
3. CREATIVE 도메인: RuleType.FORBID + "의도적 불완전성" 반-규칙 적용 확인
4. TECHNICAL 도메인: RuleType.REQUIRE + "정확성 우선" 원칙 유지 확인
5. ActionType 우선순위: WRITING(PromptCategory) + CODE_REVIEW(ActionType) → TECHNICAL 도메인 확인
6. 폴백 구분: EtcActionType → `Optional.of(GENERAL)` (의도적, 로그 없음) vs 미매핑 ActionType → `Optional.empty()` (warn 로그 + isFallback=true)
7. GuidelineRenderer: 같은 GuidelineRule 리스트 → 3개 언어 렌더러가 각각 정확한 텍스트 생성 확인
8. GuidelineRule.id: 모든 규칙이 고유 id를 가지는지 검증 (중복 id 테스트)

# Prompt 공통 도메인 (`domain/prompt/common`)

`org.example.sharedprompts.domain.prompt.common` 패키지는 **프롬프트 생성의 공통 규칙·카테고리·도메인·i18n**을 정의하는 핵심 계층입니다.  
LLM에 어떤 작업을 시키든, 이 레이어를 거치면 **일관된 도메인 결정, 가이드라인 적용, 다국어 설명**이 가능해집니다.

이 문서는 다음을 설명합니다.

- `domain/prompt/common` 패키지 구조
- **PromptObjective 이중 정의** (API용 vs 도메인용) — [2.0 절](#20-promptobjective-이중-정의-api-vs-도메인) 참고
- `TaskDomain` / `PromptCategory` / `ActionType` / `RoleType` 간 관계
- `GuidelinePolicy` / `GuidelineRule` 및 실제 가이드라인 예시
- i18n 모델(`LanguageType`, `I18nText`, `I18nUtils`)
- `DomainResolution` 을 통해 도메인을 결정하는 흐름
- **공통 용어** (V2, Unified, Intent, EngineProfile 등) — [9. 절](#9-공통-용어-정의) 참고

---

## 2.0 PromptObjective 이중 정의 (API vs 도메인)

동일한 이름의 `PromptObjective`가 **두 패키지**에 존재합니다. 역할이 다르므로 import 시 패키지를 구분해야 합니다.

| 구분 | 클래스 (전체 경로) | 용도 |
|------|---------------------|------|
| **API·라우팅용** | `common.enums.PromptObjective` | Unified API 요청/응답, 라우팅 규칙, Intent 기본값. CREATIVE, CODE, EXTRACTION 등 상위 6개 값. |
| **도메인·스펙용** | `domain.value.objective.PromptObjective` | PromptSpec, DB, 검증 파이프라인. CREATIVE_WITH_CONSTRAINTS, ANALYTICAL 등 도메인 전용 값. |

- **변환**: API → 도메인은 `common.enums.PromptObjective#toDomainObjective()` 사용.
- **문서**: 두 enum의 Javadoc에 상대편을 링크해 두었으므로, 새로 합류하는 개발자는 해당 클래스 주석을 참고하면 됩니다.

---

## 1. 패키지 구조 개요

### 1.1 상위 구조

```text
domain/prompt/
├── common/
│   ├── enums/
│   │   ├── action/          # 작업 유형 (무엇을 할지)
│   │   ├── role/            # 역할 유형 (누가 될지)
│   │   ├── serializer/      # enum <-> JSON 직렬화
│   │   ├── TaskDomain.java  # 작업 도메인 (TECHNICAL/CREATIVE/...)
│   │   ├── PromptCategory.java
│   │   ├── LanguageType.java
│   │   ├── I18nUtils.java
│   │   └── ... (ToneType, StyleType, SortType, ExperienceLevel 등)
│   └── guideline/
│       ├── content/         # 도메인별 실제 가이드라인 정의
│       ├── i18n/            # I18nText, DomainResolution
│       ├── policy/          # GuidelinePolicy 인터페이스
│       └── rule/            # GuidelineRule, RuleLevel, RuleType
```

`common` 은 **도메인 모델(`domain/prompt/domain`) 과 어댑터(`adapter/*`) 사이에 놓이는 “공통 언어 계층”** 입니다.

- UI, API, LLM Prompt Spec 모두가 **같은 카테고리/도메인/가이드라인 용어**를 공유하도록 하는 역할
- “새 카테고리 추가”, “새 언어 추가”, “가이드라인 변경”과 같은 요구사항을 이곳에서 집중 처리

---

## 2. 핵심 개념 개요

### 2.1 TaskDomain – 작업 도메인

**파일**: `common/enums/TaskDomain.java`

```java
public enum TaskDomain implements GuidelinePolicy {
    TECHNICAL("기술형", TechnicalGuidelines.INSTANCE),
    CREATIVE("창의형", CreativeGuidelines.INSTANCE),
    ANALYTICAL("분석형", AnalyticalGuidelines.INSTANCE),
    PRACTICAL("실무형", PracticalGuidelines.INSTANCE),
    EDUCATIONAL("교육형", EducationalGuidelines.INSTANCE),
    GENERAL("일반형", GeneralGuidelines.INSTANCE);
    ...
}
```

- 프롬프트의 **작업 성격**을 6개 도메인으로 구분합니다.
- 각 도메인은 `GuidelinePolicy` 를 구현하는 `*Guidelines` 인스턴스에 **위임(delegate)** 하여, 실제 가이드라인 세트를 제공합니다.
- `TaskDomain` 자체가 `GuidelinePolicy` 를 구현하므로, 호출자는 `TaskDomain` 만 알면 바로 `principles()`, `structuringRules()` 등을 사용할 수 있습니다.

> 요약: **TaskDomain = "이 프롬프트는 어떤 타입의 작업인가?"** (기술/창의/분석/실무/교육/일반)

### 2.2 PromptCategory – 상위 카테고리

**파일**: `common/enums/PromptCategory.java`

`PromptCategory` 는 사용자가 선택하는 상위 카테고리(“개발”, “마케팅”, “콘텐츠 제작”, …)를 정의합니다.

- 각 카테고리는 아래 정보를 가집니다.
  - `displayName` (ko)
  - `guidelineKo / guidelineEn / guidelineJa`: 해당 카테고리에서 **어떤 역할/용도로 LLM을 쓰는지 설명**
  - `defaultDomain`: 대표 `TaskDomain` (필요 시 ActionType/RoleType 이 override)

예시:

```java
PRODUCTIVITY(
        "생산성",
        "업무 효율 향상, 시간 관리, 자동화 전략",
        "Workflow optimization, time management, automation strategies",
        "業務効率向上、時間管理、自動化戦略",
        TaskDomain.PRACTICAL
),
DEVELOPMENT(
        "개발",
        "소프트웨어 개발, 기술 스택, 프로젝트 구조, 최적화",
        "Software development, technology stacks, system architecture, optimization",
        "ソフトウェア開発、技術スタック、プロジェクト構造、最適化",
        TaskDomain.TECHNICAL
),
...
ETC(
        "기타",
        "다양한 주제와 아이디어, 포괄적 접근",
        "Wide range of topics, comprehensive approach",
        "多様なトピック、包括的アプローチ",
        TaskDomain.GENERAL
);
```

> 요약: **PromptCategory = 사용자가 선택하는 “분야/상황”** 이고, 여기서 기본 TaskDomain 힌트를 제공합니다.

### 2.3 ActionType / RoleType – 구체적 작업/역할

- `common/enums/action/*ActionType.java`
  - 예: `WritingActionType`, `ProgrammingActionType`, `MarketingActionType`, `BusinessActionType` …
  - 각 enum 상수는 **사용자가 LLM에게 시킬 “구체적인 작업 유형”** 을 나타냅니다.
  - 보통 `displayNameKo/En/Ja` 와 함께, `getTaskDomain()` 으로 대표 도메인을 지정할 수 있습니다.

예시 (`WritingActionType` 일부):

```java
@Getter
@AllArgsConstructor
public enum WritingActionType implements ActionTypeInterface {
    ARTICLE_WRITING("기사 작성", "Article Writing", "記事執筆"),
    ESSAY_WRITING("에세이 작성", "Essay Writing", "エッセイ執筆"),
    TECHNICAL_WRITING("기술 문서 작성", "Technical Writing", "技術文書作成"),
    ...

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.CREATIVE);
    }
}
```

- `common/enums/role/*RoleType.java`
  - 예: `DevelopmentRoleType`, `MarketingRoleType`, `EducationRoleType`, `WritingRoleType` …
  - LLM이 연기해야 할 **역할(“Senior Backend Engineer”, “마케팅 전략가” 등)** 을 캡처합니다.

> 요약: **ActionType = “무엇을 할지”, RoleType = “누가 되는지”** 를 구조화해서, 도메인 해석과 가이드라인 선택의 입력으로 사용합니다.

### 2.4 GuidelinePolicy / GuidelineRule – 가이드라인 모델

**GuidelinePolicy**  
파일: `common/guideline/policy/GuidelinePolicy.java`

```java
public interface GuidelinePolicy {
    List<GuidelineRule> principles();
    List<GuidelineRule> structuringRules();
    List<GuidelineRule> qualityStandards();
    List<GuidelineRule> outputConstraints();
}
```

- “어떤 도메인의 프롬프트를 생성할 때 무엇을 지켜야 하는가?”를 **4개의 축으로 분리**합니다.
  - `principles`: 사고/행동 원칙 (ex. 정확성, 독창성, 감성 등)
  - `structuringRules`: 응답 구조/포맷 규칙
  - `qualityStandards`: 품질 기준 (완전성, 실행 가능성 등)
  - `outputConstraints`: 출력 형식/금지 사항 (인사말 금지 등)

**GuidelineRule**  
파일: `common/guideline/rule/GuidelineRule.java`

```java
public record GuidelineRule(
        String id,
        I18nText title,
        I18nText description,
        RuleLevel level,
        RuleType type
) { }
```

- `id`: 규칙 식별자 (도메인/언어와 무관한 내부 키)
- `title`: 규칙 제목(i18n)
- `description`: 규칙 설명(i18n)
- `level`: 규칙의 **강도/중요도** (`RuleLevel.HARD`, `SOFT` 등)
- `type`: 규칙의 **종류** (`RuleType.REQUIRE`, `FORBID`, `ALLOW` 등)

> 요약: **GuidelineRule = “LLM 프롬프트에 녹일 단일 규칙”** 을 구조적으로 표현한 레코드입니다.

---

## 3. TaskDomain 과 실제 가이드라인 내용

`TaskDomain` 각각은 `GuidelinePolicy` 구현체(`*Guidelines`)에 위임합니다.  
대표 도메인 3개(TECHNICAL/CREATIVE/GENERAL)의 실제 규칙 내용을 일부 발췌하면 다음과 같습니다.

### 3.1 TECHNICAL – 정확성/실용성 중심 기술 작업

**파일**: `common/guideline/content/TechnicalGuidelines.java`

#### 3.1.1 핵심 원칙 (`principles()`)

```java
private static final List<GuidelineRule> PRINCIPLES = List.of(
        new GuidelineRule(
                "TECHNICAL.PRINCIPLE.ACCURACY",
                I18nText.of("정확성", "Accuracy", "正確性"),
                I18nText.of(
                        "검증된 사실만 제공. 추측 금지",
                        "Only verified facts. No speculation",
                        "検証された事実のみ。推測禁止"
                ),
                HARD, REQUIRE
        ),
        new GuidelineRule(
                "TECHNICAL.PRINCIPLE.CONTEXT",
                I18nText.of("맥락", "Context", "文脈"),
                I18nText.of(
                        "요청 범위 내에서만 응답. 불필요한 확장 금지",
                        "Respond within request scope only. No unnecessary extension",
                        "要求範囲内のみ応答。不要な拡張禁止"
                ),
                HARD, REQUIRE
        ),
        new GuidelineRule(
                "TECHNICAL.PRINCIPLE.PRACTICALITY",
                I18nText.of("실용성", "Practicality", "実用性"),
                I18nText.of(
                        "실전 적용 가능한 내용 우선",
                        "Prioritize practical, applicable content",
                        "実践適用可能な内容を優先"
                ),
                SOFT, REQUIRE
        )
);
```

- **요지**
  - **정확성**: 검증된 사실만, 추측 금지(HARD/REQUIRE)
  - **맥락 준수**: 요청 범위 밖으로 나가지 않기(HARD/REQUIRE)
  - **실용성**: 실제로 바로 사용할 수 있는 내용 우선(SOFT/REQUIRE)

#### 3.1.2 구조 규칙 (`structuringRules()`)

```java
private static final List<GuidelineRule> STRUCTURING_RULES = List.of(
        new GuidelineRule(
                "TECHNICAL.STRUCTURE.LOGICAL",
                I18nText.of("논리적 구성", "Logical", "論理的"),
                I18nText.of(
                        "제목, 하위 섹션으로 논리적 구성",
                        "Logical structure with headings and subsections",
                        "見出し、下位セクションで論理的構成"
                ),
                HARD, REQUIRE
        ),
        new GuidelineRule(
                "TECHNICAL.STRUCTURE.READABILITY",
                I18nText.of("가독성", "Readability", "可読性"),
                I18nText.of(
                        "핵심→세부 순서로 정보 계층화",
                        "Hierarchy: core→details order",
                        "核心→詳細の順で情報階層化"
                ),
                SOFT, REQUIRE
        ),
        new GuidelineRule(
                "TECHNICAL.STRUCTURE.NO_FLUFF",
                I18nText.of("불필요 요소 제거", "No Fluff", "不要要素の除去"),
                I18nText.of(
                        "인사말, 서론, 마무리 생략",
                        "Omit greetings, intro, closing",
                        "挨拶、前置き、結び省略"
                ),
                SOFT, REQUIRE
        )
);
```

- 기술 도메인 응답은 **논리적/계층적 구조 + 불필요한 말 제거**를 강하게 요구합니다.

#### 3.1.3 품질 기준 & 출력 제약

대표 규칙:

- `TECHNICAL.QUALITY.CLARITY`: 구체적 용어 사용(HARD/REQUIRE)
- `TECHNICAL.QUALITY.ACTIONABILITY`: 실행 가능한 지침 포함(SOFT/REQUIRE)
- `TECHNICAL.OUTPUT.NO_GREETING`: 인사말 금지(HARD/FORBID)
- `TECHNICAL.OUTPUT.MINIMAL_INTRO`: 핵심으로 바로 진입(SOFT/REQUIRE)

> TECHNICAL 도메인은 **“정확·간결·실용적인 기술 문서/답변”** 을 지향합니다.

---

### 3.2 CREATIVE – 독창성/감성 중심 창작 작업

**파일**: `common/guideline/content/CreativeGuidelines.java`

#### 3.2.1 핵심 원칙 (`principles()`)

```java
private static final List<GuidelineRule> PRINCIPLES = List.of(
        new GuidelineRule(
                "CREATIVE.PRINCIPLE.ORIGINALITY",
                I18nText.of("독창성 추구", "Pursue Originality", "独創性の追求"),
                I18nText.of(
                        "기존 틀에 얽매이지 않는 새로운 관점과 표현을 적극 탐색. 창의적 도약과 예상 밖의 연결 장려",
                        "Actively explore new perspectives and expressions unconstrained by existing frameworks. Encourage creative leaps and unexpected connections",
                        "既存の枠にとらわれない新しい視点と表現を積極的に探索。創造的な飛躍と予想外のつながりを奨励"
                ),
                HARD, REQUIRE
        ),
        new GuidelineRule(
                "CREATIVE.PRINCIPLE.EMOTIONAL_RESONANCE",
                I18nText.of("감성적 공명", "Emotional Resonance", "感性的共鳴"),
                I18nText.of(
                        "독자/사용자의 감정과 공감을 불러일으키는 표현 지향. 논리적 설명보다 감각적 체험 우선",
                        "Orient towards expressions that evoke emotions and empathy. Prioritize sensory experience over logical explanation",
                        "読者/利用者の感情と共感を呼び起こす表現を指向。論理的説明よりも感覚的体験を優先"
                ),
                HARD, REQUIRE
        ),
        ...
);
```

- CREATIVE 도메인은 **논리/정확성보다 “독창성·감성·표현력”** 을 우선합니다.

특이한 점은 **“반(反) 규칙”** 들입니다:

- `CREATIVE.ANTI.NO_FORCED_COMPLETION`: 완전한 결론 강제 금지(HARD/FORBID)
- `CREATIVE.ANTI.NO_FORCED_CONCLUSION`: 명확한 결론 강제 금지(HARD/FORBID)
- `CREATIVE.ANTI.NO_FORCED_MESSAGE`: 단일 메시지 강제 금지(HARD/FORBID)
- `CREATIVE.ANTI.NO_FORCED_STRUCTURE`: 기술적 구조(목록/표/단계) 강제 금지(HARD/FORBID)

> 즉, CREATIVE 는 “일반 도메인의 기술적/논리적 강제 구조”를 일부러 풀어 주는 역할을 합니다.

#### 3.2.2 구조/품질/출력 규칙 요약

- `STRUCTURE.NATURAL_FLOW`: 기계적 단계 구분보다 자연스러운 전개와 호흡
- `STRUCTURE.SENSORY_EXPRESSION`: 비유·감각적 묘사로 추상 개념 전달
- `QUALITY.IMMERSION`: 몰입감 있는 장면/감정 묘사
- `QUALITY.ORIGINALITY`: 클리셰 회피, 새로운 시각/전개
- `OUTPUT.FORMAT_FREEDOM`: 형식 자유(강제 템플릿 없음)
- `OUTPUT.LINGERING_ENDING`: 여운 있는 열린 결말 권장

---

### 3.3 GENERAL – 보수적 안전 모드

**파일**: `common/guideline/content/GeneralGuidelines.java`

GENERAL 도메인은 “폴백(fallback) 도메인”으로, 어떤 카테고리/액션/역할에도 딱 맞지 않을 때 사용하는 **세이프 모드** 입니다.

#### 3.3.1 한계 인정 메타 규칙

```java
public static final GuidelineRule LIMITATION_ACK = new GuidelineRule(
        "GENERAL.META.LIMITATION_ACK",
        I18nText.of("도메인 비특화 안내", "Domain Non-Specialization Notice", "ドメイン非特化案内"),
        I18nText.of(
                "이 응답은 범용 모드로 생성되었습니다. 특정 분야의 전문적 깊이가 필요한 경우, "
                        + "보다 적합한 카테고리와 작업 유형을 선택하면 더 나은 결과를 얻을 수 있습니다.",
                "This response was generated in general-purpose mode. For domain-specific depth, "
                        + "selecting a more appropriate category and action type may yield better results.",
                "この応答は汎用モードで生成されました。特定分野の専門的な深さが必要な場合、"
                        + "より適切なカテゴリとアクションタイプを選択するとより良い結果が得られます。"
        ),
        SOFT, ALLOW
);
```

- GENERAL 로 폴백했을 때, **“지금은 범용 모드입니다”** 라고 명시적으로 안내하는 규칙입니다.

#### 3.3.2 원칙/구조/품질/출력 요약

- `PRINCIPLE.BALANCED_ACCURACY`: 정확성을 지키되, 합리적 추론/유연한 표현 허용
- `PRINCIPLE.ADAPTIVE_CONTEXT`: 요청 성격(정보/감성/실무)에 따라 적응적 접근
- `PRINCIPLE.UNIVERSAL_UTILITY`: 도메인 편향 없이 보편적으로 유용한 응답
- `STRUCTURE.FLEXIBLE`: 강제 포맷 없이 유연한 구성
- `QUALITY.COMPREHENSIBILITY`: 비전문가도 이해할 수 있는 쉬운 표현
- `OUTPUT.MINIMAL_GREETING`: 짧은 인사는 허용하되 과도한 인사는 지양

> GENERAL 은 “뾰족한 도메인을 못 골랐을 때, 최소한 무난하고 이해 가능한 답을 주는 안전 모드” 입니다.

---

## 4. i18n 모델

### 4.1 LanguageType

**파일**: `common/enums/LanguageType.java`

```java
@Getter
@AllArgsConstructor
public enum LanguageType {
    KOREAN("한국어", "Korean"),
    ENGLISH("영어", "English"),
    JAPANESE("일본어", "Japanese");

    private final String description;
    private final String promptToken;
}
```

- 시스템이 지원하는 언어의 집합을 정의합니다.
- `promptToken` 은 프롬프트 내에서 언어를 LLM에게 명시할 때 사용할 수 있습니다.

### 4.2 I18nText

**파일**: `common/guideline/i18n/I18nText.java`

```java
public record I18nText(String ko, String en, String ja) {

    public static I18nText of(String ko, String en, String ja) {
        return new I18nText(ko, en, ja);
    }

    public String byLang(LanguageType lang) {
        if (lang == null) {
            return ko;
        }
        return switch (lang) {
            case KOREAN -> ko;
            case ENGLISH -> en;
            case JAPANESE -> ja;
        };
    }
}
```

- 한 개의 논리적 텍스트를 **3개 언어 버전으로 함께 보관**합니다.
- `GuidelineRule.title/description` 처럼, 규칙/카테고리 설명에 사용됩니다.

### 4.3 I18nUtils

**파일**: `common/enums/I18nUtils.java`

```java
public final class I18nUtils {

    private I18nUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static String getByLang(LanguageType lang, Function<LanguageType, String> textProvider) {
        Objects.requireNonNull(lang, "lang must not be null");
        Objects.requireNonNull(textProvider, "textProvider must not be null");
        return textProvider.apply(lang);
    }

    public static String getByLang(LanguageType lang, String textKo, String textEn, String textJa) {
        Objects.requireNonNull(lang, "lang must not be null");
        return switch (lang) {
            case KOREAN -> textKo;
            case ENGLISH -> textEn;
            case JAPANESE -> textJa;
        };
    }
}
```

- enum 필드나 개별 문자열을 받을 때, **언어별 선택 로직을 한 곳에 모으는 유틸리티**입니다.

---

## 5. DomainResolution – 도메인 결정 결과

**파일**: `common/guideline/i18n/DomainResolution.java`

```java
/**
 * 도메인 결정 결과 — 폴백 여부 포함
 */
public record DomainResolution(TaskDomain domain, boolean isFallback) {
}
```

- 카테고리/액션/역할/목표 등 여러 신호를 조합해 **최종적으로 선택된 `TaskDomain`** 과,
  - 이 선택이 **폴백인지 여부**(= 명확한 매핑이 없어 GENERAL 등으로 떨어졌는지)를 함께 담습니다.
- 도메인 해석 로직은 `domain/prompt/domain/resolution/*` 에 위치하고,  
  그 결과를 이 레코드 형태로 표현해 상위 레이어에서 사용합니다.

> 예: `DomainResolution(TaskDomain.GENERAL, true)`  
> → “명확한 도메인을 찾지 못해 GENERAL 로 폴백했다”는 의미.

---

## 6. 프롬프트 생성 흐름에서의 역할

프롬프트 생성 전체 플로우에서 `domain/prompt/common` 은 다음 단계에 관여합니다.

1. **사용자 입력 분석**
   - 사용자가 선택/입력한 값:
     - PromptCategory (예: DEVELOPMENT, MARKETING)
     - RoleType (예: DevelopmentRoleType.SENIOR_BACKEND_ENGINEER)
     - ActionType (예: ProgrammingActionType.CODE_REVIEW)
     - LanguageType, ExperienceLevel, ToneType 등

2. **TaskDomain 결정**
   - 기본적으로 `PromptCategory.defaultDomain` 을 사용
   - 필요 시 ActionType/RoleType 이 `getTaskDomain()` 으로 도메인을 override
   - 최종 결과를 `DomainResolution` 으로 표현

3. **Guideline 세트 조회**
   - `TaskDomain` 이 구현한 `GuidelinePolicy` 를 통해:
     - `principles()`
     - `structuringRules()`
     - `qualityStandards()`
     - `outputConstraints()`
   - 필요 시 `TaskDomain#getRulesByLevel(RuleLevel)` 로 레벨별 필터링

4. **i18n 적용**
   - 현재 `LanguageType` 에 따라:
     - `rule.title().byLang(lang)`
     - `rule.description().byLang(lang)`
   - 또는 `I18nUtils.getByLang(...)` 를 사용해 최종 문자열 생성

5. **Prompt Spec 렌더링**
   - `adapter/out/render/*` 및 `application/service/*` 계층에서:
     - 위에서 얻은 GuidelineRule 들을 시스템 프롬프트/지침 블록에 녹여,
     - 사용자 입력과 합쳐 **최종 Prompt Spec** 을 구성합니다.

---

## 7. 확장 가이드

### 7.1 새로운 TaskDomain 추가

1. `TaskDomain` 에 enum 상수 추가
2. `GuidelinePolicy` 구현체(`XxxGuidelines`) 생성
3. 필요 시:
   - 새로운 `PromptCategory` 가 이 도메인을 default 로 사용하도록 연결
   - 특정 `ActionType`/`RoleType` 에서 `getTaskDomain()` 으로 override

### 7.2 새로운 가이드라인 규칙 추가

1. 해당 도메인의 `*Guidelines` 클래스에서 `GuidelineRule` 추가
2. 다음을 반드시 지정:
   - `id`: `DOMAIN.SECTION.KEY` 형식 등 일관성 유지
   - `RuleLevel`: HARD/SOFT 등 중요도
   - `RuleType`: REQUIRE/FORBID/ALLOW 등 성격
   - `I18nText`: ko/en/ja 설명

### 7.3 새로운 PromptCategory 추가

1. `PromptCategory` 에 enum 상수 추가
2. ko/en/ja 설명 및 대표 `TaskDomain` 지정
3. 필요하다면:
   - UI/API 에서 새로운 카테고리를 노출
   - 도메인 해석 로직에서 이 카테고리에 특화된 매핑 추가

### 7.4 새로운 언어 추가 (미래 확장)

1. `LanguageType` 에 새 enum 상수 추가
2. `I18nText`, `I18nUtils`, 관련 switch 문에 새 언어 분기 추가
3. 각 enum/가이드라인의 텍스트를 새 언어로 채워 넣기

---

## 8. 요약

- `domain/prompt/common` 은 **프롬프트 생성의 공통 언어/정책 레이어**입니다.
- `TaskDomain`, `PromptCategory`, `ActionType`, `RoleType` 을 통해 “무엇을, 어떤 톤으로, 어떤 도메인에서” 할지 결정합니다.
- `GuidelinePolicy` / `GuidelineRule` + `*Guidelines` 구현체를 통해 **도메인별 프롬프트 가이드라인**을 구조적으로 관리합니다.
- `LanguageType`, `I18nText`, `I18nUtils` 는 ko/en/ja 3개 언어에 대해 **동일한 규칙을 여러 언어로 표현**할 수 있게 합니다.
- `DomainResolution` 은 이 모든 해석 결과를 하나의 오브젝트로 표현하여 상위 계층(PromptSpec, Renderer, Orchestrator)에서 활용할 수 있게 합니다.

---

## 9. 공통 용어 정의

도메인·API·문서에서 반복 사용되는 용어를 정리합니다.

| 용어 | 설명 |
|------|------|
| **V2** | 품질 우선 파이프라인. Clarify → Solve → Verify → Repair 4단계. 현재 유일하게 구현된 생성 경로. |
| **Unified** | 단일 API(`/api/prompts/generate`)로 요청을 받아 Intent·EngineMode·Rule 기반으로 라우팅한 뒤, 내부적으로 V2 파이프라인을 호출하는 구조. |
| **Intent** | 상위 작업 의도. `ActionIntent` enum (GENERATE, SUMMARIZE, EXTRACT 등). 라우팅·기본값 결정의 입력. |
| **EngineMode** | 클라이언트가 요청하는 실행 모드: AUTO(규칙 기반 자동), V2(품질 파이프라인), V3(경량·추후). |
| **EngineProfile** | 내부 라우팅 결과. QUALITY_PIPELINE(V2), FAST_PIPELINE(V3 예정), JSON_STRICT, AUTO. |
| **Objective** | 프롬프트 목적. API용은 `common.enums.PromptObjective`, 도메인/스펙용은 `domain.value.objective.PromptObjective`. [2.0 절](#20-promptobjective-이중-정의-api-vs-도메인) 참고. |


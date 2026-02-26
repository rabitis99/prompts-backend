# Prompt 도메인 구조 정리

`src/main/java/org/example/sharedprompts/domain/prompt` 패키지의 현재 구조와 **enum** 구성을 자세히 정리한 문서다.

---

## 1. 패키지 구조 개요

```text
domain/prompt/
├── enums/                    # 모든 enum 및 직렬화
│   ├── TaskDomain.java
│   ├── PromptCategory.java
│   ├── ToneType.java
│   ├── StyleType.java
│   ├── LanguageType.java
│   ├── SortType.java
│   ├── ExperienceLevel.java
│   ├── action/               # ActionType 계열 (27개 enum)
│   │   ├── ActionTypeInterface.java
│   │   ├── CodingActionType.java
│   │   ├── AnalysisActionType.java
│   │   └── ... (25개 더)
│   ├── role/                 # RoleType 계열 (18개 enum)
│   │   ├── RoleTypeInterface.java
│   │   ├── DevelopmentRoleType.java
│   │   └── ... (17개 더)
│   └── serializer/
│       ├── ActionTypeSerializer.java
│       ├── ActionTypeDeserializer.java
│       ├── RoleTypeSerializer.java
│       ├── RoleTypeDeserializer.java
│       └── EnumResolver.java
├── guideline/                # 가이드라인 정책·규칙
│   ├── GuidelinePolicy.java
│   ├── GuidelineRule.java
│   ├── I18nText.java
│   ├── RuleType.java
│   ├── RuleLevel.java
│   ├── DomainResolution.java
│   ├── TechnicalGuidelines.java
│   ├── CreativeGuidelines.java
│   ├── AnalyticalGuidelines.java
│   ├── PracticalGuidelines.java
│   ├── EducationalGuidelines.java
│   └── GeneralGuidelines.java
└── service/                  # 도메인·가이드라인 서비스
    ├── DomainResolver.java
    ├── PromptGenerator.java
    ├── PromptAIService.java
    └── guideline/
        ├── GuidelineRenderer.java
        ├── AbstractGuidelineRenderer.java
        ├── KoreanGuidelineRenderer.java
        ├── EnglishGuidelineRenderer.java
        ├── JapaneseGuidelineRenderer.java
        ├── PromptGuidelineBuilder.java
        └── GuidelineRendererFactory.java
```

---

## 2. Enum 상세 정리

### 2.1 공통·분류용 Enum (guideline 제외)

| Enum | 패키지 | 용도 | 값 예시 |
|------|--------|------|---------|
| **TaskDomain** | `enums` | 작업 성격(기술/창의/분석/실무/교육/일반). 가이드라인 정책과 1:1 연결 | TECHNICAL, CREATIVE, ANALYTICAL, PRACTICAL, EDUCATIONAL, GENERAL |
| **PromptCategory** | `enums` | 카테고리(생산성, 개발, 코딩, 분석, 마케팅 등). 다국어 가이드라인 + default TaskDomain 보유 | PRODUCTIVITY, DEVELOPMENT, CODING, PROGRAMMING, ANALYSIS, MARKETING, CONTENT, CREATIVE, STUDY, EDUCATION, RESEARCH, BUSINESS, DESIGN, WRITING, ETC |
| **ToneType** | `enums` | 톤(어조). Ko/En/Ja 가이드라인 + 추천 TaskDomain | FRIENDLY, FORMAL, HUMOROUS, MOTIVATIONAL, CASUAL, PROFESSIONAL, EMPATHETIC, POSITIVE, INSPIRATIONAL, NEUTRAL, ENTHUSIASTIC, SARCASTIC, NEGATIVE |
| **StyleType** | `enums` | 스타일(서사형, 글머리형, 간결형 등). Ko/En/Ja 가이드라인 + 추천 TaskDomain | NARRATIVE, BULLET, CONCISE, FORMATTED, DESCRIPTIVE, INSTRUCTIVE, QUESTION_ANSWER, STORYTELLING, DIALOGUE, COMPARATIVE, ANALYTICAL, CREATIVE, TECHNICAL, DETAILED |
| **LanguageType** | `enums` | 응답/가이드라인 언어 | KOREAN, ENGLISH, JAPANESE |
| **SortType** | `enums` | 프롬프트 목록 정렬. QueryDSL OrderSpecifier 반환 | LATEST, POPULAR |
| **ExperienceLevel** | `enums` | 경험 수준(초급~전문가). Ko/En/Ja 가이드라인 | BEGINNER, INTERMEDIATE, ADVANCED, EXPERT |

### 2.2 Guideline 관련 Enum

| Enum | 패키지 | 용도 | 값 |
|------|--------|------|-----|
| **RuleType** | `guideline` | 규칙 방향 | REQUIRE(해야 한다), FORBID(하지 마라), ALLOW(해도 된다) |
| **RuleLevel** | `guideline` | 규칙 강도(위반 시 영향) | HARD(품질 실패), SOFT(가능하면 준수) |

### 2.3 ActionType 계열 (인터페이스 + 27개 enum)

**인터페이스:** `ActionTypeInterface`

- `getDisplayNameKo()`, `getDisplayNameEn()`, `getDisplayNameJa()`
- `getDisplayNameByLang(LanguageType lang)`
- `getTaskDomain()` → `Optional<TaskDomain>` (도메인 매핑; 비어 있으면 폴백/경고)

**구현 enum (모두 `ActionTypeInterface` 구현):**

| Enum | 대표 TaskDomain | 비고 |
|------|-----------------|------|
| CodingActionType | TECHNICAL | CODE_GENERATION, CODE_REVIEW, REFACTORING, DEBUGGING 등 10개 |
| ProgrammingActionType | TECHNICAL | |
| DevelopmentActionType | TECHNICAL | |
| DevOpsActionType | TECHNICAL | |
| AiMlActionType | (구현에서 지정) | |
| AnalysisActionType | ANALYTICAL | DATA_ANALYSIS, STATISTICAL_ANALYSIS, INSIGHT_EXTRACTION 등 9개 |
| ResearchActionType | ANALYTICAL | |
| WritingActionType | CREATIVE | |
| CreativeActionType | CREATIVE | |
| ContentActionType | CREATIVE | |
| DesignActionType | CREATIVE | |
| BusinessActionType | PRACTICAL | |
| MarketingActionType | PRACTICAL | |
| ProductivityActionType | PRACTICAL | |
| EducationActionType | EDUCATIONAL | |
| StudyActionType | EDUCATIONAL | |
| CustomerSupportActionType | | |
| HealthFitnessActionType | | |
| LifestyleActionType | | |
| PersonalDevelopmentActionType | | |
| CareerActionType | | |
| EmailActionType | | |
| RecommendationActionType | | |
| ShoppingActionType | | |
| SocialActionType | | |
| CybersecurityActionType | | |
| CloudServicesActionType | | |
| EtcActionType | | (미매핑 시 GENERAL 폴백) |

- 각 enum은 `displayNameKo/En/Ja` 필드와, 필요 시 `getTaskDomain()` 오버라이드로 **어느 TaskDomain에 속하는지** 명시한다.
- **직렬화:** JSON에는 enum `name()` 문자열만 저장 (`ActionTypeSerializer` / `ActionTypeDeserializer`). 역직렬화 시 `EnumResolver`로 여러 ActionType enum 클래스에 걸쳐 값 찾기 가능.

### 2.4 RoleType 계열 (인터페이스 + 18개 enum)

**인터페이스:** `RoleTypeInterface`

- `getRoleNameKo()`, `getDescriptionKo()` / En, Ja
- `getRoleNameByLang(LanguageType)`, `getDescriptionByLang(LanguageType)`

**구현 enum (모두 `RoleTypeInterface` 구현):**

- WritingRoleType, StudyRoleType, SocialRoleType, ResearchRoleType, ProductivityRoleType  
- MarketingRoleType, HealthFitnessRoleType, EtcRoleType, EducationRoleType  
- DevelopmentRoleType, DesignRoleType, CybersecurityRoleType, CustomerSupportRoleType  
- CreativeRoleType, ContentRoleType, BusinessRoleType, AiMlRoleType  

- 각 값은 역할 이름·설명을 Ko/En/Ja로 가진다 (예: DevelopmentRoleType — BACKEND_DEVELOPER, FRONTEND_DEVELOPER, FULL_STACK_DEVELOPER, DEVOPS_ENGINEER, CLOUD_ARCHITECT, SITE_RELIABILITY_ENGINEER).
- **직렬화:** `RoleTypeSerializer` / `RoleTypeDeserializer` — enum `name()` 문자열로 직렬화, 역직렬화 시 `EnumResolver`로 여러 RoleType enum에서 조회.

---

## 3. Enum 사용 흐름 요약

1. **요청 진입**
   - API에서 `PromptCategory`, `ActionType`(인터페이스), `ToneType`, `StyleType`, `RoleType`(인터페이스), `LanguageType`, `ExperienceLevel` 등이 DTO로 들어옴.
   - `ActionType`/`RoleType`은 JSON 문자열 → `EnumResolver` + 여러 enum 클래스로 역직렬화.

2. **도메인 결정**
   - `DomainResolver.resolveDomain(InputRequestDto)`:
     - 1순위: `ActionType.getTaskDomain()`이 있고 GENERAL이 아니면 그대로 사용.
     - 2순위: `PromptCategory.getDefaultDomain()` 사용.
     - 3순위: Action이 명시적으로 GENERAL이면 GENERAL.
     - 폴백: 미매핑이면 GENERAL + `isFallback=true` + 경고 로그.

3. **가이드라인 조합**
   - `TaskDomain`이 `GuidelinePolicy`를 구현하고, 각 도메인별로 `*Guidelines` 싱글톤에 위임(delegate).
   - `GuidelineRule`: id, I18nText(title/description), RuleLevel(HARD/SOFT), RuleType(REQUIRE/FORBID/ALLOW).
   - `PromptGuidelineBuilder.build()`:
     - `DomainResolver`로 도메인 결정.
     - 언어별 `GuidelineRenderer`로 페르소나 헤더(역할/톤/스타일), 경험 수준, 본문, **HARD 규칙만** 필수 제약으로 붙임.
     - 폴백이면 폴백 알림 문구 추가.

4. **TaskDomain ↔ Tone/Style 추천**
   - `TaskDomain.getRecommendedTones()`, `TaskDomain.getRecommendedStyles()`: 도메인별 추천 톤/스타일 목록.
   - `ToneType.getRecommendedDomains()`, `StyleType.getRecommendedDomains()`: 톤/스타일별 추천 도메인 (UI/검증용).

---

## 4. Enum 설계상 특징

- **ActionType / RoleType**: 여러 enum 클래스가 같은 인터페이스를 구현하고, JSON은 enum 이름 문자열 하나로 주고받음. `EnumResolver`가 카테고리 enum 목록을 받아 순서대로 `Enum.valueOf`로 찾는다.
- **다국어**: ToneType, StyleType, PromptCategory, ExperienceLevel, RoleType 등은 Ko/En/Ja 표시명·가이드라인을 필드로 갖고, `LanguageType`으로 분기.
- **TaskDomain**: 가이드라인 로직을 enum에 직접 넣지 않고 `GuidelinePolicy` delegate로 위임해 enum 비대화를 막음.
- **SortType**: QueryDSL 의존(도메인 내 `QPrompt` 사용). 정렬 규칙이 도메인에 묶여 있음.

---

## 5. 파일 개수 요약

| 구분 | 개수 |
|------|------|
| 공통/분류 enum | 7 (TaskDomain, PromptCategory, ToneType, StyleType, LanguageType, SortType, ExperienceLevel) |
| Guideline enum | 2 (RuleType, RuleLevel) |
| ActionType enum | 27 (인터페이스 1 + 구현 26) |
| RoleType enum | 18 (인터페이스 1 + 구현 17) |
| Serializer/Resolver | 5 (ActionType Serializer/Deserializer, RoleType Serializer/Deserializer, EnumResolver) |
| Guideline 정책/규칙 | I18nText, DomainResolution, GuidelinePolicy, GuidelineRule + *Guidelines 6개 |
| Service / Guideline 렌더링 | DomainResolver, PromptGenerator, PromptAIService + guideline 하위 7개 |

이 구조를 기준으로 헥사고날/정확도 설계 시 **PromptObjective, QualityPriority, QualityRubric, PromptStrategyBundle** 등을 추가하고, **ActionType/RoleType**은 그대로 두거나 통합 enum/메타데이터 구조로 점진적으로 리팩터링할 수 있다.

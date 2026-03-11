# domain/prompt/common — Enum 및 폴더 구조 분석 (최신)

## 1. 현재 폴더 구조 요약

```
common/
├── constants/
│   └── AxisSourceConstants.java       # axis/소스 관련 상수 (enum 아님)
├── contract/
│   └── StableKeyedEnum.java           # 시리얼용 key 계약 (인터페이스)
├── enums/
│   ├── action/
│   │   ├── ActionTypeInterface.java
│   │   ├── ActionTypeBehaviorRegistry.java
│   │   └── category/                  # 13개 카테고리
│   │       ├── analysis/   AnalysisActionType
│   │       ├── business/   BusinessActionType, CareerActionType, CustomerSupportActionType
│   │       ├── content/    ContentActionType, EmailActionType, RecommendationActionType
│   │       ├── creative/   CreativeActionType
│   │       ├── design/     DesignActionType
│   │       ├── development/ CodingActionType, DevelopmentActionType, AiMlActionType, ...
│   │       ├── education/  EducationActionType
│   │       ├── etc/        EtcActionType, HealthFitnessActionType, LifestyleActionType, SocialActionType
│   │       ├── marketing/  MarketingActionType
│   │       ├── productivity/ ProductivityActionType, PersonalDevelopmentActionType, ShoppingActionType
│   │       ├── research/   ResearchActionType
│   │       ├── study/      StudyActionType
│   │       └── writing/    WritingActionType
│   ├── engine/
│   │   ├── EngineMode.java
│   │   ├── EngineProfile.java
│   │   └── LanguageType.java
│   ├── experience/
│   │   ├── ExperienceLevel.java       # StableKeyedEnum
│   │   └── ExperienceLevelBucket.java
│   ├── output/                        # 출력/응답 메타 (action에서 분리됨)
│   │   ├── OutputBehaviorType.java
│   │   ├── OutputFormat.java
│   │   ├── OutputNeeds.java
│   │   ├── ResponseShape.java
│   │   └── ResponseStructure.java
│   ├── request/
│   │   ├── RequestMode.java
│   │   └── RequestType.java
│   ├── role/
│   │   ├── RoleTypeInterface.java     # extends StableKeyedEnum
│   │   ├── core/
│   │   │   └── CoreRoleType.java      # 엔진용 코어 역할 (StableKeyedEnum만)
│   │   ├── metadata/
│   │   │   └── DomainRoleType.java    # 메타데이터용 (인터페이스 없음)
│   │   └── category/                  # 12개 카테고리
│   │       ├── business/   BusinessRoleType, CustomerSupportRoleType
│   │       ├── content/    ContentRoleType
│   │       ├── creative/   CreativeRoleType
│   │       ├── design/     DesignRoleType
│   │       ├── development/ DevelopmentRoleType, AiMlRoleType, CybersecurityRoleType
│   │       ├── education/   EducationRoleType
│   │       ├── etc/        EtcRoleType, HealthFitnessRoleType, SocialRoleType
│   │       ├── marketing/  MarketingRoleType
│   │       ├── productivity/ ProductivityRoleType
│   │       ├── research/   ResearchRoleType
│   │       ├── study/      StudyRoleType
│   │       └── writing/    WritingRoleType
│   ├── semantic/
│   │   ├── ActionIntent.java
│   │   ├── PromptCategory.java        # StableKeyedEnum, TaskDomain·I18n 연동
│   │   ├── PromptObjective.java
│   │   └── TaskDomain.java            # GuidelinePolicy, StableKeyedEnum
│   ├── serializer/
│   │   ├── EnumCompatParser.java
│   │   ├── EnumResolver.java
│   │   ├── RoleTypeDeserializer.java
│   │   └── ActionTypeDeserializer.java
│   ├── sort/
│   │   └── SortType.java
│   ├── style/
│   │   ├── StyleAxis.java
│   │   ├── StyleType.java             # StableKeyedEnum
│   │   └── ToneType.java               # StableKeyedEnum
│   └── (enums 루트에 단일 enum 없음 — 모두 하위 패키지로 분류됨)
├── guideline/
│   ├── policy/    GuidelinePolicy
│   ├── rule/      RuleType, RuleLevel, GuidelineRule
│   ├── content/   *Guidelines (TaskDomain별)
│   ├── bundle/    GuidelineBundle, GuidelineBundleBuilder, RuleBudgetPolicy
│   ├── context/   RuleContext, GuidelineRuleApplicability
│   └── i18n/      I18nText, GuidelineI18nRegistry
├── i18n/
│   ├── I18nKey.java
│   ├── I18nText.java
│   └── I18nRegistry.java
└── style/
    └── ToneStyleNormalizer.java       # 유틸 (enum 아님)
```

---

## 2. Enum 다양성 분석

### 2.1 역할(role) 계열

| 구분 | 개수 | StableKeyedEnum | RoleTypeInterface | 비고 |
|------|------|------------------|-------------------|------|
| 도메인 RoleType (category 하위) | 18 | 인터페이스 상속 | 모두 구현 | 12개 카테고리, development 3개·business 2개·etc 3개 등 |
| CoreRoleType | 1 | O | X | 엔진용 코어 역할만 |
| DomainRoleType | 1 | X | X | metadata 하위, 상수만 있는 단순 enum |

- **계약**: `RoleTypeInterface extends StableKeyedEnum`이므로 모든 RoleType 구현체는 `key()`를 가짐. 일부 enum은 `implements RoleTypeInterface, StableKeyedEnum`처럼 중복 선언하고, 일부는 `implements RoleTypeInterface`만 선언(동작은 동일).
- **StableKeyedEnum 명시 구현**: 12개 (Social, HealthFitness, Design, CustomerSupport, Business, Study, Writing, Creative, Content, Marketing, AiMl, Development, Productivity).
- **RoleTypeInterface만**: 4개 (Cybersecurity, Etc, Education, Research). 인터페이스 상속으로 `key()`는 동작함.

### 2.2 액션(action) 계열

| 구분 | 개수 | ActionTypeInterface | StableKeyedEnum | 비고 |
|------|------|---------------------|------------------|------|
| 도메인 ActionType (category 하위) | 24 | 모두 구현 | 모두 구현 | 13개 카테고리 |
| ActionTypeInterface | 1 | — | X | `key()` 메서드 정의, 인터페이스는 StableKeyedEnum 미상속 |

- **출력 메타**: `OutputBehaviorType`, `ResponseStructure`, `OutputFormat`, `OutputNeeds`, `ResponseShape`는 **enums/output/** 으로 분리되어 있음 (이전 이슈 해소).

### 2.3 시맨틱·엔진·스타일·출력·기타 (enums 하위 패키지)

- **semantic/**: `PromptCategory`, `TaskDomain`, `ActionIntent`, `PromptObjective` — 시맨틱/카테고리 정책. PromptCategory·TaskDomain은 StableKeyedEnum 구현.
- **engine/**: `EngineProfile`, `EngineMode`, `LanguageType` — 엔진/실행 설정.
- **style/**: `ToneType`, `StyleType`, `StyleAxis` — 톤·스타일. ToneType·StyleType은 StableKeyedEnum.
- **output/**: `ResponseShape`, `OutputNeeds`, `OutputBehaviorType`, `ResponseStructure`, `OutputFormat` — 출력/응답 메타.
- **experience/**: `ExperienceLevel`, `ExperienceLevelBucket` — ExperienceLevel만 StableKeyedEnum.
- **request/**: `RequestMode`, `RequestType`.
- **sort/**: `SortType`.

- **계약 혼재**: 시리얼/API에 노출되는 enum 중 StableKeyedEnum을 구현하지 않은 것 — RequestMode, RequestType, PromptObjective, EngineProfile, EngineMode, LanguageType, ResponseShape, OutputNeeds, SortType, StyleAxis, ExperienceLevelBucket, DomainRoleType 등. “노출되는 모든 enum은 StableKeyedEnum”으로 통일할지 정책 결정 필요.

### 2.4 guideline 내 Enum

- `RuleType`, `RuleLevel` — `guideline/rule/` 하위. 도메인 구분 명확.

---

## 3. 구조/일관성 이슈 정리

### 3.1 폴더 구조 (현재 반영된 개선)

1. **enums 하위 패키지화**  
   - semantic, engine, style, output, request, experience, sort, role/category, action/category 등으로 재구성되어 enums 루트 비대 문제는 완화됨.

2. **출력 관련 enum 분리**  
   - `OutputBehaviorType`, `ResponseStructure`, `OutputFormat`, `OutputNeeds`, `ResponseShape`는 **enums/output/** 에 위치. action 패키지 혼재 해소됨.

3. **common 루트 정리**  
   - `AxisSourceConstants` → **constants/**, `StableKeyedEnum` → **contract/**, `ToneStyleNormalizer` → **style/** 로 위치 정리됨.

### 3.2 Enum 계약/다양성

4. **StableKeyedEnum 불일치**  
   - 시리얼/API/DB에 노출되는 enum 중 미구현: RequestMode, RequestType, PromptObjective, EngineProfile, EngineMode, LanguageType, ResponseShape, OutputNeeds, SortType, StyleAxis, ExperienceLevelBucket, DomainRoleType 등.  
   - 정책 결정 필요: “노출되는 모든 enum은 StableKeyedEnum”으로 통일할지, “필요한 것만” 명시할지.

5. **RoleType 선언 통일**  
   - “RoleType은 RoleTypeInterface를 통해 StableKeyedEnum을 만족한다”고 문서화되어 있음.  
   - 선언은 `implements RoleTypeInterface`만 하도록 통일하면 `StableKeyedEnum` 중복 제거 가능.

6. **DomainRoleType vs RoleType**  
   - `DomainRoleType`(metadata)은 RoleTypeInterface를 구현하지 않는 별도 역할 세트.  
   - CoreRoleType(엔진 코어) / DomainRoleType(메타) / category 하위 *RoleType(카테고리별) 관계를 한 곳에 정리해 두면 유지보수에 유리함.

### 3.3 데이터/레지스트리 정합성

7. **PromptCategory.EXTRACTION 프로필**  
   - `PromptCategory.EXTRACTION`은 정의되어 있으나 `DefaultCategorySemanticProfileRegistry`에는 **EXTRACTION용 프로필 미등록**.  
   - `GenerationSemanticResolver`에서 `request_mode == EXTRACTION`일 때 `resolveExtraction()`으로 별도 분기하며, 프로필 조회 없이 category=EXTRACTION, intent=EXTRACT 등으로 축 설정. 즉 “EXTRACTION은 프로필 없음” 전제로 동작 중.  
   - 이 동작을 전제로 한 주석/문서 정리만 추가해 두면 충분함.

8. **ActionTypeBehaviorRegistry**  
   - `instanceof` 기반으로 모든 ActionType 구현체를 나열. 새 ActionType 추가 시 여기 수정 필수.  
   - 인터페이스 기본 메서드나 enum별 메타데이터(예: `@OutputBehavior`) 방식으로 바꾸면 누락을 줄일 수 있음.

---

## 4. 개선 제안 (우선순위)

### 높음

- **StableKeyedEnum 정책 정리**  
  - “외부에 노출되는 enum은 모두 StableKeyedEnum”으로 두고, 해당 enum들에 구현 추가.  
  - 또는 “시리얼/API용만 StableKeyedEnum”으로 문서화하고, 나머지는 의도적 제외로 명시.

- **EXTRACTION 동작 문서화**  
  - “EXTRACTION은 CategorySemanticProfile 없이 별도 분기로 처리된다”는 점을 `DefaultCategorySemanticProfileRegistry` 또는 `GenerationSemanticResolver` 주석/문서에 명시.

### 중간

- **RoleType 선언 통일**  
  - `implements RoleTypeInterface, StableKeyedEnum` → `implements RoleTypeInterface`로 정리하여 중복 제거.

- **CoreRoleType / DomainRoleType / *RoleType 관계 문서화**  
  - common 또는 enums/role 패키지에 역할 계층(엔진 코어 vs 메타 vs 카테고리별)을 한 문단으로 정리.

### 낮음

- **ActionTypeBehaviorRegistry 리팩터**  
  - 새 ActionType 추가 시 한 곳만 수정하도록, 기본 메서드 또는 메타데이터 기반 매핑으로 전환 검토.

---

## 5. 요약

| 항목 | 현재 | 비고 |
|------|------|------|
| Enum 구조 | role 18개(12 카테고리), action 24개(13 카테고리), output 5개, semantic/engine/style/request/experience/sort 등 패키지별 분류 | 출력 메타는 output/으로 분리 완료 |
| StableKeyedEnum | 일부만 구현, 기준 불명확 | 노출 enum 기준으로 정책 수립 후 통일 권장 |
| 폴더 | enums 하위 semantic·engine·style·output·request·role/category·action/category 등으로 재구성됨 | 루트 비대·action 혼재 완화 |
| 레지스트리 | EXTRACTION은 프로필 없이 resolveExtraction() 분기로 처리, ActionType은 instanceof 의존 | EXTRACTION 동작 문서화, 매핑 방식 점진 개선 가능 |

이 문서를 기준으로 “StableKeyedEnum 적용 범위” 정책 수립과 “EXTRACTION 처리” 문서화를 먼저 적용한 뒤, 필요 시 RoleType 선언 통일 및 ActionTypeBehaviorRegistry 개선을 진행하는 순서를 권장합니다.

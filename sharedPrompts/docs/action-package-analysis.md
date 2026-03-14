# Action 패키지 분석

`org.example.sharedprompts.domain.prompt.common.enums.action` 패키지에 대한 구조·역할·설계 분석 문서입니다.

---

## 1. 개요

이 패키지는 **프롬프트의 액션(행위) 타입**을 정의·등록·해석하는 단일 진입점을 제공합니다.

- **액션**: 사용자가 프롬프트로 수행하려는 작업 유형(코드 생성, 이메일 작성, 연구 설계 등)
- **안정 키(stable key)**: 직렬화·API·저장소에서 사용하는 불변 식별자 (예: `ACTION.CODING.CODE_GENERATION`)
- **카탈로그 → 레지스트리 → 리졸버** 순서로 빌드되며, 패키지 스캔 없이 결정론적 순서를 유지합니다.

---

## 2. 디렉터리 구조

```
action/
├── ActionTypeInterface.java          # 액션 타입 공통 계약 (루트에 단일 계약만 유지)
├── catalog/
│   ├── ActionTypeCatalog.java        # 등록 대상 enum 클래스 목록 (인터페이스)
│   └── DefaultActionTypeCatalog.java # 기본 카탈로그 구현 (26개 enum 클래스)
├── registry/
│   ├── ActionTypeRegistry.java       # stable key → ActionType 저장·조회
│   ├── ActionDomainRegistry.java     # ActionType → TaskDomain 정책
│   ├── DefaultActionDomainRegistry.java
│   ├── ActionOutputBehaviorRegistry.java  # ActionType → OutputBehavior 정책
│   └── DefaultActionOutputBehaviorRegistry.java
├── resolver/
│   ├── ActionTypeResolver.java       # 문자열 → ActionType 해석 (인터페이스)
│   ├── DefaultActionTypeResolver.java    # stable key 우선, 레거시 호환
│   └── ActionTypeCompatibilityResolver.java  # 레거시 형식 해석
├── metadata/
│   └── ActionTypeMetadataLoader.java      # classpath properties 로드
├── canonical/
│   ├── ActionGroup.java              # capability family (상위 의미 그룹)
│   ├── CanonicalActionRegistry.java  # ActionType → ActionGroup 해석
│   └── DefaultCanonicalActionRegistry.java
└── category/
    ├── analysis/      (AnalysisActionType)
    ├── business/      (BusinessActionType, CareerActionType, CustomerSupportActionType)
    ├── content_creation/ (ContentCreationActionType, EmailActionType, RecommendationActionType)
    ├── creative/      (CreativeActionType)
    ├── design/        (DesignActionType)
    ├── development/   (DevelopmentActionType, CodingActionType, ProgrammingActionType, ...)
    ├── education/     (EducationActionType)
    ├── etc/           (EtcActionType, HealthFitnessActionType, LifestyleActionType, SocialActionType)
    ├── marketing/     (MarketingActionType)
    ├── productivity/  (ProductivityActionType, PersonalDevelopmentActionType, ShoppingActionType)
    ├── research/      (ResearchActionType)
    └── writing/       (WritingActionType)
```

**참고:** `ActionTypeRegistryHolder`는 인프라 계층에 위치 (infrastructure/serialization).

---

## 3. 핵심 컴포넌트

### 3.1 ActionTypeInterface

모든 액션 enum이 따르는 **공통 계약**입니다.

| 메서드 | 설명 |
|--------|------|
| `key()` | 직렬화·비교용 **안정 식별자** (필수) |
| `getDisplayNameKo()`, `getDisplayNameEn()`, `getDisplayNameJa()` | 다국어 표시명 |
| `getActionGroup()` | 상위 capability family (ActionGroup) |

Task domain과 output behavior는 enum이 아니라 **ActionDomainRegistry**, **ActionOutputBehaviorRegistry**에서 key로 조회합니다.

- Objective 정책은 enum이 아니라 별도 registry에서 해석합니다.
- 각 enum 상수는 자신이 속한 `ActionGroup`을 소유합니다.

### 3.2 ActionTypeCatalog (catalog 패키지)

- **단일 소스**: 어떤 ActionType enum 클래스가 등록되는지 정의.
- **순서 고정**: 패키지 스캔·발견 로직 없음. `getActionTypeEnumClasses()` 반환 순서가 레지스트리 빌드·레거시 해석 순서에 사용됩니다.

**DefaultActionTypeCatalog**  
26개 enum 클래스를 고정 순서로 나열 (Productivity, Development, CloudServices, … → Shopping).

### 3.3 ActionTypeRegistry (registry 패키지)

- **역할**: 저장·검증만 담당. 해석·발견 로직 없음.
- **구성**: 카탈로그에서 받은 enum 클래스 목록으로 `Map<stableKey, ActionTypeInterface>` 구성.
- **검증**:  
  - stable key null/blank 금지  
  - key 중복 금지  
  - 모든 상수에 non-null `ActionGroup` 필수  
- **조회**: `getByStableKey(String)`, `getAll()`.
- **동일 패키지**: ActionDomainRegistry, ActionOutputBehaviorRegistry (정책 레지스트리).

### 3.4 ActionTypeResolver (resolver 패키지)

- **역할**: 입력 문자열(예: JSON 값)을 `ActionTypeInterface`로 변환.
- **정책**: stable key 우선 → 실패 시 호환 레이어(레거시 형식) 위임.

**DefaultActionTypeResolver**  
1) Registry에서 stable key로 조회  
2) 없으면 `ActionTypeCompatibilityResolver`로 레거시 해석 (enum name, `EnumName.CONSTANT`)  
3) 모르는 값이면 `IllegalArgumentException`.

### 3.5 ActionTypeCompatibilityResolver (resolver 패키지)

- **역할**: 레거시 입력만 처리. stable key 조회가 실패했을 때만 사용.
- **지원 형식**:  
  - enum 상수 이름 (예: `CODE_GENERATION`)  
  - `EnumName.CONSTANT` (예: `CodingActionType.CODE_GENERATION`)  
- **순서**: 카탈로그에 정의된 enum 클래스 순서로 탐색 (결정론적).
- 신규 연동에는 stable key 사용을 권장합니다.

### 3.6 ActionTypeMetadataLoader (metadata 패키지)

- **Loader**: `action-type-output-behavior.properties`, `action-type-domain.properties`에서 key → OutputBehavior/Domain 맵 로드. config는 Loader만 사용.
- 메타데이터는 properties 리소스(`action-type-output-behavior.properties`, `action-type-domain.properties`)로만 유지.

### 3.7 ActionTypeRegistryHolder (infrastructure/serialization)

- **목적**: Spring 컨텍스트 밖(예: Jackson `ActionTypeDeserializer`)에서 Registry/Resolver 사용.
- **방식**: 정적 `registry`, `resolver`를 설정 클래스에서 주입. `getRegistry()`, `getResolver()`로 접근.
- 초기화 전 호출 시 `IllegalStateException` 발생.

---

## 4. Canonical 계층 (ActionGroup)

### 4.1 ActionGroup

- **의미**: ActionType보다 한 단계 위의 **capability family**.
- **설계 원칙**:  
  - “어떤 종류의 capability가 수행되는가?”만 표현.  
  - 출력 형태·포맷·채널·도메인 버킷·결과·지표는 표현하지 않음.  
  - 단일-leaf 그룹(해당 그룹에 ActionType 하나만 매핑)도 허용.
- **예**:  
  - `CODE_GENERATION`, `CODE_MODIFICATION`, `DEBUGGING`, `MESSAGE_COMPOSITION`, `RESEARCH_METHODOLOGY`, `DATA_ANALYSIS` 등.

### 4.2 CanonicalActionRegistry

- **역할**: ActionType ↔ ActionGroup 해석.
- **메서드**:  
  - `toCanonical(ActionTypeInterface)` → Optional&lt;ActionGroup&gt;  
  - `findByKey(String stableKey)` → Optional&lt;ActionGroup&gt;  
  - `sameCanonicalCapability(a, b)` → 동일 capability 여부 (null 안전).

**DefaultCanonicalActionRegistry**  
각 `ActionTypeInterface`의 `getActionGroup()`을 그대로 사용하고, key 조회는 `ActionTypeRegistry`에 위임합니다.

---

## 5. Category Enum 패턴

각 카테고리 enum은 다음을 따릅니다.

- `ActionTypeInterface` + `StableKeyedEnum` 구현.
- **key 규칙**:  
  - 대부분 `"ACTION.{CATEGORY}." + name()` (예: `ACTION.CODING.CODE_GENERATION`).  
  - 예외: `EmailActionType`처럼 명시적 stable key 필드 사용 (레거시 호환).
- 필드: `displayNameKo/En/Ja`, `outputBehavior`, `actionGroup`, 필요 시 `TaskDomain`.
- Lombok: `@Getter`, `@AllArgsConstructor` (또는 커스텀 생성자).

**CodingActionType 예**:  
- `CODE_GENERATION` → key `ACTION.CODING.CODE_GENERATION`, `ActionGroup.CODE_GENERATION`, `OutputBehaviorType.CODE_IMPLEMENTATION`.

**EmailActionType 예**:  
- 각 상수에 `ACTION.EMAIL.*` 형태의 명시적 stable key.  
- 목적별 상수(THANK_YOU_EMAIL 등)는 `@Deprecated`로 표시하고, “EMAIL_WRITING + purpose 메타데이터” 사용을 권장.

---

## 6. 연동 및 설정

### 6.1 ActionTypeRegistryConfig

- **Catalog** → **Registry** → **CompatibilityResolver** → **Resolver** 순으로 Bean 생성.
- Registry/Resolver 생성 직후 `ActionTypeRegistryHolder`에 설정.
- 패키지 스캔 없이 카탈로그 순서만 사용.

### 6.2 ActionTypeDeserializer (Jackson)

- JSON 문자열을 `ActionTypeInterface`로 역직렬화.
- `ActionTypeRegistryHolder.getResolver().resolve(value)` 호출.
- null/blank일 때 기본값 `EtcActionType.GENERAL_CONSULTATION` 반환 (NPE 방지).

### 6.3 StableKeyedEnum

- `key()`를 통해 API·DB·설정·로그에서 사용할 **안정적 식별자** 계약.
- 공개된 key는 변경·재사용하지 않고, deprecated 상수도 key는 유지하는 것이 원칙입니다.

---

## 7. 데이터 흐름 요약

1. **등록**: `DefaultActionTypeCatalog`의 enum 목록 → `ActionTypeRegistry` 빌드 (stable key 유일, 검증).
2. **입력 해석**: JSON 등 문자열 → `ActionTypeResolver.resolve()` → stable key 우선 조회 → 실패 시 `ActionTypeCompatibilityResolver`로 레거시 해석.
3. **의미 해석**: `ActionTypeInterface` → `getActionGroup()` 또는 `CanonicalActionRegistry.toCanonical()` → 프로필·검증·추천 등에서 capability 단위로 사용.

---

## 8. 요약 표

| 구성요소 | 역할 |
|----------|------|
| **ActionTypeInterface** | 액션 공통 계약 (key, 표시명, TaskDomain, OutputBehavior, ActionGroup) |
| **ActionTypeCatalog** | 등록 enum 클래스 목록 및 순서 (단일 소스) |
| **ActionTypeRegistry** | stable key 기반 저장·조회·검증 |
| **ActionTypeResolver** | 문자열 → ActionType (stable key 우선, 레거시 호환) |
| **ActionTypeCompatibilityResolver** | 레거시 형식만 해석 (enum name, EnumName.CONSTANT) |
| **ActionTypeRegistryHolder** | 비-Spring 컴포넌트용 정적 Registry/Resolver 접근 |
| **ActionGroup** | Capability family (의미 그룹) |
| **CanonicalActionRegistry** | ActionType ↔ ActionGroup 해석, 동일 capability 비교 |

이 구조를 통해 액션 타입의 **안정 식별자**, **다국어·출력·도메인·캐퍼빌리티** 정보가 일관되게 정의·조회·해석됩니다.

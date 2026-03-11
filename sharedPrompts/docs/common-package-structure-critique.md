# common 패키지 구조 냉정 분석

**문서 유지보수:** 이 문서는 시점 기반 분석이므로, 항목이 해결될 때마다 갱신하는 것을 권장합니다. 해결된 항목에는 체크박스(`- [ ]` → `- [x]`) 또는 "Resolved in PR #xxx" 표시를 추가하면 추적이 용이합니다. 장기적으로는 ADR(Architecture Decision Record) 형식으로 전환해 "결정 사항", "상태(제안됨/수락됨/완료됨)", "날짜" 등을 명시하는 것도 고려할 수 있습니다.

## 1. 전체 평가 요약

이 패키지는 **역할/액션/카테고리/출력/시맨틱**을 일관된 계약(StableKeyedEnum, RoleTypeInterface, ActionTypeInterface)과 레지스트리·역직렬화기로 묶어 도메인 정체성을 명확히 하려 한 설계다. EnumCompatParser로 legacy name과 stable key를 동시에 수용하는 점, output을 action에서 분리한 점, role/action을 카테고리별 enum으로 쪼갠 점은 의도가 읽힌다. **세 곳(Deserializer 목록, ActionTypeBehaviorRegistry instanceof, 실제 enum 클래스)을 반드시 동기화해야 하는 구조**이며, 과거에 `MarketingActionType`이 Registry 분기에 누락되어 `getOutputBehavior()` 호출 시 예외가 나던 사례가 있었으나 **현재는 ActionTypeBehaviorRegistry에 상수별 분기로 반영되어 해결된 상태**다. **과설계 여부**: “완전한 과설계”라기보다 **동기화 책임이 분산된 준(準)과설계**에 가깝다. 복잡도 자체보다 “enum 추가 시 반드시 수정해야 할 지점이 3곳 이상”인 것이 가장 위험하다. 결론부터 말하면, **동일한 누락 패턴이 재발하지 않도록 테스트·문서로 강제할 필요가 있으며**, 70개에 가까운 enum 클래스 규모에서 리스트/instanceof 기반 설계는 한계에 도달했다.

---

## 2. 가장 위험한 구조적 문제 3가지

### 2.1 Deserializer · Registry · Enum 클래스의 3중 목록 불일치

**문제가 되는 구조**

- `ActionTypeDeserializer.ACTION_TYPE_ENUMS`: 27개 ActionType enum 클래스 하드코딩.
- `ActionTypeBehaviorRegistry.resolveBehavior()`: Deserializer에 등록된 각 ActionType enum에 대해 `instanceof`(및 필요 시 상수별 switch) 분기가 있어야 한다. **과거 사례(해결됨):** `MarketingActionType`에 대한 분기가 없어 `getOutputBehavior()` 호출 시 예외가 났으나, 현재는 Registry에 상수별 분기로 반영되어 있음.
- 새 ActionType enum을 추가하면 (1) 새 enum 클래스, (2) Deserializer 리스트, (3) Registry의 `instanceof` + 반환값 — 세 곳을 모두 수정해야 한다.

**왜 위험한지**

- 한 곳이라도 빠지면 **런타임 실패**가 난다. Deserializer에만 넣고 Registry를 잊으면 JSON은 파싱되지만 `actionType.getOutputBehavior()` 호출 시 `IllegalArgumentException("Unmapped ActionTypeInterface: ...")` 발생.
- **과거 발생 사례(해결됨):** `MarketingActionType`은 Deserializer에는 포함되어 있었으나 Registry에 분기가 없어, 마케팅 액션 요청 시 역직렬화 후 `getOutputBehavior()` 호출 시점에 예외가 났다. 현재는 ActionTypeBehaviorRegistry에 `MarketingActionType` 상수별 분기(MARKET_RESEARCH/CUSTOMER_ANALYSIS → ANALYTICAL_REPORT, AD_CAMPAIGN/CONTENT_MARKETING/INFLUENCER_MARKETING → LONG_FORM_WRITING, 그 외 → STRATEGIC_PLAN)가 추가되어 해결된 상태다.

**나중에 왜 터지는지**

- 카테고리/액션이 늘어날수록 “Deserializer는 넣었는데 Registry를 안 넣었다” 같은 누락이 반복된다. 컴파일 타임에 잡히지 않고, 특정 요청 경로에서만 터지므로 재현·테스트가 어렵다.

**어떤 변경 때 가장 먼저 드러나는지**

- 신규 ActionType 카테고리 추가 시. (과거에는 사용 빈도가 낮았던 `MarketingActionType`에서 동일한 누락이 발생했으나, 현재는 해결됨.)

**심각도**: **높음** (과거 사례는 해결됨; 동일 패턴 재발 방지를 위해 Deserializer–Registry 동기화를 테스트로 강제할 필요 있음)

---

### 2.2 Role 3분화(Core / Domain / category)와 역직렬화·계약 불일치

**문제가 되는 구조**

- **CoreRoleType**: `StableKeyedEnum`만 구현. 엔진용 “코어 역할”.
- **DomainRoleType** (metadata): `RoleTypeInterface`·`StableKeyedEnum` 모두 미구현. 상수만 있는 단순 enum. **`RoleTypeDeserializer.ROLE_TYPE_ENUMS`에 포함되지 않음.**
- **category 하위 *RoleType** (18개 클래스): `RoleTypeInterface`만 구현. key는 `RoleTypeInterface`가 `StableKeyedEnum`을 상속하여 제공하므로, 별도 `StableKeyedEnum` 구현은 하지 않음.

즉, “역할”이라는 한 개념이 **엔진용 코어 / 메타데이터용 도메인 / API·역직렬화용 카테고리** 세 갈래로 나뉘어 있고, DomainRoleType은 역직렬화 경로에조차 없다.

**왜 위험한지**

- 새 개발자는 “역할을 추가하려면 어디에 넣어야 하지?”에서부터 헷갈린다. Core vs Domain vs category 선택 기준이 코드 한곳에 명시되어 있지 않다.
- DomainRoleType은 key가 없어 직렬화/API 스펙과의 관계가 불명확하고, 다른 레지스트리(예: 프로필·권한)에서만 쓰인다면 “역할”이 두 종류로 나뉜 사실을 모르면 버그나 중복 매핑을 만든다.

**나중에 왜 터지는지**

- “카테고리 RoleType에 넣었는데, 어떤 레이어에서는 DomainRoleType만 본다” 같은 불일치, 또는 역직렬화는 Core+category만 하고 Domain은 별도 경로로만 쓰다가 스펙이 섞이면서 키/이름 충돌이 날 수 있다.

**어떤 변경 때 가장 먼저 드러나는지**

- 새 “역할” 추가 요구 시(예: “메타데이터용 역할 하나 더”, “엔진용 코어 역할 하나 더”). 또는 API 스펙에서 role 필드에 DomainRoleType 값도 허용하기로 할 때 역직렬화 경로를 어디에 붙일지 혼란.

**심각도**: **중간** (현재는 Domain이 역직렬화 밖에 있어서 당장 터지진 않지만, 개념 분리와 확장 시 혼란·중복 위험)

---

### 2.3 EXTRACTION의 프로필 부재와 시맨틱 분기 누수

**문제가 되는 구조**

- `PromptCategory.EXTRACTION`은 `PromptCategory` enum에 정식으로 정의되어 있고, key·i18n·defaultDomain까지 갖춤.
- 반면 `DefaultCategorySemanticProfileRegistry`(domain 패키지)에는 **EXTRACTION용 `CategorySemanticProfile`을 등록하지 않음.** `getProfile(PromptCategory.EXTRACTION)` → `Optional.empty()`.
- `GenerationSemanticResolver`에서 `requestMode == EXTRACTION`이면 `resolveExtraction()`으로 **프로필 조회 없이** category=EXTRACTION, intent=EXTRACT 등으로 축을 고정하는 별도 분기.

**왜 위험한지**

- “카테고리 하나가 프로필 레지스트리에는 없고, resolver if문으로만 처리된다”는 것이 **시맨틱 해석 경로가 두 갈래**임을 의미한다. 일반 카테고리는 “레지스트리 → 프로필 → intent/role 매핑”이고, EXTRACTION만 “레지스트리 무시 → 하드코딩된 결과”라서, EXTRACTION 관련 정책 변경 시 레지스트리와 resolver 양쪽을 모두 봐야 한다.
- common 패키지의 `PromptCategory`는 EXTRACTION을 “정식 카테고리”로 노출하지만, 실제 해석은 application/domain 레이어의 if문에 묶여 있어 **도메인 모델과 해석 로직이 한곳에 정리되지 않음.**

**나중에 왜 터지는지**

- EXTRACTION에 intent/role 제한을 넣거나, 다른 “프로필 없는 카테고리”를 추가할 때 같은 패턴을 복제하게 되고, “프로필 없이 resolver 분기만” 하는 케이스가 늘어나 유지보수와 테스트가 어려워진다.

**어떤 변경 때 가장 먼저 드러나는지**

- EXTRACTION 동작 변경(예: 기본 포맷, 허용 intent 조정) 시. 또는 “EXTRACTION처럼 프로필 없이 처리하는 두 번째 카테고리”를 요구할 때.

**심각도**: **중간** (동작은 하지만, 특수 케이스가 구조적으로 문서화되지 않고 분기로만 존재)

---

## 3. 과설계 여부 냉정 평가

**이 구조는 과설계인가 아닌가**

- **완전한 과설계는 아니다.** 역할/액션/카테고리가 많고, stable key·다국어·출력 행동 매핑 등 요구사항이 실제로 많다. 인터페이스로 key/display 계약을 두고, EnumCompatParser로 마이그레이션을 지원하는 선택은 정당한 편이다.
- **다만 “과한 부분”이 분명하다.**

**과한 부분**

1. **역할/액션 identity에 display·i18n을 직접 묶은 것**  
   `RoleTypeInterface`에 getRoleNameKo/En/Ja, getDescriptionKo/En/Ja가 있고, 각 enum이 6개 문자열을 생성자로 받는다. 언어가 하나 늘어나면 모든 RoleType enum 생성자와 호출부를 건드린다. “식별자”와 “표시용 메타데이터”를 한 인터페이스에 넣어서 확장 비용이 커진다.
2. **ActionTypeBehaviorRegistry의 24개 instanceof**  
   새 ActionType 추가 시 반드시 여기 분기 추가가 필요하다. 과거에는 MarketingActionType 분기 누락으로 런타임 오류가 났으나, 현재는 상수별 분기로 반영되어 해결된 상태다. 다만 “행동”을 enum 타입 이름에 따라 나열하는 방식은 enum 개수가 70개에 가까워진 시점에서 유지보수 한계가 드러난다.
3. **Deserializer의 하드코딩된 enum 클래스 리스트**  
   새 enum 클래스를 만들면 Deserializer 리스트에 추가하는 것을 사람이 기억해야 한다. 컴파일 타임에 “이 인터페이스를 구현한 모든 enum”을 쓰는 구조가 아니라, “이 목록에 있는 것만 역직렬화”라서 누락이 발생한다.

**복잡성이 정당화되는 부분**

- StableKeyedEnum·keyPrefix()로 key 규칙을 통일한 것, output을 action과 분리한 것, semantic/engine/output/request 등 패키지로 관심사 분리한 것은 도메인 규모를 고려하면 타당하다.
- EnumCompatParser로 name()과 key() 둘 다 수용하는 것은 배포·마이그레이션 측면에서 실용적이다.

**현재 단계(프로젝트 규모/팀 규모)에서 적절한지**

- enum 클래스 수가 이미 약 66개 수준이고, 역할/액션만 해도 40개가 넘는다. “작은 팀이 한두 번만 enum 추가하는” 수준을 넘었다. 리스트·instanceof 기반 설계는 **이미 규모 대비 한계**를 보이고 있다.

**결론**

- **“부분 단순화” + “동기화 지점 일원화”가 필요하다.**  
  “지금 당장 전면 재설계”까지는 아니지만, **Deserializer/Registry/실제 enum의 3중 목록을 유지하는 현재 방식은 유지하면 안 되고**, ActionTypeBehaviorRegistry는 instanceof 목록이 아니라 enum 메타데이터 또는 명시적 등록 구조로 바꾸는 방향이 맞다.  
  정리하면: **과설계라기보다 “동기화가 분산된 설계”가 문제**이고, 그 동기화 비용을 줄이는 방향으로의 단순화가 필요하다.

---

## 4. Enum 70개+ 규모에서 실제 유지보수 터지는 지점

### 대규모 enum 체계에서 흔한 붕괴 패턴

- **역직렬화/직렬화와 실제 enum 집합 불일치**: “새 enum은 추가했는데 역직렬화 목록에 안 넣었다” → 특정 값만 400/500 또는 런타임 예외.
- **행동 매핑 레지스트리 누락**: “이 enum은 어떤 OutputBehaviorType으로?”를 사람이 매번 추가해야 해서, 한 곳이라도 빠지면 위와 같이 `getOutputBehavior()` 등에서 예외.
- **문서/스펙과 코드 드리프트**: API 스펙에는 “지원하는 role/action 목록”이 있는데, 실제 Deserializer/Registry 목록과 달라짐.
- **key vs name() 혼용**: 클라이언트는 key로 보내는데 서버는 name()으로만 비교하거나, 그 반대. EnumCompatParser가 있더라도 “어떤 필드가 key고 어떤 필드가 name인지” 스펙이 흐리면 불일치 발생.

### 이 코드베이스에서 이미 보이는 초기 징후

- **ActionTypeBehaviorRegistry에 MarketingActionType 분기 없음** → 역직렬화는 되지만 `getOutputBehavior()`에서 `IllegalArgumentException`. “레지스트리와 역직렬화 목록 동기화”가 이미 깨진 사례.
- **RoleTypeDeserializer.ROLE_TYPE_ENUMS**에 17개 클래스가 하드코딩. 여기에 없는 RoleType을 JSON에 넣으면 `EnumResolver.resolve`에서 `IllegalArgumentException("Unknown enum value: ...")`. 새 RoleType 추가 시 이 리스트를 수동으로 갱신해야 함.

### 아직은 괜찮지만 위험 신호인 부분

- **ActionType key() 구현 방식 불일치**: `ActionTypeInterface`는 `StableKeyedEnum`을 상속하지 않고, 각 enum이 `key()`를 직접 구현. WritingActionType은 `stableKey` 필드를 두고, MarketingActionType은 `"ACTION.MARKETING." + name()`로 조합. 패턴이 통일되지 않아, 나중에 key 규칙을 바꿀 때 모든 ActionType을 검토해야 함.
- **RoleType**: category 하위 *RoleType은 모두 `RoleTypeInterface`만 구현하며, key는 인터페이스 상속(RoleTypeInterface extends StableKeyedEnum)으로 제공. 확장 규칙이 정리된 상태. “어떤 것이 계약인가”가 한눈에 안 들어옴.

### 문서/레지스트리/직렬화/매핑 불일치가 장기적으로 생기는 경로

- 새 카테고리/역할/액션 추가 시: (1) enum 클래스, (2) Deserializer 리스트, (3) ActionTypeBehaviorRegistry 분기, (4) 필요 시 DefaultCategorySemanticProfileRegistry 등록, (5) API 문서. 다섯 곳 중 하나라도 빠지면 “지원한다고 문서에 썼는데 동작 안 함” 또는 “동작은 하는데 문서에 없음”이 됨.
- EXTRACTION처럼 “프로필 없이 resolver 분기로만 처리”하는 케이스가 늘면, “어떤 카테고리가 레지스트리 기반이고 어떤 것이 분기 기반인지”가 코드만으로는 파악하기 어려워짐.

### 신규 개발자가 가장 헷갈릴 지점

- **역할을 넣을 곳**: CoreRoleType / DomainRoleType / category 하위 *RoleType 중 어디에 넣어야 하는지, 그리고 DomainRoleType은 역직렬화에 안 타는지.
- **액션 추가 절차**: enum만 만들면 안 되고, Deserializer + ActionTypeBehaviorRegistry를 반드시 같이 수정해야 한다는 것을 코드 구조만 보고는 놓치기 쉬움.
- **EXTRACTION**: PromptCategory에는 있는데, CategorySemanticProfile 레지스트리에는 없고, 별도 `resolveExtraction()`으로만 처리된다는 사실.

---

## 5. 파일/구조별 상세 진단

### StableKeyedEnum

- **현재 상태**: contract 패키지에 단일 인터페이스, key() 계약만 정의. EnumCompatParser가 key()로 역파싱 지원.
- **장점**: 시리얼 키를 name()과 분리해 리팩터링·다국어 키 정책을 유지할 수 있음.
- **문제점**: “어떤 enum이 StableKeyedEnum을 구현해야 하는가”가 코드베이스 전체에 문서화되어 있지 않음. RequestMode, RequestType, ResponseShape, OutputNeeds, SortType, StyleAxis, ExperienceLevelBucket, DomainRoleType 등은 미구현. 노출 정책(API/DB/설정에 나가는 것만 구현 등)이 일관되게 적용되지 않음.
- **방치 시 리스크**: 새 enum 추가 시 구현 여부를 개발자 재량에 맡기게 되어, 일부만 key 기반 직렬화를 쓰는 혼재가 계속됨.

### RoleTypeInterface / ActionTypeInterface

- **RoleTypeInterface**: StableKeyedEnum 상속, keyPrefix() + default key(), getRoleName*/getDescription* (Ko/En/Ja) 6개 + getRoleNameByLang/getDescriptionByLang. display/설명이 인터페이스에 직접 포함.
- **ActionTypeInterface**: StableKeyedEnum 미상속. key(), getDisplayName*(Ko/En/Ja), getTaskDomain(), getDefaultObjective()(deprecated), getOutputBehavior()(기본 구현이 Registry 호출).
- **장점**: RoleType은 key 규칙을 keyPrefix()로 통일했고, ActionType은 행동을 Registry에 위임한 점은 책임 분리 측면에서 나쁘지 않음.
- **문제점**: (1) RoleType의 6개 display 메서드는 언어 추가 시 모든 구현체 수정. (2) ActionType은 key()를 구현체마다 다르게 구현(필드 vs 문자열 조합). (3) ActionTypeInterface가 StableKeyedEnum을 extends 하지 않아, “key를 쓰는 모든 것”을 한 계약으로 다루기 어렵다.
- **방치 시 리스크**: 언어/로케일 확장 시 RoleType 수정 비용이 크고, ActionType key 규칙이 더 불일치해질 수 있음.

### DomainRoleType / CoreRoleType / category RoleType

- **현재 상태**: CoreRoleType = 엔진용, StableKeyedEnum만. DomainRoleType = 메타/페르소나용, key·인터페이스 없음. category *RoleType = API/역직렬화용, RoleTypeInterface만 구현(key는 인터페이스가 StableKeyedEnum 상속으로 제공).
- **장점**: 용도별로 역할을 나눈 의도는 읽힘.
- **문제점**: 세 가지가 “역할”이라는 같은 이름을 쓰지만 역직렬화·계약·사용처가 다르고, 한곳에 “역할 계층/용도”가 정리되어 있지 않음. DomainRoleType이 Deserializer에 없어서, API에서 “role” 필드로 DomainRoleType 값을 받는 경로가 있다면 별도 처리 필요.
- **방치 시 리스크**: 새 역할 타입 추가 시 Core/Domain/category 중 어디에 넣을지 혼란, 또는 중복 정의.

### ActionTypeBehaviorRegistry

- **현재 상태**: 정적 메서드 `resolveBehavior(ActionTypeInterface)` 하나. Deserializer에 등록된 각 ActionType에 대해 `instanceof`(및 일부는 상수별 switch, 예: MarketingActionType) 분기로 OutputBehaviorType 반환. MarketingActionType은 상수별 분기로 반영됨(해결됨).
- **장점**: ActionType 수백 개 상수를 타입 단위로 묶어서 코어 행동으로 줄인 것은 이해 가능.
- **문제점**: (1) 새 ActionType enum 추가 시 반드시 여기 분기 추가 필요. (2) Deserializer 목록과 완전히 별도로 관리되어 동기화가 사람 몫. (3) 과거에는 누락으로 런타임 오류가 있었으나 현재는 해결됨; 동일 패턴 재발 방지를 위해 커버리지 테스트 등으로 강제할 필요 있음.
- **방치 시 리스크**: 액션 추가할 때마다 Registry 누락 가능, 테스트가 모든 액션을 커버하지 않으면 프로덕션에서만 터짐.

### serializer/* (EnumResolver, EnumCompatParser, RoleTypeDeserializer, ActionTypeDeserializer)

- **EnumCompatParser**: name() 우선, 그다음 StableKeyedEnum이면 key()로 매칭. LENIENT/STRICT 모드. 구조는 단순하고 역할이 명확함.
- **EnumResolver**: baseEnum + categoryEnums 리스트를 순서대로 시도. RoleType은 baseEnum 없이 categoryEnums만 사용. 동일 key/name이 두 enum에 있으면 “먼저 나온 클래스”가 이김. 충돌 가능성은 낮지만 존재함.
- **Deserializer**: ROLE_TYPE_ENUMS / ACTION_TYPE_ENUMS 하드코딩. 새 enum 추가 시 여기 추가 필수.
- **장점**: EnumCompatParser로 레거시·신규 키 모두 수용 가능. EnumResolver는 범용적으로 쓰일 수 있음.
- **문제점**: Deserializer가 “이 인터페이스를 구현한 모든 enum”을 자동으로 못 쓰고, 목록을 수동으로 맞춰야 함.
- **방치 시 리스크**: 역직렬화 목록과 실제 enum·Registry 불일치가 반복됨.

### semantic/* (PromptCategory, TaskDomain, ActionIntent, PromptObjective)

- **현재 상태**: PromptCategory는 StableKeyedEnum, I18n, defaultDomain 보유. EXTRACTION 포함. TaskDomain은 GuidelinePolicy·StableKeyedEnum. ActionIntent, PromptObjective는 key 미구현 등으로 StableKeyedEnum 아님.
- **장점**: 카테고리·도메인·의도 분리가 시맨틱 해석에 쓰이기 좋게 되어 있음.
- **문제점**: EXTRACTION은 PromptCategory에 있지만 CategorySemanticProfile 레지스트리에는 없고, 별도 resolver 분기로만 처리됨. PromptObjective 등은 API에 노출되는지에 따라 StableKeyedEnum 정책과 맞출지 결정이 필요.
- **방치 시 리스크**: EXTRACTION 유사 케이스가 늘어나 프로필 없는 분기만 늘어남.

### output/*

- **현재 상태**: OutputBehaviorType, ResponseStructure, ResponseShape, OutputNeeds, OutputFormat. output 패키지로 action과 분리됨.
- **장점**: 출력 메타를 action과 분리한 것은 응집도 측면에서 좋음.
- **문제점**: ResponseShape, OutputNeeds 등은 StableKeyedEnum 미구현. “외부 노출 enum은 모두 key” 정책을 적용할지 여부가 미정.
- **방치 시 리스크**: 직렬화 포맷 정책이 enum별로 제각각일 수 있음.

### package structure (common 전체)

- **현재 상태**: contract, constants, enums(role/action/semantic/engine/style/output/request/experience/sort, serializer), guideline, i18n, style. role·action은 category 하위로 12~13개 패키지.
- **장점**: 역할/액션/시맨틱/엔진/출력 등 관심사별로 디렉터리가 나뉘어 있어 이름만으로 대략적인 위치를 찾기 쉬움.
- **문제점**: “카테고리 하나 추가”가 한 패키지가 아니라 role category, action category, Deserializer, Registry, 필요 시 CategorySemanticProfile 등 여러 곳을 건드리게 됨. 패키지 수는 많지만 “추가 시 수정 지점”이 패키지 밖으로 흩어져 있음.
- **방치 시 리스크**: 새 개발자는 “카테고리 추가”가 단일 패키지 작업이 아니라 전역 작업이라는 것을 놓치기 쉽고, 누락이 반복됨.

---

## 6. 반드시 손봐야 하는 것 vs 지금은 두어도 되는 것

### 반드시 손봐야 하는 것

1. **ActionTypeBehaviorRegistry–MarketingActionType (완료)**  
   마케팅 액션 요청 시 getOutputBehavior()에서 예외가 나던 문제는 상수별 분기(MARKET_RESEARCH/CUSTOMER_ANALYSIS → ANALYTICAL_REPORT, AD_CAMPAIGN/CONTENT_MARKETING/INFLUENCER_MARKETING → LONG_FORM_WRITING, 그 외 → STRATEGIC_PLAN) 추가로 해결됨.

2. **Deserializer/Registry/실제 enum 동기화를 한 곳으로 끌어오기**  
   “새 ActionType 추가 시 수정할 곳”을 최대한 한 곳으로. 예: ActionType enum에 @OutputBehavior 같은 메타데이터를 두고 Registry는 리플렉션/등록 리스트로 자동 수집하거나, “등록 리스트 하나”를 만들어서 Deserializer와 Registry가 같은 소스를 참조하도록 변경. 최소한 “Registry 분기 누락”을 컴파일/테스트로 잡을 수 있게(예: 모든 ACTION_TYPE_ENUMS에 대해 resolveBehavior가 예외를 던지지 않음을 검증하는 테스트) 해야 함.

3. **EXTRACTION 동작 문서화**  
   DefaultCategorySemanticProfileRegistry 또는 GenerationSemanticResolver에 “EXTRACTION은 CategorySemanticProfile 없이 resolveExtraction() 분기로만 처리된다”는 주석/문서 추가. EXTRACTION을 “프로필 없는 특수 카테고리”로 명시해, 나중에 비슷한 케이스 추가 시 정책을 재사용할 수 있게 함.

4. **Role 3분화(Core/Domain/category) 한 곳에 정리**  
   common 또는 enums/role 패키지에 README 또는 package-info로 “CoreRoleType = 엔진용, DomainRoleType = 메타/페르소나용(역직렬화 없음), category *RoleType = API·역직렬화용”을 명시. 새 역할 추가 시 어디에 넣을지 기준을 코드와 함께 두는 것.

5. **StableKeyedEnum 적용 범위 정책 수립**  
   “API/DB/설정에 노출되는 enum은 StableKeyedEnum 구현” 등 한 줄 정책을 정하고, 해당 enum들에 구현을 붙이거나 “의도적 미구현”을 문서화. 그래야 신규 enum 추가 시 구현 여부를 일관되게 결정할 수 있음.

### 지금은 두어도 되는 것

- **RoleType의 “implements RoleTypeInterface, StableKeyedEnum” vs “RoleTypeInterface”만**: 동작은 동일하므로 리팩터는 나중에 일괄 정리해도 됨. 다만 새로 만드는 RoleType은 RoleTypeInterface만 구현하도록 팀 규칙으로 두면 됨.
- **ActionType key() 구현 방식 통일**(stableKey 필드 vs "ACTION.XXX." + name()): 당장 버그를 유발하지는 않으므로, 새 ActionType 추가 시 keyPrefix() 스타일로 통일하는 방향만 정해 두고 점진적으로 맞춰도 됨.
- **guideline/i18n 구조**: 현재 수준이면 유지 가능. 다만 RoleType의 6개 display 메서드가 언어 추가 시 비용이 크다는 점만 인지하고, “언어 추가” 요구가 들어오면 그때 descriptor/i18n 키 기반으로 빼는 걸 검토하면 됨.
- **패키지 재구성(예: role/action 카테고리 병합)**: 카테고리 수가 크게 늘지 않는 한, 현재 패키지 이름만으로도 위치 파악은 가능. “수정 지점 일원화”가 먼저이고, 패키지 구조 자체의 대대적 변경은 그 다음 검토해도 됨.

**다시 손봐야 하는 조건**

- 언어/로케일이 추가되면 RoleType display 메서드 확장 비용을 재평가하고, 필요 시 descriptor·i18n 키로 이전.
- ActionType/역할 카테고리가 크게 늘어나면(예: 20개 이상 추가) Registry를 메타데이터·자동 등록 방식으로 전환하는 것을 반드시 검토.
- “프로필 없는 카테고리”가 EXTRACTION 외에 하나 더 생기면, “프로필 없음”을 명시적으로 표현하는 공통 패턴(예: Optional profile, resolver 분기 정책)을 도입하는 것이 좋음.

---

## 7. 최종 결론

이 구조는 **“의도와 계약은 읽히지만, enum 추가·변경 시 동기화해야 할 지점이 여러 곳으로 흩어져 있는 구조”**다. **ActionTypeBehaviorRegistry–MarketingActionType 누락으로 런타임 오류가 나던 문제는 상수별 분기 추가로 해결된 상태**다.  
따라서 **“이미 복잡도가 과한 구조”**라기보다 **“복잡도는 감당 가능한데, 유지보수 지점이 분산되어 있어 팀 규모가 커지거나 enum이 더 늘어나면 위험한 구조”**에 가깝다.  
즉, **지금은 버틸 수 있지만, Deserializer·Registry·enum 목록의 3중 동기화를 그대로 두고 계속 가면 팀이 커질수록 누락과 런타임 실패가 재발할 가능성이 높다.**  
**“새 ActionType 추가 시 반드시 수정할 곳”을 한 곳으로 모으거나 테스트(커버리지)로 강제**하는 것을 진행하고, 그 다음 StableKeyedEnum 정책·EXTRACTION 문서화·역할 3분화 설명을 정리하는 순서를 권장한다.

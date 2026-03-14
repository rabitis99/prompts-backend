# common 패키지 구조 분석 — 2차 감사(감사) 보고서

**대상:** `docs/common-package-structural-analysis.md` 및 해당 md가 다루는 `org.example.sharedprompts.domain.prompt.common` 및 직접 연관 계층  
**목적:** 기존 보고서의 부족함을 공격적으로 검증하고, 놓친 문제·과소평가된 위험·불완전한 결론을 추가로 드러냄.  
**성격:** 기존 md를 승인하지 않고, "그 md조차 아직 덜 봤다"는 것을 증명하는 2차 구조 감사.

---

## 1. 기존 md 보고서의 강점

- **Catalog 순서가 해석·노출 의미를 가진다는 점을 정확히 짚음.** `ENUM_CLASSES` 순서 = 레거시 first-match = `allInCatalogOrder`라는 단일 사실을 명시했고, 순서 변경 시 역직렬화·호환 결과가 바뀐다는 위험을 올바르게 지적함.
- **직렬화 계층의 정책 침범을 구체적으로 지목.** `ActionTypeDeserializer`의 blank → `GENERAL_CONSULTATION` 하드코딩을 “계약 vs 정책 혼합”으로 분리해야 할 대상으로 명확히 제시함.
- **SOLID 위반을 파일/클래스 단위 표로 정리.** SRP/OCP/LSP/ISP/DIP 각각에 대해 “어디가, 왜, 어떤 위험”인지 나열해 재검토 포인트를 제공함.
- **Definition vs Catalog vs Index vs Compatibility vs Serialization vs Policy 분리안을 구체적으로 제시.** “Catalog는 순서 없는 집합만”, “Deserializer는 blank → null/empty만” 등 실행 가능한 방향을 적었음.
- **확정성 분석에서 순서 의존·fallback·registry 초기화·런타임 해석 일관성을 표로 정리**하고, “first match wins가 문서화만 되어 있고 코드로 강제되지 않는다”는 한계를 짚음.

---

## 2. 기존 md 보고서의 한계

### 2.1 빠진 문제

- **`getAll()` 순서가 도메인·애플리케이션 계층까지 파급되는 경로를 추적하지 않음.**  
  `ActionTypeRegistry.getAll()`은 `DefaultCategorySemanticProfileRegistry.deriveActionsFromGroups(..., actionTypeRegistry.getAll())`에서 사용된다.  
  즉 Catalog 순서는 “레거시 해석 순서”뿐 아니라 **추천 후보 목록의 구성·순서(seed 먼저, 그 다음 catalog 순서로 같은 그룹 확장)**에 직접 반영된다.  
  UI 추천 순서·검증 시 사용하는 “compatible actions” 리스트가 Catalog 순서에 종속되는데, 기존 md는 `getAll()`을 “노출 순서” 수준에서만 언급하고 **domain/semantic 레이어로의 누수**를 분석하지 않았다.

- **Canonical의 “정의 vs 런타임” 이원화를 명시하지 않음.**  
  `DefaultCanonicalActionRegistry.toCanonical(actionType)`는 `actionType.getActionGroup()`을 쓰지 않고 **`actionType.key()`로 Registry를 다시 조회**한 뒤 그 결과의 ActionGroup을 반환한다.  
  즉 “canonical 의미”의 진실 소스는 enum이 아니라 **Registry에 등록된 항목**이다.  
  미등록 ActionType(다른 모듈/테스트용 인스턴스)을 넘기면 항상 `Optional.empty()`가 되어, 인터페이스만 보면 “ActionType → ActionGroup 변환”인데 실제로는 “Registry에 있는 ActionType만 의미 있음”이라는 **Hidden source of truth**가 있다. 기존 md는 이 점을 드러내지 않았다.

- **정의 검증 시점/부트스트랩 전략의 불명확성을 root 쪽으로 올리지 않음.**  
  “Registry가 validation까지 가진다”는 것은 **언제, 어떤 단계에서 정의가 유효한지 검증하는지**에 대한 전략이 없기 때문이다.  
  Catalog → Registry 구축 시점에 한꺼번에 검증이 일어나고, 그 전에 “정의만 모아두는 단계”나 “검증만 수행하는 단계”가 분리되어 있지 않다.  
  기존 md는 “Registry에서 검증 분리”만 제안했지, **부트스트랩 단계 설계 자체의 부재**를 root로 다루지 않았다.

- **입력 해석 책임의 다중 진입점을 하나의 “입력 해석” 문제로 묶지 않음.**  
  Deserializer(blank 정책 + Holder 의존), Resolver(stable key → compatibility), CompatibilityResolver(레거시 first-match), EnumResolver(다중 enum 순차 시도), EnumCompatParser(단일 enum 내 name/key)가 각각 다른 경로로 “문자열 → ActionType”에 관여한다.  
  기존 md는 “Deserializer가 policy를 가진다”에 집중했지만, **“문자열 해석”이라는 하나의 책임이 여러 entry point에 나뉘어 있고, 각각 다른 fallback/순서/모드 정책을 가진다**는 구조적 문제는 통합적으로 서술하지 않았다.

### 2.2 약하게 본 문제

- **MetadataLoader 실패 시 “리소스 없음 = 빈 맵”을 “silent semantic corruption”으로까지 올리지 않음.**  
  기존 md는 “실패 시 빈 맵 반환” 위험을 “상”으로 두었지만, **시스템이 정상 기동하고 API는 동작하는데, getOutputBehavior/getTaskDomain이 전부 empty인 상태**가 될 수 있다는 점을 “가장 위험한 종류의 실패(silent semantic corruption)”로 명시하지 않았다.  
  운영/모니터링에서 “에러 로그 없이 의미만 빠진 상태”는 발견하기 어렵다.

- **ActionTypeCatalog 인터페이스가 이미 “순서”를 계약에 포함하고 있음을 비판하지 않음.**  
  `ActionTypeCatalog.getActionTypeEnumClasses()` 반환 타입이 `List`이고, 주석에 “catalog 순서의”라고 되어 있다.  
  즉 “목록만 제공”하는 추상과 “순서가 있는 목록을 제공”하는 추상이 **인터페이스 단에서부터 혼합**되어 있어, Catalog 구현체만 바꿔서는 “순서 없는 집합”으로의 전환이 불가능하다.  
  기존 md는 DefaultActionTypeCatalog 구현체 분리만 강조했지, **인터페이스 자체가 순서를 전제**하고 있다는 점을 지적하지 않았다.

- **static/global state 오염이 테스트·멀티 컨텍스트·점진적 모듈화에서 갖는 위험을 “중”에서 더 올리지 않음.**  
  Holder는 “한 번만 설정, clearForTest로 해제” 패턴이고, Config 빈 생성 순서에 묶여 있다.  
  같은 JVM에서 두 번째 Spring 컨텍스트를 띄우거나, 테스트 순서에 따라 Holder 상태가 달라지면, “중”이 아니라 **테스트 불안정·환경 의존**으로 이어질 수 있다.  
  기존 md는 “초기화 위험”을 “중”으로 두었지만, **병렬 테스트·멀티 컨텍스트**를 전제하면 위험도가 더 올라간다.

### 2.3 잘못 분류한 문제

- **ActionTypeMetadataLoader의 “잘못된 enum 이름 스킵”.**  
  현재 코드에서는 properties 값이 잘못된 enum 이름이면 `Enum.valueOf` 실패 시 **예외를 던지고** 빈 맵을 반환하지 않는다.  
  “스킵”이 있는 것은 **리소스가 아예 없을 때**(`in == null`)만 빈 맵 반환이다.  
  따라서 “잘못된 enum 이름 스킵”은 현재 구현과 맞지 않으며, **“리소스 부재 시 조용한 빈 맵”** 쪽으로 문제를 재분류하는 것이 맞다.

- **DefaultCanonicalActionRegistry를 “toCanonical + findByKey + sameCanonicalCapability가 한 인터페이스에 있다”는 ISP 관점으로만 다룸.**  
  실제로는 **toCanonical이 인자로 받은 ActionType의 getActionGroup()을 사용하지 않고 Registry 재조회만 한다**는 동작이 더 근본적이다.  
  즉 “조회 vs 비교” 분리보다, **“정의(enum)가 아닌 런타임(registry)이 canonical의 진실 소스”**라는 분류가 우선되어야 한다.

### 2.4 결론이 덜 강한 부분

- **“진짜 root cause”를 한 개로 단정하지 않음.**  
  기존 md는 Catalog 순서, 직렬화 정책, Registry 검증, first-match, MetadataLoader 등을 나열했지만, **이들을 일으키는 가장 깊은 원인 하나**를 명시적으로 고르지 않았다.  
  그 결과 “부분 리팩터로 부족, 핵심 계층 분리 필요”라는 판정이 **어떤 한 가지 구조적 근본 원인**에서 비롯된 것인지 읽기 어렵다.

- **“common에 있어서는 안 되는 것”을 거의 다루지 않음.**  
  common 내부 책임 분리만 제안했지, **resolver/registry/metadata/compatibility/serializer 정책이 common에 있는 것 자체**가 “공통 정의/계약” 레이어를 넘어서 인프라·부트스트랩을 삼키고 있는지, 그리고 **common 밖으로 빼야 할 후보**를 구체적으로 제시하지 않았다.

- **enum을 “정의 모델”로 유지해도 되는지에 대한 결론을 내리지 않음.**  
  “인터페이스 분리”와 “enum 중심 구조 자체의 한계(600+ 규모에서 빌드/merge/metadata 동기화/테스트 폭발)”를 구분해서, **enum을 계속 중심 모델로 쓸 것인지, 정의 모델과 실행 모델을 분리할 것인지**까지 밀어붙이지 않았다.

---

## 3. 추가로 발견한 핵심 구조 문제

(기존 md에 없던 것만 기술. 기존 내용 반복 없음.)

- **Catalog 순서가 semantic/domain 계층까지 전달됨.**  
  `DefaultCategorySemanticProfileRegistry.prepare()` → `deriveActionsFromGroups(..., actionTypeRegistry.getAll())`에서 **getAll() 순서가 “추천 후보 목록의 순서(seed 우선, 이후 catalog 순서로 같은 그룹 추가)”를 결정**한다.  
  따라서 Catalog 순서는 단순 호환성뿐 아니라 **UI 추천 순서, 검증 시 사용하는 compatible actions 리스트, 그리고 그 리스트를 소비하는 SemanticValidationService/SemanticRecommendationService**까지 영향을 준다.  
  즉 **순서 의존이 common을 넘어 domain/application 계층에 침투**해 있다.

- **Canonical 의미의 진실 소스가 Registry로 고정됨.**  
  `DefaultCanonicalActionRegistry.toCanonical(actionType)`는 `actionType.getActionGroup()`을 사용하지 않고 `actionTypeRegistry.getByStableKey(actionType.key())`로만 조회한다.  
  따라서 “동일한 capability” 판단은 **반드시 해당 ActionType이 현재 Registry에 등록되어 있을 때만** 가능하다.  
  등록되지 않은 ActionType(다른 Catalog/테스트용 집합)은 canonical 조회에서 항상 empty가 되며, **정의(enum)와 런타임(registry)이 이중화**되어 있다.

- **ActionTypeCatalog 인터페이스가 이미 “순서 있는 목록”을 계약에 포함함.**  
  `getActionTypeEnumClasses()` 반환 타입이 `List`이고, Javadoc에 “catalog 순서의”라고 명시되어 있어, **순서 없는 집합만 제공하는 Catalog**로 바꾸려면 인터페이스 자체를 바꿔야 한다.  
  즉 “Catalog 구현체만 수정”으로는 해결되지 않고, **계약(인터페이스) 단에서부터 순서와 목록이 결합**되어 있다.

- **Config가 MetadataLoader를 static으로만 사용함.**  
  `ActionTypeRegistryConfig`에서 `ActionTypeMetadataLoader.loadKeyToOutputBehavior()`, `loadKeyToDomain()`를 빈 생성 시 직접 호출한다.  
  “key→정책 매핑 제공자” 추상이 없어, **테스트·대체 구현(DB/원격) 주입이 불가**하고, 리소스 부재 시 빈 맵이 그대로 Registry에 주입된다.  
  실패 정책이 Config 단에서도 노출되지 않는다.

- **DeserializerEnumTestUtils가 production Catalog를 단일 static 인스턴스로 유지함.**  
  테스트가 `new DefaultActionTypeCatalog()` 한 번만 생성해 재사용한다.  
  **“Catalog 순서를 바꾼 구현체”로 테스트하는 경로가 없고**, 순서 변경 시 실패해야 하는 테스트도 없다.  
  따라서 **테스트가 순서 민감성을 검증하지 못하며**, 리팩터링 시 순서를 바꿔도 테스트가 막아주지 않는다.

- **ResolutionConfig가 구체 ActionType enum 상수에 직접 의존함.**  
  `EXPLICIT_MAPPINGS`가 `CodingActionType.CODE_REVIEW`, `EtcActionType.EXPLANATION` 등 **구체 enum 상수**를 키로 사용한다.  
  새 ActionType 추가 시 이 맵 수정이 필요할 수 있고, **Config 계층이 common enum에 직접 결합**되어 있다.  
  기존 md는 ActionTypeRegistryConfig의 DIP 위반만 언급했지, ResolutionConfig의 **도메인(목표 매핑)과 common enum의 직접 결합**은 다루지 않았다.

- **EnumCompatParser의 KEY_INDEX가 static ConcurrentHashMap으로 전역 상태.**  
  per-enum-class 인덱스가 클래스 로드·첫 사용 순서에 따라 lazy 구축된다.  
  **다중 클래스로더·테스트 격리**가 필요한 환경에서는 “같은 enum 클래스”라도 로더가 다르면 인덱스가 별도로 구축될 수 있고, 범용 enum 유틸이 **전역 static 상태**를 갖는 것이 장기적으로 테스트·모듈화를 해친다.

- **RuleContext가 ActionTypeInterface 전체를 보유함.**  
  `RuleContext.of(..., actionType, ...)`에서 actionType이 풀 인터페이스로 전달되며, **key만 필요한 규칙도 display/actionGroup에 간접 의존**할 수 있는 구조다.  
  guideline/context가 common의 “풀 계약”에 묶여 있어, **ISP 위반이 domain 쪽 RuleContext까지 퍼져 있다**.

---

## 4. 진짜 root cause 1개

**이 구조를 가장 깊은 수준에서 망가뜨리는 원인 한 가지:**

**“정의(Definition)와 해석·등록·실행(Resolution/Registry/Runtime)을 구분하지 않고, 하나의 모델(enum + catalog + registry)에 동시에 얹어둔 것”**

- “어떤 ActionType이 존재하는가”는 enum + Catalog에,
- “어떤 순서로 해석·노출할 것인가”는 같은 Catalog의 List 순서에,
- “문자열이 어떤 ActionType인가”는 Resolver/Compatibility/EnumResolver/Deserializer에,
- “어떤 ActionType이 어떤 OutputBehavior/TaskDomain/ActionGroup인가”는 Registry/MetadataLoader/Canonical에

붙어 있어, **정의(identity·집합)**와 **해석·정책·우선순위·메타데이터**가 서로 다른 변경 이유와 수명을 가짐에도 **같은 진입점(Catalog 순서, 같은 enum, 같은 registry)**에 묶여 있다.

그 결과:

- Catalog 순서 하나가 레거시 해석·getAll()·추천 순서·도메인 프로파일 파생까지 동시에 영향을 주고,
- enum은 “정적 정의”이면서도 display/key/group을 모두 품어 런타임 객체처럼 쓰이며,
- “문자열 → ActionType” 책임이 Deserializer/Resolver/Compatibility/EnumResolver/EnumCompatParser에 나뉘어 있고,
- canonical 의미는 enum이 아니라 Registry 재조회에만 의존한다.

**정리:**  
**Semantic identity(무엇이 존재하는가)와 resolution policy(어떤 순서·규칙으로 해석·노출·매핑할 것인가)를 한 모델에 억지로 섞어둔 것이 root cause다.**  
그래서 순서 의존, 정책 침범, 검증 시점 불명확, 다중 진입점, MetadataLoader silent failure가 모두 같은 근본 원인에서 나온 증상으로 보는 것이 맞다.

---

## 5. 위험도 재평가

기존 md보다 위험도를 올려야 하는 항목.

| 항목 | 기존 판단 | 재판정 | 상향 이유 |
|------|-----------|--------|-----------|
| MetadataLoader 리소스 없음 시 빈 맵 | 상(조용한 실패) | **상(최상)** | “정상 기동·API 동작하나 의미만 빠진 상태(silent semantic corruption)”로 명시. 운영에서 에러 없이 정책이 비활성화되는 것이 가장 위험한 실패 유형임. |
| registry/catalog 초기화·static Holder | 중 | **상** | 테스트 격리·멀티 컨텍스트·병렬 실행·점진적 모듈화 시 충돌·불안정 위험이 큼. “한 번만 설정”이 깨지면 디버깅이 매우 어려움. |
| 순서 의존 | 상 | **상(유지)** | 단, **파급 범위**를 명시: UI 추천 순서, 검증용 compatible list, 도메인 프로파일 파생(DefaultCategorySemanticProfileRegistry)까지 연결됨. |
| fallback 위험(Deserializer blank 정책) | 상 | **상(유지)** | 진입점별 정책 불일치만이 아니라, “기본값이 직렬화 계층에 하드코딩”되어 다른 진입점(API/서비스)과 의미 불일치 가능. |
| stable key 이원화(name 파생 vs 상수) | LSP 위반으로 기술 | **시간적 계약 취약성**으로 상향 | name() 리네임 시 CodingActionType 등은 외부 계약(key)이 깨짐. “동일 인터페이스 치환” 이상으로 **외부·저장된 데이터와의 시간적 안정성** 문제임. |

---

## 6. common 밖으로 빼야 할 것들

| 구성요소 | 현재 위치 이유(추정) | 왜 common에 있으면 안 되는가 | 이동 권장 레이어 |
|----------|----------------------|-----------------------------|------------------|
| ActionTypeResolver, DefaultActionTypeResolver | “ActionType 해석”을 common에서 제공 | 해석은 **런타임 정책·진입점**에 가깝고, common은 “정의·계약”만 두는 편이 맞음. common에 두면 모든 모듈이 같은 해석 정책에 묶임. | infrastructure/resolution 또는 application 쪽 “입력 해석” 전용 패키지 |
| ActionTypeCompatibilityResolver | 레거시 호환을 common enum과 같이 두기 위함 | 레거시 형식 파싱·first-match 순서는 **마이그레이션/호환 정책**이며, 정의(enum)와 같은 패키지에 있으면 “공통 정의”로 오인되고 순서 계약이 숨음. | infrastructure/serialization 또는 compatibility 전용 패키지 |
| ActionTypeRegistry | stable key 인덱스를 “ActionType의 공통 저장소”로 봄 | Registry는 **부트스트랩·런타임 인덱스**이다. common에 있으면 “정의”와 “등록된 집합”이 혼동되고, Catalog와의 결합이 당연시됨. | infrastructure/registry 또는 domain이 “등록된 ActionType 집합”을 소비할 때만 인터페이스(port)를 두고 구현은 infrastructure |
| ActionTypeMetadataLoader | 메타데이터가 “ActionType에 대한 정책”이라 common에 둠 | 로딩·실패 정책·classpath 의존은 **인프라**이다. common에 두면 테스트·대체 소스 주입이 어렵고, “정의” 패키지가 리소스 존재 여부에 의존함. | infrastructure/config 또는 policy 로딩 전용 모듈. common에는 “key→OutputBehavior/key→TaskDomain 제공자” 인터페이스만 |
| ActionTypeDeserializer | JSON↔ActionType이 common enum과 가까워서 | 역직렬화 + blank 정책 + Holder 의존은 **직렬화 인프라**이다. common에 두면 Jackson·static에 domain이 묶임. | infrastructure/serialization. common에는 “문자열→ActionType 해석기” 추상만 필요 시 |
| EnumResolver, EnumCompatParser | 범용 enum 파싱이 “공통”이라 common | “다중 enum 순차 시도”“순서=우선순위”는 **해석 정책**이다. 범용 유틸이 static KEY_INDEX를 가지면 테스트·모듈화에 악영향. | serializer/compat는 infrastructure, “단일 enum 파싱”만 common contract에 가깝게 유지할 수 있으나 static 제거 권장 |
| DefaultActionOutputBehaviorRegistry, DefaultActionDomainRegistry 구현체 | key→정책 매핑이 ActionType과 연결됨 | 인터페이스는 “key→정책 조회”로 domain이 쓸 수 있으나, **구현체(맵 채우기·메타데이터 소스)**는 인프라. common에 두면 메타데이터 로딩 방식이 common에 스며듦. | 구현체는 infrastructure. common 또는 domain에 Registry 인터페이스만 |
| ActionTypeRegistryHolder | Jackson Deserializer가 resolver 접근하려고 | static 전역 상태이므로 **어디에 두어도 안 좋지만**, common보다는 직렬화 인프라 쪽이 나음. 근본적으로는 Deserializer에 Resolver 주입으로 제거 목표. | infrastructure/serialization. 장기적으로 제거 |

---

## 6.5 반드시 찾아야 할 냄새 — 항목별 점검

| 냄새 | 존재 여부 | 관련 클래스/위치 | 왜 위험한지 |
|------|-----------|------------------|-------------|
| **Hidden source of truth** | 있음 | DefaultCanonicalActionRegistry.toCanonical(), ActionTypeRegistry | canonical 의미가 enum이 아니라 Registry 재조회에만 있음. 미등록 ActionType은 항상 empty. 호출부는 “ActionType → ActionGroup”으로 보지만 실제는 “Registry에 있는 것만 유효”. |
| **Semantic drift risk** | 있음 | Catalog 순서, getAll(), DefaultCategorySemanticProfileRegistry | 순서 변경 시 레거시 해석·추천 목록·검증용 compatible list가 함께 바뀜. 문서화만 있고 코드/테스트로 고정되지 않아 drift 방지 불가. |
| **Temporal contract fragility** | 있음 | CodingActionType 등 key() = "ACTION.XXX." + name() | name() 리네임 시 외부·저장된 key가 깨짐. EtcActionType은 상수 key로 안정. 동일 인터페이스라도 시간에 따른 계약 안정성이 구현체마다 다름. |
| **Bootstrap coupling** | 있음 | ActionTypeRegistryConfig, ActionTypeRegistry 생성자, MetadataLoader static | Catalog → Registry → Holder, MetadataLoader static 호출이 빈 생성 순서에 고정. “정의 검증 단계”와 “인덱스 구축 단계”가 분리되지 않음. |
| **Static/global state contamination** | 있음 | ActionTypeRegistryHolder, EnumCompatParser.KEY_INDEX | Holder는 set-once 전역. EnumCompatParser는 per-enum-class static 맵. 테스트·멀티 컨텍스트·병렬 실행 시 상태 공유·초기화 순서 의존. |
| **Cross-layer leakage** | 있음 | DefaultCategorySemanticProfileRegistry.getAll(), SemanticValidationService, SemanticRecommendationService | Catalog 순서가 registry.getAll() → 프로파일 파생 → 검증/추천 서비스까지 흐름. common의 순서 계약이 domain/application까지 누수. |
| **Common package gravity** | 있음 | resolver, registry, metadata, compatibility, deserializer 전부 common 내부 | “common에 있으니까” 모든 레이어가 같은 Catalog/Resolver/Registry에 끌려 들어감. 정의(enum)와 해석·등록·정책이 한 패키지에 모여 확장·대체가 어려움. |
| **Enum explosion risk** | 있음 | DefaultActionTypeCatalog.ENUM_CLASSES, 각 ActionType enum | 600+ 시 Catalog 한 파일에 600개 클래스 참조·순서 계약. 새 enum 추가 시 Catalog+properties+테스트 수정. 빌드/merge/동기화 비용이 enum 수에 비례해 증가. |
| **Metadata synchronization burden** | 있음 | action-type-output-behavior.properties, action-type-domain.properties, Catalog | Catalog에 없으면 registry에 없고, registry에 없으면 resolver에서 stable key로 못 찾음. properties만 추가하고 Catalog 수정 누락 시 일관성 깨짐. |
| **Legacy compatibility overreach** | 있음 | ActionTypeCompatibilityResolver, EnumResolver first-match, EnumCompatParser | 레거시 이름·EnumName.CONSTANT·stable key가 여러 경로에 흩어져 있고, 순서에 따라 다른 타입이 선택됨. “호환”이 해석 정책 전체를 삼키고 있음. |
| **Runtime behavior hidden in definition structures** | 있음 | ActionTypeInterface(display, key, actionGroup), enum 상수 | enum이 “정의”이면서 display/key/group을 모두 품어 런타임 객체처럼 쓰임. canonical은 실제로 registry 조회로만 결정됨. |
| **Policy encoded as defaults** | 있음 | ActionTypeDeserializer(blank→GENERAL_CONSULTATION), MetadataLoader(리소스 없음→빈 맵) | 기본값·fallback이 직렬화·로딩 계층에 하드코딩. 정책 변경 시 해당 클래스 수정 필요. 진입점별 정책 불일치 가능. |
| **“works but not trustworthy” 구조** | 있음 | 전체 | 순서·fallback·메타데이터 실패가 문서·코드로 강제되지 않아, 동작은 하지만 순서 변경·리소스 누락·다른 진입점에서 “같은 입력”이 다른 결과가 나올 수 있음. |

---

## 7. 과소분석된 클래스 TOP 10

| 순위 | 클래스/구성요소 | 왜 기존 md에서 충분히 안 다뤄졌는지 | 실제로 왜 위험한지 | 다음 분석/수정 우선순위 |
|------|-----------------|-------------------------------------|---------------------|---------------------------|
| 1 | **DefaultCategorySemanticProfileRegistry** | common 패키지 밖(domain)에 있어 common 분석 범위에서 빠짐 | `actionTypeRegistry.getAll()`에 직접 의존. Catalog 순서가 **추천 후보 목록·검증용 compatible actions**에 그대로 반영됨. common의 순서 계약이 domain/application까지 침투한 대표 사례. | 높음. “getAll() 순서에 의존하지 않는” 파생 전략으로 바꾸거나, “ordered source”를 명시적으로 주입받도록 변경 필요. |
| 2 | **ActionTypeRegistryConfig** | Config로만 언급되고, **빈 생성 순서·MetadataLoader static 호출**이 구체적으로 안 다뤄짐 | Catalog→Registry→Holder, Compatibility, Resolver→Holder, OutputBehavior/Domain Registry(MetadataLoader static) 순서가 고정됨. 리소스 없으면 빈 맵이 그대로 빈에 주입되어 silent corruption. | 높음. 메타데이터 “제공자” 빈으로 추상화하고, 실패 시 예외 또는 명시적 기본값 정책으로 변경. |
| 3 | **ActionTypeRegistryHolder** | DIP 위반·테스트 문제만 언급 | static set-once, clearForTest 패턴. **두 번째 Spring 컨텍스트·병렬 테스트**에서 초기화 순서/상태 공유로 불안정. Deserializer가 항상 Holder에 의존해 “역직렬화만 단위 테스트”하기 어려움. | 높음. Deserializer에 Resolver 주입 경로 마련 후 Holder 사용 최소화 또는 제거. |
| 4 | **CanonicalActionRegistry / DefaultCanonicalActionRegistry** | ISP·sameCanonicalCapability 분리만 언급 | **toCanonical이 인자의 getActionGroup()을 쓰지 않고 Registry만 재조회.** 정의(enum)가 아닌 런타임(registry)이 canonical의 유일한 진실 소스. 미등록 ActionType은 항상 empty. | 중상. “정의 기반 canonical” vs “registry 기반 canonical” 계약을 명시하고, 사용처가 어떤 의미를 기대하는지 정리 필요. |
| 5 | **ActionTypeCatalog 인터페이스** | DefaultActionTypeCatalog 구현체만 분석 | 반환 타입이 `List`이고 주석에 “catalog 순서” 명시. **인터페이스 자체가 순서를 계약에 포함**해, “순서 없는 집합”으로 전환하려면 계약 변경이 필요. | 중. “목록만” vs “순서 있는 목록” 인터페이스 분리(예: Set 반환 + 별도 OrderedSource). |
| 6 | **EnumCompatParser** | EnumResolver의 “순서 정책” 부재만 언급 | **KEY_INDEX가 static ConcurrentHashMap.** per-enum-class lazy 구축. 다중 클래스로더·테스트 격리 시 동작 차이 가능. 범용 유틸이 전역 상태를 가지면 모듈화·테스트에 악영향. | 중. static 인덱스를 인스턴스/캐시로 옮기거나, 호출부에서 인덱스 제공하도록 변경 검토. |
| 7 | **ResolutionConfig** | common 분석 범위 밖으로 간주됨 | **EXPLICIT_MAPPINGS가 구체 ActionType enum 상수에 직접 의존.** 새 ActionType 추가 시 Config 수정 가능성. “목표 매핑” 정책이 common enum에 직접 결합됨. | 중. ActionType은 key(또는 식별자)로만 참조하고, enum 상수 직접 의존 제거. |
| 8 | **DeserializerEnumTestUtils** | 테스트 유틸로 구조 분석 대상에서 제외됨 | **DefaultActionTypeCatalog 단일 인스턴스 고정.** “Catalog 순서를 바꾼 구현체”로 테스트하는 경로 없음. 순서 변경 시 실패하는 테스트도 없어, 리팩터링이 순서 민감성을 보호하지 못함. | 중. “순서 계약” 테스트 추가(순서 변경 시 실패), 또는 테스트용 Catalog를 주입 가능하게 해서 순서 의존성 검증. |
| 9 | **RuleContext** | guideline/context로 common 내부로만 간주됨 | **ActionTypeInterface 전체를 보유.** key만 필요한 규칙도 display/actionGroup에 간접 의존 가능. ISP 위반이 domain 쪽 RuleContext까지 퍼져 있음. | 중하. ActionType은 최소 인터페이스(예: key만) 또는 래퍼로 전달하도록 변경 검토. |
| 10 | **ActionTypeOutputBehaviorCoverageTest / properties 의존 테스트** | 테스트가 “구조를 숨긴다”는 시각으로 분석되지 않음 | **ActionTypeMetadataLoader.loadKeyToOutputBehavior()를 테스트에서 직접 호출.** properties가 classpath에 있다고 가정. 리소스 없을 때 “실패해야 한다”는 테스트는 없어, silent 빈 맵이 프로덕션에만 발생할 수 있음. | 중하. “리소스 없음 시 실패 또는 명시적 기본값”을 검증하는 테스트 추가. |

---

## 8. 리팩터링 강도 최종 재판정

**판정: 사실상 semantic core 재설계가 필요하다.**

**이유:**

1. **기존 md의 “핵심 계층 분리 필요”는 맞지만, 그 “계층 분리”만으로는 부족하다.**  
   현재 문제는 “Catalog vs Registry vs Resolver 책임만 나누면 된다”가 아니라, **정의(무엇이 존재하는가)와 해석·등록·정책(어떤 순서·규칙으로 해석·노출·매핑할 것인가)이 하나의 모델에 섞여 있다**는 것이다.  
   따라서 **Definition Model(identity·집합)**과 **Runtime Resolution Model(해석 순서·기본값·메타데이터·canonical 조회)**를 명시적으로 분리하는 설계가 필요하다.

2. **enum을 계속 “중심 모델”로 두는 한, 600+ 규모에서:**  
   - 새 enum 추가 시 Catalog 수정이 필수이고,  
   - key()가 name() 파생인 enum과 상수 key enum이 혼재하며,  
   - metadata·output behavior·task domain이 properties와 enum 양쪽에 흩어져 동기화 부담이 커지고,  
   - 테스트·빌드·merge 비용이 선형 이상으로 늘어난다.  
   **“계층만 나누고 enum+catalog+registry 구조는 유지”**하면, 같은 패턴이 600배로 반복될 뿐이다.

3. **“전면 재설계까지는 아님”이라고 한 기존 md의 판단은 과소평가다.**  
   stable key 인덱스·Resolver 흐름을 “유지 가능”이라고 했지만, **진실 소스가 Registry로 고정된 canonical, getAll() 순서에 의존하는 domain 레이어, 입력 해석의 다중 진입점**을 그대로 두고 “계층만 나누면” 해결되지 않는다.  
   **어떤 것이 “정의”이고 어떤 것이 “런타임 정책”인지 모델 단에서 재정의하지 않으면**, 리팩터는 계속 symptom만 옮기는 것이 된다.

4. **결론:**  
   - **부분 리팩터로는 부족**하다는 기존 판정은 유지한다.  
   - **“핵심 계층 분리”만으로는 부족**하다.  
   - **Definition Model과 Runtime Resolution Model의 분리**, 그리고 **common이 “정의·계약”만 담당하고 resolver/registry/metadata/compatibility/serialization 정책은 common 밖으로 빼는 것**까지 가야 하며, 이는 **semantic core의 재설계**에 해당한다.

---

## 9. 최종 한 문장 판결

**기존 보고서가 짚은 Catalog 순서·직렬화 정책·Registry 검증·first-match·MetadataLoader 문제는 맞지만, 그 원인을 “정의와 해석·등록·실행을 한 모델에 섞어둔 것”으로 단정하지 않았고, 순서 의존이 domain/application까지 파급되는 경로·Canonical의 Registry 의존·common 패키지 경계·enum 중심 구조의 한계를 충분히 밀어붙이지 않았으며, 그 결과 “핵심 계층 분리”가 아니라 정의 모델과 런타임 해석 모델의 분리와 common 밖으로의 인프라 이전을 전제로 한 semantic core 재설계가 필요하다.**

---

*이 문서는 `common-package-structural-analysis.md` 및 해당 md가 다루는 코드 구조에 대한 2차 감사 결과이며, 기존 보고서의 부족함을 공격적으로 검증하는 목적으로 작성되었습니다.*

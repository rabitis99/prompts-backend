# common 패키지 구조 분석 보고서

**대상 경로:** `org.example.sharedprompts.domain.prompt.common`  
**분석 일자:** 2025-03-14  
**목적:** SOLID 위반, 확정성 저해, 책임 분리 실패, 계층 결합, 600+ enum 확장 시 붕괴 가능성에 대한 강한 구조 분석

---

## 1. 전체 평가 요약

- **현재 구조 한 문장 정의**  
  “Catalog에 enum 클래스 목록을 하드코딩하고, 그 순서에 레거시 해석·인덱스·정책이 동시에 의존하는, 정의·등록·해석·정책·직렬화가 여러 계층에 섞인 구조”이다.

- **가장 위험한 문제 3개**  
  1. **Catalog 순서가 해석 의미를 가짐** — `DefaultActionTypeCatalog`의 `ENUM_CLASSES` 순서가 레거시 이름/EnumName.CONSTANT 해석의 “first match wins” 순서이자 `ActionTypeRegistry.allInCatalogOrder`의 순서라, 순서 변경만으로 역직렬화·호환 결과가 바뀐다.  
  2. **직렬화 계층에 도메인 정책 침범** — `ActionTypeDeserializer`가 blank 입력 시 `EtcActionType.GENERAL_CONSULTATION`을 반환하는 “기본값 정책”을 직접 소유하여, 계약(입출력)과 정책(기본 액션)이 한 클래스에 결합된다.  
  3. **enum 추가 시 수정 지점 집중** — 새 ActionType enum 1개 추가 시 `DefaultActionTypeCatalog` 수정이 필수이고, properties·metadata·테스트까지 손대야 하며, Catalog가 “목록 제공”과 “순서에 따른 해석 계약”을 동시에 책임져 OCP가 깨진다.

- **가장 먼저 분리해야 할 레이어 3개**  
  1. **Definition vs Catalog** — “어떤 enum들이 존재하는가”와 “해석/노출 시 사용할 순서·목록”을 분리.  
  2. **Serialization vs Policy** — 역직렬화(계약)와 blank/unknown 기본값 정책을 완전 분리.  
  3. **Index vs Compatibility** — stable key → ActionType 결정론적 인덱스와, 레거시 이름/EnumName.CONSTANT 해석(순서 의존)을 명시적으로 분리하고 순서 계약을 문서/코드로 고정하거나 제거 가능하게.

---

## 2. SOLID 위반 분석

### 2.1 SRP (단일 책임 원칙)

| 위반 파일/클래스 | 섞인 책임 | 왜 위반인지 | 실제 위험 | 추천 분리 방향 |
|-----------------|-----------|-------------|-----------|----------------|
| **DefaultActionTypeCatalog** | (1) 등록 대상 목록 제공 (2) 등록·해석 **순서** 계약 담당 | Catalog 인터페이스는 “목록”만 정의하는데, 구현체가 순서를 문서·코드에서 “registry/레거시 해석 순서”로 묶어서, “무엇을 등록할지”와 “어떤 순서로 해석할지”를 한 몸에 가짐 | 순서 변경 시 레거시 파싱·UI 순서가 함께 바뀌어 부수 효과 예측 불가 | Catalog는 “순서 없는 집합”만 반환하고, “해석 순서”는 별도 OrderedCatalog 또는 Resolver 전용 설정으로 분리 |
| **ActionTypeCompatibilityResolver** | (1) EnumName.CONSTANT 파싱 (2) enum 이름만으로 lookup (3) **enum 목록 순서에 따른 first-match 정책** (4) 입력 검증(null/blank 예외) | 레거시 해석 + 순서 정책 + 검증이 한 클래스에 있음 | 순서 의존 해석이 “호환 레이어”에 묻혀 있어, 순서 변경 시 호환성 깨짐을 호출부가 인지하기 어려움 | Compatibility: 형식 파싱만. “어떤 enum 목록을 어떤 순서로 시도할지”는 Index/Resolver 정책으로 분리 |
| **ActionTypeDeserializer** | (1) JSON → 문자열 계약 해석 (2) **blank 시 기본값 정책**(GENERAL_CONSULTATION) | 역직렬화(계약)와 “blank일 때 쓸 기본 ActionType” 정책이 동일 클래스에 있음 | 기본값 변경 시 직렬화 계층 수정 필요; 정책이 JSON 계약과 결합되어 테스트·교체 어려움 | Deserializer는 “값 없음 → null/빈 Optional”만 처리하고, “기본값 채우기”는 상위 애플리케이션/정책 계층에서 수행 |
| **ActionTypeRegistry** | (1) stable key 인덱스 구축 (2) catalog 순서 유지 리스트 (3) **등록 시 검증**(key null/blank, ActionGroup null, 중복 key) | 저장소 + 순서 유지 + 검증이 한 클래스에 있음 | 600+ 상수 시 검증 실패 시점·메시지가 “레지스트리 구축”에 묶여 디버깅·단계적 검증 분리 어려움 | Index 구축만 담당하고, 검증(Validation layer)은 별도 단계/컴포넌트로 분리 |
| **EnumResolver** | (1) 단일 enum 클래스 기준 파싱 (2) **여러 enum 클래스 순차 시도**(first match) (3) 검증 (4) 에러 메시지 생성 | 범용 enum 해석 유틸이 “목록 순서 = 해석 우선순위” 정책까지 포함 | ActionType 외 다른 enum에서도 사용 시, “순서가 의미 있음”이 암묵적이라 misuse 시 결정론 깨짐 | “단일 enum 파싱”과 “다중 enum 후보 중 하나 선택” 정책을 분리; 후자는 호출부에서 순서를 명시적으로 넘기도록 |
| **DefaultCanonicalActionRegistry** | (1) ActionType → ActionGroup 조회 (2) stable key → ActionGroup 조회 | 사실상 `actionType.getActionGroup()` 위임인데 “Registry”로 네이밍되어 “인덱스”처럼 보임 | 책임은 단일하나, **CanonicalActionRegistry** 인터페이스가 “toCanonical + findByKey + sameCanonicalCapability”로 조회·비교 정책이 섞여 있음 | toCanonical/findByKey만 두고, sameCanonicalCapability는 별도 Policy/Comparator로 분리 가능 |
| **ActionTypeMetadataLoader** | (1) classpath properties 로드 (2) key→OutputBehaviorType/key→TaskDomain **매핑 구축** (3) **실패 시 빈 맵 반환**(리소스 없음/예외 시) (4) **잘못된 enum 이름 스킵** | 로딩 + 매핑 생성 + 실패 정책(조용히 빈 맵)이 한곳에 있음 | 리소스 누락·오타 시 아무 로그 없이 빈 맵 → 모든 getOutputBehavior/getTaskDomain이 empty; 600+ 규모에서 조용한 붕괴 | 로드는 로드만, “리소스 없음/오타”는 실패 또는 기본값 정책을 상위에서 명시적으로 처리 |

**요약:**  
- **정의 + 해석 + 등록 + 검증 + fallback + 정책**이 Catalog, CompatibilityResolver, Deserializer, Registry, EnumResolver, MetadataLoader에 2~4개씩 섞여 있음.  
- enum 자체는 비교적 단일 책임(정의·표시명·ActionGroup)에 가깝지만, **ActionTypeInterface**가 stable key, i18n 3개, ActionGroup을 한 인터페이스에 묶어 소비자별 ISP 문제를 만듦.

---

### 2.2 OCP (개방-폐쇄 원칙)

| 위반 지점 | 새 enum 추가 시 수정 필요 위치 | 왜 폐쇄 위반인지 | 구체적 위험 |
|----------|-------------------------------|------------------|-------------|
| **DefaultActionTypeCatalog** | `ENUM_CLASSES`에 새 `Class<? extends Enum<?>>` 1개 추가 + **삽입 위치 결정** | “기존 코드 수정” 없이 확장 불가; 순서가 의미 있으므로 “맨 뒤에 추가”만 해도 기존 “순서 계약”에 영향 가능 | enum 600개 시 한 파일에 600개 클래스 참조; 순서 실수로 레거시 해석 변경 |
| **ActionTypeMetadataLoader** 사용처 | `action-type-output-behavior.properties`, `action-type-domain.properties`에 새 stable key 추가 (파일 존재 시) | 메타데이터는 properties로 열려 있으나, **Catalog와 독립적이지 않음** — Catalog에 없으면 registry에 없고, registry에 없으면 resolver에서 stable key로 못 찾음. 즉 “추가”가 Catalog 수정을 전제로 함 | properties만 추가하고 Catalog 수정 누락 시, stable key는 동작하지만 레거시 이름 해석 순서/목록에는 미포함 → 일관성 깨짐 |
| **ActionTypeRegistry** | Catalog 수정에 의존(위와 동일) | Registry는 Catalog에 닫혀 있음. “새 소스에서 enum 목록 가져오기” 같은 확장이 없음 | 스캔/플러그인 방식으로 enum 추가하려면 Catalog·Config 전부 개방 필요 |
| **ActionTypeCompatibilityResolver** | Catalog에서 넘기는 목록에 새 enum 클래스가 포함되도록 해야 함 → 결국 Catalog 수정 | 호환 레이어 자체는 “목록”에 열려 있지만, 그 목록이 **항상 Catalog와 동일한 단일 출처**라 Catalog 수정이 강제됨 | 레거시 전용 enum을 별도 목록으로 두고 싶어도 현재 구조에선 Catalog 한 군데만 수정 가능 |

**수정 지점 수 요약:**  
- **ActionType enum 1개 추가:**  
  - **필수:** `DefaultActionTypeCatalog` 1곳 (그리고 **순서 결정**).  
  - **선택(정책 유지하려면):** `action-type-output-behavior.properties`, `action-type-domain.properties` 2곳.  
- **Category/Intent/ActionGroup 같은 상위 taxonomy 1개 추가:**  
  - 해당 enum/상수 정의 + 그걸 참조하는 **모든 ActionType enum 상수**에서 `getActionGroup()` 등 반환값 수정 가능성.  
- **결론:** “기존 switch/하드코딩 목록 수정”이 필수이므로 OCP 위반. “새 enum 추가 = Catalog 수정”이 구조적으로 고정됨.

---

### 2.3 LSP (리스코프 치환 원칙)

| 문제 지점 | 인터페이스상 동일·실제 치환 불가 이유 | 실제 위험 |
|----------|--------------------------------------|-----------|
| **key() 의미 불일치** | 일부 enum은 `"ACTION.XXX." + name()`(name에 의존), 일부는 생성자에서 받은 **고정 stableKey** 반환. 동일 `ActionTypeInterface`이지만 “key가 name에서 파생되는가”가 구현체마다 다름 | name() 리네임 시 CodingActionType 등은 key가 바뀌어 **외부 계약 붕괴**; EtcActionType 등은 key가 상수라 안전. 동일 인터페이스로 치환하면 “key 안정성” 보장이 구현체에 따라 다름 |
| **레거시 해석 결과** | “RECOMMENDATION” 같은 문자열은 **catalog에서 먼저 나오는 enum**의 RECOMMENDATION으로 해석됨. EtcActionType.RECOMMENDATION vs RecommendationActionType의 다른 상수 등, “같은 이름”이 다른 enum에 있으면 **순서에 따라 다른 타입**으로 치환됨 | 호출부가 “ActionTypeInterface만 보면 동일”이라 생각하지만, **어떤 enum에서 왔는지**에 따라 legacy 입력 해석이 달라져, 치환 시 동작이 바뀜 |
| **getActionGroup() null** | 인터페이스에는 null 허용이 문서화되어 있지 않고, **ActionTypeRegistry**가 등록 시 null이면 예외를 던짐. 즉 “모든 등록된 구현체는 non-null ActionGroup”이 암묵 계약 | 인터페이스만 보면 “null 가능”으로 오해할 수 있으나, 실제로 등록된 구현체는 null을 반환하면 안 되므로, “미등록” ActionType 구현체는 LSP적으로 치환 불가 |
| **DefaultCanonicalActionRegistry** | `toCanonical(actionType)`는 단순히 `actionType.getActionGroup()` 위임. 즉 “Canonical” 변환이 **구현체의 getActionGroup() 신뢰**에 의존 | 일부 구현체만 특수 규칙(예: EXTRACTION → 별도 그룹)을 넣고 싶어도 현재는 모든 구현체가 동일 규칙으로 취급됨. “동일 계약”이지만 “특수 케이스” 확장 시 하위 타입 치환이 깨질 수 있음 |

**요약:**  
- “인터페이스상으로는 동일하지만 실제로는 치환 불가능한 지점”:  
  - **key()** — name 파생 vs 상수 키;  
  - **레거시 문자열 해석** — catalog 순서에 따라 다른 구현체 반환;  
  - **등록된 구현체만** non-null getActionGroup() 등 암묵 계약을 만족.

---

### 2.4 ISP (인터페이스 분리 원칙)

| 문제 | 어떤 소비자가 어떤 메서드 때문에 불필요하게 결합되는지 | 분해안 |
|------|------------------------------------------------------|--------|
| **ActionTypeInterface**에 key + getDisplayNameKo/En/Ja + getActionGroup() 모두 필수 | (1) Resolver/Registry/Deserializer는 **key()**만 필요. (2) Canonical/비교 로직은 **key() + getActionGroup()** 필요. (3) UI/추천은 **표시명(i18n)** 필요. key만 쓰는 소비자도 3개 언어 + ActionGroup에 의존함 | **StableKeyedActionType**: key(). **ActionTypeDisplay**: getDisplayNameKo/En/Ja. **ActionTypeCapability**: getActionGroup(). 필요에 따라 이들을 조합한 **ActionTypeInterface**를 “풀 계약”으로 두거나, 소비자는 최소 인터페이스만 의존하도록 |
| **CanonicalActionRegistry**에 toCanonical + findByKey + sameCanonicalCapability | “동일 capability 비교”만 쓰는 소비자는 toCanonical 두 번 호출로 구현 가능하지만, **findByKey**는 key→Group만 필요할 때만 쓰임. sameCanonicalCapability는 “비교 정책”이라 별도 인터페이스로 두는 편이 ISP에 맞음 | **CanonicalLookup**: toCanonical, findByKey. **CapabilityEquivalence**: sameCanonicalCapability(또는 별도 Policy 클래스) |

**요약:**  
- ActionTypeInterface가 “식별 + i18n + capability”를 한 덩어리로 강제해, key만 필요한 계층이 display/group까지 알게 됨.  
- CanonicalActionRegistry는 “조회”와 “비교 정책”이 한 인터페이스에 있어, 조회만 쓰는 클라이언트가 비교 메서드에도 결합됨.

---

### 2.5 DIP (의존성 역전 원칙)

| 위반 지점 | 정책 ↔ 구현 의존 방향 문제 | 실제 위험 |
|----------|----------------------------|-----------|
| **ActionTypeRegistryConfig** | 상위 설정이 **DefaultActionTypeCatalog**, **ActionTypeRegistry**(concrete), **DefaultActionTypeResolver**, **ActionTypeCompatibilityResolver** 등 **구체 클래스**에 직접 의존. Catalog “목록”을 제공하는 추상에만 의존하지 않음 | 테스트/대체 구현(예: 다른 순서, 다른 소스) 삽입 시 Config 자체 수정 필요 |
| **ActionTypeDeserializer** | **ActionTypeRegistryHolder**(static)에 직접 의존 → resolver를 static으로 가져옴. “역직렬화에 쓸 Resolver 주입” 추상에 의존하지 않음 | 단위 테스트에서 Deserializer만 검증하려면 Holder 초기화 강제; 병렬 테스트·다중 컨텍스트에서 충돌 가능 |
| **ActionTypeCompatibilityResolver** | **Catalog.getActionTypeEnumClasses()** 반환 목록에 직접 의존. 그 목록이 **구체 enum 클래스들의 순서된 리스트**이므로, “호환 해석에 쓸 후보 시퀀스” 추상이 아니라 “Catalog 구현체가 정한 구체 리스트”에 묶임 | Catalog 구현체를 바꾸면 Compatibility 해석 순서가 바뀌고, 이게 “호환 레이어”인지 “카탈로그 정책”인지 구분 불가 |
| **ActionTypeRegistry** | 생성자에서 **Iterable<Class<? extends Enum<?>>>**를 받지만, 실제로는 **DefaultActionTypeCatalog**에서 오는 리스트만 사용. “등록 소스” 추상이 없고, Catalog가 그 역할을 단일 구현체로 고정함 | 다른 소스(DB, 설정, 모듈)에서 enum 목록을 가져오려면 Catalog 대체가 아닌 “Catalog 구현체 추가”가 필요해, 개방-폐쇄 위반과 겹침 |
| **DefaultActionOutputBehaviorRegistry / DefaultActionDomainRegistry** | **ActionTypeMetadataLoader.loadKeyToOutputBehavior()** 등 **static** 호출로 Map을 채움. “key→정책 매핑 제공자” 추상에 의존하지 않음 | 테스트 시 properties 파일·classpath에 의존; 600+ 키 관리 시 소스가 properties 한 파일에만 묶임 |

**요약:**  
- **정책/설정 계층**이 **구체 Catalog, 구체 Registry, static Holder, static MetadataLoader**에 직접 의존해, “추상에 의존하고 구현은 주입”이 깨져 있음.  
- Resolver는 Registry·Compatibility에 의존하므로 상대적으로 나으나, 그 둘이 다시 Catalog/구체 목록에 묶여 있어 전체적으로 DIP 위반.

---

## 3. 확정성/결정론성 분석

| 항목 | 위험도 | 설명 |
|------|--------|------|
| **순서 의존** | **상** | (1) **DefaultActionTypeCatalog.ENUM_CLASSES** 순서 = **ActionTypeCompatibilityResolver**에 넘기는 enum 순서 = 레거시 이름/EnumName.CONSTANT 해석 시 **first match wins** 순서. (2) **EnumResolver.resolve(value, categoryEnums)**가 `for (Class<? extends Enum<?>> enumClass : categoryEnums)` 순서대로 시도. (3) **ActionTypeRegistry.allInCatalogOrder**도 동일 순서. 따라서 **Catalog 순서 변경 = 레거시 해석 결과 변경 + getAll() 순서 변경**. 코드 주석에 “순서 변경 시 영향”이라 적혀 있으나, 한 번만 바꿔도 기존 저장된 legacy 값 해석이 달라질 수 있음. |
| **중복 키/충돌 위험** | **중** | (1) **stable key**: ActionTypeRegistry 구축 시 `keyMap.put(key, action)`에서 중복이면 즉시 IllegalStateException. 결정론적. (2) **레거시 이름**: 서로 다른 enum에 같은 **Enum.name()** 상수가 있으면 catalog 순서에 따라 한 쪽만 선택됨. 현재는 prefix(EnumName.)로 구분되나, “상수 이름만”으로 찾는 EnumResolver 경로에서는 **이름 충돌 시 순서에 의존**. (3) **EnumCompatParser**는 enum 클래스별로 stable key 인덱스를 따로 두므로, **같은 stable key가 서로 다른 enum에 있으면** (현재 Registry가 막아서 불가능하지만) 전역적으로는 “어느 enum에서 먼저 매칭되느냐” 문제 이론상 존재. |
| **fallback 위험** | **상** | (1) **ActionTypeDeserializer**: value가 null/blank면 **EtcActionType.GENERAL_CONSULTATION** 반환. “blank = 기본 상담”이 **직렬화 계층에 하드코딩**되어 있어, 다른 진입점(API, 서비스 레이어)에서 blank 처리 정책이 다르면 **진입점별로 결과 불일치**. (2) **ActionTypeMetadataLoader**: 리소스 없음/예외 시 **빈 맵** 반환 → getOutputBehavior/getTaskDomain이 전부 Optional.empty(). “fallback이 없음”이 아니라 “정책이 조용히 없음”이 됨. (3) **EnumCompatParser**: LENIENT 시 unknown → null. 호출 경로에 따라 null이 그대로 전파되거나, 상위에서 다시 기본값으로 채워질 수 있어 **일관된 fallback 정책이 없음**. |
| **registry/catalog 초기화 위험** | **중** | (1) **ActionTypeRegistryHolder**: setRegistry/setResolver가 **한 번만** 허용되고, 초기화 전 get 시 IllegalStateException. static 초기화 순서가 Config 빈 생성 순서에 묶임. (2) **ActionTypeRegistry** 생성 시 catalog 순서대로 순회하며 key 중복·null 검증. **초기화 실패 시** 전체 앱 기동 실패. (3) **EnumCompatParser.KEY_INDEX**: per-enum-class stable key 인덱스가 **lazy static**으로 구축됨. 클래스 로드 순서에 따라 첫 사용 시점이 달라질 수 있으나, 인덱스 자체는 불변이므로 결과는 동일. (4) **ActionTypeMetadataLoader**는 static 메서드로, 리소스는 **앱 라이프사이클과 무관**하게 한 번 읽힘. 리소스 갱신 불가. |
| **런타임 해석 일관성** | **중** | (1) **동일 입력**에 대해 stable key 경로는 **항상 동일** (Registry는 불변 맵). (2) **레거시 입력**은 **catalog 순서 고정**이면 동일. 하지만 **classpath에 새 enum 클래스가 추가되거나**, Catalog 구현체가 동적 목록을 반환하도록 바뀌면 **같은 문자열이라도 다른 실행에서 다른 타입**이 나올 수 있음. (3) **Optional 처리**: getOutputBehavior/getTaskDomain이 empty일 때 상위에서 어떻게 처리하는지에 따라 “동일 actionType”이라도 결과가 달라질 수 있음. |

**종합:**  
- **확정성을 깨는 가장 큰 요인:** Catalog **순서**가 해석 의미를 가지는 것, Deserializer의 **고정 기본값 정책**, MetadataLoader의 **조용한 실패(빈 맵)**.  
- “first match wins”가 문서화만 되어 있고, “순서를 바꾸지 말라”는 계약이 코드로 강제되지 않아, 리팩터링 시 결정론이 깨지기 쉬움.

---

## 4. 구조 분리안

**판정: 부분 리팩터로는 부족하고, 핵심 계층 분리가 필요하다.**

### [8] 확장 규모(600+ enum 상수 이상) 내구성 평가

- **가정:** ActionType 600개 이상, Category/Intent/Action/Role 확장, stable key 외부 계약 유지, legacy 단계적 축소, 프론트 선택/추천/검증 지원.
- **판정:** **부분 수정으로는 버틸 수 없고, 전면 재분리는 아니지만 핵심 계층 재분리가 필요하다.**
  - **유지 가능하다고 보기 어려움:** Catalog 한 파일에 600개 클래스 참조·순서 계약이 묶이면 리팩터링·배포 시 실수로 순서 변경 시 레거시 해석이 깨짐. 메타데이터 빈 맵 조용한 실패는 600+ 키에서 정책 누락 감지 불가.
  - **부분 수정만으로 버티기 부족:** 결정론(순서/fallback/메타데이터 실패)만 고쳐도 “새 enum 추가 = Catalog 수정” 구조는 그대로라, 확장 시 수정 지점이 계속 한곳에 몰림.
  - **전면 재설계까지는 아님:** stable key 인덱스·Resolver 흐름은 유지 가능. Definition/Catalog(순서)/Index/Compatibility/Resolver/Validation/Policy/Serialization 계층만 명시적으로 나누면 600+ 규모에서도 유지 가능.

- **이유:**  
  - 결정론 문제(순서 의존, fallback 하드코딩, 메타데이터 조용한 실패)는 **지점별 수정**으로 완화 가능하나, **Catalog가 “목록 + 순서 계약”을 동시에 갖는 구조**와 **직렬화-정책 혼합**은 그대로 두면 enum 600+ 확장 시 같은 패턴이 600배로 증폭됨.  
  - “유지”만으로는 새 enum 추가 시 수정 지점이 계속 한 곳(Catalog)에 몰리고, “전면 재설계”까지는 아니더라도 **Definition / Catalog(순서) / Index / Compatibility / Resolver / Validation / Policy / Serialization**을 명시적으로 나누고, **어떤 계층이 어떤 계약에만 의존하는지**를 정리해야 함.

| 현재 파일/구성요소 | 현재 책임 | 문제점 | 이동/분리 대상 레이어 | 권장 조치 |
|-------------------|----------|--------|------------------------|----------|
| **DefaultActionTypeCatalog** | 등록 대상 enum 클래스 목록 + **순서** | 순서가 해석·노출에 영향하는데 “목록 제공”과 동일 클래스에 있음 | Catalog layer(목록만), **순서**는 Resolver/Compatibility 전용 설정으로 | Catalog는 Set 또는 순서 없는 List만 반환. “해석 순서”는 OrderedActionTypeSource 등 별도 타입으로 분리하고, Config에서만 순서 결합 |
| **ActionTypeRegistry** | stable key 인덱스 + catalog 순서 리스트 + 등록 검증 | 인덱스 + 순서 유지 + 검증 혼합 | Index layer( key→ActionType ), **검증**은 Validation layer | Registry는 “구축 시 검증” 대신 “검증된 목록을 받아서 인덱스만 구축”. 검증은 별도 ActionTypeDefinitionValidator 등 |
| **ActionTypeCompatibilityResolver** | 레거시 형식 파싱 + **enum 목록 순서에 의한 first match** | 순서 정책이 “호환” 클래스 안에 숨음 | Compatibility layer(형식 해석만), **시도 순서**는 Index/Resolver 정책 | “후보 enum 시퀀스”를 생성자로 받고, 그 순서를 문서화. 순서는 Catalog가 아닌 “호환용 순서 전략”에서 옴 |
| **DefaultActionTypeResolver** | stable key 우선 → 실패 시 compatibility 위임 | 상대적으로 단일 책임에 가깝음 | Resolver layer | 유지. 단, compatibility에 넘기는 “목록/순서”가 어디서 오는지 명시적으로 두고 DIP 만족하도록 |
| **ActionTypeDeserializer** | JSON → 문자열 + **blank → GENERAL_CONSULTATION** | 계약 + 기본값 정책 혼합 | Serialization layer(문자열→값만), **기본값**은 Policy/Application | blank 시 null 또는 Optional.empty() 반환. 기본값 채우기는 호출부(서비스/파사드)에서 수행 |
| **ActionTypeRegistryHolder** | static registry/resolver 보관 | Deserializer가 구체 static에 의존 (DIP 위반) | 인프라만 유지하되, Deserializer는 “Resolver 제공자” 인터페이스에 의존하도록 변경(Jackson Context 주입 등) | 가능하면 Jackson Deserializer에 Resolver를 주입하고, Holder는 레거시/특수 경로만 사용 |
| **ActionTypeMetadataLoader** | properties 로드 + key→enum 매핑 + **실패 시 빈 맵** | 조용한 실패로 600+ 키에서 정책 누락 감지 불가 | Policy/Metadata layer. **실패 정책**은 상위에서 | 리소스 없음/파싱 실패 시 예외 또는 명시적 “기본 매핑” 반환. 빈 맵 반환 제거 또는 최소 로깅 |
| **EnumResolver** | 다중 enum 순차 시도 + 단일 enum 파싱 | “순서 = 우선순위”가 범용 유틸에 묻혀 있음 | Serialization/Compatibility. **순서**는 호출부 책임으로 | resolve(value, categoryEnums) 사용처에서 “순서를 넘기는 쪽”이 순서 계약을 소유하도록 문서화. 또는 “OrderedEnumResolver”와 “SingleEnumParser” 분리 |
| **DefaultCanonicalActionRegistry** | ActionType→ActionGroup, key→ActionGroup | sameCanonicalCapability가 “비교 정책”을 포함 | Canonical = Lookup layer. 비교 = Policy | toCanonical/findByKey 유지. sameCanonicalCapability는 별도 Policy/Comparator로 분리 검토 |
| **ActionTypeInterface** | key + 3개 표시명 + getActionGroup | key만 필요한 소비자도 i18n·group에 의존 (ISP) | Definition layer. **인터페이스 분리** | StableKeyed + Display + Capability 등으로 역할 나누고, 풀 계약은 조합 인터페이스로 |
| **각 ActionType enum** | 정의 + 표시명 + ActionGroup + key() 구현 방식 불일치 | key()가 name() 파생 vs 상수 혼재 (LSP) | Definition layer | key()는 “항상 안정 식별자”로 통일. name() 파생은 deprecated하고, 신규는 모두 명시적 stable key 권장 |

---

## 5. 우선순위 리팩터링 계획

### 1단계: 당장 터질 수 있는 결정론 문제 제거

| 수정 대상 | 목표 | 기대 효과 | 사이드 이펙트 |
|-----------|------|-----------|----------------|
| **ActionTypeDeserializer** | blank/null 입력 시 **기본값 반환 제거**. null 또는 Optional.empty() 또는 전용 “Unknown/Blank” 타입 반환으로 변경 | 직렬화 계층이 “기본값 정책”을 갖지 않아, 진입점별 정책 일관성 확보 | 호출부(API/서비스)에서 blank 처리 필수. 기존에 GENERAL_CONSULTATION에 의존하던 코드는 명시적 fallback 추가 필요 |
| **ActionTypeMetadataLoader** | 리소스 없음 또는 파싱 예외 시 **빈 맵 대신 예외** 또는 **로깅 + 명시적 기본값** | 600+ 키에서 properties 누락/오타 시 조용한 붕괴 방지 | 기동 시 리소스 필수 또는 기본 매핑 파일 배포 필수 |
| **DefaultActionTypeCatalog** / **ActionTypeCompatibilityResolver** | **순서 계약 문서화 + 테스트**로 “이 순서가 레거시 해석 계약이다”를 고정. 순서 변경 시 실패하는 테스트 추가 | 순서를 실수로 바꿀 때 즉시 감지 | 순서를 바꾸려면 테스트(계약)도 함께 수정해야 함 |

### 2단계: 책임 분리

| 수정 대상 | 목표 | 기대 효과 | 사이드 이펙트 |
|-----------|------|-----------|----------------|
| **Catalog vs 순서** | Catalog는 “enum 클래스 집합”만 반환. “해석/노출 순서”는 별도 타입(OrderedActionTypeSource 등) 또는 Config에서만 조합 | Catalog 수정 시 순서를 건드리지 않아도 됨. 순서 변경은 “해석 정책” 변경으로만 발생 | Config/빈 설정에서 “목록 + 순서” 조합 로직 필요 |
| **ActionTypeRegistry** | 구축 시 검증(key/null/중복)을 **별도 Validator**로 분리. Registry는 “검증 통과한 목록”만 받아 인덱스 구축 | Registry가 “저장소” 역할만 하고, 검증 실패 메시지/레벨을 Validator에서 일원화 | 부트스트랩 시 Validator 실행 후 Registry 구축 순서 명확화 |
| **ActionTypeDeserializer** | 기본값 제거 후, **Resolver 주입** 경로 마련(Jackson Context 또는 생성자). Holder 의존 최소화 | 테스트·다중 컨텍스트에서 Resolver 교체 가능 | Jackson 설정에서 Deserializer에 Resolver 넘기는 방식 도입 필요 |

### 3단계: 확장 가능한 구조로 재조립

| 수정 대상 | 목표 | 기대 효과 | 사이드 이펙트 |
|-----------|------|-----------|----------------|
| **Catalog 확장** | “단일 List 반환” 대신 **등록 소스 추상화**(예: ActionTypeDefinitionSource). 기본 구현은 기존 DefaultActionTypeCatalog와 동일하되, 순서는 “OrderedSource”에서만 제공 | 새 enum 소스(모듈/플러그인/설정) 추가 시 기존 Catalog 수정 없이 소스만 추가 가능 | 인터페이스·Config 구조 변경 |
| **ActionTypeInterface 분리** | key / Display / Capability 인터페이스 분리. 소비자는 필요한 것만 의존 | Resolver·Registry는 key만, UI는 Display만 의존해 ISP 만족 | 기존 ActionTypeInterface 사용처가 “풀 계약” 또는 조합 타입으로 마이그레이션 필요 |
| **메타데이터(OutputBehavior/TaskDomain)** | “key→정책” 제공자를 인터페이스로 두고, properties 로더는 그 구현체 중 하나. 실패 시 예외 또는 기본 매핑 | 테스트·대체 매핑(DB/원격) 삽입 가능. DIP 만족 | Config에서 메타데이터 “제공자” 빈 구성 필요 |

---

## 6. 최종 판정

**“현 구조는 부분 리팩터로는 부족하고, 핵심 계층 분리가 필요하다.”**

- **유지만으로 부족한 이유:**  
  - 결정론 이슈(순서 의존, fallback 하드코딩, 메타데이터 조용한 실패)는 수정 가능하지만, **Catalog가 목록+순서를 동시에 책임지는 구조**와 **직렬화-정책 혼합**은 enum 수가 크게 늘어나면 동일 패턴이 반복되어 유지보수 비용이 급증함.  
- **전면 재설계까지는 아닌 이유:**  
  - stable key 기반 인덱스, Registry, Resolver의 “stable key 우선 → compatibility” 흐름은 명확함. **계층만 명시적으로 나누고, 순서·정책·기본값을 올바른 레이어로 옮기면** semantic core로 계속 사용 가능.

따라서 **1단계로 결정론 위험 제거**, **2단계로 Catalog/Registry/Deserializer 책임 분리**, **3단계로 확장 포인트(소스 추상화, 인터페이스 분리)**를 진행하는 것이 적절함.

---

## 7. 가장 위험한 클래스 TOP 5

### 1. DefaultActionTypeCatalog

- **왜 위험한지:**  
  - enum 목록과 **해석·노출 순서**를 한 몸에 가짐. 순서 변경 시 레거시 파싱·getAll() 순서가 동시에 바뀌어, “카탈로그만 고쳤다”는 수정이 해석 계약을 깨뜨릴 수 있음.  
  - enum 600개 시 이 파일에 600개 클래스 참조가 들어가며, OCP·가독성 모두 악화.
- **어떤 책임이 섞였는지:**  
  - (1) 등록 대상 정의 (2) 해석 우선순위(순서) (3) 노출 순서.
- **가장 먼저 쪼개야 할 방향:**  
  - “목록(집합)” 제공과 “순서가 있는 목록” 제공을 분리. 순서는 Resolver/Compatibility 전용 설정으로 옮기고, Catalog는 순서 없는 Set 또는 단일 목록만 반환하도록.

### 2. ActionTypeDeserializer

- **왜 위험한지:**  
  - JSON 계약(문자열→ActionType)과 **blank 시 기본값 정책**(GENERAL_CONSULTATION)이 결합되어, “역직렬화”만 바라보는 호출자가 사실상 도메인 정책까지 의존하게 됨.  
  - ActionTypeRegistryHolder static에 직접 의존해 테스트·교체가 어렵고 DIP 위반.
- **어떤 책임이 섞였는지:**  
  - (1) 입출력 계약(JSON→값) (2) 기본값 정책 (3) Resolver 획득(static Holder).
- **가장 먼저 쪼개야 할 방향:**  
  - blank → null/empty 반환으로 바꾸고, 기본값 채우기는 상위 계층으로. Resolver는 주입 가능하게(Jackson Context 등) 바꿔 DIP 만족.

### 3. ActionTypeCompatibilityResolver

- **왜 위험한지:**  
  - 레거시 해석의 **first match wins** 순서가 이 클래스가 받는 `enumClasses` 순서에 묶여 있는데, 그 순서가 Catalog에서만 오므로 “호환 레이어”가 Catalog 순서 계약에 암묵적으로 종속됨.  
  - 순서가 바뀌면 기존 저장된 legacy 값 해석이 달라질 수 있음.
- **어떤 책임이 섞였는지:**  
  - (1) EnumName.CONSTANT 파싱 (2) enum name만으로 lookup (3) **목록 순서에 따른 first-match 정책** (4) null/blank 검증.
- **가장 먼저 쪼개야 할 방향:**  
  - “형식 파싱”과 “시도할 enum 시퀀스 및 그 순서” 분리. 순서는 이 클래스가 아닌 “호환용 순서 전략”에서 명시적으로 받고, 순서 계약을 테스트로 고정.

### 4. ActionTypeRegistry

- **왜 위험한지:**  
  - stable key 인덱스 + catalog 순서 유지 + **등록 시 검증**(key null/blank, ActionGroup null, 중복 key)이 한 클래스에 있어, 600+ 상수에서 검증 실패 시 “레지스트리 구축 실패”로만 보임.  
  - 검증 규칙을 바꾸거나 단계별 검증을 하려면 Registry 자체를 수정해야 함.
- **어떤 책임이 섞였는지:**  
  - (1) key→ActionType 인덱스 (2) catalog 순서 리스트 (3) 등록 시 검증.
- **가장 먼저 쪼개야 할 방향:**  
  - “검증된 목록을 받아서 인덱스만 구축”하는 순수 Index 역할로 줄이고, 검증(키 형식, null, 중복)은 별도 Validator에서 수행한 뒤 Registry는 그 결과만 받도록.

### 5. ActionTypeMetadataLoader

- **왜 위험한지:**  
  - 리소스 없음/예외 시 **빈 맵**을 반환하고, 잘못된 enum 이름은 **무시**하므로, properties 오타·누락이 있으면 getOutputBehavior/getTaskDomain이 전부 empty가 되어 **조용히 정책이 비활성화**됨.  
  - 600+ 키 규모에서 “어디가 잘못됐는지” 찾기 어렵고, 테스트도 classpath 리소스에 의존함.
- **어떤 책임이 섞였는지:**  
  - (1) classpath 리소스 로드 (2) key→enum 매핑 구축 (3) **실패 정책**(빈 맵, 스킵).
- **가장 먼저 쪼개야 할 방향:**  
  - 로드는 “실패 시 예외” 또는 “기본 매핑 반환”으로 명시. 잘못된 enum 이름은 스킵하지 말고 로깅 또는 예외. “메타데이터 제공자” 인터페이스를 두고 이 로더는 그 구현체 중 하나로만 쓰이게 해 DIP 만족.

---

*이 문서는 `org.example.sharedprompts.domain.prompt.common` 패키지 및 직접 연관된 resolver/registry/catalog/serializer 인프라를 대상으로 한 구조 분석 결과입니다.*

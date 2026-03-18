# Prompt Engine 구조 리팩터링 1차~6차 감사 보고서 (통합)

**감사 목적:** 1차~6차 구조 변경 의도가 실제 코드에 반영되었는지 검증. 숨은 결합·설계 위반·가짜 분리·미완성 리팩터 적발.  
**범위:** 추가 설계 제안 없음. 구조 검증 / 리팩터링 감사 / 누락 탐지만 수행.  
**근거:** 실제 생성자 주입·호출 경로·정책 사용·config 배선·하드코딩·static 잔존·trace·테스트. 주석/이름만으로 판정하지 않음.

본 문서는 1차 감사와 2차(사후 검증) 감사 내용을 하나로 합친 최종 판단입니다. 1차 감사 시점에 있던 6차 미완료·2차 ActionTypeMetadataLoader 잔존 등은 이후 반영되어, 2차 감사 시점 기준으로 6차·2차가 완료된 상태를 반영합니다.

---

## 1. 전체 판정

**판정: PASS WITH MINOR GAPS**

**요약:**

- **1차~6차는 런타임 경로에서 구현 완료.** 입력 해석은 주입된 resolver만 사용하고, catalog와 order는 분리되어 있으며, metadata는 provider만 사용(static loader 없음). Profile은 seed만 정의하고 필요 시 compatibility를 정책 소스에 위임하며, 추천은 expander와 action/role order policy를 사용한다. **Role 추천은 정책 우선:** `SemanticRecommendationService`가 role 후보를 `RolePreferenceSource`에서 먼저 가져오고, `RoleRecommendationOrderPolicy.applyOrder`를 적용하며, 정책이 비어 있을 때만 profile로 fallback한다.
- **5차 완료.** 정책 소스·config 배선은 source-backed. **Profile seed 데이터도 source-backed:** `CategorySemanticProfileSeedSource`가 seed 내용을 제공하고, `DefaultCategorySemanticProfileRegistry`는 seed source에서 조회한 뒤 프로필을 조립만 하며 register* 메서드에 인라인 하드코딩된 action/role 목록이 없다. Seed 데이터 소유는 `DefaultCategorySemanticProfileSeedSource`(기본 인메모리 구현)에 있으며, 소스 교체 시 조립된 프로필 내용이 바뀐다. Compatibility 그룹은 `CompatibilityPolicySource`가 non-empty를 반환하면 그 값으로 오버라이드된다.
- **잔여 갭:** (1) `DefaultObjectivePolicySource` 내부 `DEFAULT_EXPLICIT_MAPPINGS`(기본 구현체 소유 매핑; 문서화됨). (2) `DefaultCategorySemanticProfileRegistry.parseActionGroup`가 `ActionGroup` enum에 결합. (3) `registry.getAll()`이 부트스트랩·테스트에서 `ConcreteActionByGroupIndex` 구성에 사용됨(추천 최종 순서는 order policy가 결정).

---

## 2. 단계별 감사 결과

### 1차: 입력 해석 구조

- **판정:** 완료
- **실제 반영**
  - `ActionTypeDeserializer`: 주입된 `ActionTypeResolver`만 사용, blank/null 시 예외(기본값 정책 없음). 17–31행.
  - Catalog와 compatibility order 분리: `ActionTypeCatalog`(정의 집합)와 `OrderedActionTypeResolutionSource`/`ActionTypeCompatibilityResolver`(해석 순서)가 config에서 분리 주입. `ActionTypeRegistryConfig`에서 `ActionTypeResolver`는 `DefaultActionTypeResolver(registry, actionTypeCompatibilityResolver)`로 구성.
  - 문자열→ActionType 해석: `DefaultActionTypeResolver`가 stable key 우선, 실패 시 `ActionTypeCompatibilityResolver` 위임. ActionType 경로에서 EnumResolver/EnumCompatParser 미사용(RoleType 등 다른 enum용).
- **덜 반영된 점:** 없음(1차 목표 기준).
- **가짜 분리/숨은 결합:** 없음.
- **증거:** `ActionTypeDeserializer.java`, `DefaultActionTypeResolver.java`, `ActionTypeCompatibilityResolver.java`, `ActionTypeResolutionStructureTest.java`.

### 2차: metadata / registry / bootstrap

- **판정:** 완료
- **실제 반영**
  - **ActionTypeMetadataLoader:** 제거됨. `ActionTypeMetadataLoader*` 파일 검색 결과 0건. 2차 “metadata loader static 구조 제거” 충족.
  - Config는 `ActionTypeMetadataProvider` 빈(ClasspathActionTypeMetadataProvider) 사용. static loader 직접 호출 없음.
  - Registry: `ActionTypeRegistry`는 stable key 맵 + catalog 기반 리스트만 보유. metadata/정책 로딩은 하지 않음.
  - `SemanticStructurePhase6Test.actionTypeMetadataLoaderRemoved()`에서 해당 로더 클래스 부재를 단언.
- **덜 반영된 점:** 없음. (1차 감사에서 지적된 “ActionTypeMetadataLoader 잔존”은 현재 코드베이스에서 해소됨.)
- **가짜 분리:** 없음.
- **증거:** `ActionTypeRegistryConfig`, `ClasspathActionTypeMetadataProvider`, `SemanticStructurePhase6Test`.

### 3차: canonical / profile / semantic policy

- **판정:** 완료
- **실제 반영**
  - `DefaultCanonicalActionRegistry.toCanonical`: `actionType.getActionGroup()` 우선, null일 때만 registry 조회. 정의 기반.
  - Profile: `DefaultCategorySemanticProfileRegistry`는 seed source에서 `getSeed(category)`로 데이터를 얻어 조립하며, `registry.getAll()` 또는 catalog 순서에 의존하지 않음. `prepare()`는 seed의 actions로 `toActionGroupMap` 구성; `compatibilityPolicySource`가 non-empty면 `getCompatibleGroupKeys(category, intent)`로 그룹 맵 오버라이드. Profile 자체는 getAll() 호출하지 않음.
  - ResolutionConfig: concrete enum 상수 직접 참조 없음. PolicySourceRegistry, ObjectivePolicySource 등 인터페이스/구현 타입만 사용.
- **덜 반영된 점:** 없음(3차 목표 기준).
- **숨은 이슈:** `parseActionGroup(String)`가 `ActionGroup.valueOf(key.trim())` 사용 — 호환 정책은 문자열이지만 파싱이 concrete enum에 결합됨(§3 참고).
- **증거:** `DefaultCanonicalActionRegistry.java`, `DefaultCategorySemanticProfileRegistry.java`, `CanonicalActionMappingTest`.

### 4차: recommendation 구조 (compatibility / expander / order)

- **판정:** 완료
- **실제 반영**
  - `RecommendationConcreteActionExpander`: 별도 계층. `SemanticRecommendationService`는 주입받아 `expand(category, intent, allowedGroups, seedActions)`만 호출. 확장 규칙은 expander + `ConcreteActionByGroupIndex`에만 있음.
  - 추천 순서: `ActionRecommendationOrderPolicy` 적용. 서비스는 `orderPolicy.applyOrder(category, intent, expanded, collector)` 호출. seed/registry 순서는 order policy 입력으로만 들어가고, 최종 순서는 preference source 기반. `DefaultPolicySourceRegistry.getActionRecommendationOrderPolicy(version)`가 preference source로 order policy 생성.
  - `DefaultRecommendationConcreteActionExpander`: Javadoc상 registry.getAll()/catalog order 미사용, 반환 순서는 미정의이며 호출자가 order policy 적용.
- **덜 반영된 점:** 없음.
- **가짜 분리:** 없음.
- **증거:** `SemanticRecommendationService.java`, `DefaultRecommendationConcreteActionExpander.java`, `SemanticStructurePhase4Test`.

### 5차: source-backed 정책 구조 (recommendation / objective / compatibility)

- **판정:** 완료
- **실제 반영**
  - Policy 소스·config: PolicySourceRegistry·PolicyBundle로 소스 교체 가능. Config에 raw map 없음. ResolutionConfig는 PolicyBundle 빌드 시 Default*Source 생성자만 호출. categorySemanticProfileRegistry는 `CategorySemanticProfileSeedSource` 빈을 주입받아 프로필을 조립하며, registry에서 `CompatibilityPolicySource`를 받아 그룹 키 오버라이드에 사용. ObjectiveMappingRegistry·ObjectiveResolver는 registry의 `ObjectivePolicySource`로 배선.
  - **Profile seed는 source-backed.** `CategorySemanticProfileSeedSource` 인터페이스로 seed 제공. `DefaultCategorySemanticProfileRegistry`는 생성자로 seedSource를 받아 `getSeed(category)`로만 데이터를 얻고, register* 메서드 없이 `buildProfile(seed)`로 프로필 조립. 카테고리/의도별 action·role 목록은 `DefaultCategorySemanticProfileSeedSource`(기본 인메모리 구현)에 있으며, seed source 교체 시 조립된 프로필 내용이 변경됨.
  - CompatibilityPolicySource: `prepare()`에서 소스가 non-null이고 `getCompatibleGroupKeys(category, intent)`가 비어 있지 않으면 그 결과로 그룹 맵 사용, 아니면 seed의 action 기반 그룹 사용.
  - ObjectivePolicySource: ObjectiveMappingRegistry → ObjectiveResolver 경로에서 사용. DefaultObjectivePolicySource는 내장 DEFAULT_EXPLICIT_MAPPINGS를 소유(문서화됨); 다른 소스로 교체 가능.
- **덜 반영된 점:** 없음(5차 목표 기준). DefaultObjectivePolicySource 내장 매핑은 기본 구현체의 소스 소유 데이터로 남김(의도적).
- **가짜 분리:** 없음.
- **증거:** `CategorySemanticProfileSeedSource`, `DefaultCategorySemanticProfileSeedSource`, `DefaultCategorySemanticProfileRegistry`, `ResolutionConfig`, `CategorySemanticProfileSeedSourceAssemblerTest`, `SemanticStructurePhase5Test`.

### 6차: role recommendation 독립 policy 축 (Category→Intent→Action→Role)

- **판정:** 완료 (1차 감사 시점의 “미완료”는 이후 반영됨)
- **실제 반영**
  - **Role 후보를 정책에서 먼저 조회:** `SemanticRecommendationService` 107–114행: `roleOrderPolicy = policySourceRegistry.getRoleRecommendationOrderPolicy(version)`, `rolePreferenceSource = policySourceRegistry.getRolePreferenceSource(version)`, `fromPolicy = roleCandidatesFromPolicy(category, intent, rolePreferenceSource)`, `roleCandidatesRaw = fromPolicy.isEmpty() && profile != null ? profile.getRecommendedRolesForIntent(intent) : fromPolicy`, `roleCandidates = roleOrderPolicy.applyOrder(category, intent, roleCandidatesRaw, collector)`. 즉 role 후보는 정책 우선, 정책이 비어 있을 때만 profile fallback.
  - **RoleRecommendationOrderPolicy:** `PolicySourceRegistry.getRoleRecommendationOrderPolicy(version)` 존재. `DefaultPolicySourceRegistry`가 `DefaultRoleRecommendationOrderPolicy(rolePreferenceSource)` 생성. 위 서비스 흐름에서 사용.
  - **순서가 profile/enum이 아님:** `DefaultRoleRecommendationOrderPolicy`는 `preferenceSource.getPreferredRoleKeys(category, intent)` 순으로 정렬, fallback은 stable key. Javadoc: “Role recommendation order is never from profile list order or enum order.”
  - **Trace:** `roleSourceFromPolicy`에 따라 `roleInclusionStage`(“role-policy” vs “profile-fallback”), `roleInclusionPolicy` source id(ROLE_POLICY_SOURCE_ID vs ROLE_PROFILE_FALLBACK_SOURCE_ID) 설정. 실행 경로와 일치.
  - **테스트:** `SemanticStructurePhase6Test`에서 role 추천이 role policy source 사용, 스왑 시 추천 role 변경, 순서가 정책 기반, trace가 role-policy/profile-fallback 반영, registry가 getRoleRecommendationOrderPolicy 노출 검증.
- **덜 반영된 점:** 없음. 1차 감사에서의 “role이 profile만 사용”, “RoleRecommendationOrderPolicy 부재”는 현재 코드베이스에서 해소됨.
- **가짜 분리:** 없음.
- **증거:** `SemanticRecommendationService.java`, `DefaultPolicySourceRegistry`, `DefaultRoleRecommendationOrderPolicy`, `SemanticStructurePhase6Test`.

---

## 3. 가장 위험한 잔존 문제 TOP 10

1. ~~**Profile seed 데이터의 비 source-backed 하드코딩 (5차)**~~ **해소됨.**  
   Seed는 `CategorySemanticProfileSeedSource`로 제공되며, `DefaultCategorySemanticProfileRegistry`는 조립만 수행. Seed 내용은 `DefaultCategorySemanticProfileSeedSource`에 있으며 소스 교체로 변경 가능.

2. **DefaultObjectivePolicySource 내장 매핑**  
   `DEFAULT_EXPLICIT_MAPPINGS` 및 도메인 기본값이 기본 구현체 내부에 있음(클래스 Javadoc에 “source-owned” 명시). 다른 ObjectivePolicySource로 교체 가능.  
   **관련:** DefaultObjectivePolicySource. **심각도:** 하.

3. **호환 그룹 파싱의 ActionGroup enum 결합**  
   `DefaultCategorySemanticProfileRegistry.resolveGroupKeys` → `parseActionGroup`에서 `ActionGroup.valueOf(key.trim())` 사용.  
   **관련:** DefaultCategorySemanticProfileRegistry 173–180행. **심각도:** 하.

4. **부트스트랩·검증에서 registry.getAll() 사용**  
   `ResolutionConfig.concreteActionByGroupIndex(actionTypeRegistry.getAll())`, `DefaultPolicyValidationContext`에서 getAll() 사용. 추천 최종 순서는 order policy가 결정하므로 영향 낮음.  
   **관련:** ResolutionConfig, DefaultPolicyValidationContext, ActionTypeRegistry. **심각도:** 하.

5. **ConcreteActionByGroupIndex가 getAll() 순서로 구성**  
   확장 시 index는 부트스트랩에서 getAll()로 만들어지나, 최종 순서는 order policy가 정함.  
   **관련:** DefaultConcreteActionByGroupIndex, ResolutionConfig. **심각도:** 하.

6. **테스트가 profile·concrete enum에 직접 의존**  
   Phase/구조 테스트가 DefaultCategorySemanticProfileRegistry·concrete enum을 직접 구성. 현재 구조는 보호하나, profile을 나중에 source-backed로 바꾸면 테스트 수정 필요 가능.  
   **관련:** SemanticStructurePhase4Test, SemanticStructurePhase5Test, SemanticStructurePhase6Test, ProfileCanonicalFirstTest. **심각도:** 하.

7. **기존 컴파일 실패**  
   `DefaultValidatedPolicyBundle` record 접근자 타입 불일치, `DefaultPolicyCompatibilityAnalyzer`의 `PolicyDiff` 부재로 빌드·전체 테스트 실행 불가.  
   **관련:** DefaultValidatedPolicyBundle, DefaultPolicyCompatibilityAnalyzer. **심각도:** CI/검증 관점 상. 구조 판단은 코드 검토 기준으로 유지.

8. **Trace source ID 상수화**  
   ROLE_POLICY_SOURCE_ID 등이 상수. 다른 role preference source 구현을 써도 trace에 동일 ID가 찍힐 수 있음.  
   **관련:** SemanticRecommendationService, DefaultRoleRecommendationOrderPolicy. **심각도:** 하.

9. **EnumResolver / 다른 enum**  
   ActionType 경로는 EnumResolver 미사용. RoleType 등 다른 enum은 lenient 파싱 사용 가능. 1차 범위가 ActionType이면 해당 없음.  
   **심각도:** 하.

10. ~~**CompatibilityPolicySource 스왑 시 profile 동작 변경 검증 테스트 부재**~~ **해소됨.**  
    `CategorySemanticProfileSeedSourceAssemblerTest.compatibilitySourceOverrideReflectedInProfile()`에서 (category, intent)에 대해 CompatibilityPolicySource가 non-empty일 때 조립된 profile 그룹이 반영됨을 검증.

---

## 4. “겉보기엔 해결됐지만 실제로는 안 끝난 것” (가짜 분리)

- **Role policy:** 1차 감사 시점에는 “RolePreferenceSource는 있으나 서비스가 profile만 사용”이었음. **현재는 가짜 분리 아님.** Role 경로가 `RolePreferenceSource`·`RoleRecommendationOrderPolicy`를 실제 사용하고, profile은 fallback만 담당. Trace도 policy vs profile 구분.
- **Action recommendation:** 가짜 분리 없음. 순서는 `ActionRecommendationOrderPolicy`·preference source에서 나옴. Expander는 최종 순서를 정하지 않음.
- **Policy source registry:** 다섯 가지(ActionRecommendationOrderPolicy, RoleRecommendationOrderPolicy, CompatibilityPolicySource, ObjectivePolicySource, RolePreferenceSource) 모두 런타임 또는 배선에서 사용됨.
- **Source-backed “데이터”:** 5차 완료 후 **seed 목록**은 `CategorySemanticProfileSeedSource`를 통해 제공되며, registry는 seed source에서 조회·조립만 수행. 기본 seed 내용은 `DefaultCategorySemanticProfileSeedSource`에 있으며, 소스 교체로 변경 가능.
- **ActionTypeMetadataLoader:** 1차 감사 시 “deprecated만 있고 삭제 안 됨”이었으나, **현재는 클래스 제거됨.** 해당 이슈 해소.

---

## 5. 실제 의존 위반 목록

- **adapter → domain.value:** 없음. `UnifiedPromptResponseMapper`는 `QualityBadgeItem`(port)만 사용. adapter.in.web이 domain.value를 직접 참조하는 패키지 의존 없음.
- **runtime → adapter:** 없음. SemanticRecommendationService, resolution 등이 adapter 패키지에 의존하지 않음.
- **policy → runtime:** 없음. policy·infrastructure.policy가 application.semantic.recommendation/resolution에 의존하지 않음.
- **observability → semantic core:** trace 타입은 domain.semantic.trace 등에 있으며, observability가 recommendation 엔진 로직에 의존하는 부분 없음.
- **ArchUnit:** `PromptEngineModuleBoundaryTest`로 위 규칙 검증. (프로젝트 컴파일 오류로 실행은 불가했으나, 검토 시점에서 import 기준 위반 없음.)
- **정리:** 금지 의존 위반으로 적발된 구체적 위치 없음. 6차 “role이 policy를 타야 함”은 1차 시점에는 “연결 누락”이었고, 현재는 연결된 상태.

---

## 6. 테스트 신뢰성 감사

- **구조를 보호하는 테스트**
  - `ActionTypeResolutionStructureTest`: catalog vs compatibility order 분리, stable key 우선 등 실제 구조 검증.
  - `SemanticStructurePhase4Test`: compatibility vs expansion vs order policy 분리, seed 순서와 order policy 순서 차이를 실제 교체로 검증.
  - `SemanticStructurePhase5Test`: preference source 스왑 시 recommendation order 변경, objective source 스왑 시 매핑 변경, Category/Intent 1급 입력 검증, CompatibilityPolicySource 사용 시 profile 그룹 반영, ResolutionConfig에 EXPLICIT_MAPPINGS 없음 검사.
  - `CategorySemanticProfileSeedSourceAssemblerTest`: profile seed source 추출(5차) 검증 — registry가 seed source에서 조립, seed source 스왑 시 조립 결과 변경, CompatibilityPolicySource 오버라이드 시 프로필 그룹 반영, 기본 소스는 EXTRACTION 제외 프로필 카테고리만 반환.
  - `SemanticStructurePhase6Test`: role 추천이 role policy source 사용, 스왑 시 추천 role 변경, 순서가 정책 기반, trace가 role-policy/profile-fallback 반영, registry의 getRoleRecommendationOrderPolicy 노출, ActionTypeMetadataLoader 제거 단언.
  - `PromptEngineModuleBoundaryTest`: core / adapter / runtime / policy / observability 패키지 경계 ArchUnit 검사.
  → 실제 빈/인스턴스 조합과 호출 경로 사용. “이름만 구조 테스트” 아님.

- **1차 감사에서 빠져 있던 것 → 현재 반영**
  - 6차 “role recommendation uses role policy source and order policy” 전용 테스트 있음 (SemanticStructurePhase6Test).
  - ActionTypeMetadataLoader 부재 검증 테스트 있음.

- **아직 보호되지 않는 부분**
  - CompatibilityPolicySource 스왑 시 profile 호환 그룹 변경: `CategorySemanticProfileSeedSourceAssemblerTest.compatibilitySourceOverrideReflectedInProfile()`로 검증됨.
  - Profile이 registry.getAll()을 호출하지 않음을 리플렉션/호출로 보장하는 테스트는 없음(주석·수동 검토만).
  - 컴파일 오류로 인해 전체 테스트 실행은 미수행.

- **회귀 시 탐지**
  - Role 경로를 다시 profile만 쓰도록 되돌리면 SemanticStructurePhase6Test(role 순서·trace) 실패.
  - Order policy 우회 시 Phase 4 테스트 실패.
  - Adapter가 domain.value에 의존하기 시작하면 ArchUnit 실패(빌드 가능해지면).

---

## 7. 최종 수정 필요 목록

| 수정 대상 | 필요한 수정 | 이유 | 난이도 | 우선순위 |
|-----------|-------------|------|--------|----------|
| (기존) `DefaultValidatedPolicyBundle` | record 접근자 반환 타입을 record 컴포넌트와 맞추거나 컴포넌트 타입 조정 | 빌드 차단 해소 | 하 | 상 |
| (기존) `DefaultPolicyCompatibilityAnalyzer` | 누락된 `PolicyDiff` 심볼 해결(import 또는 타입) | 빌드 차단 해소 | 하 | 상 |
| ~~`DefaultCategorySemanticProfileRegistry`~~ | **완료:** profile seed는 `CategorySemanticProfileSeedSource`로 제공, registry는 조립만 수행. | 5차 source-backed seed 반영됨. | — | — |
| `DefaultObjectivePolicySource` | 선택: 기본 매핑은 현재 클래스 Javadoc에 “source-owned” 명시됨. 별도 defaults 리소스 분리는 선택 사항. | 5차(경미) | 하 | 하 |
| `DefaultCategorySemanticProfileRegistry.parseActionGroup` | 선택: 그룹 키→그룹 해석을 전략/레지스트리로 추상화해 ActionGroup enum 직접 의존 완화 | 3차(concrete enum 결합) | 중 | 하 |
| ~~테스트~~ | **반영됨:** `CategorySemanticProfileSeedSourceAssemblerTest`에서 seed source 스왑·CompatibilityPolicySource 오버라이드 검증. | 5차/테스트 | — | — |

**이미 반영된 수정(1차 감사 대비):**

- SemanticRecommendationService: Role 후보·순서를 PolicySourceRegistry의 RolePreferenceSource 및 RoleRecommendationOrderPolicy에서 가져오도록 변경 완료. Profile은 fallback만 사용.
- PolicySourceRegistry/DefaultPolicySourceRegistry: getRoleRecommendationOrderPolicy(version) 추가 및 RolePreferenceSource로 role order policy 빌드 반환 완료.
- RoleRecommendationOrderPolicy 인터페이스 및 DefaultRoleRecommendationOrderPolicy 구현 완료.
- ActionTypeMetadataLoader 제거 완료.
- SemanticRecommendationService role trace: policy vs profile에 따라 source id·stage 반영 완료.
- “Role recommendation uses role policy source and order policy” 검증 테스트(SemanticStructurePhase6Test) 추가 완료.
- **5차 profile seed source-backed:** CategorySemanticProfileSeedSource 도입, DefaultCategorySemanticProfileRegistry는 seed source에서 조회·조립만 수행(register* 인라인 하드코딩 제거). DefaultCategorySemanticProfileSeedSource가 기본 seed 데이터 소유. CategorySemanticProfileSeedSourceAssemblerTest로 seed source 스왑·CompatibilityPolicySource 오버라이드 검증 추가 완료.

---

*감사 기준: 실제 참조·생성자 주입·config 배선·호출 경로·하드코딩·static 잔존·source-backed 여부·trace·테스트. 주석/이름만으로 반영 판정하지 않음. 1차·2차 감사 내용을 통합하여 현재 코드베이스 기준으로 정리함.*

# 구조 재감사 점검 결과 — 코드 검증 및 추가 작업 정리

## 1. 문서 인용 vs 실제 코드 검증 결과

### 1차 — PromptSpecFactory 경계 복구
**판정: 완료 (검증 일치)**

- `PromptSpecFactory`에 `org.example.sharedprompts.domain.prompt.application` 계층 의존 없음
- 생성자/필드는 domain 타입만 사용 (ObjectiveRegistry, StrategyBundlePolicy, ObjectiveResolverPort 등)
- Javadoc에만 "application layer" 언급, 실제 타입 의존 없음

---

### 2차 — null → default new 제거
**판정: 완료 (검증 일치)**

- `domain.prompt.domain` 내 `== null ? new` / `requireNonNullElse` 패턴 미발견
- `DefaultCategorySemanticProfileRegistry`: `seedSource`에 `Objects.requireNonNull` 사용
- `GuidelineVerifier`: `ruleChecker`에 `Objects.requireNonNull` 사용

---

### 3차 — ObjectiveMappingRegistry 책임 분리
**판정: 완료 (검증 일치)**

- 생성자: `ObjectivePolicySource` + `ObjectiveHeuristicInferencePolicy`만 의존
- `findByActionType`: overlay → policySource → heuristicInferencePolicy 순서 위임
- 휴리스틱은 `DefaultObjectiveHeuristicInferencePolicy`에 KEYWORD_MAP 기반으로 분리

---

### 4차 — Category seed source 중앙 switch 제거
**조회 경로: 완료 | seed 데이터 소유: 미완료 (검증 일치)**

- `getSeed()`: `SEED_SUPPLIERS` 맵 조회, 중앙 switch 없음
- 현재 코드는 `category.canonical()`을 사용해 legacy 카테고리 지원 (문서 미기재 개선)
- seed 본문은 여전히 `DefaultCategorySemanticProfileSeedSource`의 `buildXxx()`에 집중

---

### 5차 — IntentDictionary 구조 개선
**판정: 완료 (검증 일치)**

- `IntentDefinitionDataSource.definitionsEntries()` / `resolutionDefaultEntries()` 결과만 사용
- `EnumSet.allOf(ActionIntent.class)` 기반 fail-fast completeness 검증 유지
- "데이터 소유"는 DataSource, "인덱싱/검증/조회"는 Dictionary에 분리

---

### 5차 보강 — IntentDefinitionDataSource 데이터 소유 분산
**판정: 완료 (검증 일치)**

- provider 패턴으로 definitions/defaults 분산
- 기본 provider 목록은 `DEFAULT_*_PROVIDERS`로 유지하고, `public` 생성자 + `collectDefinitionsEntries` / `collectResolutionDefaultEntries`로 임의 provider 목록 주입·집계 가능 (`IntentDefinitionEntriesProvider` / `IntentResolutionDefaultsEntriesProvider`도 public)
- 프로덕션 집계는 `definitionsEntries()` / `resolutionDefaultEntries()`가 내부 `DEFAULT` 인스턴스에 위임
- 문서 미기재 개선: intent 중복 시 `IllegalStateException` throw (동일 intent 중복 등록 방지)

---

## 2. 추가로 필요한 작업 (우선순위)

**상태: 권장 작업 1~3 반영 완료 (2026-03)**

| 우선순위 | 조치 |
|----------|------|
| 1 | `IntentDefinitionDataSource` 생성자 + `collect*` 인스턴스 메서드, static 엔트리는 `DEFAULT`에 위임 |
| 2 | `DefaultCategorySemanticProfileRegistry` 생성 시 `profileCategoriesForRegistry()`마다 `requireSeed` 필수, 누락 시 `IllegalStateException`; `DefaultCategorySemanticProfileSeedSource`는 enum 집합과 정의 카탈로그 일치를 생성자에서 검증 (6차) |
| 3 | `parseActionGroup` 파싱 실패 시 SLF4J `warn` (키 무시는 유지, 로그로 관측) |

---

## 3. 검증 중 발견된 문서 미반영 개선 사항

1. **DefaultCategorySemanticProfileSeedSource**
   - `getSeed` / `requireSeed`: `category.canonical()` 기준 조회; `requireSeed`는 미등록·null 시 즉시 예외

2. **IntentDefinitionDataSource.definitionsEntries() / resolutionDefaultEntries()**
   - intent 중복 등록 시 `IllegalStateException` 발생

3. **DefaultObjectiveHeuristicInferencePolicy.inferByActionName()**
   - `actionType instanceof Enum<?>` 검사 후 `e.name()` 사용 (document는 `String.valueOf(actionType)` 인용)

---

## 4. 여전히 남아 있는 구조 이슈 (문서 요약)

| 이슈 | 설명 |
|------|------|
| 카테고리 seed 조각 배치 | 본문은 `semantic/seed/*SemanticProfileSeed`에 분산; `CategorySemanticProfileSeedDefinitions`가 목록만 유지 |
| enum 대비 카탈로그 | 새 `PromptCategory` 대표값 추가 시 `canonicalSemanticProfileCategories()`에 포함 → `defaultDefinitions()`에 정의 추가 필수 (생성자 fail-fast) |
| 기타 조용한 drop | `putByStableKey`(null/blank 키·object null 시 no-op), `parseActionGroup`은 무효 키 시 warn 후 drop |

**6차 반영:** `requireSeed` / `getSeed(null)` 거부, 레지스트리는 `requireSeed`만 사용. 비 프로필 카테고리는 `getSeed` empty가 허용되나 `requireSeed`는 예외.

---

## 5. 권장 작업 순서

1~3 항목 및 6차(seed 완전성·`requireSeed`) 반영 완료. 이후는 `CategorySemanticProfileSeedDefinitions` 목록 자동 수집(provider/registry) 등 구조적 개선 시 별도 검토.

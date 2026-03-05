---

## 9. EngineMode / EngineProfile 관계

- **외부 계약 (`EngineMode`)**
  - `AUTO`: 요청/규칙에 따라 내부 프로파일을 자동 선택 (기본값)
  - `V2`  : 품질 우선 V2 품질 파이프라인을 명시적으로 사용
  - `V3`  : 향후 Fast/경량 파이프라인(FAST_PIPELINE)에 매핑 예정
- **내부 의미 기반 프로파일 (`EngineProfile`)**
  - `QUALITY_PIPELINE`: 품질 우선 표준 파이프라인 (현재 V2에 매핑)
  - `FAST_PIPELINE`   : Fast/경량 경로 (현재는 품질 파이프라인으로 폴백, `routing_reasons` 에 사유 기록)
  - `JSON_STRICT`     : JSON Schema/포맷 엄격 준수 프로파일 (json_schema 존재 시 우선 선택)
  - `AUTO`            : Intent/규칙 기반으로 위 프로파일 중 하나를 선택
- **AUTO 결정 규칙 (요약)**
  - json_schema 존재 시: `EngineProfile.JSON_STRICT` + `EngineMode.V2`
  - json_schema 없음 + 명시적 V2/V3 없음: IntentDefaults 기반 추천 프로파일(기본 `QUALITY_PIPELINE`) + `EngineMode.V2`
  - 모든 결정 사유는 `routing_reasons` 필드에 기록된다.

---

## 10. Schema 검증 결과 점진 이관 로드맵 (요약)

1. 현재: Verify 단계의 품질 배지(`FORMAT_VERIFIED`)를 기반으로 `SchemaContractEvaluator` 가 스키마/포맷 준수 여부를 추론한다.
2. 다음 단계: `VerifyResult` 에 `formatValid/jsonValid/schemaValid` 및 `reasons[]` 를 추가하고, 배지 대신 이 필드를 1차 소스로 사용한다.
3. `GeneratePromptResult` 에도 최소한 `schemaValid` 및 관련 사유를 전달해 상위 레이어에서 직접 활용 가능하게 한다.
4. 충분한 이행 기간 이후에는 배지 기반 추론 로직을 제거하고, 전적으로 VerifyResult/GeneratePromptResult 의 명시적 필드만 사용한다.
5. 전환 과정에서 메트릭 태그와 `schema_failure_reasons` 응답 필드는 유지해 모니터링 공백을 방지한다.

---

## 11. 테스트 전략 (최소 세트 유지)

- **라우팅/정책 계층 단위 테스트**
  - `IntentDefaultsResolverTest`: 각 `ActionIntent` 가 올바른 `PromptObjective`/`OutputNeeds` 기본값을 제공하는지 검증.
  - `RoutingRuleEngineTest`: priority/조건 구체성/등록 순서에 따른 Rule 충돌 해결 및 `appliedRuleIds` 반환 검증.
  - `DomainFinalizerTest`: `DomainResolutionService.resolveForUnified` + `ResolvedDomain` 결과를 이용한 최종 TaskDomain 보정 검증.
  - `OutputContractPlannerTest`: `json_schema` 존재 시 EXTRACTION/JSON_SCHEMA_REQUIRED 강제 및 사유 문자열 확인.
  - `UnifiedRoutingFacadeTest`: end-to-end 라우팅 결정이 결정적이며 `engine_profile`/`appliedRuleIds`/`routing_reasons` 가 포함되는지 검증.
- **오케스트레이션 얇은 계층 테스트**
  - `OrchestratorThinTest`: `UnifiedPromptGenerationOrchestrator` 가 정책 계산을 직접 수행하지 않고 `UnifiedRoutingFacade` 에 완전히 위임하는지 (스파이/목 기반) 확인.

## Unified Prompt Engine 변경 요약

- **작성일**: 2026-03-04
- **브랜치**: dev (로컬 변경 기준)

---

## 1. 상위 개요

- **단일 Unified 엔진 진입점 도입**
  - 컨트롤러: `UnifiedPromptEngineController` (`POST /api/prompts/generate`)
  - 요청/응답 DTO: `UnifiedGeneratePromptRequest` / `UnifiedGeneratePromptResponse`
  - 유즈케이스: `GenerateUnifiedPromptUseCase`
  - 커맨드/결과: `UnifiedGeneratePromptCommand`, `UnifiedGeneratePromptResult`
  - 오케스트레이터: `UnifiedPromptGenerationOrchestrator` (단일 엔드포인트 플로우의 진입점)

- **기존 생성 엔드포인트/DTO 제거**
  - 컨트롤러
    - `PromptController` (기존 `/prompts` 생성 엔드포인트 포함)
    - `PromptEngineController` (`/v2/prompts/generate`)
    - `PromptEngineV3Controller` (`/api/v3/prompts/generate`)
  - DTO
    - `GeneratePromptRequest`, `GeneratePromptResponse`
    - `GeneratePromptV3Request`, `GeneratePromptV3Response`
  - V3 유즈케이스
    - `GeneratePromptV3UseCase` 및 관련 `*Command`, `*Result`, `GeneratePromptV3Service`
  - 생성 플로우 파사드/서비스
    - `PromptCreationFlow`, `CreatePromptUseCase`
    - `PromptAIService`, `PromptGenerator`
    - `PromptPersistenceService`
    - `PromptSanitizationService`, `InputRequestDto`

- **V2 품질 파이프라인은 내부 표준 생성기로 유지**
  - `GeneratePromptUseCase` / `GeneratePromptService` 는 그대로 남기고,
    Unified 엔진 내부에서만 호출하는 canonical generator 로 사용.

---

## 2. 오케스트레이션 & 라우팅 레이어

- **`UnifiedPromptGenerationOrchestrator`**
  - 단일 엔드포인트에서 들어오는 `UnifiedGeneratePromptCommand` 를 받아 V2 품질 파이프라인(`GeneratePromptUseCase`)으로 위임.
  - `UnifiedRoutingPolicy` 를 통해 Objective / OutputNeeds / Domain / EngineMode / Role 등을 규칙 기반으로 결정.
  - `SchemaContractEvaluator` 를 통해 최종 결과에 대한 스키마/출력 계약 준수 여부를 평가하고,
    `PromptEngineMetrics` 로 성공/실패, 지연시간, repair 횟수, 스키마 실패 여부 등을 기록.
  - 최종적으로 통합된 `UnifiedGeneratePromptResult` 로 응답 메타데이터(요청/실제 엔진 모드, 도메인, 목적, 출력 요구사항, 역할, 검증 결과)를 함께 반환.

- **`UnifiedRoutingPolicy`**
  - 입력 `UnifiedGeneratePromptCommand` 기반으로 다음을 결정하는 순수 정책 클래스:
    - `EngineMode` (AUTO/V2/V3)
    - `PromptObjective`
    - `OutputNeeds`
    - `TaskDomain`
    - `CoreRoleType`, `DomainRoleType`
  - JSON Schema 유무에 따라 **EXTRACTION + JSON_SCHEMA_REQUIRED** 조합 및 V2 강제 사용 여부를 판단.
  - `ActionIntent` 메타데이터(기본 Objective, 선호 OutputNeeds, 도메인 affinity)를 활용하여 기본값 설정.
  - `DomainResolutionService.resolveForUnified` 와 연동해 Intent/Category 기반 기본 도메인을 계산한 뒤,
    `DomainResolver` 의 해석 결과(`ResolvedDomain`)에 따라 최종 도메인을 보정.

- **`SchemaContractEvaluator`**
  - V2 파이프라인 결과(`GeneratePromptResult`)의 품질 배지(특히 `FORMAT_VERIFIED`)를 기반으로
    JSON Schema / OutputContract 준수 여부를 평가.
  - 평가 결과(`SchemaContractEvaluation`)는:
    - `schemaContractFailed`
    - `schemaFailureReasons`
    필드로 `UnifiedGeneratePromptResult` 에 포함되어 상위 계층에서 활용 가능.

---

## 3. Intent/도메인/출력 타입 시스템 정비

- **새로운 상위 개념 Enum 추가**
  - `ActionIntent`: 상위 작업 의도 (예: GENERATE, REWRITE, SUMMARIZE, EXPLAIN, PLAN, ANALYZE, EVALUATE, EXTRACT, CLASSIFY, DECIDE, DEBUG, DESIGN, CODE 등)
    - 각 Intent 별로 `PromptObjective`, `OutputNeeds`, `ResponseShape`, 선택적인 `TaskDomain` affinity 메타데이터를 보유.
  - `EngineMode`: AUTO / V2 / V3 모드 추가, Unified 라우팅에서 공통 사용.
  - `PromptObjective`: CREATIVE, FACTUAL, REASONING, EXTRACTION, PLANNING, CODE 등 목적 중심 분류.
  - `OutputNeeds`: FREE_FORM, STRUCTURED_TEXT, BULLET_LIST_REQUIRED, TABLE_REQUIRED, JSON_REQUIRED, JSON_SCHEMA_REQUIRED, CODE_BLOCK_REQUIRED 등 출력 형태 요구사항.
  - `ResponseShape`: NARRATIVE, CONCISE, STEP_BY_STEP, STRUCTURED 등 응답 구조 힌트.
  - `ExperienceLevelBucket`: 사용자 경험 수준을 버킷 단위로 표현하기 위한 Enum 추가.

- **기존 Enum 확장/정비**
  - `PromptCategory`, `StyleType`, `ToneType`, `TaskDomain`, `ExperienceLevel` 등에서
    - Intent/Objective/OutputNeeds 체계와 정합성을 맞추기 위한 값/메타데이터 보완.
    - 도메인 분류 및 기본 도메인 매핑(`getDefaultDomain`) 정비.

- **안정적인 키 기반 Enum 추상화**
  - `StableKeyedEnum` 도입 및 `EnumResolver`, `RoleTypeDeserializer` 수정.
  - 외부/클라이언트와의 계약을 Enum 이름 변화에 덜 민감하게 만들기 위한 안정된 키 전략 적용.

- **액션/역할 택소노미 보강**
  - `ActionTypeBehaviorRegistry` 추가.
  - `*ActionType`, `*RoleType` 들과 Intent/Domain/Objective 체계를 연결하는 레지스트리 역할을 수행하도록 설계.

---

## 4. 도메인 해석 레이어 변경

- **`DomainResolutionService` / `DomainResolver` / `ResolvedDomain`**
  - Unified 플로우를 고려하여 도메인 해석 로직을 재정렬.
  - Intent 및 Category 기반 기본 도메인을 계산한 뒤, 해석 결과(`ResolvedDomain`)에 따라 최종 도메인을 보정하는 구조.
  - Unified 용 전용 API `resolveForUnified(PromptCategory, TaskDomain)` 추가:
    - ActionType 없이도 Category + Intent 기반 도메인 힌트로 도메인을 결정할 수 있도록 `DomainResolverPort` 에 위임.

- **`DomainResolution` i18n 가이드라인 제거**
  - 기존 다국어 도메인 가이드라인용 `DomainResolution` 클래스 삭제.
  - 새로운 Intent/Domain/OutputNeeds 기반 정책으로 대체.

---

## 5. 스펙/검증 레이어 변경

- **`PromptSpecFactory` / `PromptSpecValidator`**
  - Unified 모델 및 새 Enum 체계에 맞도록 스펙 생성 및 검증 로직을 보완.
  - Intent/Objective/OutputNeeds/Domain 조합을 기반으로 한 규칙을 정비.

- **스키마 계약 평가 (`SchemaContractEvaluator`)**
  - JSON Schema 기반 요청의 경우, 생성 결과가 스키마/출력 계약을 만족하는지 평가.
  - 형식 검증 배지(`FORMAT_VERIFIED`) 부재를 스키마/포맷 계약 실패로 간주.
  - 실패 여부 및 사유 리스트를 `UnifiedGeneratePromptResult` 에 포함시켜 상위 계층에서 활용 가능하도록 설계.

---

## 6. 메트릭 & 테스트

- **`PromptEngineMetrics` 확장**
  - Unified 엔진 플로우에 맞춰 다음 정보를 기록:
    - EngineMode 별 성공/실패 카운트
    - 엔드투엔드 지연시간(ms)
    - repair 시도 횟수 분포
    - Verify 실패 비율
    - 스키마/출력 계약 실패 여부 및 비율

- **기존 테스트 제거 (재구성 예정)**
  - 다음과 같은 레거시 테스트 클래스 삭제:
    - `PromptMetadataControllerTest`
    - `GeneratePromptServiceTest`
    - `PromptSpecFactoryTest`
    - `PromptSpecValidatorTest`
    - `RecommendationRegistryTest`
    - `TaskDomainTest`
    - `ActionTypeTaskDomainTest`
    - `BadgeResponseAssemblerTest`
  - Unified 엔드포인트 및 새로운 Intent/Domain/OutputNeeds 기반 설계를 반영하는 테스트는
    향후 별도 추가 필요.

---

## 7. 외부 연동/마이그레이션 가이드 (요약)

- **API 엔드포인트**
  - 기존 생성 관련 엔드포인트:
    - `/prompts` (카드 생성 + 저장)
    - `/v2/prompts/generate`
    - `/api/v3/prompts/generate`
    는 모두 제거되었으며,
    **새로운 단일 생성 엔드포인트**로 마이그레이션 필요:
    - `POST /api/prompts/generate` (`UnifiedPromptEngineController`)

- **요청 스키마 (`UnifiedGeneratePromptRequest`)**
  - 주요 필드:
    - `input` (필수): 사용자 입력 텍스트
    - `category` (선택): `PromptCategory`, 없으면 GENERAL 로 폴백
    - `intent` (선택): `ActionIntent`, 없으면 GENERATE 로 폴백
    - `engine_mode` (선택): `EngineMode` (기본값 AUTO, 필요 시 V2/V3 강제)
    - `json_schema` (선택): JSON Schema 문자열, 존재 시 EXTRACTION + JSON_SCHEMA_REQUIRED 및 V2 품질 파이프라인 강제
    - 고급 옵션:
      - 스타일/톤/언어/경험: `tone`, `style`, `language`, `experience`
      - 액션/역할 오버라이드: `action_type`, `role_type`
      - V3 메타 역할: `coreRole`, `domainRole`
      - 태그: `tags`
  - `disable_quality_pipeline` 는 현재 지원하지 않으며, true 로 전달 시 400 계열 예외가 발생하도록 방어적으로 처리됨.

- **응답 스키마 (`UnifiedGeneratePromptResponse`)**
  - 주요 필드:
    - `output`: 최종 생성된 콘텐츠
    - `requested_engine_mode`: 요청 시 지정한 EngineMode
    - `effective_engine_mode`: 라우팅 후 실제 사용된 EngineMode (현재는 대부분 V2)
    - `resolved_domain`: 최종 TaskDomain
    - `objective`: PromptObjective
    - `outputNeeds`: OutputNeeds
    - `intent`, `variant`
    - `coreRole`, `domainRole`
    - `quality_badges`: 품질 배지 리스트 (`QualityBadge` 기반)
    - `verify_passed`, `repair_count`, `finally_passed`
    - `schema_contract_failed`, `schema_failure_reasons`

> 이 문서는 현재 작업 중인 Unified Prompt Engine 변경 사항을 요약한 것으로,
> 이후 설계 변경 및 추가 구현에 따라 업데이트될 수 있습니다.

현재 dev 브랜치에서는 단순화를 위해 기존 생성 엔드포인트를 제거했으며,
실제 운영 배포 시에는 최소 1~2 릴리즈 동안 @Deprecated 상태로 공존시킨 뒤 제거하는 전략을 권장한다.

---

## 8. 운영·확장 원칙 (Routing/Enum/Spec/검증/호환성)

1) **Unified 엔진의 정체성**

- Unified Prompt Engine은 **“Prompt 카드 생성기”가 아니라 LLM Task Engine / Orchestration Layer**이다.
- Unified Engine은 단일 요청 단위의 1-step LLM Task 실행을 책임지며, 멀티스텝/툴 호출/워크플로우 오케스트레이션은 상위 레이어에서 담당한다.
- Prompt 카드(저장/공유)는 Unified 엔진의 소비자(consumer)로서 상위 레이어에서 구현한다.

2) **UX Enum 최소화 원칙**

- 외부(클라이언트) 계약에서 사용자가 직접 선택하는 축은 최소화한다:
  - `ActionIntent` (작고 안정적인 상위 집합, 10~15개 수준 유지)
  - `PromptCategory` (UI 카테고리)
  - (선택) `OutputNeeds` 또는 outputFormat (TEXT/TABLE/JSON 등)
- 세부 분류/튜닝은 Enum 확장 대신 `ActionTypeBehaviorRegistry`/RuleTable로 관리한다.
  - 새로운 `ActionIntent` 추가는 다음 조건을 모두 만족할 때만 허용한다:
    1. 기존 Intent 조합/옵션으로는 표현이 불가능한 새로운 작업 범주일 것
    2. 실제 UX에서 버튼/옵션 등으로 노출될 필요가 있을 것
    3. 최소 2개 이상의 `ActionType` 이 해당 Intent 에 매핑될 것

3) **Routing 규칙의 계층 구조**

- Routing은 다음 순서로 결정한다:
  1. Intent 메타데이터 기반 기본값 산출 (`PromptObjective` / `OutputNeeds` / `ResponseShape` / `TaskDomain` affinity)
  2. RuleTable(예외 규칙) 적용 (조건 매칭 시 override)
  3. `DomainResolutionService` 로 최종 도메인 보정
- `UnifiedRoutingPolicy`는 위 3단계를 조립하는 오케스트레이터이며, “if-else 정책 로직”의 지속적 증식을 지양한다.

4) **RuleTable 충돌 해결 규칙**

- Rule에는 `priority`(int)를 부여하며, 값이 높을수록 우선한다.
- 동일 `priority`에서 충돌 시:
  - 더 구체적인 조건(조건 필드 수가 많은 rule)이 우선
  - 최종 tie는 등록 순서로 결정(결정적 동작)
- 적용된 rule id 목록은 structured log 및 (필요 시) 응답 meta(`appliedRuleIds`)에 남긴다.

5) **Intent vs ActionType 관계 원칙**

- `ActionIntent`는 정책/라우팅의 1순위 축이다.
- `ActionType`은 세부 튜닝 신호로 사용하며, 기본적으로 Intent를 변경하지 않는다.
- Intent 변경이 필요한 경우는 “예외”로 취급하고, 별도 `IntentOverrideRule`로만 허용한다.

6) **Spec/Validator/Schema 역할 분리**

- `UnifiedRoutingPolicy`: “무엇을 할지”에 대한 전략 메타데이터 결정
- `PromptSpecFactory`: “어떻게 시킬지”에 대한 Spec 구성(섹션/제약/루브릭/전략 번들)
- `PromptSpecValidator`: Spec 정합성/모순/정책 위반 검증
- Schema validation은 Verify 단계에서 수행하며, `SchemaContractEvaluator`는 검증 결과를 집계/응답/메트릭에 반영하는 역할로 제한한다.
  - 향후에는 `VerifyResult`(예: `jsonValid`/`schemaValid`/`formatValid` 및 `reasons[]`)에 검증 결과를 명시적으로 담고, `SchemaContractEvaluator`가 이를 참조하도록 점진 이관한다.

7) **관측(Observability) 원칙**

- 모든 요청에 대해 Routing decision을 로그/메트릭으로 남긴다:
  - intent/objective/outputNeeds/domain/engineMode + appliedRuleIds
- 성공/실패/지연시간/repair 횟수/스키마 계약 실패율을 지속 관측하고, 정책 튜닝에 활용한다.

8) **호환성/마이그레이션 원칙**

- 기존 엔드포인트는 운영 안정성을 위해 최소 1~2 릴리즈 기간 `@Deprecated` 상태로 공존시킨다.
- `StableKeyedEnum`을 외부 계약의 기준으로 삼고, unknown key는 기본적으로 400으로 처리한다(조용한 fallback 지양).


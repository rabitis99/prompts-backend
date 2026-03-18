# Prompt Engine 구조 리팩토링 7차~13차 실행 보고 (2차 보정 포함)

**목적:** 7차~13차 리팩토링 결과를 production 경로 기준으로 재검증하고, 반쪽 수정·가짜 분리·미연결 wiring·테스트 전용 완료 상태를 실제 운영 경로로 재수정한 결과를 기록한다.

---

## A. 현재 상태 재점검 결과 (2차 보정 전·후)

### 반쪽 수정으로 남아 있던 항목

| 항목 | 재점검 결과 | 2차 보정 조치 |
|------|-------------|----------------|
| **Policy runtime 공급** | 1차에서 `PolicyRuntimeBootstrap`가 빌더로만 `ValidatedPolicyBundle` 생성 후 bind → repo/registry 구성. **Pipeline(load→parse→validate→bind)이 production 경로에서 호출되지 않음.** | `PolicyRuntimeBootstrap`에 `PolicyDocumentPipeline`·`PolicyDocumentLoader` 주입. classpath `policy/default-recommendation.json` 존재 시 `loadValidateBundle` → validated 저장·bind → 동일 versionId로 repository + registry 구성. 문서 없거나 pipeline/loader null이면 기존 빌더 fallback 유지. |
| **Response assembler** | 1차에서 이미 인터페이스(`RecommendationExplanationAssembler`)만 의존하도록 수정됨. 재확인 시 **production 소스에 explanation 구현체 import 0개.** | 추가 수정 없음. ArchUnit 규칙 7로 회귀 방지. |
| **Audit record** | 1차에서 facade → `auditPublisher.publish(result)` 호출, `DefaultRecommendationAuditPublisher`가 `RecommendationAuditRecord.of(...)` 호출 후 sink 전달. **production 경로에서 생성·전달 완료.** | 추가 수정 없음. trace 기반 `RecommendationTraceSnapshot`(policyVersionId 등)은 이미 `RecommendationAuditRecord.of(..., trace)` → `RecommendationTraceSnapshot.from(trace)`로 채워짐. |
| **VersionedPolicyRepository** | 1차에서 bootstrap으로 한 버전이라도 채워짐. **Pipeline 경로 사용 시** 해당 version의 `ValidatedPolicyBundle`이 repository에 저장되어 `getPolicy(versionId)`·`getLatest()`가 의미 있는 값을 반환. | Pipeline 연동으로 **문서 기반 default 버전**이 repository에 저장됨. selection strategy·trace·metrics·repository·registry의 version id 일치(동일 `defaultPolicyVersion` 사용). |

### 실제 production 경로에서 비어 있던 연결부

- **Policy document → runtime registry:** 1차 후에도 **PolicySchemaConfig의 pipeline 빈이 PolicyRuntimeBootstrap에 연결되지 않음.** Bootstrap이 pipeline·loader를 받지 않아, 문서 로드 결과가 repository/registry에 반영되는 경로가 없었음. → 2차에서 **PolicyBootstrapConfig가 PolicyDocumentPipeline·PolicyDocumentLoader를 bootstrap에 주입**, bootstrap이 `policy/default-recommendation.json` 로드 시 pipeline 경로로 repo/registry 구성.
- **Repository ↔ registry version 일치:** 1차에서 이미 동일 versionId로 repository와 registry에 넣고 있음. 2차에서도 pipeline 성공 시 동일 versionId 사용하여 유지.

---

## B. 이번 2차 보정에서 수정한 파일

| 파일 경로 | 수정 요약 (1줄) |
|-----------|------------------|
| `src/main/resources/policy/default-recommendation.json` | 신규: production 기본 정책 문서(RecommendationPreference, policyVersion 2026-03-recommendation-v1). |
| `infrastructure/config/PolicyRuntimeBootstrap.java` | Pipeline·Loader 주입 추가; classpath 문서 존재 시 `loadValidateBundle` → validated+bundle로 repo/registry 구성, 없으면 빌더 fallback. |
| `infrastructure/config/PolicyBootstrapConfig.java` | `PolicyRuntimeBootstrap` 빈에 `PolicyDocumentPipeline`, `PolicyDocumentLoader` 인자 추가. |
| `test/.../PolicyRuntimeBootstrapIntegrationTest.java` | `PolicyRuntimeBootstrap` 4-arg 생성자(pipeline=null, loader=null) 사용으로 fallback 경로 단위 테스트 유지. |
| `test/.../PromptEngineModuleBoundaryTest.java` | 규칙 10 추가: ResolutionConfig가 DefaultPolicySourceRegistry를 참조하지 않음(registry는 bootstrap 전용). |

**1차에서 이미 반영되어 2차에서 변경 없음:**  
`DefaultRecommendationResponseAssembler`(인터페이스만 의존), `PromptRecommendationFacade`(auditPublisher.publish), `DefaultRecommendationAuditPublisher`(record 생성·sink 전달), `AuditConfig`, `ResolutionConfig`(registry 빈 제거), `RecommendationAuditRecord`/`RecommendationTraceSnapshot`(trace 기반 필드).

---

## C. 실제로 닫힌 런타임 흐름

### 1. Audit 생성 → publish/sink 경로

- **생성:** 추천 요청 → `RecommendationController` → `PromptRecommendationFacade.recommend(command)` → `recommendPromptAxesUseCase.recommend(command)` → `RecommendPromptResult` 반환 → **`auditPublisher.publish(result)`** 호출.  
  `DefaultRecommendationAuditPublisher.publish(result)` 내부에서 **`RecommendationAuditRecord.of(category, intent, selectedActionKey, selectedRoleKey, actionCandidates, roleCandidates, result.trace().orElse(null))`** 호출.  
  trace가 있으면 `RecommendationTraceSnapshot.from(trace)`로 policyVersionId·policySourceId·experimentId·variantId 등이 snapshot에 채워짐.
- **전달:** 동일 publisher에서 `List<RecommendationAuditSink>`에 대해 `sink.accept(record)` 호출. 기본 빈은 `NoOpRecommendationAuditSink`.  
  **설정:** `AuditConfig`에서 `RecommendationAuditPublisher`(DefaultRecommendationAuditPublisher), `RecommendationAuditSink`(기본 no-op) 빈 등록. `PromptDomainConfig`가 `AuditConfig`를 Import.

### 2. Policy load/validate/bind → repository → runtime registry 경로

- **Production (문서 존재 시):**  
  `PolicyBootstrapConfig.policyRuntimeBootstrap(defaultPolicyVersion, policyBinder, policyDocumentPipeline, policyDocumentLoader)` →  
  `PolicyRuntimeBootstrap` 생성 시 `loader.loadFromClasspath("policy/default-recommendation.json")` 호출.  
  raw가 비어 있지 않으면 **`pipeline.loadValidateBundle(defaultPolicyVersion, Map.of("RecommendationPreference", raw))`** 호출.  
  성공 시 `ValidatedPolicyBundle`을 repository에 저장, `policyBinder.bind(validated)` → `PolicyBundle`을 registry에 동일 versionId로 등록.  
  실패 시 또는 raw 비어 있으면 빌더로 만든 validated → bind → 동일하게 repo/registry 구성.
- **Repository:** `VersionedPolicyRepository`(InMemoryVersionedPolicyRepository)는 bootstrap이 채운 `Map<versionId, ValidatedPolicyBundle>`과 version 순서 리스트를 보유. `getPolicy(versionId)`, `getLatest()`, `listVersions()`가 bootstrap에서 넣은 기본 버전을 반환.
- **Registry:** `PolicySourceRegistry`는 `PolicyBootstrapConfig.policySourceRegistry(bootstrap)`로만 제공. `ResolutionConfig`는 registry 빈을 정의하지 않으며, `DefaultPolicySourceRegistry`를 직접 참조하지 않음(ArchUnit 규칙 10).
- **Selection/trace/metrics/repository/registry version 일치:** `ResolutionConfig.defaultPolicyVersion()`이 반환하는 `PolicyVersion`(id `2026-03-recommendation-v1`)이 bootstrap·selection strategy·trace의 PolicyTraceInfo·metrics event·repository key·registry key에 동일하게 사용됨.

### 3. Response assembler 인터페이스 주입 경로

- **Production:** `DefaultRecommendationResponseAssembler`는 생성자에서 **`RecommendationExplanationAssembler`(인터페이스)** 만 받음.  
  Spring이 `DefaultRecommendationExplanationAssembler`(@Component)를 해당 인터페이스 타입으로 주입.  
  adapter 쪽 소스에는 `DefaultRecommendationExplanationAssembler` import 없음.  
  **설정:** 별도 config에서 구현체를 지정하지 않아도, 유일한 구현체 빈이 인터페이스 타입으로 주입됨.

---

## D. 추가/수정 테스트

| 테스트 | 보호/검증 내용 |
|--------|-----------------|
| `PromptEngineModuleBoundaryTest` 규칙 7 | adapter assembler(`DefaultRecommendationResponseAssembler`)가 explanation **구현체**(`DefaultRecommendationExplanationAssembler`)를 참조하지 않음 → assembler 구현체 직접 결합 회귀 방지. |
| `PromptEngineModuleBoundaryTest` 규칙 8 | observability/audit 패키지가 controller·web DTO에 의존하지 않음 → 감사/메트릭이 API 계층에 묶이지 않도록 유지. |
| `PromptEngineModuleBoundaryTest` 규칙 9 | core 패키지(value, model, enums, contract)가 adapter·observability·audit·infrastructure.policy에 의존하지 않음. |
| **`PromptEngineModuleBoundaryTest` 규칙 10 (2차 추가)** | **ResolutionConfig가 DefaultPolicySourceRegistry를 참조하지 않음** → policy registry가 bootstrap 전용으로만 제공되며, config에서 하드코딩 registry 재도입 방지. |
| `PolicyRuntimeBootstrapIntegrationTest` | Bootstrap(pipeline=null, loader=null) fallback 경로에서 repository에 default version 존재, registry가 해당 version으로 bundle 반환, versionId와 repository key 일치. |
| `PromptRecommendationFacadeTest` | recommend 시 `auditPublisher.publish(result)` 호출 검증 → production 경로에서 audit 발행 보장. |
| `RecommendationAuditPublisherTest` | publish(result) 시 record 1건 생성·sink 전달, trace 있을 때 traceSummary 및 policyVersionId 포함 검증. |
| `DefaultRecommendationResponseAssemblerTest` | 설명 assembler를 인터페이스 타입으로 주입해도 동작함을 검증. |

---

## E. 남은 미완료 (이번 2차 범위 내)

- **Policy default 문서 없을 때:** classpath에 `policy/default-recommendation.json`이 없거나 pipeline/loader가 null이면, 기존처럼 빌더로 만든 빈 validated bundle로 bind 후 repo/registry 구성. 이는 의도된 fallback이며, “문서 기반 1버전 이상”은 문서가 있을 때만 충족.
- **ArchUnit:** policy 규칙에서 테스트 클래스 제외는 `haveSimpleNameNotEndingWith("Test")` 기준. 다른 테스트 네이밍 컨벤션 사용 시 규칙 조정 필요.
- **SharedPromptsApplicationTests:** 전체 `@SpringBootTest`는 DB/Redis 등 외부 의존성으로 인해 이번 변경과 무관하게 실패할 수 있음.

---

## 최종 완료 조건 점검 (2차 보정 후)

| 조건 | 상태 |
|------|------|
| 1. response assembler는 explanation **인터페이스만** 의존한다 | ✅ 유지. production 소스에 구현체 import 0. |
| 2. 추천 요청 production 경로에서 audit record가 실제 생성되고 전달된다 | ✅ Facade → auditPublisher.publish(result) → DefaultRecommendationAuditPublisher에서 record 생성·sink 전달. |
| 3. runtime registry는 hardcoded source가 아니라 pipeline/repository 기반이다 | ✅ Registry는 PolicyBootstrapConfig → PolicyRuntimeBootstrap에서만 제공. 문서 존재 시 pipeline 결과로, 없으면 binder 결과(빌더 fallback)로 구성. |
| 4. repository는 실제 기본 policy version을 가진다 | ✅ Bootstrap이 pipeline 성공 시 또는 fallback 시 한 버전을 repository에 저장. |
| 5. selection strategy / trace / metrics / repository / registry version이 일치한다 | ✅ 동일 PolicyVersion(default) 사용. |
| 6. 구조 테스트가 위 경계를 회귀 방지한다 | ✅ 규칙 7·8·9·10 및 기존 규칙으로 assembler 결합·audit 분리·policy bootstrap 전용·core 독립 보호. |
| 7. public API 응답 shape와 stable key 계약은 유지된다 | ✅ 기존 DTO·필드·key 계약 변경 없음. |

---

*2차 보정 기준: 실제 production 호출 경로·bean wiring·데이터 흐름. 이름·타입 존재만으로 완료로 보지 않음.*

# Branch Split Analysis

## 1. Overall judgment

**The 4-branch plan is still valid against the current diff.**

- The diff cleanly separates into: (1) enum/action resolution and serialization/metadata compatibility, (2) policy-runtime resolution wiring including trace and semantic data sources (category seed/profile, heuristic objective inference, and intent definition/default providers), (3) audit/observability contracts and the RecommendPromptResult port, (4) recommendation/prompt generation API pieces (controller/facade/DTOs plus downstream prompt spec + verification).
- **RecommendPromptResult** still depends on **RecommendationTrace** (domain/semantic/trace). Trace is introduced in branch 2, so branch 3 can safely add the port that references it. No reordering needed.
- **ResolutionConfig** stays policy-runtime: the diff rewrites ResolutionConfig to wire policy sources/versions and also beans for `ObjectiveHeuristicInferencePolicy` + `ObjectiveMappingRegistry` and for `CategorySemanticProfileSeedSource`-driven profile assembly. Keeping them together in branch 2 is correct.
- **PromptDomainConfig** has a single-line change (four new @Import classes). Partial staging is required: branch 2 adds PolicySchemaConfig and PolicyBootstrapConfig; branch 3 adds ObservabilityConfig and AuditConfig. The split is feasible by editing the file when staging each branch.

**Minimal adjustment required: Branch 2 must explicitly include the new semantic intent definition/default provider structure and heuristic inference policy files.**

---

## 2. File classification

### Branch 1 — refactor/enum-action-resolution

- **Files:**
  - **Modified:** `ActionTypeCatalog.java`, `DefaultActionTypeCatalog.java`, `ActionTypeCompatibilityResolver.java`, `ActionTypeDeserializer.java`, `EnumCompatParser.java`, `EnumResolver.java`, `DefaultCanonicalActionRegistry.java`, `ActionTypeRegistryConfig.java`, `JacksonConfig.java`
  - **Deleted:** `ActionTypeMetadataLoader.java`, `ActionTypeRegistryHolder.java`, `docs/common-package-structural-analysis.md`, `docs/common-package-structural-audit-2nd.md`
  - **New:** `ActionTypeMetadataProvider.java`, `OrderedActionTypeResolutionSource.java`, `DefaultOrderedActionTypeResolutionSource.java`, `RoleTypeResolver.java`, `infrastructure/metadata/ClasspathActionTypeMetadataProvider.java`
  - **Tests (modified):** `ActionTypeDeserializerCoverageTest.java`, `ActionTypeOutputBehaviorCoverageTest.java`, `ActionTypeMetadataGeneratorTest.java`, `ActionTaxonomyGuardrailTest.java`, `CanonicalActionMappingTest.java`, `SharedPromptsApplicationTests.java`, `PaymentControllerIntegrationTest.java`
  - **Tests (new):** `common/enums/action/metadata/*` (test providers/helpers), `common/enums/action/resolver/*`, `infrastructure/config/TestActionTypeMetadataConfig.java`, `infrastructure/metadata/ActionTypeMetadataProviderTest.java`

- **Reasoning:** All of these implement or test enum/action resolution, catalog, compatibility, deserialization, and removal of static metadata/registry. No policy or recommendation API behavior. SharedPromptsApplicationTests and PaymentControllerIntegrationTest only add `@Import(TestActionTypeMetadataConfig.class)` and a test property so context loads after removing the old holder/loader; they belong with branch 1.

- **Must move together:** ActionTypeDeserializer, EnumCompatParser, EnumResolver, ActionTypeRegistryConfig, JacksonConfig (serialization wiring). Deletions of ActionTypeMetadataLoader and ActionTypeRegistryHolder with additions of ActionTypeMetadataProvider and ClasspathActionTypeMetadataProvider. TestActionTypeMetadataConfig with the two app tests that import it.

- **Directly coupled tests:** All enum/action tests listed above; TestActionTypeMetadataConfig and ActionTypeMetadataProviderTest.

- **Partial staging required:** No.

- **If yes, exact files and hunk ownership:** N/A.

---

### Branch 2 — refactor/policy-runtime-resolution

- **Files:**
  - **Modified:** `ResolutionConfig.java`, `ObjectiveMappingRegistry.java`, `ObjectiveMappingRegistryPort.java`, `DefaultCategorySemanticProfileRegistry.java`, `DefaultCategorySemanticProfileSeedSource.java`, `IntentDictionary.java`
  - **New (domain, heuristic objective inference):** `domain/resolutions/ObjectiveHeuristicInferencePolicy.java`, `domain/resolutions/DefaultObjectiveHeuristicInferencePolicy.java`
  - **New (domain, semantic intent definitions/defaults data source):** `domain/semantic/IntentDefinitionDataSource.java`, `IntentDefinitionEntriesProvider.java`, `IntentResolutionDefaults.java`, `IntentResolutionDefaultsEntriesProvider.java`, and all `Intent*IntentDefinitionsProvider.java` + `Intent*IntentResolutionDefaultsProvider.java` implementations.
  - **New (domain):** `domain/semantic/CategorySemanticProfileSeed.java`, `CategorySemanticProfileSeedSource.java`, `impl/DefaultCategorySemanticProfileSeedSource.java`, entire `domain/semantic/policy/*` (registry, schema, recommendation, compatibility, objective, role, version, validation, diff, migration), entire `domain/semantic/trace/*`
  - **New (application):** entire `application/semantic/policy/*` (loader, parser, validator, binder, selection strategy, repository, etc.), entire `application/semantic/experiment/*` (ExperimentContext, ExperimentPolicySelector)
  - **New (infrastructure):** `PolicyBootstrapConfig.java`, `PolicyRuntimeBootstrap.java`, `PolicySchemaConfig.java`, entire `infrastructure/policy/*`. (Do not add `infrastructure/metadata/` in branch 2 — ClasspathActionTypeMetadataProvider is branch 1 only.)
  - **Resources:** `src/main/resources/policy/*`
  - **Tests (new):** `PolicyRuntimeBootstrapIntegrationTest.java`, `PolicyDocumentLoadValidateBindTest.java`, `PolicySelectionStrategyTest.java`, `VersionedPolicyRepositoryTest.java`, `PolicyDocumentClasspathLoadIntegrationTest.java`, `ClasspathJsonRecommendationPreferenceSourceTest.java`, `PolicyDiffServiceTest.java`, `PolicyCompatibilityAnalyzerTest.java`, `StructuralVsSemanticValidationTest.java`, `PolicyVersionReproducibilityTest.java`, `TraceAuditPolicyVersionLookupTest.java`, `PolicyChangeTypeTest.java`, `PolicyValidationResultTest.java`, `SemanticStructurePhase4Test.java`, `SemanticStructurePhase5Test.java`, `SemanticStructurePhase6Test.java`, `CategorySemanticProfileSeedSourceAssemblerTest.java`, `SemanticStructurePhase3Test.java`, `ProfileCanonicalFirstTest.java`, all `domain/semantic/trace/*` tests, `domain/semantic/policy/*` tests, `infrastructure/policy/*` tests
  - **Build:** `build.gradle` (archunit dependency used by PromptEngineModuleBoundaryTest)
  - **Tests (new, architecture):** `architecture/PromptEngineModuleBoundaryTest.java`
  - **PromptDomainConfig:** Partial staging — see Shared-adjustment.

- **Reasoning:** Policy document pipeline, bootstrap, repository, registry, and ResolutionConfig wiring; category semantic profile seed and profile registry; heuristic objective inference (`ObjectiveMappingRegistry` + `ObjectiveHeuristicInferencePolicy`); semantic intent definition/default data sources (`IntentDefinitionDataSource` with intent-group providers, powering `IntentDictionary`); and trace types used by recommendation ordering and later by audit. ResolutionConfig and PolicyBootstrapConfig stay together; trace stays with policy-runtime per constraint.

- **Must move together:** ResolutionConfig, PolicyBootstrapConfig, PolicyRuntimeBootstrap, PolicySchemaConfig; ResolutionConfig with PolicySourceRegistry and policy beans (including objective heuristic inference beans); DefaultCategorySemanticProfileRegistry with CategorySemanticProfileSeedSource and CompatibilityPolicySource; and `IntentDictionary` together with `IntentDefinitionDataSource` + all intent-group provider implementations (because `IntentDictionary` fail-fast validates completeness at class initialization). Also move domain/semantic/trace with policy/recommendation ordering.

- **Directly coupled tests:** All policy, resolution, trace, and profile seed tests listed; PromptEngineModuleBoundaryTest and archunit in build.gradle.

- **Partial staging required:** Yes (PromptDomainConfig only).

- **If yes, exact files and hunk ownership:**
  - **PromptDomainConfig.java:** Single hunk adds four classes to @Import. For branch 2, add only `PolicySchemaConfig.class`, `PolicyBootstrapConfig.class`. So the committed line must be: `@Import({ActionTypeRegistryConfig.class, ResolutionConfig.class, PolicySchemaConfig.class, PolicyBootstrapConfig.class})`. Do not add ObservabilityConfig or AuditConfig in this branch.

---

### Branch 3 — refactor/audit-observability

- **Files:**
  - **Modified:** `application/port/in/query/RecommendPromptResult.java`
  - **New (application):** entire `application/semantic/audit/*`, entire `application/semantic/observability/*`
  - **New (domain):** entire `domain/semantic/observability/*`
  - **New (infrastructure):** `AuditConfig.java`, `ObservabilityConfig.java`
  - **Tests (new):** `RecommendationAuditPublisherTest.java`, `RecommendationObservabilityTest.java`, `PolicyTraceInfoAndAuditSnapshotTest.java`, and any other audit/observability test packages
  - **PromptDomainConfig:** Partial staging — add only `ObservabilityConfig.class`, `AuditConfig.class` to @Import. Committed line: `@Import({ActionTypeRegistryConfig.class, ResolutionConfig.class, ObservabilityConfig.class, AuditConfig.class})`. Do not add PolicySchemaConfig or PolicyBootstrapConfig in this branch.

- **Reasoning:** RecommendPromptResult is the query port consumed by audit and observability publishers; it carries Optional&lt;RecommendationTrace&gt; (trace comes from branch 2). Contract-focused; no controller or facade.

- **Must move together:** RecommendPromptResult (port) with audit and observability interfaces/implementations that depend on it; AuditConfig and ObservabilityConfig with their respective publisher/sink beans.

- **Directly coupled tests:** RecommendationAuditPublisherTest, RecommendationObservabilityTest, PolicyTraceInfoAndAuditSnapshotTest.

- **Partial staging required:** Yes (PromptDomainConfig only).

- **If yes, exact files and hunk ownership:** PromptDomainConfig: for branch 3, add only ObservabilityConfig and AuditConfig to @Import (see above). Manually edit before staging so that PolicySchemaConfig and PolicyBootstrapConfig are not included in this branch’s commit.

---

### Branch 4 — refactor/recommendation-api

- **Files:**
  - **Modified:** `RecommendationController.java`, `RecommendPromptRequest.java`, `RecommendationRequestMapper.java`, `UnifiedPromptResponseMapper.java`, `AdvancedGeneratePromptRequest.java`, `ConfirmedGeneratePromptRequest.java`, `UnifiedGeneratePromptResultBuilder.java`, `RecommendPromptCommand.java`, `UnifiedGeneratePromptResult.java`, `SemanticRecommendationService.java`, `CoreSemanticResolver.java`, `RecommendationSemanticResolver.java`, `RecommendationResult.java`, `BadgeResolver.java`
  - **New (adapter):** `adapter/in/web/assembler/*` (DefaultRecommendationExplanationAssembler, DefaultRecommendationResponseAssembler, RecommendationResponseAssembler), `adapter/in/web/dto/response/PromptRecommendationResponse.java`, `RecommendationExplanation.java`, `RecommendedActionResponse.java`, `RecommendedRoleResponse.java`, `adapter/in/web/facade/PromptRecommendationFacade.java`, `adapter/in/web/mapper/RecommendPromptResponseMapper.java` (if present as new)
  - **New (application):** `application/port/in/generate/QualityBadgeItem.java`, `application/semantic/explanation/*`, `application/semantic/recommendation/RecommendPromptAxesService.java` (if new), `application/port/in/recommend/RecommendPromptAxesUseCase.java` (if new), `application/semantic/resolution/RecommendationResolutionResult.java` (if new)
  - **Tests (modified):** `SemanticResolutionServiceTest.java`
  - **Tests (new):** `adapter/in/web/assembler/*` tests, `adapter/in/web/dto/PromptRecommendationResponseContractTest.java`, `adapter/in/web/facade/PromptRecommendationFacadeTest.java`, `application/semantic/recommendation/*` tests

- **Reasoning:** Controller, facade, DTOs, assemblers, recommendation use case, semantic resolution service and resolvers that produce RecommendPromptResult, explanation assembly, UnifiedGeneratePromptResult/QualityBadgeItem, BadgeResolver, RecommendationResult. Integrates audit/observability only after their contracts exist (branch 3).

- **Must move together:** RecommendationController with facade and DTOs; RecommendPromptCommand and RecommendPromptResult (usage) with SemanticRecommendationService and resolvers; assemblers with response DTOs; production code with directly coupled tests.

- **Directly coupled tests:** DefaultRecommendationResponseAssemblerTest, RecommendationExplanationAssemblerTest, PromptRecommendationFacadeTest, PromptRecommendationResponseContractTest, SemanticResolutionServiceTest, recommendation package tests.

- **Partial staging required:** No.

- **If yes, exact files and hunk ownership:** N/A.

---

### Shared-adjustment

- **File:** `sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/infrastructure/config/PromptDomainConfig.java`

- **Why shared:** One hunk adds four config classes to @Import. Branch 2 must add PolicySchemaConfig and PolicyBootstrapConfig; branch 3 must add ObservabilityConfig and AuditConfig.

- **Which hunks belong to which branch:** The only change is one line: from `@Import({ActionTypeRegistryConfig.class, ResolutionConfig.class})` to `@Import({ActionTypeRegistryConfig.class, ResolutionConfig.class, ObservabilityConfig.class, PolicySchemaConfig.class, PolicyBootstrapConfig.class, AuditConfig.class})`. Branch 2 owns the addition of PolicySchemaConfig and PolicyBootstrapConfig. Branch 3 owns the addition of ObservabilityConfig and AuditConfig.

- **Recommended handling:** When staging branch 2, edit PromptDomainConfig so the @Import line contains only ActionTypeRegistryConfig, ResolutionConfig, PolicySchemaConfig, PolicyBootstrapConfig; then stage and commit. When staging branch 3, edit PromptDomainConfig so the @Import line contains ActionTypeRegistryConfig, ResolutionConfig, ObservabilityConfig, AuditConfig (no policy configs); stage and commit. Do not use the full six-import version in either branch; each branch gets a different four-import version. When merging branches later, the merge will combine the imports to all six.

---

### Follow-up-fix

- **File:** None. All changed files are assigned to one of the four branches or to shared-adjustment.

- **Why not included in the 4-branch split:** N/A.

- **Recommended later branch/commit:** N/A.

---

### Keep-out-of-scope

- **Files:** `sharedPrompts/docs/recommended-branch-split-plan.md`, `sharedPrompts/docs/refactoring-phase-1-6-audit-report.md`, `sharedPrompts/docs/refactoring-phase-7-13-execution-report.md`, `sharedPrompts/docs/structure-reaudit-prompt-domain-1-5-plus5.md`

- **Why excluded:** Planning and audit documentation only; not production or test code. Do not commit them as part of the refactor branches unless you want them on a docs-only branch. Exclude from the four-branch split so the split stays code-focused.

---

## 3. Hidden coupling warnings

- **PromptDomainConfig:** Single @Import line shared across branch 2 and branch 3. Must be split by manually editing the file when staging each branch (see Shared-adjustment). Do not commit the full six-import version on either branch.

- **ResolutionConfig:** Fully rewritten to depend on PolicySourceRegistry, PolicyVersion, ObjectivePolicySource, CompatibilityPolicySource, CategorySemanticProfileSeedSource, and policy types. Must stay with PolicyBootstrapConfig and policy runtime in branch 2. Do not split ResolutionConfig.

- **Objective heuristic inference wiring:** `ObjectiveMappingRegistry` must stay with `ObjectiveHeuristicInferencePolicy` (and its default keyword inference implementation) because `ObjectiveMappingRegistry` delegates fallback inference to it.

- **Semantic intent definitions/defaults data source:** `IntentDictionary` depends on `IntentDefinitionDataSource` (static aggregation) and therefore depends on *every* intent-group provider implementation it references. If any provider file lands outside branch 2, `IntentDictionary` class initialization fails-fast (completeness check) and tests that touch `IntentDictionary` break.

- **Provider registration static list:** `IntentDefinitionDataSource` uses private static provider lists. This is a hidden coupling point: adding a new `ActionIntent` or changing provider responsibilities requires updating the aggregator registration lists in the same branch.

- **RecommendPromptResult:** Depends on `RecommendationTrace` (domain/semantic/trace). Branch 2 must introduce trace before branch 3 introduces RecommendPromptResult. Merge order 2 → 3 is required.

- **Spring @Import / @Bean:** PromptDomainConfig imports ResolutionConfig, ActionTypeRegistryConfig (branch 1), then in branch 2 adds PolicyBootstrapConfig and PolicySchemaConfig, in branch 3 adds AuditConfig and ObservabilityConfig. Each branch must leave the config compilable (branch 2 and 3 use edited four-import versions).

- **Trace dependencies:** domain/semantic/trace is used by policy ordering and by RecommendPromptResult. Keeping trace in branch 2 avoids pulling recommendation API into policy branch and keeps RecommendPromptResult (branch 3) depending only on trace, not on facade/controller.

- **Response DTO / facade / service:** RecommendPromptResult is the port (branch 3). RecommendationResponseAssembler and PromptRecommendationResponse are adapter (branch 4). Do not put the port in branch 4 or the assembler in branch 3.

- **Tests that cannot be separated cleanly:** SemanticResolutionServiceTest (branch 4) uses RecommendPromptResult (branch 3). So branch 4 tests assume branch 3 is merged. When running branch 4 in isolation from a branch that has branch 3 merged (e.g. dev after 3 is merged), tests will compile. ProfileCanonicalFirstTest (branch 2) tests DefaultCategorySemanticProfileRegistry; keep with branch 2. TestActionTypeMetadataConfig is used by SharedPromptsApplicationTests and PaymentControllerIntegrationTest (branch 1) and possibly by PolicyRuntimeBootstrapIntegrationTest (branch 2); the config belongs to branch 1; branch 2 tests that need it rely on branch 1 being present when testing the full stack.

- **Tests precondition (updated):** Any test that triggers `IntentDictionary` class loading now implicitly requires the full set of `Intent*DefinitionsProvider` and `Intent*ResolutionDefaultsProvider` files (owned by branch 2), because `IntentDictionary` performs a fail-fast completeness check across all `ActionIntent` values.

- **infrastructure/metadata:** ClasspathActionTypeMetadataProvider is branch 1 (enum/action metadata). If there are other files under infrastructure/metadata, they are policy-related and branch 2. Do not mix them.

---

## 4. Safe branch creation order

1. **Branch 1 — refactor/enum-action-resolution**  
   Base; no dependency on other refactor branches. Removes legacy loader/holder and adds provider/ordered resolution and RoleTypeResolver. Merge first.

2. **Branch 2 — refactor/policy-runtime-resolution**  
   Depends on branch 1 (ActionTypeRegistryConfig and enum types). Introduces policy pipeline, bootstrap, ResolutionConfig wiring, trace, category profile seed, heuristic objective inference, and semantic intent definitions/default providers (so `IntentDictionary` can be initialized). Must be before branch 3 so that RecommendationTrace exists.

3. **Branch 3 — refactor/audit-observability**  
   Depends on branch 2 (RecommendPromptResult has Optional&lt;RecommendationTrace&gt;). Adds audit/observability contracts and RecommendPromptResult port. No dependency on branch 4.

4. **Branch 4 — refactor/recommendation-api**  
   Depends on branch 3 (uses RecommendPromptResult and may wire audit/observability). Implements controller, facade, use case, resolvers, explanation, and response assembly. Merge last.

This order keeps each branch compilable when applied on top of dev in sequence and avoids introducing the port before trace exists.

---

## 5. Exact git execution plan

Assume all changes are in one working tree on `dev`. Use a clean clone or stash of unrelated work. Paths are relative to repo root (e.g. `c:\Users\joo\prompts\backend`); `sharedPrompts/` is the module path.

### Branch 1 commands

```bash
git checkout dev
git switch -c refactor/enum-action-resolution

# Stage enum/action resolution and serialization (no PromptDomainConfig)
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/common/enums/action/catalog/ActionTypeCatalog.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/common/enums/action/catalog/DefaultActionTypeCatalog.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/common/enums/action/resolver/ActionTypeCompatibilityResolver.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/common/enums/serializer/ActionTypeDeserializer.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/common/enums/serializer/EnumCompatParser.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/common/enums/serializer/EnumResolver.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/common/enums/action/canonical/DefaultCanonicalActionRegistry.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/infrastructure/config/ActionTypeRegistryConfig.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/global/config/object/JacksonConfig.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/common/enums/action/metadata/ActionTypeMetadataProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/common/enums/action/resolver/OrderedActionTypeResolutionSource.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/common/enums/action/resolver/DefaultOrderedActionTypeResolutionSource.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/common/enums/role/RoleTypeResolver.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/infrastructure/metadata/ClasspathActionTypeMetadataProvider.java

# Deletions
git add sharedPrompts/docs/common-package-structural-analysis.md
git add sharedPrompts/docs/common-package-structural-audit-2nd.md
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/common/enums/action/metadata/ActionTypeMetadataLoader.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/infrastructure/serialization/ActionTypeRegistryHolder.java

# Tests
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/common/enums/ActionTypeDeserializerCoverageTest.java
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/common/enums/ActionTypeOutputBehaviorCoverageTest.java
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/common/enums/action/ActionTypeMetadataGeneratorTest.java
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/common/enums/action/canonical/ActionTaxonomyGuardrailTest.java
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/common/enums/action/canonical/CanonicalActionMappingTest.java
git add sharedPrompts/src/test/java/org/example/sharedprompts/SharedPromptsApplicationTests.java
git add sharedPrompts/src/test/java/org/example/sharedprompts/controller/payment/PaymentControllerIntegrationTest.java
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/infrastructure/config/TestActionTypeMetadataConfig.java
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/infrastructure/metadata/ActionTypeMetadataProviderTest.java
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/common/enums/action/metadata/
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/common/enums/action/resolver/

git status
git diff --staged --stat
# Verify: no PromptDomainConfig, no ResolutionConfig, no policy/audit/observability files
git commit -m "refactor(prompt): replace ActionType loader/holder with provider and ordered resolution

- Remove ActionTypeMetadataLoader and ActionTypeRegistryHolder
- Add ActionTypeMetadataProvider and OrderedActionTypeResolutionSource
- Add RoleTypeResolver; wire catalog/compatibility/deserializer via ActionTypeRegistryConfig
- Update JacksonConfig and enum serialization for new resolution flow
- Remove obsolete docs (common-package-structural-*)
- Add TestActionTypeMetadataConfig for app and payment tests"
```

**Verification:** Build: `./gradlew :sharedPrompts:compileJava :sharedPrompts:compileTestJava` (or equivalent). Run enum/action tests. Confirm no references to PolicyBootstrapConfig, ObservabilityConfig, AuditConfig, or RecommendationController.

---

### Branch 2 commands

```bash
git checkout dev
git switch -c refactor/policy-runtime-resolution

# ResolutionConfig and policy configs (do not add PromptDomainConfig yet)
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/infrastructure/config/ResolutionConfig.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/infrastructure/config/PolicyBootstrapConfig.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/infrastructure/config/PolicyRuntimeBootstrap.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/infrastructure/config/PolicySchemaConfig.java

# Objective mapping + heuristic inference policy
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/resolutions/ObjectiveMappingRegistryPort.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/resolutions/ObjectiveHeuristicInferencePolicy.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/resolutions/DefaultObjectiveHeuristicInferencePolicy.java

# Domain: seed, profile, policy, trace
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/CategorySemanticProfileSeed.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/CategorySemanticProfileSeedSource.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/impl/DefaultCategorySemanticProfileSeedSource.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/impl/DefaultCategorySemanticProfileRegistry.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/resolutions/ObjectiveMappingRegistry.java

# Semantic intent definitions/defaults data source + providers (powering IntentDictionary)
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentDictionary.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentDefinitionDataSource.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentDefinitionEntriesProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentResolutionDefaults.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentResolutionDefaultsEntriesProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentCreationIntentDefinitionsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentCreationIntentResolutionDefaultsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentModificationIntentDefinitionsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentModificationIntentResolutionDefaultsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentAnalysisIntentDefinitionsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentAnalysisIntentResolutionDefaultsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentExplanationIntentDefinitionsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentExplanationIntentResolutionDefaultsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentPlanningIntentDefinitionsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentPlanningIntentResolutionDefaultsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentDecisionIntentDefinitionsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentDecisionIntentResolutionDefaultsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentResearchIntentDefinitionsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentResearchIntentResolutionDefaultsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentExtractionIntentDefinitionsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentExtractionIntentResolutionDefaultsProvider.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/policy/
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/trace/

# Application policy and experiment
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/semantic/policy/
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/semantic/experiment/

# Infrastructure policy and resources
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/infrastructure/policy/
git add sharedPrompts/src/main/resources/policy/

# build.gradle (archunit for boundary test)
git add sharedPrompts/build.gradle

# PromptDomainConfig: edit to only add PolicySchemaConfig and PolicyBootstrapConfig
# Before staging: set @Import to {ActionTypeRegistryConfig.class, ResolutionConfig.class, PolicySchemaConfig.class, PolicyBootstrapConfig.class}
# Then:
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/infrastructure/config/PromptDomainConfig.java

# Policy/resolution/trace/seed tests
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/application/semantic/policy/
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/application/semantic/experiment/
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/domain/semantic/CategorySemanticProfileSeedSourceAssemblerTest.java
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/domain/semantic/SemanticStructurePhase3Test.java
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/domain/semantic/ProfileCanonicalFirstTest.java
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/domain/semantic/policy/
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/domain/semantic/trace/
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/infrastructure/config/PolicyRuntimeBootstrapIntegrationTest.java
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/infrastructure/policy/
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/architecture/

git diff --staged --stat
git commit -m "feat(prompt): policy document pipeline, bootstrap, and resolution wiring

- Add policy load/validate/bind pipeline, VersionedPolicyRepository, PolicySourceRegistry
- Add PolicyBootstrapConfig and PolicyRuntimeBootstrap; PolicySchemaConfig
- Wire ResolutionConfig to policy sources, profile seed, trace
- Add domain/application policy types and infrastructure policy + resources
- Extend PromptDomainConfig with PolicySchemaConfig and PolicyBootstrapConfig only
- Add archunit and PromptEngineModuleBoundaryTest"
```

**Verification:** Build and run policy/resolution/trace tests. PromptDomainConfig must import only ActionTypeRegistryConfig, ResolutionConfig, PolicySchemaConfig, PolicyBootstrapConfig (no ObservabilityConfig or AuditConfig). After commit, restore working tree (e.g. `git stash pop` or re-apply remaining changes) before starting branch 3.

---

### Branch 3 commands

```bash
git checkout dev
git switch -c refactor/audit-observability

# Port and audit/observability
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/port/in/query/RecommendPromptResult.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/semantic/audit/
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/semantic/observability/
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/observability/
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/infrastructure/config/AuditConfig.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/infrastructure/config/ObservabilityConfig.java

# PromptDomainConfig: edit to add only ObservabilityConfig and AuditConfig
# Set @Import to {ActionTypeRegistryConfig.class, ResolutionConfig.class, ObservabilityConfig.class, AuditConfig.class}
# Then:
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/infrastructure/config/PromptDomainConfig.java

# Tests
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/application/semantic/audit/
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/application/semantic/observability/
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/domain/semantic/trace/PolicyTraceInfoAndAuditSnapshotTest.java

git diff --staged --stat
git commit -m "feat(prompt): recommendation audit and observability

- Add RecommendPromptResult port (query result with optional trace)
- Add RecommendationAuditPublisher/Sink and RecommendationObservabilityPublisher/MetricsSink
- Add AuditConfig and ObservabilityConfig
- Extend PromptDomainConfig with ObservabilityConfig and AuditConfig only"
```

**Verification:** Build; run audit and observability tests. RecommendPromptResult references RecommendationTrace (must be present from branch 2 when merging). PromptDomainConfig must not import PolicySchemaConfig or PolicyBootstrapConfig in this commit.

---

### Branch 4 commands

```bash
git checkout dev
git switch -c refactor/recommendation-api

# Controller, facade, DTOs, mappers, assemblers
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/adapter/in/web/controller/RecommendationController.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/adapter/in/web/dto/request/RecommendPromptRequest.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/adapter/in/web/dto/request/AdvancedGeneratePromptRequest.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/adapter/in/web/dto/request/ConfirmedGeneratePromptRequest.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/adapter/in/web/mapper/RecommendationRequestMapper.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/adapter/in/web/mapper/UnifiedPromptResponseMapper.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/adapter/in/web/assembler/
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/adapter/in/web/dto/response/PromptRecommendationResponse.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/adapter/in/web/dto/response/RecommendationExplanation.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/adapter/in/web/dto/response/RecommendedActionResponse.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/adapter/in/web/dto/response/RecommendedRoleResponse.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/adapter/in/web/facade/
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/adapter/in/web/mapper/RecommendPromptResponseMapper.java

# Commands, ports, result types, use case, resolvers, explanation, badge
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/port/in/command/RecommendPromptCommand.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/port/in/generate/UnifiedGeneratePromptResult.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/port/in/generate/QualityBadgeItem.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/port/in/recommend/
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/engine/generation/UnifiedGeneratePromptResultBuilder.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/semantic/recommendation/
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/semantic/resolution/CoreSemanticResolver.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/semantic/resolution/RecommendationSemanticResolver.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/semantic/resolution/SemanticResolutionService.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/semantic/resolution/RecommendationResolutionResult.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/application/semantic/explanation/
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/RecommendationResult.java
git add sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/service/badge/BadgeResolver.java

# Tests
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/adapter/in/web/assembler/
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/adapter/in/web/dto/PromptRecommendationResponseContractTest.java
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/adapter/in/web/facade/
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/application/semantic/resolution/SemanticResolutionServiceTest.java
git add sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/application/semantic/recommendation/

git diff --staged --stat
git commit -m "feat(prompt): recommendation API (controller, facade, use case, resolvers)

- Add RecommendationController, PromptRecommendationFacade, recommendation DTOs and assemblers
- Add RecommendPromptCommand, RecommendPromptAxesUseCase, SemanticRecommendationService
- Wire CoreSemanticResolver and RecommendationSemanticResolver to RecommendPromptResult
- Add explanation assembly, UnifiedGeneratePromptResult/QualityBadgeItem, BadgeResolver
- Add recommendation and resolution tests"
```

**Verification:** Build and run recommendation and resolution tests. Ensure no duplicate or conflicting changes to PromptDomainConfig (branch 4 does not stage PromptDomainConfig). After all four branches are created, merge in order 1 → 2 → 3 → 4; resolve PromptDomainConfig merge to include all six imports.

---

## 6. Commit messages

### Branch 1

**type(scope): title**  
`refactor(prompt): replace ActionType loader/holder with provider and ordered resolution`

**What changed:**  
Removed ActionTypeMetadataLoader and ActionTypeRegistryHolder. Added ActionTypeMetadataProvider, OrderedActionTypeResolutionSource, DefaultOrderedActionTypeResolutionSource, and RoleTypeResolver. Updated ActionTypeCatalog, DefaultActionTypeCatalog, ActionTypeCompatibilityResolver, ActionTypeDeserializer, EnumCompatParser, EnumResolver, DefaultCanonicalActionRegistry, ActionTypeRegistryConfig, and JacksonConfig. Deleted obsolete docs (common-package-structural-*). Added TestActionTypeMetadataConfig and updated SharedPromptsApplicationTests and PaymentControllerIntegrationTest.

**Why:**  
Eliminate static metadata/registry legacy and introduce provider-based and ordered resolution for ActionType and RoleType so serialization and resolution are testable and extensible without policy or recommendation API changes.

**Impact:**  
Enum/action resolution and JSON deserialization use the new provider and ordered resolution. No change to policy runtime or recommendation API. Tests that load full context use TestActionTypeMetadataConfig.

---

### Branch 2

**type(scope): title**  
`feat(prompt): policy document pipeline, bootstrap, and resolution wiring`

**What changed:**  
Added policy load/validate/bind pipeline, VersionedPolicyRepository, PolicySourceRegistry, PolicyBootstrapConfig, PolicyRuntimeBootstrap, PolicySchemaConfig. Rewrote ResolutionConfig to use policy sources, PolicyVersion, CategorySemanticProfileSeedSource, and CompatibilityPolicySource. Added heuristic objective inference (`ObjectiveHeuristicInferencePolicy` + `DefaultObjectiveHeuristicInferencePolicy`) and updated ObjectiveMappingRegistry/ObjectiveMappingRegistryPort to delegate fallback inference to the heuristic policy. Added semantic intent definitions/default data source (`IntentDefinitionDataSource` + intent-group providers) and updated IntentDictionary to index/validate completeness via that data source. Added domain/semantic/policy and domain/semantic/trace, application/semantic/policy, infrastructure/policy and resources. Extended PromptDomainConfig with PolicySchemaConfig and PolicyBootstrapConfig only. Added build.gradle archunit and PromptEngineModuleBoundaryTest. Added policy/resolution/trace/seed tests.

**Why:**  
Introduce policy document lifecycle and runtime bootstrap so resolution and recommendation ordering use versioned policy and trace without pulling in the recommendation HTTP API.

**Impact:**  
ResolutionConfig and profile registry depend on policy registry and version. Trace types are available for audit/observability and recommendation. `IntentDictionary` can be safely initialized because all intent definition/default providers are owned in this branch. Objective resolution has both policy mappings and heuristic fallback inference. PromptDomainConfig imports only the two policy configs in this branch.

---

### Branch 3

**type(scope): title**  
`feat(prompt): recommendation audit and observability`

**What changed:**  
Added RecommendPromptResult (port) with Optional&lt;RecommendationTrace&gt;. Added RecommendationAuditPublisher, RecommendationAuditSink, DefaultRecommendationAuditPublisher, NoOpRecommendationAuditSink, and AuditConfig. Added RecommendationObservabilityPublisher, RecommendationMetricsAssembler, RecommendationMetricsSink, and ObservabilityConfig. Added domain/semantic/observability event types. Extended PromptDomainConfig with ObservabilityConfig and AuditConfig only.

**Why:**  
Define the recommendation result contract and audit/observability publishing so the recommendation API can integrate with publishers after this branch is merged, without putting controller/facade in this branch.

**Impact:**  
Audit and observability beans are registered; RecommendPromptResult is the shared contract. PromptDomainConfig does not import policy configs in this branch. Depends on branch 2 for RecommendationTrace.

---

### Branch 4

**type(scope): title**  
`feat(prompt): recommendation API (controller, facade, use case, resolvers)`

**What changed:**  
Added RecommendationController, PromptRecommendationFacade, recommendation request/response DTOs, assemblers, and RecommendPromptResponseMapper. Implemented RecommendPromptCommand, RecommendPromptAxesUseCase, SemanticRecommendationService, CoreSemanticResolver, RecommendationSemanticResolver, SemanticResolutionService, RecommendationResolutionResult, and explanation assembly. Updated UnifiedGeneratePromptResultBuilder, UnifiedGeneratePromptResult, QualityBadgeItem, BadgeResolver, RecommendationResult. Added recommendation and resolution tests. Also included downstream prompt spec domain boundary cleanup (`PromptSpecFactory`) and guideline verifier contract tightening (`GuidelineVerifier`).

**Why:**  
Expose recommendation via HTTP and orchestrate the semantic recommendation flow, explanation, and response assembly using the RecommendPromptResult and audit/observability contracts from branch 3.

**Impact:**  
Recommendation API is available; resolvers produce RecommendPromptResult; facade and controller use assemblers and DTOs. No change to PromptDomainConfig in this branch; merge with 2 and 3 yields the full six-import PromptDomainConfig.

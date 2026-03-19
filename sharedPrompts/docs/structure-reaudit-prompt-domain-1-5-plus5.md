## 구조 재감사 결과 — 1차~5차(+5차 보강) 반영 검증

### 한 문장 총평
1차~5차는 “조회/조합/데이터 소유/실패 처리”의 중심을 provider·registry·정책 인터페이스 쪽으로 이동시키는 방향으로 실제 코드가 정리된 편이며, 특히 `ObjectiveMappingRegistry`와 `IntentDictionary` 계열은 책임 분리와 fail-fast가 명확합니다. 다만 카테고리 seed 데이터 소유는 여전히 단일 클래스에 고정되어 있고, 정책 키/파싱 실패는 `Optional.empty`로 조용히 drop되는 경로가 남아 있습니다.

### 가장 확실히 개선된 구조 2개
1. `ObjectiveMappingRegistry`: overlay 조회 -> policy source 조회 -> 휴리스틱 추론을 순서대로 수행하고, 휴리스틱 규칙은 `ObjectiveHeuristicInferencePolicy`로 위임
2. `IntentDictionary`: `IntentDefinitionDataSource`에서 entries를 모아 인덱싱하고, completeness를 `IllegalStateException`으로 fail-fast 강제

### 아직 가장 덜 끝난 구조 2개
1. 카테고리 seed 데이터 소유: `DefaultCategorySemanticProfileSeedSource`가 `buildDesign/buildDevelopment/buildWriting/buildResearch/buildBusiness/buildProductivity/buildMarketing/buildCustomerSupport/buildDataAnalysis/buildLegal/buildCreative/buildEducation/buildEtc` seed 본문을 직접 소유(중앙 고정)
2. intent 데이터 확장 seam: `IntentDefinitionDataSource`의 provider 등록이 static 고정(`private static final List`)이라 테스트에서 provider 교체가 구조적으로 어려움

---

## 1. 항목별 판정

### 1차 — PromptSpecFactory 경계 복구
판정: **완료**

코드 근거:
- `PromptSpecFactory` import 목록에 `org.example.sharedprompts.domain.prompt.application` 같은 application 계층 타입 컴파일 의존이 없습니다.
```1:27:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/service/spec/PromptSpecFactory.java
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.domain.model.spec.Constraints;
import org.example.sharedprompts.domain.prompt.domain.model.spec.ContentSandbox;
import org.example.sharedprompts.domain.prompt.domain.model.contract.OutputContract;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSection;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveRegistry;
import org.example.sharedprompts.domain.prompt.domain.policy.strategy.StrategyBundlePolicy;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveResolverPort;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.strategy.PromptStrategyBundle;
import org.example.sharedprompts.domain.prompt.common.enums.experience.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.engine.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.guideline.bundle.GuidelineBundle;
import org.example.sharedprompts.domain.prompt.common.guideline.bundle.GuidelineBundleBuilder;
import org.example.sharedprompts.domain.prompt.common.guideline.context.RuleContext;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.example.sharedprompts.domain.prompt.domain.descriptor.RoleDescriptorPort;
```
- 생성자/필드/핵심 메서드 시그니처에 application 타입이 없습니다. 생성자 계약은 domain 타입들만 받습니다.
```42:65:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/service/spec/PromptSpecFactory.java
public class PromptSpecFactory {

    private final ObjectiveRegistry objectiveRegistry;
    private final StrategyBundlePolicy strategyBundlePolicy;
    @Getter
    private final ObjectiveResolverPort objectiveResolver;
    private final GuidelineBundleBuilder guidelineBundleBuilder;
    private final RoleDescriptorPort roleDescriptorPort;
    private final CanonicalActionRegistry canonicalActionRegistry;

    public PromptSpecFactory(ObjectiveRegistry objectiveRegistry,
                             StrategyBundlePolicy strategyBundlePolicy,
                             ObjectiveResolverPort objectiveResolver,
                             GuidelineBundleBuilder guidelineBundleBuilder,
                             RoleDescriptorPort roleDescriptorPort,
                             CanonicalActionRegistry canonicalActionRegistry) {
        this.objectiveRegistry = Objects.requireNonNull(objectiveRegistry, "objectiveRegistry must not be null");
        this.strategyBundlePolicy = Objects.requireNonNull(strategyBundlePolicy, "strategyBundlePolicy must not be null");
        this.objectiveResolver = Objects.requireNonNull(objectiveResolver, "objectiveResolver must not be null");
        // GuidelineBundleBuilder 선택(구체 구현/조합 책임)은 조합 계층에서 수행해야 한다.
        this.guidelineBundleBuilder = Objects.requireNonNull(guidelineBundleBuilder, "guidelineBundleBuilder must not be null");
        this.roleDescriptorPort = roleDescriptorPort;
        this.canonicalActionRegistry = canonicalActionRegistry;
    }
```
- “application layer” 표현은 Javadoc 설명 문장에만 존재하며, 코드 타입 의존 근거로는 연결되지 않습니다.
```67:74:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/service/spec/PromptSpecFactory.java
 * PromptCategory → ActionIntent → RoleType/ActionType via the semantic resolution pipeline (application layer).
 */
```

남은 문제:
- 없음(본 항목의 목적: 컴파일 의존 제거 및 시그니처/필드에 application 타입 은닉 여부). 본 감사 범위에서 잔존 증거를 확인하지 못했습니다.

---

### 2차 — null -> default new 제거

대상 범위 판정: **완료**  
패키지 전체 관점 판정: **완료**

코드 근거:

A. `DefaultCategorySemanticProfileRegistry`
- seed source는 생성자 인자로 강제되며, 내부에서 `new DefaultCategorySemanticProfileSeedSource()` 같은 fallback 생성이 없습니다.
```43:61:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/impl/DefaultCategorySemanticProfileRegistry.java
public DefaultCategorySemanticProfileRegistry(CanonicalActionRegistry canonicalActionRegistry,
                                                 CategorySemanticProfileSeedSource seedSource) {
    this(canonicalActionRegistry, null, seedSource);
}

public DefaultCategorySemanticProfileRegistry(CanonicalActionRegistry canonicalActionRegistry,
                                               CompatibilityPolicySource compatibilityPolicySource,
                                               CategorySemanticProfileSeedSource seedSource) {
    this.canonicalActionRegistry = Objects.requireNonNull(canonicalActionRegistry, "canonicalActionRegistry");
    this.compatibilityPolicySource = compatibilityPolicySource;
    Objects.requireNonNull(seedSource, "seedSource");
    Map<PromptCategory, CategorySemanticProfile> map = new HashMap<>();
    for (PromptCategory category : PROFILE_CATEGORIES) {
        seedSource.getSeed(category).ifPresent(seed -> map.put(category, buildProfile(seed)));
    }
    this.profiles = Collections.unmodifiableMap(map);
}
```

B. `GuidelineVerifier`
- `GuidelineVerifier`는 `ruleChecker`를 생성자에서 `requireNonNull`로 계약화합니다.
```9:15:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/verification/guideline/GuidelineVerifier.java
public final class GuidelineVerifier {

    private final GuidelineRuleChecker ruleChecker;

    public GuidelineVerifier(GuidelineRuleChecker ruleChecker) {
        this.ruleChecker = Objects.requireNonNull(ruleChecker, "ruleChecker");
    }
```

C. 추가 점검(패키지 전체 관점)
- `domain.prompt.domain` 범위에서 `== null ? new` / `? new Default` / `requireNonNullElse` 류 패턴은 검색 결과로 확인되지 않았습니다(감사 중 `rg` 기반 패턴 검색 수행).

남은 문제:
- 본 항목이 지시한 “null -> default new” 패턴 자체는 잔존 근거를 확인하지 못했습니다.

---

### 3차 — ObjectiveMappingRegistry 책임 분리
판정: **완료**

코드 근거:
- 생성자 의존이 `ObjectivePolicySource` + `ObjectiveHeuristicInferencePolicy`로 축소되어, CanonicalActionRegistry 등 불필요 의존이 없습니다.
```20:32:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/resolutions/ObjectiveMappingRegistry.java
public class ObjectiveMappingRegistry implements ObjectiveMappingRegistryPort {

    private final ObjectivePolicySource policySource;
    private final ObjectiveHeuristicInferencePolicy heuristicInferencePolicy;
    /** Optional overlay (e.g. tests); checked before policy source. */
    private final Map<String, PromptObjective> overlayByStableKey = new ConcurrentHashMap<>();

    public ObjectiveMappingRegistry(
            ObjectivePolicySource policySource,
            ObjectiveHeuristicInferencePolicy heuristicInferencePolicy) {
        this.policySource = Objects.requireNonNull(policySource, "policySource");
        this.heuristicInferencePolicy = Objects.requireNonNull(heuristicInferencePolicy, "heuristicInferencePolicy");
    }
```
- 내부에는 `inferByActionName`로 위임되는 휴리스틱 추론이 있으며, ObjectiveMappingRegistry 자체에 “keyword 판정 문자열 로직”은 없습니다.
```52:61:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/resolutions/ObjectiveMappingRegistry.java
@Override
public Optional<PromptObjective> findByActionType(ActionTypeInterface actionType) {
    if (actionType == null) return Optional.empty();
    String key = actionType.key();
    Optional<PromptObjective> fromOverlay = Optional.ofNullable(overlayByStableKey.get(key));
    if (fromOverlay.isPresent()) return fromOverlay;
    Optional<PromptObjective> fromSource = policySource.findByStableKey(key);
    if (fromSource.isPresent()) return fromSource;
    return heuristicInferencePolicy.inferByActionName(actionType);
}
```
- 휴리스틱 규칙은 `DefaultObjectiveHeuristicInferencePolicy`에 있습니다(키워드 매핑 + exact/partial match).
```17:87:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/resolutions/DefaultObjectiveHeuristicInferencePolicy.java
private static final Map<PromptObjective, Set<String>> KEYWORD_MAP;

static {
    Map<PromptObjective, Set<String>> m = new LinkedHashMap<>();
    m.put(PromptObjective.EXTRACTION, Set.of(
            "EXTRACT", "EXTRACTION", "PARSE", "JSON", "SCHEMA",
            "STRUCTURE", "NORMALIZE", "TAG", "LABEL", "CLASSIFY",
            "KEY_VALUE", "FIELDS", "MAPPING", "REGEX", "PATTERN_EXTRACT"
    ));
    m.put(PromptObjective.FACTUAL, Set.of(
            "SUMMARIZE", "SUMMARY", "TLDR", "ABSTRACT",
            "TRANSLATE", "TRANSLATION",
            "REWRITE", "PARAPHRASE", "POLISH", "PROOFREAD",
            "GRAMMAR", "SPELL", "CLEANUP",
            "FORMAT", "CONVERT", "TRANSFORM",
            "MINUTES", "MEETING_NOTES",
            "DATA_SUMMARY", "REPORT", "DOCUMENTATION"
    ));
    m.put(PromptObjective.ANALYTICAL, Set.of(
            "COMPARE", "COMPARISON", "EVALUATE", "EVALUATION",
            "REVIEW", "CRITIQUE", "PROS_CONS",
            "RISK", "TRADEOFF", "BENCHMARK",
            "ROOT_CAUSE", "DIAGNOSE", "ANALYZE", "ANALYSIS"
    ));
    m.put(PromptObjective.PLANNING, Set.of(
            "PLAN", "PLANNING", "ROADMAP", "STRATEGY",
            "OUTLINE", "STRUCTURE_PLAN", "CHECKLIST",
            "SPEC", "REQUIREMENTS", "DESIGN", "ARCHITECTURE",
            "MIGRATION", "REFACTOR_PLAN",
            "TASK_BREAKDOWN", "STEPS", "WORKFLOW"
    ));
    m.put(PromptObjective.CREATIVE_WITH_CONSTRAINTS, Set.of(
            "DRAFT", "WRITE", "GENERATE", "CREATE", "COMPOSE",
            "BRAINSTORM", "IDEATE",
            "STORY", "POEM", "SCRIPT",
            "MARKETING_COPY", "COPY", "SLOGAN",
            "TITLE", "HEADLINE",
            "CHARACTER", "SCENE"
    ));
    m.put(PromptObjective.REASONING, Set.of(
            "EXPLAIN", "TEACH", "TUTOR", "WHY", "HOW",
            "DEBUG", "TROUBLESHOOT", "FIX",
            "SOLVE", "PROBLEM_SOLVING", "DERIVE",
            "CODE_REVIEW", "REFACTOR", "OPTIMIZE",
            "ALGORITHM", "IMPLEMENT", "INTEGRATE"
    ));
    KEYWORD_MAP = Map.copyOf(m);
}

@Override
public Optional<PromptObjective> inferByActionName(ActionTypeInterface actionType) {
    if (actionType == null) return Optional.empty();
    String name = String.valueOf(actionType).trim().toUpperCase();
    if (name.isEmpty()) return Optional.empty();

    // 1) exact match first (e.g. CODE_REVIEW)
    for (Map.Entry<PromptObjective, Set<String>> entry : KEYWORD_MAP.entrySet()) {
        if (entry.getValue().contains(name)) {
            return Optional.of(entry.getKey());
        }
    }
    // 2) partial match fallback
    for (Map.Entry<PromptObjective, Set<String>> entry : KEYWORD_MAP.entrySet()) {
        for (String keyword : entry.getValue()) {
            if (name.contains(keyword)) {
                return Optional.of(entry.getKey());
            }
        }
    }
    return Optional.empty();
}
```

남은 문제:
- `ObjectiveMappingRegistry`는 overlay 및 정책 소스에 대한 분기/예외를 포함하지만, “휴리스틱 추론 로직 소유”는 별도 정책 구현으로 분리되어 있으므로 반쪽 수정 소지는 낮습니다.

---

### 4차 — Category seed source 중앙 switch 제거

조회 경로 판정: **완료**  
seed 데이터 소유 판정: **미완료**

코드 근거:

- `getSeed(PromptCategory)`에서 `SEED_SUPPLIERS` 맵 조회로 seed 선택을 수행하며, central `switch(category)` 분기 구조가 없습니다.
```66:76:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/impl/DefaultCategorySemanticProfileSeedSource.java
@Override
public Optional<CategorySemanticProfileSeed> getSeed(PromptCategory category) {
    if (category == null) {
        return Optional.empty();
    }
    Supplier<CategorySemanticProfileSeed> supplier = SEED_SUPPLIERS.get(category);
    if (supplier == null) {
        return Optional.empty();
    }
    return Optional.ofNullable(supplier.get());
}
```
- supplier 등록은 `SEED_SUPPLIERS` 맵으로 고정되어 있습니다.
```50:64:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/impl/DefaultCategorySemanticProfileSeedSource.java
private static final Map<PromptCategory, Supplier<CategorySemanticProfileSeed>> SEED_SUPPLIERS = Map.ofEntries(
        Map.entry(PromptCategory.DESIGN, DefaultCategorySemanticProfileSeedSource::buildDesign),
        Map.entry(PromptCategory.DEVELOPMENT, DefaultCategorySemanticProfileSeedSource::buildDevelopment),
        Map.entry(PromptCategory.WRITING, DefaultCategorySemanticProfileSeedSource::buildWriting),
        Map.entry(PromptCategory.RESEARCH, DefaultCategorySemanticProfileSeedSource::buildResearch),
        Map.entry(PromptCategory.BUSINESS, DefaultCategorySemanticProfileSeedSource::buildBusiness),
        Map.entry(PromptCategory.PRODUCTIVITY, DefaultCategorySemanticProfileSeedSource::buildProductivity),
        Map.entry(PromptCategory.MARKETING, DefaultCategorySemanticProfileSeedSource::buildMarketing),
        Map.entry(PromptCategory.CUSTOMER_SUPPORT, DefaultCategorySemanticProfileSeedSource::buildCustomerSupport),
        Map.entry(PromptCategory.DATA_ANALYSIS, DefaultCategorySemanticProfileSeedSource::buildDataAnalysis),
        Map.entry(PromptCategory.LEGAL, DefaultCategorySemanticProfileSeedSource::buildLegal),
        Map.entry(PromptCategory.CREATIVE, DefaultCategorySemanticProfileSeedSource::buildCreative),
        Map.entry(PromptCategory.EDUCATION, DefaultCategorySemanticProfileSeedSource::buildEducation),
        Map.entry(PromptCategory.ETC, DefaultCategorySemanticProfileSeedSource::buildEtc)
);
```

seed 데이터 소유가 미완료인 이유(중앙화 증거):
- seed 본문 소유는 여전히 단일 클래스에 고정되어 있고, class-level 문서가 “Owns category/intent action and role seed data”로 책임 소유를 명시합니다.
```45:49:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/impl/DefaultCategorySemanticProfileSeedSource.java
/**
 * Default in-memory profile seed source. Owns category/intent action and role seed data;
 * registry assembles profiles from this, not from inline register* methods.
 */
public class DefaultCategorySemanticProfileSeedSource implements CategorySemanticProfileSeedSource {
```

남은 문제:
- `PromptCategory` 추가 시 최소 `DefaultCategorySemanticProfileSeedSource.SEED_SUPPLIERS` + `DefaultCategorySemanticProfileRegistry.PROFILE_CATEGORIES`의 업데이트가 동시에 필요합니다.
- supplier 누락/오타는 `Optional.empty`로 drop됩니다(관측 불가능 실패 가능).

---

### 5차 — IntentDictionary 구조 개선
판정: **완료**

코드 근거:
- `IntentDictionary`는 `IntentDefinitionDataSource.definitionsEntries()` / `resolutionDefaultEntries()` 결과로만 정의/기본값 맵을 구성합니다.
```27:37:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentDictionary.java
static {
    Map<ActionIntent, IntentDefinition> defs = new HashMap<>();
    for (IntentDefinitionDataSource.IntentDefinitionEntry e : IntentDefinitionDataSource.definitionsEntries()) {
        defs.put(e.intent(), e.definition());
    }

    Map<ActionIntent, IntentResolutionDefaults> res = new HashMap<>();
    for (IntentDefinitionDataSource.IntentResolutionDefaultEntry e : IntentDefinitionDataSource.resolutionDefaultEntries()) {
        res.put(e.intent(), e.defaults());
    }
```
- fail-fast completeness 검증이 유지됩니다.
```38:49:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentDictionary.java
EnumSet<ActionIntent> allIntents = EnumSet.allOf(ActionIntent.class);
if (!defs.keySet().containsAll(allIntents) || !res.keySet().containsAll(allIntents)) {
    EnumSet<ActionIntent> missingDefinitions = EnumSet.copyOf(allIntents);
    missingDefinitions.removeAll(defs.keySet());
    EnumSet<ActionIntent> missingDefaults = EnumSet.copyOf(allIntents);
    missingDefaults.removeAll(res.keySet());
    throw new IllegalStateException(
            "IntentDictionary is incomplete. missingDefinitions=" + missingDefinitions
                    + ", missingDefaults=" + missingDefaults
    );
}
```
- `IntentDictionary`는 “데이터 소유자”가 아니라 인덱싱/검증/조회 역할을 수행합니다.
```69:79:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentDictionary.java
public static Optional<IntentDefinition> get(ActionIntent intent) {
    return Optional.ofNullable(DEFINITIONS.get(intent));
}

public static IntentDefinition getOrThrow(ActionIntent intent) {
    IntentDefinition d = DEFINITIONS.get(intent);
    if (d == null) {
        throw new IllegalArgumentException("No IntentDefinition for: " + intent);
    }
    return d;
}
```

남은 문제:
- “정책/데이터” 본문이 provider로 분리된 것 자체는 성공했지만, 데이터 provider 등록이 static 고정이라 5차 보강 항목에서 추가 리스크가 남아 있습니다.

---

### 5차 보강 — IntentDefinitionDataSource 데이터 소유 분산
판정: **완료**(목적 반영) + **단, 테스트 seam/등록점 중앙 고정 리스크 남음**

코드 근거:
- 데이터는 provider 들이 `entries()`로 반환하며, `IntentDefinitionDataSource`는 집계/인덱싱만 수행합니다.
```20:40:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentDefinitionDataSource.java
private static final List<IntentDefinitionEntriesProvider> DEFINITIONS_PROVIDERS = List.of(
        new IntentCreationIntentDefinitionsProvider(),
        new IntentModificationIntentDefinitionsProvider(),
        new IntentAnalysisIntentDefinitionsProvider(),
        new IntentExplanationIntentDefinitionsProvider(),
        new IntentPlanningIntentDefinitionsProvider(),
        new IntentDecisionIntentDefinitionsProvider(),
        new IntentResearchIntentDefinitionsProvider(),
        new IntentExtractionIntentDefinitionsProvider()
);

private static final List<IntentResolutionDefaultsEntriesProvider> RESOLUTION_DEFAULTS_PROVIDERS = List.of(
        new IntentCreationIntentResolutionDefaultsProvider(),
        new IntentModificationIntentResolutionDefaultsProvider(),
        new IntentAnalysisIntentResolutionDefaultsProvider(),
        new IntentExplanationIntentResolutionDefaultsProvider(),
        new IntentPlanningIntentResolutionDefaultsProvider(),
        new IntentDecisionIntentResolutionDefaultsProvider(),
        new IntentResearchIntentResolutionDefaultsProvider(),
        new IntentExtractionIntentResolutionDefaultsProvider()
);
```
- provider 결과를 map으로 합치는 부분만 존재합니다.
```44:54:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentDefinitionDataSource.java
static List<IntentDefinitionEntry> definitionsEntries() {
    Map<ActionIntent, IntentDefinition> defs = new HashMap<>();
    for (IntentDefinitionEntriesProvider provider : DEFINITIONS_PROVIDERS) {
        for (IntentDefinitionEntry e : provider.entries()) {
            defs.put(e.intent(), e.definition());
        }
    }
    return defs.entrySet().stream()
            .map(e -> new IntentDefinitionEntry(e.getKey(), e.getValue()))
            .toList();
}
```
- 예시 provider는 정의 본문(`new IntentDefinition(`)을 반환합니다.
```8:58:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/impl/IntentCreationIntentDefinitionsProvider.java
final class IntentCreationIntentDefinitionsProvider implements IntentDefinitionEntriesProvider {

    @Override
    public List<IntentDefinitionDataSource.IntentDefinitionEntry> entries() {
        return List.of(
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.CREATE,
                        new IntentDefinition(
                                ActionIntent.CREATE,
                                "Author new artifact from scratch; emphasis on original composition and ownership.",
                                "User wants to author something new (document, design, piece of content) where originality and structure matter.",
                                "When the goal is to produce output quickly from a spec (use GENERATE) or to list ideas without a single artifact (use BRAINSTORM).",
                                "CREATE = original authored composition; GENERATE = produce requested output (may be template-driven); BRAINSTORM = ideation, multiple options.",
                                "Narrative or structured new artifact; single coherent output.",
                                List.of(PromptCategory.DESIGN, PromptCategory.WRITING, PromptCategory.CONTENT_CREATION, PromptCategory.CREATIVE),
                                List.of("UI_DESIGN", "UX_DESIGNER", "ARTICLE_WRITING", "CONTENT_CREATION"),
                                List.of("UI_UX_DESIGNER", "CONTENT_WRITER", "CREATIVE_DIRECTOR"),
                                "In CONTENT: prefer CREATE for long-form authored pieces; GENERATE for quick posts or templated output."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.GENERATE,
                        new IntentDefinition(
                                ActionIntent.GENERATE,
                                "Produce requested output from a description or spec; emphasis on fulfilling the request efficiently.",
                                "User wants the system to produce something (code, copy, design, list) that matches a given description or template.",
                                "When the user explicitly wants original authorship or heavy creative ownership (use CREATE).",
                                "GENERATE = produce to spec; CREATE = author from scratch with stronger creative control.",
                                "Narrative or structured output matching the request; may be more template- or spec-driven than CREATE.",
                                List.of(PromptCategory.DEVELOPMENT, PromptCategory.CONTENT_CREATION, PromptCategory.MARKETING, PromptCategory.CREATIVE),
                                List.of("CODE_GENERATION", "CONTENT_CREATION", "CONTENT_MARKETING"),
                                List.of("FULL_STACK_DEVELOPER", "CONTENT_CREATOR", "DIGITAL_MARKETER"),
                                "Default fallback for many categories when intent is unspecified."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.BRAINSTORM,
                        new IntentDefinition(
                                ActionIntent.BRAINSTORM,
                                "Generate multiple ideas, options, or directions without committing to a single artifact.",
                                "User wants ideation, alternatives, or exploratory options rather than one final deliverable.",
                                "When the user wants a single concrete output (use CREATE or GENERATE).",
                                "BRAINSTORM = many ideas; CREATE/GENERATE = one deliverable.",
                                "Structured list or short descriptions of options; no single narrative.",
                                List.of(PromptCategory.CREATIVE, PromptCategory.BUSINESS, PromptCategory.DESIGN),
                                List.of("IDEA_GENERATION", "CREATIVE_WRITING"),
                                List.of("CREATIVE_DIRECTOR", "MARKETING_STRATEGIST"),
                                "Often pairs with CREATIVE or BUSINESS; less common in DEVELOPMENT for code."
                        )
                )
        );
    }
}
```

남은 문제:
- intent provider 등록이 static 고정이라, 테스트에서 intent 데이터를 더 쉽게 주입/대체하려면 구조 변경이 필요합니다.

---

## 3. 반쪽 수정 / 주의 포인트

### (1) 겉으로 분리됐지만 중앙화가 남아 있는 부분
1. 카테고리 seed 데이터 소유가 여전히 단일 클래스에 고정
   - `DefaultCategorySemanticProfileSeedSource`가 seed 본문을 소유하며, 실제 seed 선택은 `SEED_SUPPLIERS` 맵으로만 분리돼 조회 경로는 개선됐지만 데이터 소유는 분산되지 않았습니다.
2. intent 확장 seam의 중앙 고정
   - `IntentDefinitionDataSource`는 provider 인스턴스를 `DEFINITIONS_PROVIDERS`/`RESOLUTION_DEFAULTS_PROVIDERS`의 `private static final List`로 고정 생성합니다.

### (2) 향후 `enum/category/intent` 추가 시 다시 터질 수 있는 부분(코드 기반)
1. `PromptCategory` 추가
   - 최소 `DefaultCategorySemanticProfileSeedSource.SEED_SUPPLIERS`와 `DefaultCategorySemanticProfileRegistry.PROFILE_CATEGORIES`를 동시에 갱신해야 seed가 프로필로 조립됩니다.
   - seed supplier 누락 시 `DefaultCategorySemanticProfileSeedSource.getSeed`에서 `Optional.empty`로 drop됩니다.
2. `ActionIntent` 추가
   - `IntentDictionary`는 `EnumSet.allOf(ActionIntent.class)` 기반 completeness를 강제하므로, `IntentDefinitionDataSource`의 definitions/defaults entries에 해당 intent가 모두 포함되지 않으면 기동 시 실패합니다.
   - 정의/기본값 provider 등록 구조는 `IntentDefinitionDataSource.DEFINITIONS_PROVIDERS`/`RESOLUTION_DEFAULTS_PROVIDERS` static 리스트에 고정되어 있어, 새 intent가 기존 provider의 책임 범위를 벗어나면 해당 등록점도 건드려야 합니다.

---

## 4. 실제 구조 개선 효과

OCP(확장 개방) 측면
- `PromptSpecFactory`: objective/제약/섹션 구성을 `ObjectiveRegistry`와 `ObjectiveProfile`에 위임하며, 이 클래스 내부에 objective별 분기 로직(스위치/케이스)은 없습니다(생성자/메서드 코드상 분기 증거 없음).
- `ObjectiveMappingRegistry`: 정책 소스 및 휴리스틱 로직을 인터페이스로 위임해 변경점이 registry 내부로 고정되지 않습니다.

DIP(의존 역전) 측면
- `GuidelineVerifier`: `GuidelineRuleChecker`를 생성자에서 `requireNonNull`으로 받음.
- `ObjectiveMappingRegistry`: `ObjectivePolicySource`와 `ObjectiveHeuristicInferencePolicy`를 생성자 주입으로 받음.

테스트 대체 가능성 개선 여부
- `CategorySemanticProfileSeedSource`: `DefaultCategorySemanticProfileRegistry` 생성자가 seed source를 직접 받으므로 테스트에서 대체 구현 주입이 가능합니다.
  - 예: `CategorySemanticProfileSeedSourceAssemblerTest`에서 anonymous `CategorySemanticProfileSeedSource`로 WRITING seed를 바꾸는 구조가 코드로 확인됩니다.
```66:90:sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/domain/semantic/CategorySemanticProfileSeedSourceAssemblerTest.java
CategorySemanticProfileSeedSource customSource = new CategorySemanticProfileSeedSource() {
    @Override
    public Optional<CategorySemanticProfileSeed> getSeed(PromptCategory category) {
        if (category != PromptCategory.WRITING) {
            return defaultSource.getSeed(category);
        }
        CategorySemanticProfileSeed original = defaultSource.getSeed(category).orElseThrow();
        Map<ActionIntent, List<RoleTypeInterface>> roles = new java.util.HashMap<>(original.rolesByIntent());
        roles.put(ActionIntent.GENERATE, List.of(WritingRoleType.TECHNICAL_WRITER, WritingRoleType.EDITOR));
        return Optional.of(new CategorySemanticProfileSeed(
                original.category(), original.taskDomain(), original.allowedIntents(), original.intentFitLevels(),
                roles, original.actionsByIntent(), original.discouragedTonesByIntent(), original.discouragedStylesByIntent(),
                ActionIntent.REWRITE, original.fallbackCandidates()));
    }
};
DefaultCategorySemanticProfileRegistry registryCustom =
        new DefaultCategorySemanticProfileRegistry(canonical, null, customSource);
```
- 반대로 intent provider는 static 고정이라 테스트 seam이 약합니다.

여전히 코드 박제로 남은 정책/데이터
- `DefaultCategorySemanticProfileSeedSource`: `SEED_SUPPLIERS`와 `buildXxx()` seed 데이터 본문이 단일 클래스에 고정
- `DefaultObjectiveHeuristicInferencePolicy`: `KEYWORD_MAP` 기반 keyword 규칙이 단일 구현체에 고정
- `IntentDefinitionDataSource`: provider 등록점이 static 리스트로 고정

---

## 5. 가장 먼저 이어서 수정할 3개

### 우선순위 1: `IntentDefinitionDataSource`의 provider 등록 seam 개선
- 왜 지금 손봐야 하는지
  - provider 등록이 `DEFINITIONS_PROVIDERS`/`RESOLUTION_DEFAULTS_PROVIDERS`의 `private static final List`로 고정되어 있어, 테스트에서 provider 교체(대체 데이터 소유)를 구조적으로 하기 어렵습니다.
- 어떤 원칙과 연결되는지
  - DIP(주입 가능한 seam), 테스트 용이성
- 구조적으로 어떤 위험을 줄이는지
  - 새 intent 추가/데이터 변경 시, 테스트/구조 회귀를 줄이기 위해 “대체 데이터 공급”을 가능하게 해야 합니다.

근거(등록점 중앙 고정):
```20:40:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/IntentDefinitionDataSource.java
private static final List<IntentDefinitionEntriesProvider> DEFINITIONS_PROVIDERS = List.of(
        new IntentCreationIntentDefinitionsProvider(),
        new IntentModificationIntentDefinitionsProvider(),
        new IntentAnalysisIntentDefinitionsProvider(),
        new IntentExplanationIntentDefinitionsProvider(),
        new IntentPlanningIntentDefinitionsProvider(),
        new IntentDecisionIntentDefinitionsProvider(),
        new IntentResearchIntentDefinitionsProvider(),
        new IntentExtractionIntentDefinitionsProvider()
);
```

### 우선순위 2: 카테고리 seed 완전성을 fail-fast로 전환(누락 drop 방지)
- 왜 지금 손봐야 하는지
  - seed supplier 누락/오타가 `Optional.empty`로 조용히 drop됩니다.
- 어떤 원칙과 연결되는지
  - Fail-fast/관측 가능 실패
- 구조적으로 어떤 위험을 줄이는지
  - `PromptCategory` 추가 시 프로필 구성 실패가 늦게/조용히 나타나는 회귀를 줄입니다.

근거(drop 경로):
```66:75:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/impl/DefaultCategorySemanticProfileSeedSource.java
if (category == null) {
    return Optional.empty();
}
Supplier<CategorySemanticProfileSeed> supplier = SEED_SUPPLIERS.get(category);
if (supplier == null) {
    return Optional.empty();
}
```

### 우선순위 3: `compatibilityPolicySource` key 파싱 실패를 관측 가능화(최소한의 오류/메트릭 경로)
- 왜 지금 손봐야 하는지
  - `parseActionGroup`이 파싱 실패를 catch 후 `Optional.empty`로 drop합니다.
- 어떤 원칙과 연결되는지
  - 가시성(관측 가능 실패), 정책 키 변경 안정성
- 구조적으로 어떤 위험을 줄이는지
  - 정책 키 변경 시 group override가 조용히 무시되는 회귀를 줄입니다.

근거(조용한 drop):
```139:145:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/impl/DefaultCategorySemanticProfileRegistry.java
private Optional<ActionGroup> parseActionGroup(String key) {
    if (key == null || key.isBlank()) return Optional.empty();
    try {
        return Optional.of(ActionGroup.valueOf(key.trim()));
    } catch (IllegalArgumentException e) {
        return Optional.empty();
    }
}
```

---

### 추가 감사 포인트(요청 항목)

A. 조합 책임 위치
- 카테고리 프로필 조합은 `DefaultCategorySemanticProfileRegistry`가 담당합니다. seed + compatibility override를 `prepare()`에서 groupMap/actions로 조립합니다.
```71:127:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/semantic/impl/DefaultCategorySemanticProfileRegistry.java
private CategorySemanticProfile buildProfile(CategorySemanticProfileSeed seed) {
    GroupMapAndActions prepared = prepare(seed.category(), seed.actionsByIntent());
    return new DefaultCategorySemanticProfile(
            seed.category(),
            seed.taskDomain(),
            seed.allowedIntents(),
            seed.intentFitLevels(),
            seed.rolesByIntent(),
            prepared.actions(),
            seed.discouragedTonesByIntent(),
            seed.discouragedStylesByIntent(),
            seed.fallbackIntent(),
            seed.fallbackCandidates(),
            prepared.groupMap());
}

private GroupMapAndActions prepare(PromptCategory category, Map<ActionIntent, List<ActionTypeInterface>> actions) {
    Map<ActionIntent, List<ActionGroup>> fromActions = toActionGroupMap(actions);
    if (compatibilityPolicySource == null) {
        return new GroupMapAndActions(fromActions, actions != null ? actions : Map.of());
    }
    Map<ActionIntent, List<ActionGroup>> groupMap = new HashMap<>();
    Set<ActionIntent> intents = actions != null ? actions.keySet() : Set.of();
    for (ActionIntent intent : intents) {
        List<String> keys = compatibilityPolicySource.getCompatibleGroupKeys(category, intent);
        if (!keys.isEmpty()) {
            List<ActionGroup> fromSource = resolveGroupKeys(keys);
            if (!fromSource.isEmpty()) {
                groupMap.put(intent, fromSource);
                continue;
            }
        }
        if (fromActions.containsKey(intent)) {
            groupMap.put(intent, fromActions.get(intent));
        }
    }
    return new GroupMapAndActions(groupMap, actions != null ? actions : Map.of());
}
```

B. 조용한 실패(drop) 경로
- `parseActionGroup`: 파싱 실패는 catch 후 `Optional.empty`로 drop.
- `ObjectiveMappingRegistry.putByStableKey`: stableKey null/blank 또는 objective null이면 return(drop).
- `DefaultCategorySemanticProfileSeedSource.getSeed`: category null 또는 SEED_SUPPLIERS에 없는 경우 Optional.empty(drop).

근거:
```35:43:sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/domain/resolutions/ObjectiveMappingRegistry.java
public void putByStableKey(String stableKey, PromptObjective objective) {
    if (stableKey == null || stableKey.isBlank() || objective == null) return;
    String key = stableKey.trim();
    PromptObjective previous = overlayByStableKey.putIfAbsent(key, objective);
    if (previous != null && !previous.equals(objective)) {
        throw new IllegalStateException(
                "Conflicting overlay objective for " + stableKey + ": " + previous + " vs " + objective);
    }
}
```

C. 테스트 대체 가능성
- `DefaultCategorySemanticProfileRegistry`는 seed source를 생성자에서 받으므로 테스트 대체가 쉽습니다.
- `IntentDefinitionDataSource`는 static provider 등록이라 provider를 바꿔치기하기 어렵습니다.

D. 숨은 회귀
- 본 감사 범위(요청한 1~5차 대상 패턴)에서는 “null -> default new” 형태의 fallback이 새로 생성된 증거는 확인되지 않았습니다(감사 중 패턴 검색으로 확인).


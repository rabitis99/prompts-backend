## sharedPrompts 도메인 품질 리뷰 (`domain.prompt.common` 중심)

### 1. Executive Summary

- `domain.prompt.common`과 `domain.prompt.domain.*` 전반을 보면, 규칙 메타데이터·도메인 결정·검증은 잘 분리되어 있으나, 실제 동작에서는 이 메타데이터가 충분히 활용되지 않아 **“설계 의도 vs 실효성” 간 간극**이 존재한다.
- 특히 GuidelineRule 수준/타입, CREATIVE anti-rule, 도메인 체크리스트는 LLM 프롬프트 텍스트에는 강하게 반영되지만, **검증·우선순위·토큰 예산 관점에서 제어 장치가 부족**하다.
- 도메인 결정 결과는 `ResolvedDomain` 단일 타입으로 통일되었으나 V2/Unified 소비 경로가 이원화되어 있고, i18n 분산 하드코딩·테스트 레이어의 부분적 커버리지도 **장기 유지보수·회귀 위험**을 키운다.
- 아래 Findings에서 각 항목별로 **Observation–Evidence–Impact–Severity**를 정리했고, 마지막에 **Hotspots**와 **Non-Issues**를 분리했다.

---

### 2. Findings

#### [F1] RuleLevel/RuleType이 검증 단계에서는 전혀 사용되지 않음 (A, G)

- **Observation**  
  GuidelineRule의 `RuleLevel(HARD/SOFT)`, `RuleType(REQUIRE/FORBID/ALLOW)`은 렌더링에는 사용되지만, 실제 검증(verification) 로직에서는 전혀 참조되지 않아 **“위반 시 품질 실패”라는 의미가 검증 계층에서 사라진다**.
- **Evidence**  
  - 모델 정의  
    - `GuidelineRule` record: `common.guideline.rule.GuidelineRule` (id, title, description, `RuleLevel level`, `RuleType type`)  
    - `RuleLevel` enum: `common.guideline.rule.RuleLevel`  
    - `RuleType` enum: `common.guideline.rule.RuleType`
  - 렌더링 사용  
    - `GuidelineBundleBuilder.build(TaskDomain domain)`에서 `domain.getRulesByLevel(RuleLevel.HARD)` 호출  
      - `src/main/java/org/example/sharedprompts/domain/prompt/common/guideline/bundle/GuidelineBundleBuilder.java` L30-31  
    - `AbstractGuidelineRenderer.renderEssentialConstraints(List<GuidelineRule> hardRules)`에서 `RuleType`별 그룹핑 후 텍스트 생성  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/guideline/AbstractGuidelineRenderer.java` L65-103
  - 검증 미사용  
    - `BaseVerification`, `StandardVerification`, `ChainOfVerification`, `SchemaFirstVerification`, `SoftVerification` 어디에서도 `GuidelineRule`/`RuleLevel`/`RuleType` import·사용 없음  
      - `src/main/java/org/example/sharedprompts/domain/prompt/domain/verification/base/BaseVerification.java` L17-107  
      - `src/main/java/org/example/sharedprompts/domain/prompt/domain/verification/standard/StandardVerification.java` L18-47  
      - `src/main/java/org/example/sharedprompts/domain/prompt/domain/verification/chain/ChainOfVerification.java` L12-28  
      - `src/main/java/org/example/sharedprompts/domain/prompt/domain/verification/schema/SchemaFirstVerification.java` L12-25  
      - `src/main/java/org/example/sharedprompts/domain/prompt/domain/verification/soft/SoftVerification.java` L20-47
- **Impact**  
  규칙 메타데이터(HARD/FORBID 등)는 LLM 입력 텍스트 안에서만 의미를 갖고, 실제 **품질 판정·배지 부여·Repair 대상 결정에는 반영되지 않는다**. 규칙 정의를 수정해도 검증 결과/품질 메트릭은 그대로일 수 있고, “규칙 설계 변경 → 검증 정책 자동 반영”이라는 기대를 깨뜨려 **prompt text와 verifier 사이에 개념 드리프트**를 유발한다.
- **Severity**  
  **Medium** – 즉각적인 장애는 없지만, 품질 정책을 코드로 캡처하려는 설계 의도를 검증 계층이 따라가지 못해 **장기적인 일관성·정책 관리에 부담**을 준다.

---

#### [F2] HARD 규칙 풀셋을 도메인 단위로 항상 부착, 규칙 수/길이 상한·선택 로직 부재 (B)

- **Observation**  
  `GuidelineBundleBuilder`는 주어진 `TaskDomain`의 HARD/SOFT 규칙을 `RuleBudgetPolicy`로 선택하지만, **규칙 수·길이 상한은 정책에 위임**되어 있으며, ActionType/Objective/요청 길이에 따른 동적 상한은 문서화·제어가 제한적이다.
- **Evidence**  
  - HARD 규칙 추출  
    - `GuidelineBundleBuilder.build(TaskDomain domain)`에서 `domain.getRulesByLevel(RuleLevel.HARD)` / `getRulesByLevel(RuleLevel.SOFT)` 호출 후 `budgetPolicy.selectRules(all)`로 선택  
      - `src/main/java/org/example/sharedprompts/domain/prompt/common/guideline/bundle/GuidelineBundleBuilder.java` L30-42
  - 도메인 규칙 집합  
    - `TaskDomain.getRulesByLevel(RuleLevel level)`은  
      `principles()`, `structuringRules()`, `qualityStandards()`, `outputConstraints()` 네 카테고리를 스트림으로 합쳐 level만 필터  
      - `src/main/java/org/example/sharedprompts/domain/prompt/common/enums/TaskDomain.java` L86-95
  - 상한/선택 로직  
    - RuleLevel 사용처는 `TaskDomain`, `GuidelineBundleBuilder`, `DomainStrategyTextProvider`, `PromptSpecValidator` 등이며, “max rules”/“max characters”는 `RuleBudgetPolicy`에 일부 위임되어 있으나 ActionType/Objective 기반 서브셋 선택은 제한적.
- **Impact**  
  CREATIVE/TECHNICAL 같은 도메인의 규칙이 늘어날수록 “필수 제약” 섹션 길이가 선형으로 증가해 **LLM prompt 토큰을 잠식**한다. 긴 사용자 입력/JSON Schema와 합쳐졌을 때 **모델 컨텍스트 한계(토큰 컷오프)를 더 쉽게 초과**하며, 요약·간단 작업에도 동일한 풀셋이 붙어 **“task-specific 최소 핵심 규칙” 개념이 없다**.
- **Severity**  
  **High** – 규칙이 늘어날수록 **토큰 비용·latency·cut-off 위험이 선형(또는 준선형)으로 증가**하는데, 이를 제어할 메커니즘이 전혀 없다.

---

#### [F3] PromptSpecFactory 체크리스트 섹션이 RuleLevel/RuleType을 고려하지 않고 모든 규칙을 나열 (B, A)

- **Observation**  
  `PromptSpecFactory.buildSections`는 `TaskDomain.principles/structuringRules/outputConstraints` 전체를 `appendGuidelineRules`로 그대로 텍스트화하며, **HARD/SOFT나 REQUIRE/FORBID/ALLOW와 상관없이 모두 `- 설명` 형식**으로 붙인다. 이 체크리스트는 다른 계층에서 다시 붙이는 규칙 텍스트와 쉽게 중복된다.
- **Evidence**  
  - 섹션 구성  
    - `buildSections(...)`에서:
      - Role 섹션 생성 후,  
      - `GuidelineBundle bundle = guidelineBundleBuilder.build(taskDomain)`로 HARD/SOFT 규칙 획득  
      - `appendGuidelineRules(checklistBuilder, bundle.hardRules(), locale);`  
      - `appendGuidelineRules(checklistBuilder, bundle.softRules(), locale);`  
      - 비어 있지 않으면 `VERIFICATION_CHECKLIST` 섹션으로 추가  
      - `src/main/java/org/example/sharedprompts/domain/prompt/domain/service/spec/PromptSpecFactory.java` L264-273
  - 규칙 텍스트화  
    - `appendGuidelineRules`는 `GuidelineRule`의 description만 언어별로 꺼내 `- {description}\n` 추가  
      - `PromptSpecFactory.java` L290-299  
      - RuleLevel/RuleType 정보는 사용하지 않음.
  - 중복 가능 지점  
    - SOFT 규칙: `DomainStrategyTextProvider.buildQualityHints`가 다시 SOFT 규칙 description.en을 모아 “Quality Hints” 섹션 생성  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/strategy/DomainStrategyTextProvider.java` L82-105  
    - HARD 규칙: `GuidelineBundleBuilder`가 HARD 규칙을 선택해 `GuidelineBundle.constraints`로 전달하고, `AbstractGuidelineRenderer.renderEssentialConstraints`에서 RuleType별 텍스트 생성  
      - `GuidelineBundleBuilder.java` L30-42  
      - `AbstractGuidelineRenderer.java` L65-103
- **Impact**  
  하나의 요청에 대해 “도메인 체크리스트(섹션)” + “Generation Strategy” + “Quality Hints” + “Essential Constraints” 등 **여러 층위에서 같은 규칙군이 반복적으로 텍스트화** 된다. 이는 **토큰 낭비**뿐 아니라 규칙을 수정할 때 **여러 위치를 동시에 변경해야 하는 유지보수 부담**을 키운다.
- **Severity**  
  **High** – 도메인 규칙을 늘릴수록 PromptSpec 기반 메타프롬프트가 빠르게 비대해지고, **단일 규칙 변경이 여러 구문 표현에 흩어져 반영되어야 하는 구조**다.

---

#### [F4] DomainResolution/ResolvedDomain이 “왜 이 도메인이 선택됐는지”에 대한 설명력을 제공하지 못함 (C)

- **Observation**  
  도메인 결정 결과는 `ResolvedDomain(TaskDomain domain, boolean fallback, ResolutionSource source)`(domain 레이어) 단일 record로 표현되며, **Unified 경로에서는 `DomainFinalizer`가 `DomainResolutionService.resolveForUnified`를 통해 도메인을 결정**한다. `ResolutionSource`로 어느 경로(ACTION_TYPE, PROMPT_CATEGORY, INTENT_AFFINITY, FALLBACK 등)에서 왔는지 구분할 수 있으나, API/응답에 노출되는 설명 필드는 제한적이다.
- **Evidence**  
  - 도메인 결정 로직  
    - `DomainResolver.resolveDomainWithFallback(ActionTypeInterface actionType, PromptCategory promptCategory)`는:
      - actionType이 null이면 `GENERAL, fallback=true`  
      - `actionType.getTaskDomain()`이 GENERAL이 아니면 그 도메인 + `fallback=false`  
      - category가 null이면 GENERAL + `fallback=true`  
      - `promptCategory.getDefaultDomain()`이 GENERAL이 아니면 그 도메인 + `fallback=false`  
      - 그 외 GENERAL + fallback (조건에 따라 true/false)  
      - `src/main/java/org/example/sharedprompts/domain/prompt/domain/resolutions/DomainResolver.java` L18-46
  - 결과 타입  
    - `ResolvedDomain(TaskDomain domain, boolean fallback, ResolutionSource source)`  
      - `src/main/java/org/example/sharedprompts/domain/prompt/domain/resolutions/ResolvedDomain.java` L4-16
  - 파사드  
    - `DomainResolutionService.resolveDomainInternal` / `resolveForUnified`에서 `domainResolver.resolveDomainWithFallback(...)` 호출 후 `ResolvedDomain` 반환  
    - Unified 경로: `DomainFinalizer.finalizeDomain` → `domainResolutionService.resolveForUnified(command.category(), baseDomain)`  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/orchestration/DomainResolutionService.java`  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/orchestration/unified/DomainFinalizer.java` L41-44
- **Impact**  
  운영 환경에서 “왜 이 요청이 CREATIVE가 아니라 GENERAL로 떨어졌는지”, “ActionType 매핑이 비어 fallback이 발생했는지” 등을 **API/도메인 모델만으로는 알 수 없고, 로그 검색에만 의존**해야 한다. 도메인 매핑 튜닝, A/B 테스트, 버그 재현 시 **분석 비용이 커진다**.
- **Severity**  
  **Medium** – 기능은 동작하지만, 도메인 해석이 시스템 핵심인 구조에서 **설명력이 부족해 장기 운용·디버깅 난이도가 높다**.

---

#### [F5] 도메인 결정 결과의 소비 경로 이원화 (F, C)

- **Observation**  
  도메인 결정 결과는 **단일 타입 `ResolvedDomain`(domain 레이어)** 로 통일되어 있으나, **소비 경로가 이원화**되어 있다. V2 생성 파이프라인은 `DomainResolverPort`를 통해 `ResolvedDomain`을 직접 사용하고, Unified 경로는 `UnifiedRoutingFacade` → `DomainFinalizer` → `DomainResolutionService.resolveForUnified`를 통해 도메인을 결정해 `RoutingDecision.finalDomain`으로 전달한다. 두 경로가 동일한 `DomainResolver`를 공유하지만, 호출 지점과 DTO 노출 방식이 달라 **정책 변경 시 두 경로를 모두 검토해야 할 수 있다**.
- **Evidence**  
  - 도메인 레이어 결과  
    - `public record ResolvedDomain(TaskDomain domain, boolean fallback, ResolutionSource source)`  
      - `src/main/java/org/example/sharedprompts/domain/prompt/domain/resolutions/ResolvedDomain.java` L4-16
  - 사용 위치  
    - V2 생성 파이프라인: `GeneratePromptService.clarify`에서 `DomainResolverPort.resolveDomainWithFallback` → `ResolvedDomain` 직접 사용  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/generate/GeneratePromptService.java` L141-148  
    - Unified 경로: `UnifiedPromptGenerationOrchestrator.generate` → `UnifiedRoutingFacade.decide` → `DomainFinalizer.finalizeDomain` → `DomainResolutionService.resolveForUnified`  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/orchestration/UnifiedPromptGenerationOrchestrator.java` L32-34  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/orchestration/unified/UnifiedRoutingFacade.java` L59-61  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/orchestration/unified/DomainFinalizer.java` L41-44
- **Impact**  
  단일 타입으로 통일된 뒤에도, **호출 경로가 V2 vs Unified로 나뉘어** 도메인 정책 변경 시 두 경로를 모두 점검해야 한다. `ResolutionSource`로 근거는 구분 가능하나, API/클라이언트에 설명력을 전달하는 필드는 별도 설계가 필요할 수 있다.
- **Severity**  
  **Low–Medium** – 기능적 오류는 없으나, **도메인 결정 정책의 일관된 소비·확장**을 위해 경로별 문서화가 유용하다.

---

#### [F6] CREATIVE anti-rule이 구조/결론/요약을 강하게 요구하는 경우와 충돌할 때 완화 장치 없음 (D)

- **Observation**  
  CREATIVE 도메인은 “완전성 강제 금지, 결론 강제 금지, 메시지/구조 강제 금지” 같은 HARD/FORBID anti-rule을 갖고 있고, DomainStrategyTextProvider도 “rigid structure 금지, list-based formatting 회피”를 상시 지시한다. 하지만 **ActionType/Objective/사용자 입력이 명시적으로 구조화·결론·요약을 요구하는 경우를 감지해 anti-rule을 조정·무시하는 로직은 없다**.
- **Evidence**  
  - Anti-rule 정의  
    - `CreativeGuidelines.PRINCIPLES` 내:
      - `CREATIVE.ANTI.NO_FORCED_COMPLETION` – HARD, FORBID  
      - `CREATIVE.ANTI.NO_FORCED_CONCLUSION` – HARD, FORBID  
      - `CREATIVE.ANTI.NO_FORCED_MESSAGE` – HARD, FORBID  
      - `CREATIVE.ANTI.NO_FORCED_STRUCTURE` – HARD, FORBID  
      - `src/main/java/org/example/sharedprompts/domain/prompt/common/guideline/content/CreativeGuidelines.java` L50-90
  - 도메인 전략 텍스트  
    - `DomainStrategyTextProvider.buildDomainStrategy`의 CREATIVE 분기에서:
      - “Allow open-ended exploration without forcing rigid structure”  
      - “Avoid clinical or list-based formatting — let the creative flow naturally”  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/strategy/DomainStrategyTextProvider.java` L34-41
  - Guideline 적용 방식  
    - `PromptSpecFactory.buildSections`에서 CREATIVE 도메인의 원칙/구조/출력 제약을 그대로 체크리스트에 포함  
      - `PromptSpecFactory.java` L264-273  
    - `GuidelineBundleBuilder`는 도메인·RuleContext 기반으로 규칙을 필터링하나, ActionType/Objective/입력 내용 기반 anti-rule 완화 조건은 없음  
      - `GuidelineBundleBuilder.java` L29-42
- **Impact**  
  예를 들어 CREATIVE 도메인에서 JSON/목록/명확한 결론을 요구하는 ActionType·Objective를 매핑해도, 메타프롬프트는 한편으로 “rigid structure 금지”를 강하게 요구해 **모델에게 상충된 신호**를 준다. 이 충돌은 구조 준수 실패나 과도한 자유도(창의성)로 나타날 수 있지만, **어디에서도 anti-rule을 완화·우선순위 조정하지 않는다**.
- **Severity**  
  **Medium** – 모든 작업이 CREATIVE + free text 조합은 아니겠지만, CREATIVE_WITH_CONSTRAINTS 프로파일 자체가 “제약이 있는 창의 작업”을 표방하는 만큼, **구조/제약과의 충돌 위험이 실제로 발생할 수 있다**.

---

#### [F7] Guideline/i18n 텍스트는 테스트가 있지만, Role/Objective/기타 enum의 i18n은 구조적으로 분산·하드코딩 (E, G)

- **Observation**  
  도메인 규칙(`GuidelineRule`)의 I18nText는 전용 테스트(`TaskDomainTest`)로 ko/en/ja 필드의 null/blank 여부와 id 유일성이 검증되지만, 역할(RoleType), ObjectiveProfile의 instruction 텍스트, Tone/Style 설명 등 **다른 i18n 소스는 각 enum/클래스에 하드코딩**되어 있고, 이를 전역적으로 검증하는 장치는 없다.
- **Evidence**  
  - 규칙 i18n 검증 테스트  
    - `TaskDomainTest`에서 각 도메인의 `principles/structuringRules/qualityStandards/outputConstraints`를 순회하며 `I18nText`의 ko/en/ja가 null/blank가 아닌지, rule id가 유일한지 검사  
      - `src/test/java/org/example/sharedprompts/domain/prompt/enums/TaskDomainTest.java` L64-133
  - Role i18n 하드코딩  
    - `WritingRoleType`이 각 상수에 ko/en/ja 이름·설명을 필드로 직접 보유  
      - `src/main/java/org/example/sharedprompts/domain/prompt/common/enums/role/WritingRoleType.java` L8-55  
    - 이 값들에 대한 별도 검증 테스트는 존재하지 않음(Grep 기준).
  - ObjectiveProfile i18n  
    - `BaseObjectiveProfile.i18n(locale, ko, en, ja)`는 단순 분기만 수행하고, 개별 프로파일(예: `ExtractionObjectiveProfile.instructionContent`, `AnalyticalObjectiveProfile.instructionContent`, `CreativeObjectiveProfile.instructionContent`)이 세 언어 문자열을 직접 전달  
      - `src/main/java/org/example/sharedprompts/domain/prompt/domain/objective/profiles/BaseObjectiveProfile.java` L46-66  
      - `.../ExtractionObjectiveProfile.java` L67-71  
      - `.../AnalyticalObjectiveProfile.java` L60-64  
      - `.../CreativeObjectiveProfile.java` L57-61  
    - 이 텍스트들의 일관성/누락 여부를 확인하는 테스트는 존재하지 않음.
- **Impact**  
  TaskDomain 규칙 외의 i18n 리소스에서 번역 누락, 오타, 의미 불일치가 발생해도 **컴파일·단위 테스트에서 잡히지 않으며, 실제 런타임에서만 드러난다**. 특히 Role/Objective 설명은 UI·프롬프트 품질에 직접 영향을 주는데, 이를 자동 검증·집계할 수 있는 중앙 구조가 없어서 **유지보수 비용이 커진다**.
- **Severity**  
  **Medium** – 이미 규칙에는 좋은 테스트가 있으나, 나머지 i18n 경로는 커버되지 않아 **“부분적 안전망” 상태**다.

---

#### [F8] `domain.prompt.common` 경계는 지켜지지만, 상위 레이어에서 공통 정책 소비 방식이 일관되지 않음 (F)

- **Observation**  
  `domain.prompt.common` 패키지 자신은 애플리케이션/어댑터에 의존하지 않고 enums/guideline 등만 포함하지만, **상위 계층의 도메인 결정 소비 경로가 이원화**되어 있다. Unified 경로는 `DomainFinalizer` → `DomainResolutionService.resolveForUnified`로 `ResolvedDomain`을 얻고, V2 생성 경로는 `GeneratePromptService.clarify`에서 `DomainResolverPort.resolveDomainWithFallback`으로 `ResolvedDomain`을 직접 사용한다.
- **Evidence**  
  - common 계층 의존성  
    - `TaskDomain`, `GuidelineRule`, `RuleLevel`, `RuleType` 등은 `common.*` 패키지 안에서 서로만 import하고, `application.service`나 `adapter.*` 패키지를 참조하지 않음(파일들의 import 목록 기준).
  - 상위 계층 사용 차이  
    - Unified 경로: `UnifiedRoutingFacade.decide` → `DomainFinalizer.finalizeDomain` → `DomainResolutionService.resolveForUnified`로 `ResolvedDomain`을 얻어 `RoutingDecision.finalDomain`으로 전달  
      - `UnifiedRoutingFacade.java` L59-61, `DomainFinalizer.java` L41-44  
    - V2 Generate 파이프라인: `GeneratePromptService.clarify`에서 `DomainResolverPort.resolveDomainWithFallback` 결과(`ResolvedDomain`)만 직접 사용  
      - `GeneratePromptService.java` L141-148
- **Impact**  
  common 레이어는 잘 분리되어 있으나, 실제 애플리케이션 계층들이 이를 소비하는 방식이 제각각이어서 **“도메인 결정 정책은 여기서만 거친다” 같은 명확한 진입점이 없다**. 이는 나중에 도메인 결정 정책을 바꿀 때, **어느 경로를 수정해야 전체에 반영되는지 파악하기 어렵게** 만든다.
- **Severity**  
  **Low–Medium** – 구조적 설계의 일관성 문제로, **변화에 대한 방어력(변경 용이성)**이 떨어진다.

---

#### [F9] Guideline 렌더링/도메인 결정 파사드/렌더러-검증 연계에 대한 테스트 공백 (G, A, B, C)

- **Observation**  
  PromptSpecFactory/PromptSpecValidator/GeneratePromptService 등 핵심 도메인 로직에 대한 단위 테스트는 존재하지만, **Guideline 렌더러(`AbstractGuidelineRenderer` + 언어별 구현), `GuidelineBundleBuilder`, `DomainResolutionService`, `UnifiedRoutingFacade`, `PromptSpecRendererAdapter`와 같은 “가이드라인 렌더링·도메인 파사드·Unified 라우팅·렌더링 결과”는 테스트에서 거의 다뤄지지 않는다**.
- **Evidence**  
  - 도메인/검증 테스트  
    - `PromptSpecFactoryTest`, `PromptSpecValidatorTest`, `GeneratePromptServiceTest`는 Objective 매핑, Constraints scaling, Verification 전략 선택, Repair 루프 등을 검증하지만, **GuidelineRule/TaskDomain 규칙을 어떻게 렌더링·연결하는지는 다루지 않음**  
      - `src/test/java/org/example/sharedprompts/domain/prompt/domain/service/PromptSpecFactoryTest.java` 전반  
      - `src/test/java/org/example/sharedprompts/domain/prompt/domain/service/PromptSpecValidatorTest.java` 전반  
      - `src/test/java/org/example/sharedprompts/domain/prompt/application/service/GeneratePromptServiceTest.java` 전반
  - 테스트 내 참조 부재  
    - `src/test/java`에서 `GuidelineRenderer`, `GuidelineBundleBuilder`, `DomainResolutionService`, `UnifiedPromptGenerationOrchestrator`, `UnifiedRoutingFacade`, `PromptSpecRendererAdapter` 이름을 가진 테스트 클래스나 직접 참조 없음(Grep 기준).
- **Impact**  
  도메인 규칙을 추가·수정하거나, DomainResolver/DomainResolutionService의 동작을 바꿀 때, **최종 프롬프트에 어떤 텍스트가 실제로 포함되는지 / 폴백 알림이 언제 붙는지 / 검증 루프와 어떤 상호작용을 하는지**에 대해 회귀 테스트가 없다. 이는 **프롬프트 텍스트·토큰 구조와 같은 “보이지 않는 API”가 쉽게 깨질 수 있음을 의미**한다.
- **Severity**  
  **Medium–High** – LLM 기반 시스템에서 prompt 구조와 guideline 적용은 사실상 **public API**에 해당하므로, 이 영역이 테스트 공백인 것은 **회귀 리스크가 크다**.

---

### 3. Hotspots (위험도가 높은 파일/클래스 Top 5)

- **`PromptSpecFactory` (`domain.prompt.domain.service.spec.PromptSpecFactory`)**  
  TaskDomain 규칙을 PromptSection으로 변환하고 Objective/Constraints/OutputContract를 동시에 구성하는 **중심 허브**다. 규칙 수 증가에 따른 체크리스트 비대화, i18n 선택, Objective 매핑 등 여러 축의 변경이 이 파일에 집중된다.

- **`GuidelineBundleBuilder` (`domain.prompt.common.guideline.bundle.GuidelineBundleBuilder`)**  
  `TaskDomain`에서 HARD/SOFT 규칙을 추출하고 `RuleBudgetPolicy`·`GuidelineRuleApplicability`로 선택해 `GuidelineBundle`을 만든다. `PromptSpecFactory.buildSections`와 연동되어 체크리스트·제약 텍스트 구성에 사용되며, **규칙 수·토큰 예산·적용 조건**에 직접적인 영향을 준다.

- **`CreativeGuidelines` (`domain.prompt.common.guideline.content.CreativeGuidelines`)**  
  CREATIVE 도메인의 핵심·구조·품질·출력 규칙과 anti-rule이 모두 정의된 클래스다. 구조/결론 강제 금지처럼 다른 Objective·ActionType과 쉽게 충돌할 수 있는 규칙을 여럿 포함해, **잘못된 조합에서 품질 문제가 발생할 수 있는 지점**이다.

- **`DomainResolver` / `DomainResolutionService` (`domain.prompt.domain.resolutions.DomainResolver`, `domain.prompt.application.service.orchestration.DomainResolutionService`)**  
  ActionType/PromptCategory → TaskDomain 매핑과 fallback 정책 로그를 담당한다. 도메인 변경·신규 카테고리 추가 시 가장 먼저 수정되는 지점이며, **설명력이 부족한 상태라 디버깅 난이도가 높은 부분**이다.

- **`GeneratePromptService` (`domain.prompt.application.service.generate.GeneratePromptService`)**  
  V2 Clarify–Solve–Verify–Repair 파이프라인을 오케스트레이션하며, `clarify`에서 `DomainResolverPort.resolveDomainWithFallback`으로 `ResolvedDomain`을 얻어 PromptSpec을 생성한다. 검증 전략/Repair 루프/저장/배지까지 연결되어 있어, **작은 정책 변경도 전체 플로우에 파급**될 수 있다.

- **`UnifiedPromptGenerationOrchestrator` / `UnifiedRoutingFacade` (`orchestration.UnifiedPromptGenerationOrchestrator`, `orchestration.unified.UnifiedRoutingFacade`)**  
  단일 엔드포인트용 Unified 생성 경로를 담당한다. `UnifiedRoutingFacade.decide`로 Objective/OutputNeeds/Domain/EngineProfile을 결정하고, `GeneratePromptUseCase`(V2)를 호출한 뒤 `SchemaContractEvaluator`로 결과를 평가한다. **라우팅 규칙·도메인 결정·품질 메트릭**이 이 경로에 집중된다.

---

### 4. Non-Issues (검토했지만 문제 없다고 판단한 지점)

- **N1. RuleLevel 자체는 “Hard vs Soft” 규칙 분리에 실제로 사용 중**  
  - **Evidence**  
    - `TaskDomain.getRulesByLevel`로 level별 규칙을 필터링하고, `GuidelineBundleBuilder.build`는 HARD/SOFT 규칙을 선택해 `GuidelineBundle`로 전달하며, `DomainStrategyTextProvider.buildQualityHints`는 SOFT 규칙만 “Quality Hints” 섹션에 사용  
      - `TaskDomain.java` L86-95  
      - `GuidelineBundleBuilder.java` L30-42  
      - `DomainStrategyTextProvider.java` L82-105
  - **이유**  
    RuleLevel이 검증 단계에는 연결되지 않았지만, **“필수 제약 vs 품질 힌트”라는 역할 분리에는 실질적인 동작 차이**를 제공하고 있어, 완전히 장식 데이터라고 보긴 어렵다.

- **N2. `domain.prompt.common` 계층은 상위 레이어에 의존하지 않고 레이어 경계를 잘 지킴**  
  - **Evidence**  
    - `TaskDomain`, `GuidelineRule`, `RuleLevel`, `RuleType`, `GuidelineBundle`, `GuidelineBundleBuilder` 등은 `common.*` 패키지 안에서 서로만 import하고, `application.service`나 `adapter.*` 패키지에 대한 import는 없다(각 파일의 import 목록 기준).
  - **이유**  
    common 계층이 “순수 도메인/공통 모델”로 유지되고 있어, 상위 계층 변경이 하위 공통 모델을 직접 끌어들이지 않는다는 점은 **레이어링 관점에서 양호**하다.

- **N3. GuidelineRule/I18nText에 대한 기본 i18n 품질 검사는 존재**  
  - **Evidence**  
    - `TaskDomainTest`는 모든 TaskDomain에 대해 `principles/structuringRules/qualityStandards/outputConstraints` 목록이 비어 있지 않은지, 각 GuidelineRule의 `title`/`description` I18nText가 null/blank가 아닌지, 모든 규칙 id가 유일한지 검증  
      - `src/test/java/org/example/sharedprompts/domain/prompt/enums/TaskDomainTest.java` L20-61, L64-133
  - **이유**  
    규칙 레벨의 다국어 텍스트는 최소한 “누락/빈 문자열/중복 id”에 대한 자동 검증이 있어, 완전한 i18n 솔루션은 아니더라도 **가장 핵심적인 규칙 리소스의 품질은 일정 수준 보장**된다.


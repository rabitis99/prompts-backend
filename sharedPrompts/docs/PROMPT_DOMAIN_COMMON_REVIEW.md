## sharedPrompts 도메인 품질 리뷰 (`domain.prompt.common` 중심)

### 1. Executive Summary

- `domain.prompt.common`과 `domain.prompt.domain.*` 전반을 보면, 규칙 메타데이터·도메인 결정·검증은 잘 분리되어 있으나, 실제 동작에서는 이 메타데이터가 충분히 활용되지 않아 **“설계 의도 vs 실효성” 간 간극**이 존재한다.
- 특히 GuidelineRule 수준/타입, CREATIVE anti-rule, 도메인 체크리스트는 LLM 프롬프트 텍스트에는 강하게 반영되지만, **검증·우선순위·토큰 예산 관점에서 제어 장치가 부족**하다.
- DomainResolver/DomainResolution 이중 구조, i18n 분산 하드코딩, 테스트 레이어의 부분적 커버리지도 **장기 유지보수·회귀 위험**을 키운다.
- 아래 Findings에서 각 항목별로 **Observation–Evidence–Impact–Severity**를 정리했고, 마지막에 **Hotspots**와 **Non-Issues**를 분리했다.

---

### 2. Findings

#### [F1] RuleLevel/RuleType이 검증 단계에서는 전혀 사용되지 않음 (A, G)

- **Observation**  
  GuidelineRule의 `RuleLevel(HARD/SOFT)`, `RuleType(REQUIRE/FORBID/ALLOW)`은 렌더링에는 사용되지만, 실제 검증(verification) 로직에서는 전혀 참조되지 않아 **“위반 시 품질 실패”라는 의미가 검증 계층에서 사라진다**.
- **Evidence**  
  - 모델 정의  
    - `GuidelineRule` record: `GuidelineRule.java` (id, title, description, `RuleLevel level`, `RuleType type`)  
    - `RuleLevel` enum: `RuleLevel.java`  
    - `RuleType` enum: `RuleType.java`
  - 렌더링 사용  
    - `PromptGuidelineBuilder.extractHardRules(TaskDomain domain)`에서 `domain.getRulesByLevel(RuleLevel.HARD)` 호출  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/guideline/PromptGuidelineBuilder.java` L88-93  
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
  `PromptGuidelineBuilder`는 주어진 `TaskDomain`의 모든 HARD 규칙(4 카테고리)을 한 번에 모아 렌더러에 넘기며, **ActionType/Objective/요청 길이/토큰 예산에 따른 규칙 수·길이 상한이 없다**.
- **Evidence**  
  - HARD 규칙 추출  
    - `extractHardRules(TaskDomain domain)`가 `domain.getRulesByLevel(RuleLevel.HARD)`만 호출  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/guideline/PromptGuidelineBuilder.java` L88-93
  - 도메인 규칙 집합  
    - `TaskDomain.getRulesByLevel(RuleLevel level)`은  
      `principles()`, `structuringRules()`, `qualityStandards()`, `outputConstraints()` 네 카테고리를 스트림으로 합쳐 level만 필터  
      - `src/main/java/org/example/sharedprompts/domain/prompt/common/enums/TaskDomain.java` L71-85
  - 상한/선택 로직 부재  
    - RuleLevel 사용처는 `TaskDomain`, `PromptGuidelineBuilder`, `DomainStrategyTextProvider`뿐이며, “max rules”, “max characters” 같은 상한이나 ActionType/Objective 기반 서브셋 선택 로직 없음.
- **Impact**  
  CREATIVE/TECHNICAL 같은 도메인의 규칙이 늘어날수록 “필수 제약” 섹션 길이가 선형으로 증가해 **LLM prompt 토큰을 잠식**한다. 긴 사용자 입력/JSON Schema와 합쳐졌을 때 **모델 컨텍스트 한계(토큰 컷오프)를 더 쉽게 초과**하며, 요약·간단 작업에도 동일한 풀셋이 붙어 **“task-specific 최소 핵심 규칙” 개념이 없다**.
- **Severity**  
  **High** – 규칙이 늘어날수록 **토큰 비용·latency·cut-off 위험이 기하급수적으로 증가**하는데, 이를 제어할 메커니즘이 전혀 없다.

---

#### [F3] PromptSpecFactory 체크리스트 섹션이 RuleLevel/RuleType을 고려하지 않고 모든 규칙을 나열 (B, A)

- **Observation**  
  `PromptSpecFactory.buildSections`는 `TaskDomain.principles/structuringRules/outputConstraints` 전체를 `appendGuidelineRules`로 그대로 텍스트화하며, **HARD/SOFT나 REQUIRE/FORBID/ALLOW와 상관없이 모두 `- 설명` 형식**으로 붙인다. 이 체크리스트는 다른 계층에서 다시 붙이는 규칙 텍스트와 쉽게 중복된다.
- **Evidence**  
  - 섹션 구성  
    - `buildSections(...)`에서:
      - Role 섹션 생성 후,  
      - `appendGuidelineRules(checklistBuilder, taskDomain.principles(), locale);`  
      - `appendGuidelineRules(checklistBuilder, taskDomain.structuringRules(), locale);`  
      - `appendGuidelineRules(checklistBuilder, taskDomain.outputConstraints(), locale);`  
      - 비어 있지 않으면 `VERIFICATION_CHECKLIST` 섹션으로 추가  
      - `src/main/java/org/example/sharedprompts/domain/prompt/domain/service/spec/PromptSpecFactory.java` L186-215
  - 규칙 텍스트화  
    - `appendGuidelineRules`는 `GuidelineRule`의 description만 언어별로 꺼내 `- {description}\n` 추가  
      - `PromptSpecFactory.java` L232-241  
      - RuleLevel/RuleType 정보는 사용하지 않음.
  - 중복 가능 지점  
    - SOFT 규칙: `DomainStrategyTextProvider.buildQualityHints`가 다시 SOFT 규칙 description.en을 모아 “Quality Hints” 섹션 생성  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/strategy/DomainStrategyTextProvider.java` L82-105  
    - HARD 규칙: `PromptGuidelineBuilder`가 HARD 규칙을 또 한 번 수집해 `renderEssentialConstraints`로 넘김  
      - `PromptGuidelineBuilder.java` L68-73  
      - `AbstractGuidelineRenderer.java` L65-103
- **Impact**  
  하나의 요청에 대해 “도메인 체크리스트(섹션)” + “Generation Strategy” + “Quality Hints” + “Essential Constraints” 등 **여러 층위에서 같은 규칙군이 반복적으로 텍스트화** 된다. 이는 **토큰 낭비**뿐 아니라 규칙을 수정할 때 **여러 위치를 동시에 변경해야 하는 유지보수 부담**을 키운다.
- **Severity**  
  **High** – 도메인 규칙을 늘릴수록 PromptSpec 기반 메타프롬프트가 빠르게 비대해지고, **단일 규칙 변경이 여러 구문 표현에 흩어져 반영되어야 하는 구조**다.

---

#### [F4] DomainResolution/ResolvedDomain이 “왜 이 도메인이 선택됐는지”에 대한 설명력을 제공하지 못함 (C)

- **Observation**  
  도메인 결정 결과는 `ResolvedDomain(TaskDomain, boolean isFallback)`(domain 레이어)와 이를 다시 감싼 `DomainResolution(TaskDomain, boolean isFallback)`(common) 두 record로 표현되지만, 둘 다 **“ActionType에서 왔는지 / PromptCategory에서 왔는지 / 어떤 매핑을 탔는지” 같은 근거 필드가 전혀 없다**. 유일한 근거는 `DomainResolutionService`의 fallback 로그 한 줄뿐이다.
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
    - `ResolvedDomain(TaskDomain domain, boolean isFallback)`  
      - `src/main/java/org/example/sharedprompts/domain/prompt/domain/resolutions/ResolvedDomain.java` L5-11  
    - `DomainResolution(TaskDomain domain, boolean isFallback)`  
      - `src/main/java/org/example/sharedprompts/domain/prompt/common/guideline/i18n/DomainResolution.java` L5-8
  - 파사드  
    - `DomainResolutionService.resolveDomainInternal`에서 `ResolvedDomain resolved = domainResolver.resolveDomainWithFallback(...)` 호출 후,  
      - fallback인 경우 `log.warn("Domain fallback used — ...")`  
      - `new DomainResolution(resolved.domain(), resolved.isFallback())` 반환  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/orchestration/DomainResolutionService.java` L52-60
- **Impact**  
  운영 환경에서 “왜 이 요청이 CREATIVE가 아니라 GENERAL로 떨어졌는지”, “ActionType 매핑이 비어 fallback이 발생했는지” 등을 **API/도메인 모델만으로는 알 수 없고, 로그 검색에만 의존**해야 한다. 도메인 매핑 튜닝, A/B 테스트, 버그 재현 시 **분석 비용이 커진다**.
- **Severity**  
  **Medium** – 기능은 동작하지만, 도메인 해석이 시스템 핵심인 구조에서 **설명력이 부족해 장기 운용·디버깅 난이도가 높다**.

---

#### [F5] DomainResolution/ResolvedDomain 이중 표현으로 인한 개념 중복과 경계 모호 (F, C)

- **Observation**  
  `domain.prompt.domain.resolutions.ResolvedDomain`과 `domain.prompt.common.guideline.i18n.DomainResolution`은 사실상 **동일 정보를 담는 두 record**로 공존하며, 서로 다른 계층에서 유사한 역할을 한다. 결과적으로 도메인 결정 결과에 대한 “단일 소스”가 없고, **어느 계층에서 어떤 타입을 써야 할지 경계가 모호**하다.
- **Evidence**  
  - 도메인 레이어 결과  
    - `public record ResolvedDomain(TaskDomain domain, boolean isFallback)`  
      - `src/main/java/org/example/sharedprompts/domain/prompt/domain/resolutions/ResolvedDomain.java` L5-11
  - common 레이어 결과  
    - `public record DomainResolution(TaskDomain domain, boolean isFallback)`  
      - `src/main/java/org/example/sharedprompts/domain/prompt/common/guideline/i18n/DomainResolution.java` L5-8
  - 사용 위치  
    - 프롬프트 생성 파이프라인: `GeneratePromptService.clarify`는 `DomainResolverPort.resolveDomainWithFallback` → `ResolvedDomain`을 직접 사용  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/generate/GeneratePromptService.java` L141-148  
    - 가이드라인 빌더/PromptGenerator: `DomainResolutionService`를 통해 `DomainResolution` 사용  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/guideline/PromptGuidelineBuilder.java` L24-26, L39-44  
      - `src/main/java/org/example/sharedprompts/domain/prompt/application/service/orchestration/PromptGenerator.java` L21-24, L29-30, L181-185
- **Impact**  
  같은 개념이 두 타입으로 분산되어 있어, **새로운 호출자가 어느 쪽을 써야 할지, 추후 필드가 추가될 때 양쪽을 모두 확장해야 하는지 판단이 어렵다**. 이는 레이어 간 의존성을 이해하기 어렵게 만들고, 도메인 결정 결과를 해석·확장할 때 **버그를 유발할 수 있다**.
- **Severity**  
  **Low–Medium** – 당장 기능적 오류는 없지만, 설계 복잡성을 불필요하게 올리고, **도메인 해석 모델의 일관성을 해칠 수 있다**.

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
      - `PromptSpecFactory.java` L205-215  
    - `PromptGuidelineBuilder`는 도메인 기반으로만 HARD 규칙을 수집하고, ActionType/Objective/입력 내용 기반 condition은 없음  
      - `PromptGuidelineBuilder.java` L68-73
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
  `domain.prompt.common` 패키지 자신은 애플리케이션/어댑터에 의존하지 않고 enums/guideline/i18n만 포함하지만, **상위 계층의 사용 방식은 일관되지 않다**. 예를 들어 도메인 결정은 어떤 곳에서는 `DomainResolutionService`+`common.guideline.i18n.DomainResolution`을 쓰고, 다른 곳에서는 `DomainResolverPort`+`ResolvedDomain`을 직접 사용한다.
- **Evidence**  
  - common 계층 의존성  
    - `TaskDomain`, `GuidelineRule`, `I18nText`, `DomainResolution` 등은 모두 `common.*` 패키지 안에서 서로만 import하고, `application.service`나 `adapter.*` 패키지를 참조하지 않음(파일들의 import 목록 기준).
  - 상위 계층 사용 차이  
    - Prompt 가이드라인 쪽은 `DomainResolutionService.resolveDomain`으로 `common.guideline.i18n.DomainResolution`을 받고 fallback notice를 렌더링  
      - `PromptGuidelineBuilder.java` L39-44, L76-81  
    - Generate 파이프라인은 `DomainResolverPort.resolveDomainWithFallback` 결과만 직접 사용하고 `DomainResolutionService`를 통하지 않음  
      - `GeneratePromptService.java` L141-148
- **Impact**  
  common 레이어는 잘 분리되어 있으나, 실제 애플리케이션 계층들이 이를 소비하는 방식이 제각각이어서 **“도메인 결정 정책은 여기서만 거친다” 같은 명확한 진입점이 없다**. 이는 나중에 도메인 결정 정책을 바꿀 때, **어느 경로를 수정해야 전체에 반영되는지 파악하기 어렵게** 만든다.
- **Severity**  
  **Low–Medium** – 구조적 설계의 일관성 문제로, **변화에 대한 방어력(변경 용이성)**이 떨어진다.

---

#### [F9] Guideline 렌더링/도메인 결정 파사드/렌더러-검증 연계에 대한 테스트 공백 (G, A, B, C)

- **Observation**  
  PromptSpecFactory/PromptSpecValidator/GeneratePromptService 등 핵심 도메인 로직에 대한 단위 테스트는 존재하지만, **Guideline 렌더러(`AbstractGuidelineRenderer` + 각 언어 렌더러), `PromptGuidelineBuilder`, `DomainResolutionService`, `PromptSpecRendererAdapter`와 같은 “가이드라인 렌더링·도메인 파사드·렌더링 결과”는 테스트에서 거의 다뤄지지 않는다**.
- **Evidence**  
  - 도메인/검증 테스트  
    - `PromptSpecFactoryTest`, `PromptSpecValidatorTest`, `GeneratePromptServiceTest`는 Objective 매핑, Constraints scaling, Verification 전략 선택, Repair 루프 등을 검증하지만, **GuidelineRule/TaskDomain 규칙을 어떻게 렌더링·연결하는지는 다루지 않음**  
      - `src/test/java/org/example/sharedprompts/domain/prompt/domain/service/PromptSpecFactoryTest.java` 전반  
      - `src/test/java/org/example/sharedprompts/domain/prompt/domain/service/PromptSpecValidatorTest.java` 전반  
      - `src/test/java/org/example/sharedprompts/domain/prompt/application/service/GeneratePromptServiceTest.java` 전반
  - 테스트 내 참조 부재  
    - `src/test/java`에서 `GuidelineRenderer`, `PromptGuidelineBuilder`, `DomainResolutionService`, `PromptSpecRendererAdapter` 이름을 가진 테스트 클래스나 직접 참조 없음(Grep 기준).
- **Impact**  
  도메인 규칙을 추가·수정하거나, DomainResolver/DomainResolutionService의 동작을 바꿀 때, **최종 프롬프트에 어떤 텍스트가 실제로 포함되는지 / 폴백 알림이 언제 붙는지 / 검증 루프와 어떤 상호작용을 하는지**에 대해 회귀 테스트가 없다. 이는 **프롬프트 텍스트·토큰 구조와 같은 “보이지 않는 API”가 쉽게 깨질 수 있음을 의미**한다.
- **Severity**  
  **Medium–High** – LLM 기반 시스템에서 prompt 구조와 guideline 적용은 사실상 **public API**에 해당하므로, 이 영역이 테스트 공백인 것은 **회귀 리스크가 크다**.

---

### 3. Hotspots (위험도가 높은 파일/클래스 Top 5)

- **`PromptSpecFactory` (`domain.prompt.domain.service.spec.PromptSpecFactory`)**  
  TaskDomain 규칙을 PromptSection으로 변환하고 Objective/Constraints/OutputContract를 동시에 구성하는 **중심 허브**다. 규칙 수 증가에 따른 체크리스트 비대화, i18n 선택, Objective 매핑 등 여러 축의 변경이 이 파일에 집중된다.

- **`PromptGuidelineBuilder` (`domain.prompt.application.service.guideline.PromptGuidelineBuilder`)**  
  도메인 결정 파사드(`DomainResolutionService`)와 GuidelineRenderer를 통해 최종 사용자용 프롬프트를 구성한다. HARD 규칙 추출·토큰 예산·폴백 알림 정책 등 **프론트 도메인 품질에 직접적인 영향**을 미친다.

- **`CreativeGuidelines` (`domain.prompt.common.guideline.content.CreativeGuidelines`)**  
  CREATIVE 도메인의 핵심·구조·품질·출력 규칙과 anti-rule이 모두 정의된 클래스다. 구조/결론 강제 금지처럼 다른 Objective·ActionType과 쉽게 충돌할 수 있는 규칙을 여럿 포함해, **잘못된 조합에서 품질 문제가 발생할 수 있는 지점**이다.

- **`DomainResolver` / `DomainResolutionService` (`domain.prompt.domain.resolutions.DomainResolver`, `domain.prompt.application.service.orchestration.DomainResolutionService`)**  
  ActionType/PromptCategory → TaskDomain 매핑과 fallback 정책 로그를 담당한다. 도메인 변경·신규 카테고리 추가 시 가장 먼저 수정되는 지점이며, **설명력이 부족한 상태라 디버깅 난이도가 높은 부분**이다.

- **`GeneratePromptService` (`domain.prompt.application.service.generate.GeneratePromptService`)**  
  Clarify–Solve–Verify–Repair 전체 파이프라인을 오케스트레이션하며, 도메인 결정 결과(`ResolvedDomain`)를 기반으로 PromptSpec을 생성한다. 검증 전략/Repair 루프/저장/배지까지 연결되어 있어, **작은 정책 변경도 전체 플로우에 파급**될 수 있다.

---

### 4. Non-Issues (검토했지만 문제 없다고 판단한 지점)

- **N1. RuleLevel 자체는 “Hard vs Soft” 규칙 분리에 실제로 사용 중**  
  - **Evidence**  
    - `TaskDomain.getRulesByLevel`로 level별 규칙을 필터링하고, `PromptGuidelineBuilder`는 HARD 규칙만 최종 프롬프트 제약으로 붙이며, `DomainStrategyTextProvider.buildQualityHints`는 SOFT 규칙만 “Quality Hints” 섹션에 사용  
      - `TaskDomain.java` L71-85  
      - `PromptGuidelineBuilder.java` L88-93  
      - `DomainStrategyTextProvider.java` L82-105
  - **이유**  
    RuleLevel이 검증 단계에는 연결되지 않았지만, **“필수 제약 vs 품질 힌트”라는 역할 분리에는 실질적인 동작 차이**를 제공하고 있어, 완전히 장식 데이터라고 보긴 어렵다.

- **N2. `domain.prompt.common` 계층은 상위 레이어에 의존하지 않고 레이어 경계를 잘 지킴**  
  - **Evidence**  
    - `TaskDomain`, `GuidelineRule`, `I18nText`, `DomainResolution` 등은 모두 `common.*` 패키지 안에서 서로만 import하고, `application.service`나 `adapter.*` 패키지에 대한 import는 없다(각 파일의 import 목록 기준).
  - **이유**  
    common 계층이 “순수 도메인/공통 모델”로 유지되고 있어, 상위 계층 변경이 하위 공통 모델을 직접 끌어들이지 않는다는 점은 **레이어링 관점에서 양호**하다.

- **N3. GuidelineRule/I18nText에 대한 기본 i18n 품질 검사는 존재**  
  - **Evidence**  
    - `TaskDomainTest`는 모든 TaskDomain에 대해 `principles/structuringRules/qualityStandards/outputConstraints` 목록이 비어 있지 않은지, 각 GuidelineRule의 `title`/`description` I18nText가 null/blank가 아닌지, 모든 규칙 id가 유일한지 검증  
      - `src/test/java/org/example/sharedprompts/domain/prompt/enums/TaskDomainTest.java` L20-61, L64-133
  - **이유**  
    규칙 레벨의 다국어 텍스트는 최소한 “누락/빈 문자열/중복 id”에 대한 자동 검증이 있어, 완전한 i18n 솔루션은 아니더라도 **가장 핵심적인 규칙 리소스의 품질은 일정 수준 보장**된다.


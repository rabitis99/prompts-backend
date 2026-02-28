# Prompt 도메인 폴더 구조 (확장성 중심)

`org.example.sharedprompts.domain.prompt` 하위 패키지 구조를 정리한 문서입니다.  
**모든 파일은 `domain/prompt/` 아래에 위치시킨다.** (entity, adapter, application, domain, common, infrastructure, event 등 전부 동일 루트)  
**현재 구조(as-is)**와 **확장성을 고려한 제안 구조(to-be)**를 나누어 기술합니다.

---

## 1. 현재 구조 (As-Is)

```text
domain/prompt/
├── Prompt.java                          # 엔티티 (JPA) — 루트에 위치
├── adapter/
│   ├── in/
│   │   └── web/                         # HTTP 입력 어댑터
│   │       ├── BadgeResponseAssembler.java
│   │       ├── GeneratePromptRequest.java
│   │       ├── GeneratePromptResponse.java
│   │       └── PromptEngineController.java
│   └── out/                             # 출력 어댑터 (인프라 구현)
│       ├── ConstrainedDecodingAdapter.java
│       ├── LLMClientAdapter.java
│       ├── PromptPersistenceAdapter.java
│       ├── PromptSpecRenderHelper.java
│       ├── PromptSpecRendererAdapter.java
│       ├── RepairPromptRenderer.java
│       └── SavePromptVersionAdapter.java
├── application/
│   ├── port/
│   │   ├── in/                          # 유즈케이스(인바운드) 포트
│   │   │   ├── CreatePromptUseCase.java
│   │   │   ├── GeneratePromptCommand.java
│   │   │   ├── GeneratePromptResult.java
│   │   │   ├── GeneratePromptUseCase.java
│   │   │   ├── PromptCommandUseCase.java
│   │   │   └── PromptQueryUseCase.java
│   │   └── out/                         # 아웃바운드 포트
│   │       ├── ConstrainedDecodingPort.java
│   │       ├── LLMClientPort.java
│   │       ├── PromptCommandPort.java
│   │       ├── PromptQueryPort.java
│   │       ├── PromptSpecRendererPort.java
│   │       ├── SavePromptVersionPort.java
│   │       └── ValidateUserPort.java
│   └── service/
│       └── GeneratePromptService.java
├── domain/
│   ├── model/                           # 도메인 모델(스펙/계약 등)
│   │   ├── ContentSandbox.java
│   │   ├── Constraints.java
│   │   ├── OutputContract.java
│   │   ├── PromptSection.java
│   │   ├── PromptSpec.java
│   │   ├── QualityRubric.java
│   │   └── VerifyResult.java
│   ├── value/                           # 값 객체
│   │   ├── PromptObjective.java
│   │   ├── PromptingStrategy.java
│   │   ├── PromptStrategyBundle.java
│   │   ├── QualityBadge.java
│   │   └── QualityPriority.java
│   ├── service/                         # 도메인 서비스
│   │   ├── BadgeResolver.java
│   │   ├── PromptSpecFactory.java
│   │   ├── PromptSpecValidator.java
│   │   └── RecommendationRegistry.java
│   ├── policy/                          # 도메인 정책
│   │   ├── StrategyBundlePolicy.java
│   │   └── StrategyPromotionPolicy.java
│   ├── resolution/                      # 해석(도메인/목표 매핑) 관련
│   │   ├── DomainResolver.java
│   │   ├── DomainResolverPort.java
│   │   ├── ExplicitObjectiveMapping.java
│   │   ├── ExplicitObjectiveMappingPort.java
│   │   ├── ObjectiveMappingRegistry.java
│   │   ├── ObjectiveMappingRegistryPort.java
│   │   ├── ObjectiveResolver.java
│   │   ├── ObjectiveResolverPort.java
│   │   └── ResolvedDomain.java
│   ├── objective/                       # 목표(Objective) 레지스트리·프로파일
│   │   ├── DefaultObjectiveRegistry.java
│   │   ├── ObjectiveProfile.java
│   │   ├── ObjectiveRegistry.java
│   │   └── profiles/
│   │       ├── AnalyticalObjectiveProfile.java
│   │       ├── BaseObjectiveProfile.java
│   │       ├── CreativeObjectiveProfile.java
│   │       ├── ExtractionObjectiveProfile.java
│   │       ├── FactualObjectiveProfile.java
│   │       ├── PlanningObjectiveProfile.java
│   │       └── ReasoningObjectiveProfile.java
│   └── verification/                    # 검증 전략
│       ├── BaseVerification.java
│       ├── ChainOfVerification.java
│       ├── SchemaFirstVerification.java
│       ├── SoftVerification.java
│       ├── StandardVerification.java
│       ├── VerificationContext.java
│       └── VerificationStrategy.java
├── enums/
│   ├── action/                          # 액션 타입별 enum
│   │   ├── ActionTypeInterface.java
│   │   ├── AnalysisActionType.java
│   │   ├── ... (다수)
│   │   └── WritingActionType.java
│   ├── role/                            # 역할 타입별 enum
│   │   ├── RoleTypeInterface.java
│   │   ├── ... (다수)
│   │   └── WritingRoleType.java
│   ├── serializer/                      # JSON 직렬화
│   │   ├── ActionTypeDeserializer.java
│   │   ├── ActionTypeSerializer.java
│   │   ├── EnumResolver.java
│   │   ├── RoleTypeDeserializer.java
│   │   └── RoleTypeSerializer.java
│   ├── ExperienceLevel.java
│   ├── I18nUtils.java
│   ├── LanguageType.java
│   ├── PromptCategory.java
│   ├── SortType.java
│   ├── StyleAxis.java
│   ├── StyleType.java
│   ├── TaskDomain.java
│   └── ToneType.java
├── guideline/                           # 가이드라인 규칙(도메인 규칙)
│   ├── AnalyticalGuidelines.java
│   ├── CreativeGuidelines.java
│   ├── DomainResolution.java
│   ├── EducationalGuidelines.java
│   ├── GeneralGuidelines.java
│   ├── GuidelinePolicy.java
│   ├── GuidelineRule.java
│   ├── I18nText.java
│   ├── PracticalGuidelines.java
│   ├── RuleLevel.java
│   ├── RuleType.java
│   └── TechnicalGuidelines.java
├── service/                             # 애플리케이션/오케스트레이션 서비스 (도메인 외부)
│   ├── guideline/                       # 가이드라인 렌더링
│   │   ├── AbstractGuidelineRenderer.java
│   │   ├── EnglishGuidelineRenderer.java
│   │   ├── GuidelineRenderer.java
│   │   ├── GuidelineRendererFactory.java
│   │   ├── JapaneseGuidelineRenderer.java
│   │   ├── KoreanGuidelineRenderer.java
│   │   └── PromptGuidelineBuilder.java
│   ├── DomainResolutionService.java
│   ├── DomainStrategyTextProvider.java
│   ├── PromptAIService.java
│   ├── PromptGenerator.java
│   ├── PromptPersistenceService.java
│   ├── PromptServiceImpl.java
│   ├── PromptUsageCountService.java
│   └── PromptUsageCountServiceImpl.java
├── facade/
│   └── PromptCreationFlow.java
├── infrastructure/
│   └── config/
│       ├── PromptDomainConfig.java
│       └── ResolutionConfig.java
├── repository/
│   └── PromptRepository.java
└── event/
    └── PromptViewedEventListener.java
```

---

## 2. 확장성 중심 제안 구조 (To-Be)

아래는 **헥사고날 + DDD**를 유지하면서, **역할별·기능별로 더 쪼갠** 폴더 구조 제안입니다.  
새 기능(예: 새 목표 타입, 새 검증 방식, 새 어댑터) 추가 시 **어디에 넣을지**가 명확해지도록 구성했습니다.

```text
domain/prompt/
│
├── entity/                              # [1] 영속성 엔티티 (JPA). 현재 단계: 여기. 장기: infrastructure/persistence/entity
│   └── Prompt.java
│
├── adapter/
│   ├── in/                              # 인바운드 어댑터
│   │   ├── web/                         # HTTP API 전용
│   │   │   ├── controller/
│   │   │   │   └── PromptEngineController.java
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   │   └── GeneratePromptRequest.java
│   │   │   │   └── response/
│   │   │   │       ├── GeneratePromptResponse.java
│   │   │   │       └── BadgeResponseAssembler.java
│   │   │   └── (추후) graphql/, grpc/ 등 프로토콜별 하위 폴더
│   │   └── (추후) cli/, event/ 등
│   └── out/                             # 아웃바운드 어댑터 — 역할별 분리
│       ├── persistence/                 # 저장소
│       │   ├── PromptPersistenceAdapter.java
│       │   └── SavePromptVersionAdapter.java
│       ├── llm/                          # LLM 호출 (프로바이더별 하위 권장)
│       │   ├── LLMClientAdapter.java     # 또는 openai/, anthropic/, local/ 등으로 분리
│       │   └── ConstrainedDecodingAdapter.java
│       ├── render/                       # 스펙 → 텍스트 렌더링
│       │   ├── PromptSpecRendererAdapter.java
│       │   ├── PromptSpecRenderHelper.java
│       │   └── RepairPromptRenderer.java
│       └── (추후) search/, notification/ 등
│
├── application/
│   ├── port/
│   │   ├── in/                          # 인바운드 포트(유즈케이스 인터페이스)
│   │   │   ├── command/                  # 명령 유즈케이스
│   │   │   │   ├── CreatePromptUseCase.java
│   │   │   │   ├── PromptCommandUseCase.java
│   │   │   │   └── GeneratePromptCommand.java  (DTO)
│   │   │   ├── query/                    # 조회 유즈케이스
│   │   │   │   ├── PromptQueryUseCase.java
│   │   │   │   └── GeneratePromptResult.java   (DTO)
│   │   │   └── GeneratePromptUseCase.java      # 생성 플로우 진입점
│   │   └── out/                          # 아웃바운드 포트 — 관심사별 분리
│   │       ├── persistence/
│   │       │   ├── PromptCommandPort.java
│   │       │   ├── PromptQueryPort.java
│   │       │   └── SavePromptVersionPort.java
│   │       ├── llm/
│   │       │   ├── LLMClientPort.java
│   │       │   └── ConstrainedDecodingPort.java
│   │       ├── render/
│   │       │   └── PromptSpecRendererPort.java
│   │       └── identity/
│   │           └── ValidateUserPort.java
│   ├── service/                         # 유즈케이스 구현 전부 (오케스트레이션 포함)
│   │   ├── generate/
│   │   │   └── GeneratePromptService.java
│   │   ├── orchestration/
│   │   │   ├── PromptServiceImpl.java
│   │   │   ├── PromptPersistenceService.java
│   │   │   ├── DomainResolutionService.java
│   │   │   └── PromptGenerator.java
│   │   ├── guideline/                   # 가이드라인 렌더링
│   │   │   ├── GuidelineRenderer.java
│   │   │   ├── AbstractGuidelineRenderer.java
│   │   │   ├── EnglishGuidelineRenderer.java
│   │   │   ├── KoreanGuidelineRenderer.java
│   │   │   ├── JapaneseGuidelineRenderer.java
│   │   │   ├── GuidelineRendererFactory.java
│   │   │   └── PromptGuidelineBuilder.java
│   │   ├── strategy/
│   │   │   └── DomainStrategyTextProvider.java
│   │   ├── ai/
│   │   │   └── PromptAIService.java
│   │   ├── usage/
│   │   │   ├── PromptUsageCountService.java
│   │   │   └── PromptUsageCountServiceImpl.java
│   │   └── (추후) repair/, recommend/ 등
│   └── facade/                          # 복합 플로우
│       └── PromptCreationFlow.java
│
├── domain/                              # 순수 도메인 (프레임워크 무의존)
│   ├── model/                           # 스펙·계약·섹션 등 핵심 모델
│   │   ├── spec/
│   │   │   ├── PromptSpec.java
│   │   │   ├── PromptSection.java
│   │   │   ├── ContentSandbox.java
│   │   │   └── Constraints.java
│   │   ├── contract/
│   │   │   └── OutputContract.java
│   │   └── result/
│   │       ├── VerifyResult.java
│   │       └── QualityRubric.java
│   ├── value/                           # 값 객체
│   │   ├── objective/
│   │   │   └── PromptObjective.java
│   │   ├── strategy/
│   │   │   ├── PromptingStrategy.java
│   │   │   └── PromptStrategyBundle.java
│   │   └── quality/
│   │       ├── QualityBadge.java
│   │       └── QualityPriority.java
│   ├── service/                         # 도메인 서비스 (무상태)
│   │   ├── spec/
│   │   │   ├── PromptSpecFactory.java
│   │   │   └── PromptSpecValidator.java
│   │   ├── badge/
│   │   │   └── BadgeResolver.java
│   │   └── recommendation/
│   │       └── RecommendationRegistry.java
│   ├── policy/
│   │   ├── strategy/
│   │   │   ├── StrategyBundlePolicy.java
│   │   │   └── StrategyPromotionPolicy.java
│   │   └── (추후) rate/, quota/ 등
│   ├── resolution/                      # 도메인/목표 해석
│   │   ├── spi/                         # 해석 계약(도메인 내부 전략 인터페이스)
│   │   │   ├── DomainResolverPort.java
│   │   │   ├── ObjectiveResolverPort.java
│   │   │   └── ExplicitObjectiveMappingPort.java
│   │   ├── domain/                      # 도메인 해석 구현
│   │   │   ├── DomainResolver.java
│   │   │   └── ResolvedDomain.java
│   │   ├── objective/                   # 목표 해석 구현
│   │   │   ├── ObjectiveResolver.java
│   │   │   └── ExplicitObjectiveMapping.java
│   │   └── (추후) locale/, tenant/ 등
│   ├── objective/                       # 목표 프로파일(레지스트리)
│   │   ├── ObjectiveProfile.java
│   │   ├── ObjectiveRegistry.java
│   │   ├── registry/                    # 등록 책임 분리 (등록 누락 방지)
│   │   │   ├── DefaultObjectiveRegistry.java
│   │   │   └── (선택) ObjectiveRegistration.java
│   │   └── profiles/
│   │       ├── BaseObjectiveProfile.java
│   │       ├── AnalyticalObjectiveProfile.java
│   │       ├── CreativeObjectiveProfile.java
│   │       ├── ExtractionObjectiveProfile.java
│   │       ├── FactualObjectiveProfile.java
│   │       ├── PlanningObjectiveProfile.java
│   │       └── ReasoningObjectiveProfile.java
│   └── verification/                    # 검증 전략
│       ├── VerificationStrategy.java
│       ├── VerificationContext.java
│       ├── base/
│       │   └── BaseVerification.java
│       ├── chain/
│       │   └── ChainOfVerification.java
│       ├── schema/
│       │   └── SchemaFirstVerification.java
│       ├── soft/
│       │   └── SoftVerification.java
│       └── standard/
│           └── StandardVerification.java
│
├── common/                              # 프롬프트 BC 내부 공용 (전사 shared 아님)
│   ├── enums/                           # 프롬프트 도메인용 열거형
│   │   ├── action/
│   │   │   ├── ActionTypeInterface.java
│   │   │   └── ... (각 ActionType 구현체)
│   │   ├── role/
│   │   │   ├── RoleTypeInterface.java
│   │   │   └── ... (각 RoleType 구현체)
│   │   ├── serializer/
│   │   │   ├── ActionTypeSerializer.java
│   │   │   ├── ActionTypeDeserializer.java
│   │   │   ├── RoleTypeSerializer.java
│   │   │   ├── RoleTypeDeserializer.java
│   │   │   └── EnumResolver.java
│   │   ├── TaskDomain.java
│   │   ├── PromptCategory.java
│   │   ├── ToneType.java
│   │   ├── StyleType.java
│   │   ├── StyleAxis.java
│   │   ├── SortType.java
│   │   ├── ExperienceLevel.java
│   │   ├── LanguageType.java
│   │   └── I18nUtils.java
│   └── guideline/                      # 가이드라인 규칙(도메인 규칙)
│       ├── rule/
│       │   ├── GuidelineRule.java
│       │   ├── RuleType.java
│       │   └── RuleLevel.java
│       ├── policy/
│       │   └── GuidelinePolicy.java
│       ├── i18n/
│       │   ├── I18nText.java
│       │   └── DomainResolution.java
│       ├── content/                     # 도메인별 가이드라인 내용
│       │   ├── GeneralGuidelines.java
│       │   ├── AnalyticalGuidelines.java
│       │   ├── CreativeGuidelines.java
│       │   ├── EducationalGuidelines.java
│       │   ├── TechnicalGuidelines.java
│       │   └── PracticalGuidelines.java
│       └── (추후) versioning/ 등
│
├── infrastructure/                     # 도메인 인프라(설정·구현 배선)
│   ├── config/
│   │   ├── PromptDomainConfig.java
│   │   └── ResolutionConfig.java
│   ├── persistence/                     # (선택) JPA 리포지토리·엔티티 구현 위치
│   │   ├── PromptRepository.java
│   │   └── (장기) entity/ → PromptEntity 등 영속성 전용 모델
│   └── (추후) cache/, messaging/ 등
│
└── event/                              # 도메인 이벤트 리스너
    └── PromptViewedEventListener.java
```

---

## 3. 폴더별 역할 및 확장 포인트

| 폴더 | 역할 | 확장 시 |
|------|------|---------|
| **entity/** | JPA 엔티티. 현재는 여기, 장기적으로는 `infrastructure/persistence/entity` 이동 권장 | 새 엔티티(예: PromptVersion)는 동일 레벨에 추가 |
| **adapter/in/web/controller** | HTTP 컨트롤러 | REST 외 GraphQL·gRPC는 `adapter/in/graphql`, `adapter/in/grpc` 등으로 분리 |
| **adapter/in/web/dto/request, response** | API DTO | 요청/응답 DTO 추가 시 해당 하위에 추가 |
| **adapter/out/persistence** | DB·저장소 연동 | 새 저장소(예: 캐시)는 `adapter/out/cache` 등 추가 |
| **adapter/out/llm** | LLM 호출 | **프로바이더별** `openai/`, `anthropic/`, `local/` 등 하위 폴더로 쪼개면 새 프로바이더 추가 시 위치 명확 |
| **adapter/out/render** | 스펙→텍스트 렌더링 | 포맷별(마크다운, HTML 등) 하위 추가 가능 |
| **application/port/in/command, query** | CQRS 스타일 분리 | 명령/쿼리 유즈케이스 추가 시 해당 폴더에 추가 |
| **application/port/out/** | 관심사별 포트(persistence, llm, render, identity) | 새 외부 연동은 새 하위 폴더로 추가 |
| **application/service/** | **유즈케이스 구현 전부**(generate, orchestration, guideline, strategy, ai, usage 포함) | 수리·추천 등은 `repair/`, `recommend/` 등 하위 추가 |
| **application/facade** | 복합 유즈케이스 | 새 플로우는 facade에 클래스 추가 또는 하위 폴더 |
| **domain/model/spec, contract, result** | 스펙·계약·결과 모델 | 새 모델 타입은 해당 하위에 추가 |
| **domain/value/objective, strategy, quality** | 값 객체 분류 | 새 값 객체는 목적에 맞는 하위에 추가 |
| **domain/service/spec, badge, recommendation** | 도메인 서비스 | 새 도메인 서비스는 역할별 하위에 추가 |
| **domain/policy/strategy** | 전략·번들 정책 | 정책 종류 늘면 하위 폴더 추가 |
| **domain/resolution/spi** | 도메인 내부 해석 계약(전략 인터페이스). `port` 아님 → application/port/out와 혼동 방지 | 새 해석 차원(로케일 등)은 하위 추가 |
| **domain/resolution/domain, objective** | 해석 구현 | 구현체 추가 |
| **domain/objective/registry** | 목표 등록 책임 분리. 등록 누락 방지 | 새 프로파일은 profiles에 추가 후 레지스트리 등록 |
| **domain/objective/profiles** | 목표별 프로파일 | 새 목표는 새 Profile 클래스 + registry 등록 |
| **domain/verification/** | 검증 전략 종류별 | 새 검증 방식은 새 하위 폴더 또는 클래스 |
| **common/enums** | 프롬프트 BC 내부 공용 열거형 | 새 enum은 action/role 등 기존 분류에 추가 또는 새 하위 |
| **common/guideline/rule, policy, content** | 가이드라인 규칙·정책·내용 | 새 규칙/도메인 가이드라인은 해당 하위에 추가 |
| **infrastructure/config** | 빈 설정·해석기 주입 | 새 인프라 설정은 config 또는 새 하위 |
| **event** | 이벤트 리스너 | 새 이벤트 종류는 여기 또는 하위 폴더로 추가 |

---

## 4. 적용 시 유의사항

1. **점진적 적용**: 한 번에 옮기지 말고, 새 코드는 제안(To-Be) 구조에 맞추고 기존 코드는 리팩터 시점에 점진적으로 이동하는 것을 권장합니다.
2. **entity 위치**: `Prompt.java`가 JPA에 의존하므로 `domain` 코어보다는 `entity/` 또는 `infrastructure/persistence/entity/`에 두는 것이 헥사고날 관점에서는 더 일관됩니다. 기존 `controller`(프로젝트 루트의 `controller.prompt`)와의 관계는 유지한 채, 도메인 패키지 내에서는 `entity/`로만 옮겨도 됩니다.
3. **service vs appservice**: 현재 `domain/prompt/service`는 “애플리케이션 서비스”에 가깝습니다. `domain.service`는 “도메인 서비스”로 한정하고, 애플리케이션 오케스트레이션은 `application/service` 쪽으로 모으면 역할이 분리됩니다.
4. **enums → common/enums**: `enums`를 `common/enums`로 두면 “프롬프트 도메인 내부 공용” 열거형임이 드러나고, 다른 common 개념(guideline 등)과 대칭됩니다.
5. **포트 패키지 분리**: `application/port/out`을 persistence, llm, render, identity 등으로 나누면 새 아웃바운드가 생길 때마다 새 폴더만 추가하면 되어 확장이 쉽습니다.

---

## 5. 가장 위험한 혼동 포인트 5개 (및 수정 반영)

문서/트리에서 독자가 "어디에 둘지" 헷갈리기 쉬운 부분을 정리하고, 위 To-Be에 반영한 결정을 명시합니다.

### A. application/이 트리에서 두 번 등장하던 문제

- **문제**: To-Be 트리 중간에 `application/`을 정의한 뒤, 아래에 `application/ └── ...`가 한 번 더 나와 "두 개가 다른 건가?" 오해 유발.
- **수정**: 하단의 중복 `application/` 블록을 **삭제**함. 유즈케이스·포트·서비스·facade는 **한 번만** 등장하는 `application/` 아래에만 둠.

### B. appservice/ vs application/service/ 역할 겹침

- **문제**: `application/service/generate`가 있는데 `appservice/orchestration`에 PromptServiceImpl, PromptGenerator 등이 있으면 "유즈케이스 구현은 어디에?" 혼란.
- **수정**: **appservice/ 제거**. 유즈케이스 구현(오케스트레이션 포함)은 **application/service/** 아래에만 둠.  
  → `application/service/generate/`, `orchestration/`, `guideline/`, `strategy/`, `ai/`, `usage/` 등으로 기능별 하위 분리.

### C. shared/의 의미 애매함 (domain/prompt 안에 두면 더더욱)

- **문제**: `shared/enums`, `shared/guideline`이 "프롬프트 도메인 내부 공용"인데, 이름이 전사 공용(shared)처럼 느껴짐.
- **수정**: **shared/ → common/** 으로 변경.  
  프롬프트 bounded context 내부 공용임을 분명히 함. (`common/enums`, `common/guideline`)

### D. 엔티티 위치 entity/ vs infrastructure/persistence/entity/

- **문제**: 문서에서 "entity/ 또는 infrastructure/persistence/entity/" 둘 다 가능하다고 하면, 실제 선택이 흔들림.
- **결정**:  
  - **현재 단계**: `entity/`에 둠. (기존 코드 이동 최소화)  
  - **장기**: JPA 엔티티를 "영속성 모델"로만 보려면 `infrastructure/persistence/entity/`로 이동하고, 도메인에는 순수 `domain/model`만 두는 방식이 헥사고날과 일치.  
  - 문서에는 "현재 단계에선 entity/, 장기적으로 persistence/entity 이동"을 **단계 명시**로 적어 둠.

### E. domain/resolution 안의 "port" 소유감 혼동

- **문제**: `domain/resolution/port`에 DomainResolverPort 등이 있으면, 이게 "도메인이 외부에 요구하는 아웃바운드 포트"인지 "도메인 내부 컴포넌트 간 계약"인지 헷갈림.
- **정리**: 여기서 쓰는 건 **도메인 내부 전략 인터페이스**이므로 application/port/out와 구분하기 위해 **domain/resolution/port → domain/resolution/spi** 로 명명 변경.  
  (선택: `contract`도 가능. 목적은 "헥사고날 아웃바운드 포트"와 혼동 방지.)

---

## 6. 적용 시 유의사항 (요약·재참조)

상세 가이드는 위 **4. 적용 시 유의사항**을 기준으로 유지합니다.  
이 절에서는 핵심만 다시 요약합니다.

1. **점진적 적용**: 새 코드는 To-Be 구조에만 생성하고, 기존 코드는 리팩터 타이밍에 이동합니다.
2. **entity 위치**: 현재는 `entity/`에 두되, 장기적으로는 `infrastructure/persistence/entity/`로 옮겨 도메인 코어를 프레임워크 무의존으로 유지합니다.
3. **common 명명**: `common/`은 프롬프트 BC 내부 공용만 의미합니다. 전사 공용이면 상위 루트(예: `org.example.sharedprompts.shared`)로 승격을 검토합니다.
4. **resolution 계약**: `domain/resolution/spi`는 "도메인 내부 전략/계약"이며, application 레이어의 아웃바운드 포트와는 구분됩니다.
5. **application/port/out 분리**: persistence, llm, render, identity 등 역할별 폴더로 나누면 새 아웃바운드 추가 시 위치가 명확해집니다.

---

## 7. 요약

- **현재**: adapter / application / domain / enums / guideline / service / facade / infrastructure / repository / event 가 한 단계로 나뉘어 있음.
- **제안(결정 사항 반영)**:
  - **엔티티** → `entity/` (현재). 장기: `infrastructure/persistence/entity/`
  - **어댑터** → `adapter/in/web`(controller, dto 분리), `adapter/out`(persistence, llm, render 등). llm은 프로바이더별 `openai/`, `anthropic/`, `local/` 등으로 쪼개기 권장.
  - **유즈케이스** → `application/port/in`(command, query), `application/port/out`(역할별), **`application/service`에 유즈케이스 구현 전부(오케스트레이션 포함)**, `application/facade`
  - **도메인** → `domain/model`, `domain/value`, `domain/service`, `domain/policy`, `domain/resolution`(계약은 **spi**), `domain/objective`(**registry**로 등록 책임 분리), `domain/verification`
  - **공용** → **`common/enums`**, **`common/guideline`** (프롬프트 BC 내부)
  - **인프라** → `infrastructure/config`, `infrastructure/persistence`
  - **appservice/ 없음** → 전부 `application/service/` 하위로 통일.

기능이 늘어나도 **어디에 새 클래스를 둘지**가 명확해지도록 했습니다.

---

## 8. 리팩터링 시 지키면 좋은 규칙

- **모든 파일은 `domain/prompt/` 아래에 위치시킨다.** (adapter, application, domain, common, infrastructure, event 등 동일 루트)
- **새 기능 클래스는 To-Be 구조에만 생성한다.** (기존 코드는 점진 이동)
- **유즈케이스 구현(오케스트레이션 포함)은 `application/service` 아래에만 둔다.**
- **도메인(domain/)은 프레임워크 import 금지(예: Spring, JPA).** (entity·infrastructure 예외)

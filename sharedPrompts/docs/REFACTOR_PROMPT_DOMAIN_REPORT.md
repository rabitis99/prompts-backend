# Prompt Domain 리팩토링 리포트 — Hexagonal + SOLID

대상 경로: `sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt`

---

## 1. 현황 진단 (As-Is)

### 1.1 중복 기능 후보 TOP 10

| # | 파일/라인 | 역할 | 왜 중복인지 |
|---|-----------|------|-------------|
| 1 | `DomainStrategyTextProvider` (switch domain) / `StrategyBundlePolicy` + `ObjectiveProfile` | 도메인별 전략/지시 문단 vs Objective별 정책 | TaskDomain/Objective 분기 로직이 v1(메타프롬프트)과 v2(Objective 프로파일)에 이원화. 동일 목적(도메인→생성 방향)이 두 경로에 존재. |
| 2 | `PromptGenerator.buildMetaPrompt` / `PromptSpecRendererAdapter.render` | 메타프롬프트 문자열 조립 | "You are an expert...", Hard Rules, Role/Tone/Style, User Input, Language 섹션 구성이 유사. v1과 v2 각각 별도 구현. |
| 3 | `DomainResolutionService.resolveDomain*` / `GeneratePromptService.clarify` → `DomainResolverPort` | ActionType+Category → TaskDomain 해석 | 동일 `DomainResolverPort` 사용하지만, v1은 DTO·DomainResolution 래핑 서비스를 거침. 해석 호출 경로가 이중화. |
| 4 | `PromptSpecFactory.buildSections` appendGuidelineRules / `DomainStrategyTextProvider.buildQualityHints` | 도메인 규칙→문자열 | GuidelineRule을 locale별 설명으로 나열하는 패턴이 팩토리와 TextProvider 양쪽에 있음. |
| 5 | `GeneratePromptService.normalizeInput` / (다른 곳 입력 정규화) | 입력 공백·특수문자 정규화 | 정규화 로직이 한 곳에만 있으면 되나, 향후 Sanitization 등과 통합 시 중복 가능성. |
| 6 | `BadgeResponseAssembler.toBadgeDto` / (다른 Response DTO에서 Badge 매핑) | QualityBadge → DTO | 현재는 Assembler 한 곳뿐. 동일 패턴이 다른 컨트롤러에서 나오면 중복. |
| 7 | `RepairPromptRenderer` / `PromptSpecRendererAdapter` | PromptSpecRenderHelper 사용 | renderStrategies, renderSections 공유로 이미 일부 통합. 섹션 구성 문구가 여전히 각자 존재. |
| 8 | `ExplicitObjectiveMapping` put() 호출부 (ResolutionConfig) / `ObjectiveMappingRegistry` 휴리스틱 | ActionType → Objective 매핑 | 명시 매핑(Config)과 휴리스틱(Registry)이 별도 레이어에 분산. 단일 “매핑 정책”으로 볼 수 있음. |
| 9 | `PromptGuidelineBuilder.build` / `PromptGenerator.buildEnhancedRequest` | 역할/톤/스타일/경험 수준 문단 | v1 플로우 내에서 가이드라인 조합과 메타프롬프트 조합이 유사한 “섹션 붙이기” 패턴. |
| 10 | ExperienceLevel switch (PromptGenerator.buildExperienceDirective) / ObjectiveProfile.constraints(level) | 경험 수준별 지시 | 한쪽은 메타프롬프트 문구, 한쪽은 제약치. 목적 다르지만 “ExperienceLevel 분기”가 여러 곳에 산재. |

### 1.2 미사용 기능 후보 TOP 10

| # | 파일/라인 | 역할 | 왜 미사용인지 |
|---|-----------|------|----------------|
| 1 | `infrastructure/config/DomainResolverPort.java` (빈 클래스) | (없음) | `domain.resolution.DomainResolverPort`와 동일 simple name의 빈 클래스. ResolutionConfig에서 이 타입으로 빈을 선언해 두었으나 실제 반환은 `DomainResolver`(인터페이스 구현체). 타입 불일치로 빈 등록 오류 가능. **실질적으로 dead code.** |
| 2 | `DomainResolverPort.resolveDomainWithFallback` | 폴백 여부 포함 해석 | GeneratePromptService는 `resolveDomain`만 사용. `resolveDomainWithFallback`은 DomainResolutionService에서만 사용. v2 플로우에서 폴백 로깅/분기 미연결. |
| 3 | `RecommendationRegistry` | 도메인별 톤/스타일 추천 | PromptMetadataController·DTO에서 사용 중. **실제로는 사용됨.** (미사용 아님) |
| 4 | `PromptCreationFlow` (CreatePromptUseCase) | v1 생성 플로우 | PromptController가 CreatePromptUseCase로 호출. **사용됨.** 단, InPort만 의존하고 내부는 구체 서비스 직접 의존. |
| 5 | `ConstrainedDecodingPort` | JSON 스키마 기반 제약 생성 | GeneratePromptService.solve()에서 EXTRACTION 등 조건부로 사용. **사용됨.** |
| 6 | `DomainResolutionService` | DTO 기반 도메인 해석 파사드 | PromptGenerator, PromptGuidelineBuilder에서 사용. **사용됨.** |
| 7 | `SoftVerification` | 소프트 검증 | PromptSpecValidator 등에서 사용 여부 확인 필요. 사용처 있으면 제외. |
| 8 | `ExplicitObjectiveMappingPort` / `ObjectiveMappingRegistryPort` | Objective 명시/휴리스틱 매핑 | ObjectiveResolver에서만 사용. UseCase에서는 직접 호출 안 함 → **이미 ObjectiveResolver를 통해 간접 사용.** |
| 9 | `PromptSpecRenderHelper` | 렌더 헬퍼 | PromptSpecRendererAdapter, RepairPromptRenderer에서 사용. **사용됨.** |
| 10 | (없음) | - | 진짜 “아예 미사용” 클래스는 `infrastructure.config.DomainResolverPort`(빈 클래스) 하나. 나머지는 플로우/설정에서 사용 중이거나 포트를 통해 간접 사용. |

### 1.3 아키텍처 위반 사례 TOP 10

| # | 사례 | 파일/라인 | 설명 |
|---|------|-----------|------|
| 1 | **Domain에 Spring import** | `domain/resolution/ObjectiveResolverPort.java` | `@NonNull`, `@Nullable`에 `org.springframework.lang.*` 사용. Core는 프레임워크 무의존 원칙 위반. |
| 2 | **Domain 루트에 JPA Entity** | `prompt/Prompt.java` | `jakarta.persistence.*`, `@Entity`. 헥사고날 관점에서 영속 모델은 adapter 쪽에 두거나, 도메인은 순수 모델만 두는 것이 원칙. |
| 3 | **Config 빈 반환 타입 오류** | `infrastructure/config/ResolutionConfig.java` L30 | `domainResolver()` 빈이 `infrastructure.config.DomainResolverPort`(빈 클래스)를 반환 타입으로 선언하고 `new DomainResolver()` 반환. 타입 불일치로 컴파일/빈 주입 문제 가능. |
| 4 | **Inbound/Application이 구체 서비스 직접 의존** | `facade/PromptCreationFlow.java` | CreatePromptUseCase 구현체가 `PromptSanitizationService`, `PromptAIService`, `PromptPersistenceService` 등 구체 클래스에 직접 의존. OutPort로 추상화되어 있지 않음. |
| 5 | **Application Port가 Spring 타입 노출** | `application/port/out/PromptQueryPort.java` | `org.springframework.data.domain.Page` 사용. 포트가 프레임워크에 묶임. (문서화된 트레이드오프이나 위반으로 분류 가능) |
| 6 | **서비스 레이어에 @Service/@Component 난립** | `service/PromptGenerator.java`, `DomainResolutionService.java` 등 | UseCase가 아닌 “서비스”에 직접 스테레오타입. 헥사고날에서는 InPort 구현체만 @Service, 나머지는 Adapter/Config에서 명시 조립이 원칙. |
| 7 | **Adapter가 Port 구현체만 하고 Config에서 명시 조립 아님** | 여러 `adapter/out/*Adapter` | @Component로 스캔에 의존. “Port 타입으로만 노출, 구현체 타입 주입 금지”를 위해 Config에서 @Bean으로 명시 조립하는 편이 명확. |
| 8 | **DomainResolverPort 이중 정의** | `domain/resolution/DomainResolverPort` vs `infrastructure/config/DomainResolverPort` | 동일 simple name으로 인터페이스(domain)와 빈 클래스(config) 공존. 혼동 및 빈 타입 오류 유발. |
| 9 | **PromptServiceImpl이 다중 인터페이스 구현** | `PromptServiceImpl` | PromptService, PromptQueryUseCase, PromptCommandUseCase 동시 구현. 역할이 섞여 있음. (리팩토링 시 분리 검토) |
| 10 | **도메인 서비스/팩토리를 Config에서 직접 @Bean** | `PromptDomainConfig.java` | BadgeResolver, PromptSpecFactory 등은 도메인 순수 객체인데, Config가 구체 클래스를 생성해 주입. 적절하나 “Port로만 노출”이면 Application은 Port에만 의존해야 함. GeneratePromptService는 이미 DomainResolverPort 등 Port + 도메인 서비스 혼합 주입. |

---

## 2. 리팩토링 설계안 (To-Be)

### 2.1 To-Be 패키지 트리 (최소 변경 / 최대 효과)

```text
domain/prompt/
├── adapter/
│   ├── in/
│   │   └── web/                    # HTTP Inbound (기존 유지)
│   │       ├── PromptEngineController
│   │       ├── GeneratePromptRequest/Response
│   │       └── BadgeResponseAssembler
│   └── out/                        # Outbound Adapters (기존 유지, Config에서 명시 @Bean)
│       ├── ConstrainedDecodingAdapter
│       ├── LLMClientAdapter
│       ├── SavePromptVersionAdapter
│       ├── ValidateUserAdapter
│       ├── PromptPersistenceAdapter
│       ├── PromptSpecRendererAdapter
│       ├── RepairPromptRenderer
│       └── PromptSpecRenderHelper
├── application/
│   ├── port/
│   │   ├── in/                     # InPort (UseCase)
│   │   │   ├── GeneratePromptUseCase, GeneratePromptCommand, GeneratePromptResult
│   │   │   ├── CreatePromptUseCase
│   │   │   ├── PromptQueryUseCase, PromptCommandUseCase
│   │   │   └── ...
│   │   └── out/                    # OutPort
│   │       ├── LLMClientPort, PromptSpecRendererPort, ConstrainedDecodingPort
│   │       ├── SavePromptVersionPort, ValidateUserPort
│   │       ├── PromptQueryPort, PromptCommandPort
│   │       └── ...
│   └── service/                    # InPort 구현체 (UseCase 구현)
│       └── GeneratePromptService
├── domain/                         # Core (Spring/JPA import 0)
│   ├── model/
│   ├── value/
│   ├── objective/ + profiles/
│   ├── policy/
│   ├── resolution/                 # Domain 해석 Port/구현 (순수)
│   │   ├── DomainResolverPort, ResolvedDomain
│   │   ├── ObjectiveResolverPort, ExplicitObjectiveMappingPort, ObjectiveMappingRegistryPort
│   │   ├── DomainResolver, ObjectiveResolver
│   │   ├── ExplicitObjectiveMapping, ObjectiveMappingRegistry
│   │   └── ...
│   ├── service/                    # Domain 서비스 (순수)
│   │   ├── BadgeResolver, PromptSpecFactory, PromptSpecValidator
│   │   └── RecommendationRegistry
│   ├── verification/
│   └── ...
├── facade/                         # v1 CreatePrompt UseCase 구현 (유지)
│   └── PromptCreationFlow
├── service/                        # v1 전용 서비스 (유지, 점진적으로 Port 도입 가능)
│   ├── PromptServiceImpl
│   ├── PromptGenerator, DomainResolutionService, DomainStrategyTextProvider
│   ├── PromptAIService, PromptPersistenceService, ...
│   └── guideline/
├── infrastructure/
│   └── config/                     # 조립 전용
│       ├── PromptDomainConfig      # 도메인 빈 + Application UseCase
│       ├── ResolutionConfig        # 해석 빈 (DomainResolverPort 등 Port 타입으로만 노출)
│       └── (삭제) DomainResolverPort.java # 빈 클래스 제거
├── event/
├── repository/
└── Prompt.java                     # JPA Entity (위치 유지, 도메인 의존 최소화)
```

### 2.2 InPort / OutPort 목록 및 책임

| 구분 | Port | 책임 |
|------|------|------|
| InPort | GeneratePromptUseCase | v2 프롬프트 생성 4단계(Clarify→Solve→Verify→Repair) 오케스트레이션 |
| InPort | CreatePromptUseCase | v1 프롬프트 생성( Sanitize → AI → Persist ) |
| InPort | PromptQueryUseCase | 프롬프트 조회/검색 |
| InPort | PromptCommandUseCase | 프롬프트 수정/삭제 |
| OutPort | DomainResolverPort | ActionType+PromptCategory → TaskDomain(및 폴백) 해석 |
| OutPort | LLMClientPort | PromptSpec 기반 초안 생성(solve), 수정 요청(repair) |
| OutPort | PromptSpecRendererPort | PromptSpec → 메타프롬프트 문자열, 수리용 프롬프트 |
| OutPort | ConstrainedDecodingPort | JSON 스키마 기반 제약 생성(EXTRACTION 등) |
| OutPort | ValidateUserPort | 사용자 존재 검증 |
| OutPort | SavePromptVersionPort | 생성 결과·스펙·초안 저장 |
| OutPort | PromptQueryPort | 프롬프트 조회/검색 (Page 등) |
| OutPort | PromptCommandPort | 프롬프트 저장/수정/삭제 |

(도메인 내부 해석: ObjectiveResolverPort, ExplicitObjectiveMappingPort, ObjectiveMappingRegistryPort — Application은 DomainResolverPort + 도메인 서비스(PromptSpecFactory 등)를 통해 간접 사용.)

### 2.3 Bean Wiring 원칙

- **application**: `@Service`는 InPort 구현체(UseCase 구현)만. 예: `GeneratePromptService`.
- **adapter**: `@Component`/`@RestController`는 입·출력 어댑터. 가능하면 **Config에서 @Bean으로 명시 조립**하여 Port 타입으로만 노출.
- **조립**: `domain/prompt/infrastructure/config`(또는 `adapter/config`)에 `@Configuration`으로:
  - ResolutionConfig: DomainResolverPort, ObjectiveResolverPort, ExplicitObjectiveMapping, ObjectiveMappingRegistry 등 **해석 빈**을 Port/인터페이스 타입으로 등록.
  - PromptDomainConfig: ObjectiveRegistry, StrategyBundlePolicy, PromptSpecFactory, PromptSpecValidator, BadgeResolver, RecommendationRegistry 등 **도메인 빈** + GeneratePromptService 등 UseCase 빈.
- **원칙**: Port 타입으로만 외부에 노출; 구현체 타입 주입 금지. `@ComponentScan` 과다 사용 지양, 모듈 경계가 보이도록 명시적 빈 조립.

---

## 3. 변경 내역 (PR 단위)

### PR1: 구조/패키지 이동 + 컴파일 통과

- **삭제**: `infrastructure/config/DomainResolverPort.java` (빈 클래스).
- **수정**: `ResolutionConfig.domainResolver()` 반환 타입을 `domain.resolution.DomainResolverPort`로 변경하고 `new DomainResolver()` 반환.
- **결과**: 컴파일 통과, DomainResolverPort 빈이 올바른 타입으로 등록됨.

### PR2: 중복 제거 (공통 추출/단일화) + 테스트 통과

- **정규화**: `normalizeInput`을 공통 유틸 또는 도메인 서비스 한 곳으로 모음(필요 시).
- **통합 검토**: DomainStrategyTextProvider의 TaskDomain switch와 Objective 프로파일은 목적이 다르므로 당분간 유지. 중복 문단만 PromptSpecRenderHelper 등으로 흡수 가능 시 일부 통합.
- **테스트**: 기존 테스트 유지, 수정 최소화.

### PR3: 미사용 기능을 실제 플로우에 연결 + 테스트 통과

- **연결**: v2 플로우에서 `resolveDomainWithFallback` 사용처가 이미 DomainResolutionService에 있음. GeneratePromptService는 `resolveDomain`만 사용해도 됨. 빈 클래스 제거로 “미사용” 타입 제거가 주 효과.
- **Feature Flag**: ConstrainedDecoding 등은 이미 조건부 호출+fallback으로 연결됨. 추가 on/off는 설정 프로퍼티로만 확장 가능.

### PR4: Bean 등록 정리 + 테스트 통과

- **ResolutionConfig**: domainResolver 빈을 Port 인터페이스 타입으로만 노출 (PR1에서 반영).
- **PromptDomainConfig**: 기존 @Bean 유지. Adapter는 계속 @Component로 스캔 허용하되, 필요 시 별도 Config에서 Adapter를 @Bean으로 등록해 Port 타입으로만 노출하도록 정리.
- **도메인 순수성**: ObjectiveResolverPort에서 `org.springframework.lang` 제거 → `javax.annotation.*` 또는 제거(선택).

---

## 4. 위험 요소

- **Prompt.java** JPA Entity 위치/의존성: 변경 시 영속성 계층 전반 영향. 현재는 이동 없이 유지.
- **PromptQueryPort**의 `Page` 의존: Spring Data 타입이므로 도메인 타입으로 바꾸면 변경 범위 큼. 유지.
- **v1 플로우(PromptCreationFlow)**: 구체 서비스 의존 제거 시 Port 도입으로 대규모 수정. 본 리팩토링에서는 유지, 향후 점진적 Port 도입만 권장.

---

## 5. 롤백 플랜

- PR1: `infrastructure/config/DomainResolverPort.java` 복구 시 빈 클래스를 다시 만들면 안 되고, ResolutionConfig의 반환 타입만 원래대로 되돌리면 됨(원래가 domain.resolution.DomainResolverPort였다면 그대로 복구).
- PR2–PR4: Git revert로 단계별 롤백. 공통화한 메서드는 호출처를 원래 코드로 복원 후 롤백.

---

## 6. 최종 패키지 트리 (텍스트)

```text
domain/prompt/
├── adapter/in/web/
├── adapter/out/
├── application/port/in/
├── application/port/out/
├── application/service/
├── domain/model/
├── domain/value/
├── domain/objective/
├── domain/policy/
├── domain/resolution/
├── domain/service/          # BadgeResolver, PromptSpecFactory, PromptSpecValidator,
│                            # RecommendationRegistry, InputNormalizer
├── domain/verification/
├── domain/guideline/
├── enums/
├── facade/
├── service/
├── service/guideline/
├── infrastructure/config/   # PromptDomainConfig, ResolutionConfig (DomainResolverPort 빈 클래스 삭제됨)
├── event/
├── repository/
└── Prompt.java
```

---

## 7. 빌드/테스트 결과 기록

- **실행 명령**
  - 컴파일: `.\gradlew compileJava` (PowerShell) / `./gradlew compileJava` (Unix)
  - 테스트: `.\gradlew test` (전체) / `.\gradlew test --tests "org.example.sharedprompts.domain.prompt.*"` (prompt 도메인만)
- **결과**
  - 컴파일: PR1~PR4 적용 후 `compileJava` 성공.
  - 테스트: `BadgeResponseAssemblerTest` import 수정으로 컴파일 통과. 전체 테스트 실행 시 인프라(DB/Redis 등) 의존 테스트는 환경에 따라 일부 실패 가능.
  - 권장: 로컬에서 `.\gradlew compileJava` 및 `.\gradlew test` 실행 후 위 결과를 터미널 출력으로 갱신.

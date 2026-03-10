# Prompt Application Layer: 폴더 구조 및 SOLID 분석

**대상 경로:** `src/main/java/org/example/sharedprompts/domain/prompt/application`  
**분석 일자:** 2025-03-10

---

## 1. 폴더 구조 상세

### 1.1 전체 디렉터리 트리

```
application/
├── exception/                          # 도메인/애플리케이션 예외
│   ├── PromptDomainException.java      # 도메인 예외 베이스
│   ├── PromptNotFoundException.java
│   ├── PromptAccessDeniedException.java
│   ├── InvalidPromptUpdateException.java
│   ├── UnsupportedQualityPipelineOptionException.java
│   └── SemanticResolutionException.java
│
├── mapping/                            # DTO/도메인 변환
│   └── ConfirmedAxesMapper.java
│
├── port/
│   ├── in/                             # 입 port (유즈케이스, 명령/쿼리, DTO)
│   │   ├── GeneratePromptUseCase.java
│   │   ├── GenerateUnifiedPromptUseCase.java
│   │   ├── GeneratePromptFromConfirmedAxesUseCase.java
│   │   ├── RecommendPromptAxesUseCase.java
│   │   ├── command/                    # 명령 DTO 및 명령 유즈케이스
│   │   │   ├── GeneratePromptCommand.java
│   │   │   ├── UnifiedGeneratePromptCommand.java
│   │   │   ├── ConfirmedGeneratePromptCommand.java
│   │   │   ├── RecommendPromptCommand.java
│   │   │   ├── UpdatePromptCommand.java
│   │   │   ├── DeletePromptCommand.java
│   │   │   ├── PromptCommandUseCase.java
│   │   │   └── normalization/          # 정규화 옵션 (SemanticSelection, OutputOptions, ExpressionOptions)
│   │   └── query/                      # 쿼리 DTO, 결과/뷰 모델
│   │       ├── SearchPromptsQuery.java
│   │       ├── GeneratePromptResult.java
│   │       ├── UnifiedGeneratePromptResult.java
│   │       ├── RecommendPromptResult.java
│   │       ├── PromptPageResult.java
│   │       ├── PromptSummaryView.java
│   │       ├── PromptDetailView.java
│   │       ├── PromptQueryUseCase.java
│   │       └── ...
│   │
│   └── out/                            # 출 port (외부 연동)
│       ├── persistence/
│       │   ├── PromptCommandPort.java
│       │   ├── PromptQueryPort.java
│       │   ├── PromptSearchQuery.java
│       │   └── SavePromptVersionPort.java
│       └── llm/
│           ├── LLMClientPort.java
│           └── ConstrainedDecodingPort.java
│
└── service/                            # 유즈케이스 구현 및 내부 서비스
    ├── descriptor/
    │   └── DefaultRoleDescriptorProvider.java   # RoleDescriptorPort 구현
    ├── generate/
    │   ├── GeneratePromptService.java          # GeneratePromptUseCase 구현
    │   └── GeneratePromptFromConfirmedAxesService.java  # GeneratePromptFromConfirmedAxesUseCase 구현
    ├── guideline/
    │   ├── GuidelineRenderer.java              # 인터페이스
    │   ├── AbstractGuidelineRenderer.java
    │   ├── KoreanGuidelineRenderer.java
    │   ├── EnglishGuidelineRenderer.java
    │   ├── JapaneseGuidelineRenderer.java
    │   └── GuidelineRendererFactory.java
    ├── orchestration/
    │   ├── UnifiedPromptGenerationOrchestrator.java   # GenerateUnifiedPromptUseCase 구현
    │   ├── PromptServiceImpl.java                     # PromptQueryUseCase + PromptCommandUseCase 구현
    │   ├── SchemaContractEvaluator.java
    │   ├── PromptUpdateValidator.java
    │   ├── UpdatePromptPayload.java
    │   ├── DomainResolutionService.java
    │   └── legacy/                                    # Deprecated
    │       ├── UnifiedRoutingFacade.java
    │       ├── UnifiedRoutingPolicy.java
    │       ├── RoutingRuleEngine.java
    │       ├── IntentDefaultsResolver.java
    │       ├── IntentDefaults.java
    │       ├── DomainFinalizer.java
    │       ├── FinalDomainDecision.java
    │       ├── OutputContractPlanner.java
    │       ├── ContractDecision.java
    │       └── ...
    ├── semantic/
    │   ├── SemanticResolutionService.java
    │   ├── SemanticValidationService.java
    │   ├── SemanticRecommendationService.java
    │   └── RecommendPromptAxesService.java           # RecommendPromptAxesUseCase 구현
    ├── strategy/
    │   └── DomainStrategyTextProvider.java
    └── usage/
        ├── PromptUsageCountService.java
        └── PromptUsageCountServiceImpl.java
```

### 1.2 패키지별 역할 요약

| 패키지 | 역할 | 비고 |
|--------|------|------|
| `exception` | 프롬프트 도메인/애플리케이션 예외 정의 | `PromptDomainException` 상속 계층 |
| `mapping` | 요청/도메인 간 변환 | `ConfirmedAxesMapper` 등 |
| `port.in` | 유즈케이스 인터페이스, Command/Query, Result/View | 헥사고날 “입” 포트 |
| `port.out` | 영속성·LLM 등 외부 연동 추상화 | 헥사고날 “출” 포트 |
| `service.descriptor` | 역할 설명 제공 (port 구현) | 도메인 `RoleDescriptorPort` 구현 |
| `service.generate` | 프롬프트 생성 파이프라인 (Solve → Verify → Repair) | UseCase 구현체 |
| `service.guideline` | 언어별 가이드라인 렌더링 | 전략 패턴 + 팩토리 |
| `service.orchestration` | 통합 생성 오케스트레이션 + **CRUD 구현** + 레거시 | 책임 혼재 구간 |
| `service.semantic` | 의미 해석, 검증, 추천 | SemanticResolution 진입점 |
| `service.strategy` | 도메인 전략 텍스트 제공 | |
| `service.usage` | 조회수 등 사용량 처리 | Redis 연동 |

---

## 2. SOLID 원칙별 상세 분석

### 2.1 SRP (Single Responsibility Principle)

> 한 클래스는 하나의 변경 이유만 가져야 한다.

#### 2.1.1 위반: `PromptServiceImpl`

**위치:** `service/orchestration/PromptServiceImpl.java`

**담당하는 일:**

1. **조회**
   - `searchPrompts` — 검색 조건으로 페이지 조회
   - `getPrompts` — 공개/검색 목록
   - `getPromptDetail` — 단건 상세 (권한·차단·태그·좋아요·조회 이벤트)
   - `getMyPrompts` — 내 프롬프트 목록
   - `getUserPrompts` — 특정 사용자 프롬프트 목록
2. **명령**
   - `updatePrompt` — 제목/설명/공개/내용/태그 수정, 검증, 저장
   - `deletePrompt` — 태그 제거, 엔티티 삭제, 삭제 이벤트 발행
3. **부가 로직**
   - 권한 검사 (작성자 일치, 비공개 시 본인만)
   - `FollowBlockPolicy`로 차단 관계 검사
   - `PromptTagService`로 태그 조회/갱신
   - `LikeCountService`로 좋아요 수 조회
   - `PromptEventPublisher`로 조회/삭제 이벤트 발행
   - `PromptUpdateValidator`로 수정 요청 검증
   - `Page<Prompt>` → `PromptPageResult<PromptSummaryView>` 매핑

**문제점:**

- 하나의 클래스가 **조회 유즈케이스 전체**와 **수정/삭제 유즈케이스 전체**를 구현하고, 그 과정에서 **다른 도메인(follow, like, tag, event)** 과 인프라(트랜잭션, 이벤트)까지 다룸.
- “프롬프트 목록 조회 로직 변경”, “수정 검증 규칙 변경”, “이벤트 발행 방식 변경”, “태그 연동 방식 변경” 등 **서로 다른 변경 이유**가 한 클래스에 모여 있음.

**개선 방향:**

- **Query / Command 분리**
  - `PromptQueryUseCase` 구현체: 조회 전용 (검색, 상세, 내글, 타인글). 필요 시 `PromptDetailAssembler` 등으로 “상세 뷰 조립(태그·좋아요·권한)”만 분리.
  - `PromptCommandUseCase` 구현체: 수정·삭제 전용. 검증·태그·이벤트는 위 구현체 내부 또는 작은 헬퍼로 한정.
- **다른 도메인 연동**
  - “차단 여부”, “태그 조회/갱신”, “좋아요 수”, “이벤트 발행”을 **application port.out** 으로 정의하고, adapter에서 `FollowBlockPolicy`, `PromptTagService`, `LikeCountService`, `PromptEventPublisher`를 주입해 구현하면, application 쪽 책임이 “오케스트레이션만”으로 줄어듦.

---

#### 2.1.2 위반: `service/orchestration` 패키지 책임 혼재

**한 패키지에 모여 있는 것:**

| 클래스 | 성격 |
|--------|------|
| `UnifiedPromptGenerationOrchestrator` | 통합 프롬프트 생성 오케스트레이션 |
| `PromptServiceImpl` | 프롬프트 CRUD (조회/수정/삭제) |
| `SchemaContractEvaluator` | 스키마·출력 계약 평가 |
| `PromptUpdateValidator` | 수정 요청 검증·정규화 |
| `UpdatePromptPayload` | 수정용 내부 DTO |
| `DomainResolutionService` | 도메인 해석 |
| `legacy/*` | Deprecated 라우팅 파이프라인 |

**문제점:**

- “오케스트레이션”이라는 이름과 달리 **CRUD 구현체**, **검증**, **내부 DTO**, **레거시**가 한곳에 있어, 패키지 단위로 봤을 때 **단일 책임**을 갖지 않음.

**개선 방향:**

- CRUD 구현체: `service.crud` 또는 `service.command` 패키지로 이동 (예: `PromptQueryServiceImpl`, `PromptCommandServiceImpl`).
- 검증·페이로드: `service.validation` 또는 orchestration 하위의 `validation` 등으로 분리.
- `SchemaContractEvaluator`, `DomainResolutionService`는 “생성 파이프라인”과 더 가깝다면 `service.generate` 또는 `service.orchestration` 중 한쪽으로 명확히 몰아서 경계 정리.

---

#### 2.1.3 경미한 이슈: `UnifiedPromptGenerationOrchestrator`

**위치:** `service/orchestration/UnifiedPromptGenerationOrchestrator.java`

**하는 일:**

- `SemanticResolutionService.resolve` 호출
- `GeneratePromptUseCase.generate` 호출 (Command 변환 포함)
- `SchemaContractEvaluator.evaluate` 호출
- `PromptEngineMetrics` 기록
- `UnifiedGeneratePromptResult` 생성 (summary, axis_sources 맵 포함)

**판단:**

- “통합 생성 오케스트레이션”이라는 하나의 유즈케이스에 집중되어 있어 SRP 위반은 크지 않음.
- 다만 **메트릭 기록**과 **axis_sources 맵 빌드**를 별 컴포넌트로 빼면, “오케스트레이션만” 담당하는 형태로 더 단순해질 수 있음 (선택).

---

### 2.2 OCP (Open/Closed Principle)

> 확장에는 열려 있고, 수정에는 닫혀 있어야 한다.

#### 2.2.1 위반: `GuidelineRendererFactory`

**위치:** `service/guideline/GuidelineRendererFactory.java`

**현재 코드 (요지):**

```java
@Component
@RequiredArgsConstructor
public class GuidelineRendererFactory {
    private final KoreanGuidelineRenderer koreanRenderer;
    private final EnglishGuidelineRenderer englishRenderer;
    private final JapaneseGuidelineRenderer japaneseRenderer;

    public GuidelineRenderer getRenderer(LanguageType languageType) {
        return switch (languageType) {
            case KOREAN -> koreanRenderer;
            case ENGLISH -> englishRenderer;
            case JAPANESE -> japaneseRenderer;
        };
    }
}
```

**문제점:**

- 새 `LanguageType`(예: CHINESE) 추가 시 **이 클래스를 반드시 수정**해야 함. 즉, 확장 시 기존 코드 수정이 필요해 OCP 위반.

**개선 방향:**

- `LanguageType` → `GuidelineRenderer` 매핑을 **외부에서 주입**하거나, `GuidelineRenderer`를 구현한 빈들을 `List`/`Map`으로 수집해 `LanguageType`으로 조회하도록 변경.
- 예: `Map<LanguageType, GuidelineRenderer>`를 생성자로 받거나, `@PostConstruct`에서 `List<GuidelineRenderer>`를 순회하며 “언어 타입을 반환하는 메서드”로 맵 구성.

---

#### 2.2.2 참고: 레거시 `UnifiedRoutingFacade`

**위치:** `service/orchestration/legacy/UnifiedRoutingFacade.java`

- 이미 `@Deprecated`. 파이프라인 단계가 하드코딩되어 있어, 단계 추가 시 Facade 수정이 필요하나, 제거 예정이므로 OCP 개선보다는 제거 일정 관리가 우선.

---

### 2.3 LSP (Liskov Substitution Principle)

> 하위 타입은 상위 타입을 대체해도 프로그램의 올바름이 깨지면 안 된다.

#### 2.3.1 준수: GuidelineRenderer 계층

- `AbstractGuidelineRenderer` 서브클래스(`KoreanGuidelineRenderer`, `EnglishGuidelineRenderer`, `JapaneseGuidelineRenderer`)는 모두 `GuidelineRenderer` 인터페이스 계약(반환 형식, null 처리 등)을 유지.
- 클라이언트는 `GuidelineRenderer`만 의존하므로, 어떤 언어 구현체로 교체해도 동작이 깨지지 않아 LSP를 만족한다고 볼 수 있음.

#### 2.3.2 참고: PromptServiceImpl

- `PromptQueryUseCase`와 `PromptCommandUseCase`를 동시에 구현. 각 인터페이스만으로 사용할 때는 하위 타입으로 대체 가능하므로 LSP 자체는 문제 없음. 다만 **구현체 하나가 지나치게 많은 책임**을 가진 점은 SRP 이슈.

---

### 2.4 ISP (Interface Segregation Principle)

> 클라이언트는 사용하지 않는 메서드에 의존하지 않아야 한다.

#### 2.4.1 현재 상태

- **PromptQueryUseCase**: `getPrompts`, `getPromptDetail`, `getMyPrompts`, `getUserPrompts` — 조회 전용.
- **PromptCommandUseCase**: `updatePrompt`, `deletePrompt` — 명령 전용.
- 두 인터페이스가 분리되어 있어, “조회만 하는” 클라이언트는 Command 메서드를 보지 않아도 됨. **ISP는 잘 지켜진 편.**

#### 2.4.2 참고

- 단, 두 인터페이스를 **한 구현체(`PromptServiceImpl`)가 동시에 구현**하고 있어, 구현체가 비대해지는 문제는 SRP에서 다룬 것과 동일.

---

### 2.5 DIP (Dependency Inversion Principle)

> 상위 모듈은 하위 모듈에 의존하지 않고, 둘 다 추상화에 의존해야 한다.

#### 2.5.1 위반: `UnifiedPromptGenerationOrchestrator`

**위치:** `service/orchestration/UnifiedPromptGenerationOrchestrator.java`

**의존성:**

| 의존 대상 | 타입 | 비고 |
|-----------|------|------|
| `GeneratePromptUseCase` | port (인터페이스) | ✅ 추상화에 의존 |
| `SemanticResolutionService` | 구체 클래스 | ❌ application 서비스 구현체에 직접 의존 |
| `SchemaContractEvaluator` | 구체 클래스 | ❌ 직접 의존 |
| `PromptEngineMetrics` | 구체 클래스 | ❌ 직접 의존 |

**문제점:**

- 오케스트레이터가 “의미 해석”, “스키마 평가”, “메트릭 기록”을 **구체 클래스**에 의존해, 테스트 시 목 처리나 교체가 포트를 통하지 않고 어렵고, 상위 모듈이 하위 구현에 묶임.

**개선 방향:**

- `SemanticResolutionPort`(또는 `ResolveSemanticAxesUseCase` 등) 인터페이스를 `port.in` 또는 `port.out`에 두고, `SemanticResolutionService`가 이를 구현하도록 함. 오케스트레이터는 해당 포트만 의존.
- `SchemaContractEvaluator` 역시 `SchemaContractEvaluationPort` 같은 작은 인터페이스로 추상화하면, 오케스트레이터는 추상화에만 의존하게 됨.
- `PromptEngineMetrics`는 이미 “기록” 역할이므로, `MetricsPort` 또는 `PromptGenerationMetricsPort` 형태로 추상화 후 구현체를 adapter 쪽에 두는 방안 검토.

---

#### 2.5.2 위반: `PromptServiceImpl`

**위치:** `service/orchestration/PromptServiceImpl.java`

**의존성:**

| 의존 대상 | 타입 | 비고 |
|-----------|------|------|
| `PromptQueryPort` | port (인터페이스) | ✅ |
| `PromptCommandPort` | port (인터페이스) | ✅ |
| `FollowBlockPolicy` | 다른 도메인 구체 클래스 | ❌ follow 도메인에 직접 의존 |
| `PromptTagService` | 다른 도메인 구체 클래스 | ❌ tag 도메인에 직접 의존 |
| `LikeCountService` | 다른 도메인 구체 클래스 | ❌ like 도메인에 직접 의존 |
| `PromptEventPublisher` | 이벤트 발행 구체 클래스 | ❌ 인프라/이벤트에 직접 의존 |
| `PromptUpdateValidator` | application 내부 구체 클래스 | △ (같은 레이어이지만 구체 클래스) |

**문제점:**

- application 계층이 **다른 도메인(follow, like, tag)** 과 **이벤트 발행**에 직접 의존해, 도메인 경계가 무너지고 테스트·교체가 어려움. DIP 위반이자 경계 침범.

**개선 방향:**

- **차단 여부 조회**: 예) `BlockCheckPort.isBlocked(viewerId, authorId)` — adapter에서 `FollowBlockPolicy` 사용.
- **태그 조회/갱신**: 예) `PromptTagPort.getTags(prompt)`, `PromptTagPort.updateTags(prompt, tagNames)` — adapter에서 `PromptTagService` 사용.
- **좋아요 수 조회**: 예) `LikeCountPort.getPromptLikeCounts(promptIds)` — adapter에서 `LikeCountService` 사용.
- **이벤트 발행**: 예) `PromptEventPort.publishViewed(promptId, viewerId)`, `publishDeleted(promptId, authorId)` — adapter에서 `PromptEventPublisher` 사용.

이렇게 하면 application은 “포트”에만 의존하고, follow/like/tag/event 쪽은 모두 adapter가 담당하게 되어 DIP와 도메인 경계가 맞춰짐.

---

### 2.6 SOLID 요약 표

| 원칙 | 심각도 | 대상 | 내용 |
|------|--------|------|------|
| **SRP** | 높음 | `PromptServiceImpl` | 조회·수정·삭제·권한·태그·좋아요·이벤트를 한 클래스에서 처리 |
| **SRP** | 중간 | `service.orchestration` 패키지 | 오케스트레이션 + CRUD 구현 + 검증 + 레거시가 한 패키지에 혼재 |
| **OCP** | 중간 | `GuidelineRendererFactory` | 언어 추가 시 switch 및 필드 수정 필요 |
| **LSP** | 낮음 | GuidelineRenderer 계층 | 대체 가능성 유지, 문제 없음 |
| **ISP** | 낮음 | Query/Command UseCase | 인터페이스가 역할별로 분리됨 |
| **DIP** | 중간 | `UnifiedPromptGenerationOrchestrator` | SemanticResolution, SchemaContract, Metrics를 구체 클래스에 의존 |
| **DIP** | 높음 | `PromptServiceImpl` | follow, like, tag, event를 구체 도메인/인프라에 직접 의존 |

---

## 3. 개선 작업 우선순위 제안

1. **높음 — PromptServiceImpl**
   - Query/Command 구현체 분리 (`PromptQueryServiceImpl`, `PromptCommandServiceImpl`).
   - follow, like, tag, event 연동을 port 도입으로 역전 (DIP + 경계 정리).

2. **높음 — orchestration 패키지**
   - CRUD 구현체를 `service.crud`(또는 `service.query`/`service.command`)로 이동.
   - 검증·페이로드는 별 패키지 또는 하위 패키지로 분리해 “오케스트레이션” 패키지 책임을 좁힘.

3. **중간 — UnifiedPromptGenerationOrchestrator**
   - SemanticResolution, SchemaContract, Metrics를 포트(인터페이스)로 추상화하고 오케스트레이터는 포트만 의존하도록 변경.

4. **중간 — GuidelineRendererFactory**
   - `LanguageType` → `GuidelineRenderer` 맵 주입 또는 자동 등록 방식으로 변경해, 언어 추가 시 팩토리 수정 없이 확장 가능하게 함.

5. **낮음 — 레거시**
   - `legacy` 패키지 제거 일정에 맞춰 제거하거나, 사용처가 없다면 조기 제거 검토.

---

## 4. 참고: 포트–구현체 매핑

| Port (interface) | 구현체 위치 |
|------------------|-------------|
| `GeneratePromptUseCase` | `service.generate.GeneratePromptService` |
| `GenerateUnifiedPromptUseCase` | `service.orchestration.UnifiedPromptGenerationOrchestrator` |
| `GeneratePromptFromConfirmedAxesUseCase` | `service.generate.GeneratePromptFromConfirmedAxesService` |
| `RecommendPromptAxesUseCase` | `service.semantic.RecommendPromptAxesService` |
| `PromptQueryUseCase` | `service.orchestration.PromptServiceImpl` |
| `PromptCommandUseCase` | `service.orchestration.PromptServiceImpl` |
| `RoleDescriptorPort` | `service.descriptor.DefaultRoleDescriptorProvider` |
| `PromptUsageCountService` | `service.usage.PromptUsageCountServiceImpl` |

이 문서는 리팩터링 시 참고용으로 사용할 수 있습니다.

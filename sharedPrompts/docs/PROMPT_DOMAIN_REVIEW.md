# Prompt 도메인 레이어 리뷰

`domain/prompt` 패키지의 **잘한 점**과 **아쉬운 점**을 정리한 문서입니다.

---

## 잘한 점

### 1. 헥사고날 아키텍처 준수

- **포트·어댑터 분리**: `application/port/in`, `application/port/out`으로 유즈케이스 경계가 명확함.
- **단일 진입점**: `GenerateUnifiedPromptUseCase` / `GeneratePromptUseCase`만 노출하고, 컨트롤러는 포트에만 의존.
- **도메인 순수성**: `DomainResolver`, `ResolvedDomain`, `PromptSpec` 등은 Spring/인프라 의존 없이 도메인 규칙만 담당.

### 2. 통합 엔진 설계 (Unified)

- **단일 API**: `/api/prompts/generate` 하나로 요청을 받고, 내부에서 Intent·EngineMode·Rule 기반 라우팅.
- **라우팅 파사드**: `UnifiedRoutingFacade`가 IntentDefaults → RoutingRuleEngine → DomainFinalizer → OutputContractPlanner 순으로 단계별 결정을 조합.
- **V2 파이프라인 재사용**: 기존 `GeneratePromptUseCase`(4단계 파이프라인)를 그대로 활용해 중복 없이 통합.

### 3. 도메인·API 계층 분리 (Objective)

- **이중 Objective 모델**: API/라우팅용 `common.enums.PromptObjective`와 도메인/스펙용 `domain.value.objective.PromptObjective`를 구분.
- **명시적 변환**: `toDomainObjective()`로 API → 도메인 변환 경로가 문서화되어 있고, DB/레거시와의 역할 구분이 잘 되어 있음.

### 4. 확장 가능한 설정

- **Objective 프로파일**: `ObjectiveProfile` 구현체를 `DefaultObjectiveRegistry`에 등록하는 방식으로 새 목적 추가 시 OCP 유지.
- **ResolutionConfig**: `DomainResolver`, `ObjectiveMappingRegistry` 등 해석 로직을 설정 클래스에서 조립해 도메인 서비스는 인터페이스에만 의존.

### 5. 품질·검증 파이프라인

- **GuidelineRuleChecker**: 전략 패턴으로 규칙 위반 검사 구현체 교체 가능.
- **SchemaContractEvaluator**: V2 결과의 품질 배지(FORMAT_VERIFIED 등)를 활용해 스키마/계약 준수 여부를 응답·메트릭에 반영.

### 6. 비동기·타임아웃 처리

- **WebAsyncTask**: 장시간 AI 생성에 대해 `ASYNC_TIMEOUT_MS` 기반 타임아웃과 `onTimeout`/`onError` 콜백으로 일관된 에러 응답 제공.

### 7. 요청·응답 설계

- **Record DTO**: `UnifiedGeneratePromptRequest`, `UnifiedGeneratePromptCommand` 등 불변 레코드로 계약이 명확함.
- **기본값·검증**: Command 생성 시 null/blank 검증과 안전한 기본값(category, intent, engineMode 등) 적용.

### 8. 메트릭·관찰성

- **PromptEngineMetrics**: 성공/실패, 레이턴시, repair 횟수, 스키마 계약 실패 여부를 기록해 운영 모니터링에 활용 가능.

---

## 아쉬운 점 (보완 반영)

### 1. 동일 이름·역할의 중복 클래스 — ✅ 보완

- **PromptObjective 이중 정의**: `docs/PROMPT_COMMON.md` 2.0절에 API용 vs 도메인용 구분 표와 변환 방법을 추가했고, 두 enum Javadoc에 상대편·문서 링크를 넣었다.
- **I18nRegistry 중복**: guideline 쪽 클래스를 `GuidelineI18nRegistry`로 이름 변경해 역할이 드러나도록 했다. (`common.i18n.I18nRegistry`는 그대로.)

### 2. SchemaContractEvaluator의 간접 추론 — ✅ 보완

- **변경**: `GeneratePromptResult`에 Verify 단계의 FORMAT_COMPLIANCE 루브릭 결과를 반영한 `formatValid` 필드를 추가했다. `SchemaContractEvaluator`는 이제 배지 추론 없이 `result.formatValid()`로 계약 실패 여부를 판단한다.

- **(구) 현재**: `FORMAT_VERIFIED` 배지 유무로 “스키마 계약 실패”를 추론함. 실제 Verify 단계의 `formatValid`/`schemaValid` 플래그를 쓰지 않음.
- **개선**: 주석에 “향후 Verify 결과에서 직접 전달받도록 점진 이관”이라고 되어 있으므로, 가능하면 Verify 결과 DTO에 스키마 준수 플래그를 추가하고 Evaluator가 이를 직접 사용하도록 리팩터링하는 것이 좋음.

### 3. UnifiedRoutingFacade의 switch 의존 — ✅ 보완

- **변경**: `ActionIntent`에 `getDefaultCoreRole()`를 추가하고, 각 상수에 기본 `CoreRoleType`을 부여했다. `UnifiedRoutingFacade`의 `defaultCoreRoleForIntent` switch는 제거하고 `defaults.intent().getDefaultCoreRole()`를 사용한다.

### 4. disableQualityPipeline 미구현 — ✅ 보완

- **변경**: `UnifiedGeneratePromptCommand.of()` 예외 메시지를 "준비 중" 및 "추후 지원 예정"으로 수정했고, `UnifiedGeneratePromptRequest`의 해당 필드 Javadoc에 "true 요청 시 400, 추후 지원 예정"을 명시했다. (구) **UnifiedGeneratePromptRequest**에는 `disable_quality_pipeline` 필드가 있으나, **UnifiedGeneratePromptCommand.of()**에서 `true`이면 `IllegalArgumentException`으로 막고 있음. “현재 미지원”이면 API 스펙에서 제거하거나, 문서/에러 메시지로 “준비 중”임을 명시하는 편이 좋음.

### 5. toV2Command의 합성 타이틀 — ✅ 보완

- **변경**: V2 파이프라인 내부 식별용이며, 클라이언트 노출용이면 API 응답의 별도 title 필드로 교체 가능하다는 주석을 `toV2Command` 메서드에 추가했다.

### 6. EngineMode와 실제 파이프라인 — ✅ 보완

- **변경**: `EngineMode`와 `EngineProfile` Javadoc에 "의미 및 로드맵"을 추가했다. AUTO/V2/V3 의미, 현재는 모든 요청이 V2로 수렴·V3 전용 경로는 추후 도입 예정임을 명시했다.

- **(구) mapProfileToEngineMode**: 현재는 모든 프로파일이 V2로 수렴하고, FAST_PIPELINE도 “V3 요청이 아니면 V2로 폴백”하는 형태. V3/Fast path가 아직 없어서 코드만 복잡해 보일 수 있음.
- **개선**: “AUTO/V2/V3” 의미를 한곳(예: EngineMode javadoc 또는 ADR)에 정리하고, “현재는 V2만 사용, V3는 추후” 같은 로드맵을 명시하면 이해하기 쉬움.

### 7. 도메인 이벤트·보상 처리 (보완)

- **Unified 플로우에서의 저장·이벤트**: 오케스트레이터(`UnifiedPromptGenerationOrchestrator`)는 `GeneratePromptUseCase.generate(v2Command)`만 호출한다. **DB 저장**은 V2 파이프라인 구현체인 `GeneratePromptService` 내부에서 `SavePromptVersionPort.save(command, spec, draft, ...)`로 수행된다. 즉, Unified 요청도 동일한 저장 포트를 통해 버전이 영속되며, 저장 후 이벤트는 해당 포트/어댑터 구현체에서 발행하는 구조이다. 삭제 시 이벤트(`publishPromptDeleted` 등)는 `PromptServiceImpl` 등 기존 서비스에 한정되며, 생성 플로우는 “저장 → (선택) 이벤트”가 V2 서비스와 그 아웃포트 구현에 위임되어 있음을 문서로 명시해 두었다.

### 8. 테스트·문서 — ✅ 보완

- **공통 용어**: `docs/PROMPT_COMMON.md` 9절에 V2, Unified, Intent, EngineMode, EngineProfile, Objective 등 공통 용어 정의 표를 추가했다. 도메인 핵심 단위 테스트는 기존 테스트 유지·추가는 별도 작업으로 진행하면 됨.

- **(구) 도메인 핵심**: `DomainResolver`, `UnifiedRoutingFacade`, `IntentDefaultsResolver` 등 규칙이 많은 부분에 대한 단위 테스트가 풍부하면 리팩터링 시 안전망이 됨.
- **공통 용어**: “V2”, “Unified”, “Intent”, “EngineProfile” 등이 문서(PROMPT_COMMON.md, PROMPT_DOMAIN_COMMON_REVIEW.md 등)에 정의되어 있으면 온보딩과 유지보수에 유리함.

---

## 배포가능성

### 잘 갖춰진 부분

| 항목 | 내용 |
|------|------|
| **배포 체크리스트** | `DEPLOYMENT_CHECKLIST.md`에 DB 마이그레이션, 환경 변수, OPERATIONS(로그/모니터링/백업), Outbox 설정이 정리되어 있음. |
| **기동 시 검증** | `EnvironmentValidator`가 prod 프로필에서 필수 환경 변수(JWT, DB, CORS, OAuth, **GOOGLE_GEMINI_API_KEY**, **SPRING_AI_OPENAI_API_KEY**) 및 더미 API 키 사용을 막고, JPA DDL·로깅 레벨 등 위험 설정을 검사함. 누락 시 **기동 실패**로 배포 전에 드러남. |
| **Actuator·Health** | `spring-boot-starter-actuator` 사용, `/actuator/health`(공개), metrics/prometheus(관리자/내부 제한). prod에서 health, metrics, prometheus 노출. DB·Redis·RabbitMQ·S3(해당 시) Health 포함. |
| **Graceful shutdown** | `server.shutdown: graceful`, `spring.lifecycle.timeout-per-shutdown-phase`(기본 30s)로 종료 시 진행 중 요청 마무리 가능. |
| **Docker** | `docker-compose.yml` 등에서 앱 헬스체크(`/api/actuator/health`) 기반 의존 서비스 순서·재시작 정책 사용. |
| **AI 호출 내성** | Gemini/OpenAI 등에 Circuit Breaker(failureRateThreshold, slowCallRateThreshold), 타임아웃(`GOOGLE_GEMINI_TIMEOUT_SECONDS` 등) 설정으로 장애 전파 완화. |
| **Rate Limit** | `rate-limit.promptCreate` 등으로 프롬프트 생성 호출 제한 가능. Redis 장애 시 fail-open 정책 옵션 있음. |
| **비동기·타임아웃** | Unified 컨트롤러가 `WebAsyncTask`(60초 타임아웃)로 장시간 AI 생성 요청을 비동기 처리하고, onTimeout/onError 시 일관된 에러 응답 반환. |

### 확인·보완 권장

| 항목 | 내용 |
|------|------|
| **타임아웃 정렬** | HTTP 레이어는 `UnifiedPromptEngineController`에서 **60초** 고정, 앱 설정은 `APP_PROMPT_CREATION_TIMEOUT_MS`(기본 **40초**). 내부 파이프라인 타임아웃이 먼저 나면 클라이언트는 60초까지 대기할 수 있음. 필요 시 컨트롤러 타임아웃을 설정값으로 주입하거나, 40초/60초 관계를 문서에 명시. |
| **DB 마이그레이션** | Flyway 미사용. Outbox·production_id·ShedLock 등 `db/migration/` SQL은 **수동 실행** 전제. 배포 절차에 “마이그레이션 실행 순서·담당”을 포함할 것. |
| **프로덕션 인프라** | `REDIS_HOST`/`RABBITMQ_HOST`가 localhost이면 경고만 하고 기동은 함. 실제 배포 시 외부 Redis/RabbitMQ 주소·자격 증명 적용 필요. |
| **Actuator 보안** | 체크리스트에 “운영에서는 health만 외부 노출, metrics/prometheus는 내부망·인증 제한” 권장이 있음. `SecurityPathConstants` 등에서 실제 경로 제한이 의도대로인지 배포 전 확인. |

### 정리

- **배포 가능**: 필수 환경 변수·프로필 검증, Health/메트릭, Graceful shutdown, Docker·헬스체크, AI 타임아웃·Circuit Breaker, Rate Limit이 갖춰져 있어 **prod 배포에 필요한 조건은 대체로 충족**함.
- **배포 전**: `DEPLOYMENT_CHECKLIST.md` 4가지(마이그레이션, 환경 변수, OPERATIONS, Outbox) 수행 여부와, 타임아웃/Actuator 경로 한 번 더 점검하면 안정적임.

---

## 요약

| 구분 | 내용 |
|------|------|
| **강점** | 헥사고날/포트 분리, 통합 엔진과 라우팅 파사드, Objective 이중 모델로 API·도메인 분리, 확장 가능한 Objective/Resolution 설정, 비동기·메트릭·검증 파이프라인 |
| **보완** | 동일 이름 클래스 정리(I18nRegistry, Objective 명칭), SchemaContractEvaluator의 직접 플래그 사용, Intent→CoreRole 매핑 전략화, disableQualityPipeline 스펙 정리, EngineMode/저장·이벤트 플로우 문서화 |
| **배포** | 체크리스트·EnvironmentValidator·Actuator·Graceful shutdown·Docker·AI 내성·Rate Limit으로 배포 가능성 확보. 타임아웃 정렬·수동 마이그레이션·인프라/Actuator 보안만 배포 전 점검하면 됨. |

전반적으로 **도메인 경계와 확장성**을 잘 고려한 구조이며, 위 보완 사항은 대부분 정리·문서화와 작은 리팩터링으로 개선 가능한 수준입니다.

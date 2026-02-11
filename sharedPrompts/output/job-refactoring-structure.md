# Job 처리 시스템 리팩토링 구조

## 최종 폴더 구조 (세분화 완료)

```
module/domain/production/service/job/
├── JobRecoveryScheduler.java            // 스케줄러: stale job 복구 스케줄링
├── JobStateService.java                 // (기존) Job 상태 관리
└── process/
    ├── JobProcessor.java                // orchestration만 담당
    ├── execution/
    │   ├── ai/
    │   │   ├── AIJobExecutor.java       // AI 호출 orchestration
    │   │   ├── AIResponseHandler.java   // AI 응답 파싱
    │   │   ├── builder/
    │   │   │   └── AIRequestBuilder.java // AI 요청 빌더
    │   │   ├── circuitbreaker/
    │   │   │   └── AICircuitBreakerWrapper.java // CircuitBreaker 래퍼
    │   │   ├── retry/
    │   │   │   ├── AIRetryExecutor.java  // Retry 실행 로직
    │   │   │   ├── AIRetryPolicy.java    // Retry 정책 (backoff 계산)
    │   │   │   └── AIErrorClassifier.java // 영구/일시 오류 분류
    │   │   └── token/
    │   │       └── TokenExtractor.java   // 토큰 사용량 추출
    │   └── content/
    │       ├── ContentRenderer.java      // ProductionRenderer 래퍼
    │       ├── ContentFormatter.java     // 포맷 변환 래퍼
    │       └── ContentStorageService.java // StorageStrategy 래퍼
    ├── recovery/
    │   ├── JobRecoveryService.java       // 복구 orchestration
    │   ├── AiCalledRecoveryService.java  // AI_CALLED 상태 복구
    │   ├── ParsedRecoveryService.java    // PARSED 상태 복구
    │   ├── RenderedRecoveryService.java // RENDERED 상태 복구
    │   └── StoredRecoveryService.java    // STORED 상태 복구
    ├── exception/
    │   ├── JobExceptionHandler.java      // Job 관련 Exception 처리
    │   ├── JobProcessingException.java   // (기존) 기본 예외
    │   ├── AIServiceException.java        // AI 호출/파싱 예외
    │   ├── ContentRenderException.java    // 렌더링/포맷 변환 예외
    │   ├── StorageException.java         // 저장 예외
    │   └── RecoveryException.java         // 복구 예외
    └── util/
        ├── CommandDeserializer.java      // Command 역직렬화
        ├── FileNameGenerator.java        // 파일명 생성
        └── ContentTypeDeterminer.java    // ContentType 판단
```

## 분석 결과

### 현재 JobProcessor의 책임 (SRP 위반)
1. ✅ AI 호출 (callAI, callAIWithRetry, callAIWithRetryInternal) → **AIJobExecutor로 분리**
2. ✅ AI 응답 파싱 (직접 호출) → **AIResponseHandler로 분리**
3. ✅ 렌더링 (직접 호출) → **ContentRenderer로 분리**
4. ✅ 포맷 변환 (직접 호출) → **ContentFormatter로 분리**
5. ✅ 저장 (직접 호출) → **ContentStorageService로 분리**
6. ✅ 상태 복구 (retryFromAiCalled, retryFromParsed, retryFromRendered, retryFromStored) → **JobRecoveryService로 분리**
7. ✅ Command deserialize → **JobUtils로 분리**
8. ✅ 파일명 생성 → **JobUtils로 분리**
9. ✅ ContentType 판단 → **JobUtils로 분리**
10. ✅ 예외 처리 (분산) → **JobExceptionHandler로 분리**

### 개선 포인트
- **SRP 준수**: 각 서비스가 단일 책임만 담당
- **테스트 용이성**: 각 서비스를 독립적으로 테스트 가능
- **재사용성**: AI 호출, 파싱, 렌더링 등을 다른 곳에서도 사용 가능
- **유지보수성**: 변경 영향 범위 최소화
- **예외 처리**: 중앙화된 예외 처리 전략

## 구현 완료 사항

### ✅ 생성된 파일들

1. **예외 클래스** (`exception/`)
   - `AIServiceException.java` - AI 호출/파싱 실패
   - `ContentRenderException.java` - 렌더링/포맷 변환 실패
   - `StorageException.java` - 저장 실패
   - `RecoveryException.java` - 복구 실패

2. **유틸리티** (`process/`)
   - `JobUtils.java` - Command deserialize, 파일명 생성, ContentType 판단

3. **서비스 클래스** (`process/`)
   - `AIJobExecutor.java` - AI 호출 + retry + CircuitBreaker
   - `AIResponseHandler.java` - AI 응답 파싱 래퍼
   - `ContentRenderer.java` - ProductionRenderer 래퍼
   - `ContentFormatter.java` - FormatConverterRegistry 래퍼
   - `ContentStorageService.java` - StorageStrategy 래퍼
   - `JobRecoveryService.java` - 상태별 복구 (AI_CALLED, PARSED, RENDERED, STORED)
   - `JobExceptionHandler.java` - 중앙화된 예외 처리
   - `JobProcessor.java` - **orchestration만 담당** (리팩토링 완료)

4. **스케줄러** (`job/`)
   - `JobRecoveryScheduler.java` - 기존 JobRecoveryService를 이름 변경

### 📋 각 서비스의 책임

| 서비스 | 책임 | 의존성 |
|--------|------|--------|
| `JobProcessor` | Orchestration (전체 플로우 조율) | 모든 process 서비스 |
| `AIJobExecutor` | AI 호출 + retry + CircuitBreaker | AIServiceRegistry, CircuitBreakerRegistry |
| `AIResponseHandler` | AI 응답 파싱 | AIResponseParser |
| `ContentRenderer` | 콘텐츠 렌더링 | RendererRegistry |
| `ContentFormatter` | 포맷 변환 | FormatConverterRegistry |
| `ContentStorageService` | 파일 저장 | StorageStrategyFactory |
| `JobRecoveryService` | 상태별 복구 로직 | 위 서비스들 |
| `JobExceptionHandler` | 예외 처리 및 메트릭 기록 | JobStateService, JobMetrics |
| `JobUtils` | 유틸리티 메서드 | ObjectMapper |

### 🔄 Job 처리 플로우

```
JobProcessor.processJobAsync()
  ├─> JobUtils.deserializeCommand()
  ├─> PromptTemplateService.mergePrompt()
  ├─> AIJobExecutor.execute()           // AI 호출 + retry + CircuitBreaker
  │     └─> AIService.generateContent()
  ├─> AIResponseHandler.parse()         // AI 응답 파싱
  ├─> ContentRenderer.render()          // 렌더링
  ├─> ContentFormatter.format()         // 포맷 변환
  ├─> ContentStorageService.store()     // 저장
  └─> JobStateService.markCompleted()
```

### 🔄 복구 플로우

```
JobRecoveryScheduler.recoverStaleJobs() (스케줄러)
  └─> JobProcessor.recoverJob()
       └─> JobRecoveryService.recoverFrom{Status}()
            ├─> AIResponseHandler.parse()      // (AI_CALLED, PARSED)
            ├─> ContentRenderer.render()        // (AI_CALLED, PARSED)
            ├─> ContentFormatter.format()       // (AI_CALLED, PARSED, RENDERED)
            └─> ContentStorageService.store()  // (AI_CALLED, PARSED, RENDERED)
```

### ✅ SRP 준수 확인

- ✅ **JobProcessor**: Orchestration만 담당 (570줄 → 127줄)
- ✅ **AIJobExecutor**: AI 호출 + retry + CircuitBreaker만 담당
- ✅ **AIResponseHandler**: AI 응답 파싱만 담당
- ✅ **ContentRenderer**: 렌더링만 담당
- ✅ **ContentFormatter**: 포맷 변환만 담당
- ✅ **ContentStorageService**: 저장만 담당
- ✅ **JobRecoveryService**: 상태별 복구만 담당
- ✅ **JobExceptionHandler**: 예외 처리만 담당
- ✅ **JobUtils**: 유틸리티 메서드만 담당


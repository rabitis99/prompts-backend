# 브랜치 전략 가이드

## 현재 변경사항 분석

현재 `dev` 브랜치에 많은 변경사항이 있습니다. 이를 논리적으로 분리하여 여러 브랜치로 나누어 관리하는 전략입니다.

## 변경사항 분류


**브랜치명**: `feature/production-validation`

**변경 파일**:
- `src/main/java/org/example/sharedprompts/module/domain/production/validation/DocumentCommandValidator.java` (신규)
- `src/main/java/org/example/sharedprompts/module/domain/production/validation/BlogCommandValidator.java` (신규)
- `src/main/java/org/example/sharedprompts/module/domain/production/validation/EmailCommandValidator.java` (신규)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/validator/DocumentValidator.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/validator/BlogValidator.java` (수정)

**설명**: Production 모듈에 Command Validator 패턴을 도입하여 검증 로직을 분리

---

### 2. Production 모듈 - AI 서비스 개선
**브랜치명**: `feature/production-ai-service`

**변경 파일**:
- `src/main/java/org/example/sharedprompts/module/domain/production/service/ai/text/GroqTextAiClient.java` (신규)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/ai/image/LeonardoImageAiClient.java` (신규)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/ai/AIServiceRegistry.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/ai/text/TextAIService.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/ai/image/ImageAIService.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/ai/config/` (신규 디렉토리)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/ai/exception/` (신규 디렉토리)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/ai/prompt/` (신규 디렉토리)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/ai/retry/` (신규 디렉토리)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/ai/strategy/` (신규 디렉토리)

**설명**: AI 서비스에 Groq, Leonardo 등 새로운 클라이언트 추가 및 구조 개선

---

### 3. Production 모듈 - Presigned URL 전략 패턴
**브랜치명**: `feature/production-presigned-strategy`

**변경 파일**:
- `src/main/java/org/example/sharedprompts/module/domain/production/service/production/presign/DocumentPresignedStrategy.java` (신규)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/production/presign/ImagePresignedStrategy.java` (신규)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/production/presign/PresignedStrategy.java` (신규)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/production/presign/PresignedStrategyResolver.java` (신규)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/production/presign/S3PresignedUrlGenerator.java` (수정)

**설명**: Presigned URL 생성 로직을 Strategy 패턴으로 리팩토링

---

### 4. Production 모듈 - Format Converter 및 Renderer
**브랜치명**: `feature/production-format-renderer`

**변경 파일**:
- `src/main/java/org/example/sharedprompts/module/domain/production/service/format/XmlFormatConverter.java` (신규)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/format/FormatConverterRegistry.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/format/PdfFormatConverter.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/renderer/ImageRenderer.java` (신규)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/renderer/TextRenderer.java` (신규)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/parser/ExtractingJsonParser.java` (신규)

**설명**: Format 변환 및 렌더링 기능 추가 및 개선

---

### 5. Production 모듈 - Application Service 및 DTO 개선
**브랜치명**: `feature/production-application-service`

**변경 파일**:
- `src/main/java/org/example/sharedprompts/module/domain/production/application/ArtifactApplicationService.java` (신규)
- `src/main/java/org/example/sharedprompts/module/domain/production/application/ProductionResultApplicationService.java` (수정)
- `src/main/java/org/example/sharedprompts/module/dto/response/production/ArtifactDetailResponseDto.java` (신규)
- `src/main/java/org/example/sharedprompts/module/dto/response/production/ArtifactSummaryDto.java` (신규)
- `src/main/java/org/example/sharedprompts/module/dto/response/production/ProductionResponseDto.java` (수정)
- `src/main/java/org/example/sharedprompts/module/dto/response/production/ProductionResponseDtoMapper.java` (수정)
- `src/main/java/org/example/sharedprompts/module/dto/response/production/FileArtifactDto.java` (수정)
- `src/main/java/org/example/sharedprompts/module/dto/response/production/ImageArtifactDto.java` (수정)

**설명**: Application Service 레이어 추가 및 DTO 구조 개선

---

### 6. Production 모듈 - Entity 및 Repository 개선
**브랜치명**: `feature/production-entity-improvement`

**변경 파일**:
- `src/main/java/org/example/sharedprompts/module/domain/production/entity/job/JobEntity.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/entity/production/ProductionArtifactEntity.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/entity/production/ProductionArtifactDetailEntity.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/repository/production/ProductionArtifactRepository.java` (수정)

**설명**: Entity 구조 개선 및 Repository 메서드 추가

---

### 7. Production 모듈 - Job Processing 개선
**브랜치명**: `feature/production-job-processing`

**변경 파일**:
- `src/main/java/org/example/sharedprompts/module/domain/production/service/job/JobEntityCreationService.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/job/JobStateService.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/job/process/JobProcessor.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/job/process/execution/ai/builder/AIRequestBuilder.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/job/process/execution/ai/retry/AIErrorClassifier.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/job/process/execution/ai/retry/AIRetryExecutor.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/job/queue/JobAsyncScheduler.java` (수정)

**설명**: Job 처리 로직 개선 및 에러 처리 강화

---

### 8. Production 모듈 - Storage 및 Config
**브랜치명**: `feature/production-storage-config`

**변경 파일**:
- `src/main/java/org/example/sharedprompts/module/domain/production/config/S3Config.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/config/condition/` (신규 디렉토리)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/storage/LocalStorageStrategy.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/storage/S3StorageStrategy.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/storage/StorageStrategyFactory.java` (수정)

**설명**: Storage 전략 개선 및 설정 파일 개선

---

### 9. Production 모듈 - Controller 및 기타 서비스
**브랜치명**: `feature/production-controller-service`

**변경 파일**:
- `src/main/java/org/example/sharedprompts/module/controller/production/ProductionResultController.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/production/ProductionArtifactService.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/production/ArtifactAccessServiceImpl.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/artifact/ImageArtifactHandler.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/image/JavaImageProcessor.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/image/ThumbnailService.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/service/parser/AIResponseParser.java` (수정)
- `src/main/java/org/example/sharedprompts/module/dto/request/production/ProductionRequest.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/model/ai/AIContentRequest.java` (수정)
- `src/main/java/org/example/sharedprompts/module/domain/production/model/contract/result/ArtifactType.java` (수정)
- `src/main/java/org/example/sharedprompts/module/exception/ModuleErrorCode.java` (수정)

**설명**: Controller 및 기타 서비스 로직 개선

---

### 10. Prompt 모듈 - Guideline 개선
**브랜치명**: `feature/prompt-guideline-improvement`

**변경 파일**:
- `src/main/java/org/example/sharedprompts/domain/prompt/guideline/AnalyticalGuidelines.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/guideline/CreativeGuidelines.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/guideline/DomainResolution.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/guideline/EducationalGuidelines.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/guideline/GeneralGuidelines.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/guideline/GuidelinePolicy.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/guideline/GuidelineRule.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/guideline/I18nText.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/guideline/PracticalGuidelines.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/guideline/RuleLevel.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/guideline/RuleType.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/guideline/TechnicalGuidelines.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/service/guideline/EnglishGuidelineRenderer.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/service/guideline/GuidelineRenderer.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/service/guideline/JapaneseGuidelineRenderer.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/service/guideline/KoreanGuidelineRenderer.java` (수정)

**설명**: Guideline 구조 개선 및 다국어 지원 강화

---

### 11. Prompt 모듈 - Controller 및 DTO
**브랜치명**: `feature/prompt-controller-dto`

**변경 파일**:
- `src/main/java/org/example/sharedprompts/controller/prompt/PromptMetadataController.java` (수정)
- `src/main/java/org/example/sharedprompts/dto/prompt/response/SimpleDomainResponseDto.java` (수정)
- `src/main/java/org/example/sharedprompts/dto/prompt/response/SimpleStyleResponseDto.java` (수정)
- `src/main/java/org/example/sharedprompts/dto/prompt/response/SimpleToneResponseDto.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/enums/StyleType.java` (수정)
- `src/main/java/org/example/sharedprompts/domain/prompt/enums/TaskDomain.java` (수정)
- `src/test/java/org/example/sharedprompts/domain/prompt/enums/TaskDomainTest.java` (수정)

**설명**: Prompt Controller 및 DTO 개선

---

### 12. Global 설정 및 기타
**브랜치명**: `feature/global-config-improvement`

**변경 파일**:
- `src/main/java/org/example/sharedprompts/global/config/async/security/SecurityContextTaskDecorator.java` (수정)
- `src/main/resources/application.yml` (수정)
- `env.example` (수정)
- `postman/Postman_Collection.json` (수정)

**설명**: 전역 설정 및 환경 변수 개선

---

## 브랜치 생성 및 푸시 절차

### 방법 1: Git Stash를 이용한 순차적 브랜치 생성

```bash
# 1. 현재 변경사항을 stash에 저장
git stash push -m "WIP: 모든 변경사항 임시 저장"

# 2. 각 기능별로 브랜치 생성 및 변경사항 적용
# 예시: Production Validation 기능
git checkout -b feature/production-validation
git stash pop
# 필요한 파일만 add하고 commit
git add src/main/java/org/example/sharedprompts/module/domain/production/validation/
git commit -m "feat: Production 모듈에 Command Validator 패턴 도입"
git push origin feature/production-validation

# 3. 다음 브랜치로 이동하여 반복
git checkout dev
git stash push -m "WIP: 남은 변경사항"
# ... 반복
```

### 방법 2: Interactive Staging을 이용한 선택적 커밋

```bash
# 1. 각 브랜치 생성
git checkout -b feature/production-validation

# 2. Interactive staging으로 필요한 파일만 선택
git add -p
# 또는 특정 파일만 add
git add src/main/java/org/example/sharedprompts/module/domain/production/validation/

# 3. 커밋 및 푸시
git commit -m "feat: Production 모듈에 Command Validator 패턴 도입"
git push origin feature/production-validation

# 4. 다음 브랜치로 이동
git checkout dev
git checkout -b feature/production-ai-service
# ... 반복
```

### 방법 3: 스크립트를 이용한 자동화 (권장)

아래 스크립트를 사용하여 각 브랜치를 자동으로 생성할 수 있습니다:

```bash
# 브랜치별 파일 목록을 정의한 스크립트 실행
# (각 브랜치에 해당하는 파일만 선택적으로 커밋)
```

---

## 브랜치 생성 스크립트

다음 PowerShell 스크립트를 사용하여 브랜치를 생성할 수 있습니다:

```powershell
# 브랜치별 파일 그룹 정의
$branches = @{
    "feature/production-validation" = @(
        "src/main/java/org/example/sharedprompts/module/domain/production/validation/"
    )
    "feature/production-ai-service" = @(
        "src/main/java/org/example/sharedprompts/module/domain/production/service/ai/"
    )
    # ... 나머지 브랜치 정의
}

# 각 브랜치 생성 및 푸시
foreach ($branch in $branches.Keys) {
    Write-Host "Creating branch: $branch"
    git checkout -b $branch
    git add $branches[$branch]
    git commit -m "feat: $branch 관련 변경사항"
    git push origin $branch
    git checkout dev
}
```

---

## 브랜치 병합 순서 권장사항

의존성을 고려한 병합 순서:

1. **기반 인프라** (먼저 병합)
   - `feature/production-entity-improvement`
   - `feature/production-storage-config`

2. **핵심 기능** (다음 병합)
   - `feature/production-validation`
   - `feature/production-ai-service`
   - `feature/production-job-processing`

3. **서비스 레이어** (그 다음)
   - `feature/production-application-service`
   - `feature/production-format-renderer`
   - `feature/production-presigned-strategy`

4. **API 및 최종 통합** (마지막)
   - `feature/production-controller-service`
   - `feature/prompt-guideline-improvement`
   - `feature/prompt-controller-dto`
   - `feature/global-config-improvement`

---

## 주의사항

1. **의존성 확인**: 각 브랜치 간 의존성을 확인하고 순서대로 병합하세요.
2. **충돌 해결**: 여러 브랜치에서 같은 파일을 수정한 경우 충돌이 발생할 수 있습니다.
3. **테스트**: 각 브랜치를 병합하기 전에 충분한 테스트를 수행하세요.
4. **작은 단위로**: 가능하면 더 작은 단위로 브랜치를 나누는 것이 좋습니다.

---

## 빠른 참조: 브랜치별 주요 변경사항 요약

| 브랜치명 | 주요 변경사항 | 우선순위 |
|---------|-------------|---------|
| `feature/production-validation` | Command Validator 패턴 도입 | 높음 |
| `feature/production-ai-service` | AI 서비스 클라이언트 추가 | 높음 |
| `feature/production-presigned-strategy` | Presigned URL Strategy 패턴 | 중간 |
| `feature/production-format-renderer` | Format 변환 및 렌더링 | 중간 |
| `feature/production-application-service` | Application Service 레이어 | 중간 |
| `feature/production-entity-improvement` | Entity 구조 개선 | 높음 |
| `feature/production-job-processing` | Job 처리 로직 개선 | 높음 |
| `feature/production-storage-config` | Storage 전략 개선 | 중간 |
| `feature/production-controller-service` | Controller 및 서비스 개선 | 낮음 |
| `feature/prompt-guideline-improvement` | Guideline 구조 개선 | 중간 |
| `feature/prompt-controller-dto` | Prompt Controller/DTO 개선 | 낮음 |
| `feature/global-config-improvement` | 전역 설정 개선 | 낮음 |

---

## 다음 단계

1. 이 문서를 검토하고 브랜치 분리 전략을 확인하세요.
2. 각 브랜치의 변경사항을 검토하여 추가/제거할 파일을 결정하세요.
3. 위의 방법 중 하나를 선택하여 브랜치를 생성하고 푸시하세요.
4. 각 브랜치에 대해 Pull Request를 생성하여 코드 리뷰를 진행하세요.




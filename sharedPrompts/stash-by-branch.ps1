# 브랜치별로 변경사항을 stash에 저장하는 스크립트

# Git 루트로 이동
$gitRoot = git rev-parse --show-toplevel
Set-Location $gitRoot

Write-Host "브랜치별로 변경사항을 stash에 저장합니다...`n" -ForegroundColor Green
Write-Host "Git 루트: $gitRoot`n" -ForegroundColor Yellow

# 1. Production Validation
Write-Host "[1/12] feature/production-validation 저장 중..." -ForegroundColor Cyan
git stash push -u -m "feature/production-validation: Command Validator 패턴 도입" -- `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/validation/DocumentCommandValidator.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/validation/BlogCommandValidator.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/validation/EmailCommandValidator.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/validator/DocumentValidator.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/validator/BlogValidator.java

# 2. Production AI Service
Write-Host "[2/12] feature/production-ai-service 저장 중..." -ForegroundColor Cyan
git stash push -u -m "feature/production-ai-service: AI 서비스 클라이언트 추가" -- `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/ai/text/GroqTextAiClient.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/ai/image/LeonardoImageAiClient.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/ai/AIServiceRegistry.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/ai/text/TextAIService.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/ai/image/ImageAIService.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/ai/config/ `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/ai/exception/ `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/ai/prompt/ `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/ai/retry/ `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/ai/strategy/

# 3. Production Presigned Strategy
Write-Host "[3/12] feature/production-presigned-strategy 저장 중..." -ForegroundColor Cyan
git stash push -u -m "feature/production-presigned-strategy: Presigned URL Strategy 패턴" -- `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/production/presign/DocumentPresignedStrategy.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/production/presign/ImagePresignedStrategy.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/production/presign/PresignedStrategy.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/production/presign/PresignedStrategyResolver.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/production/presign/S3PresignedUrlGenerator.java

# 4. Production Format Renderer
Write-Host "[4/12] feature/production-format-renderer 저장 중..." -ForegroundColor Cyan
git stash push -u -m "feature/production-format-renderer: Format 변환 및 렌더링" -- `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/format/XmlFormatConverter.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/format/FormatConverterRegistry.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/format/PdfFormatConverter.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/renderer/ImageRenderer.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/renderer/TextRenderer.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/parser/ExtractingJsonParser.java

# 5. Production Application Service
Write-Host "[5/12] feature/production-application-service 저장 중..." -ForegroundColor Cyan
git stash push -u -m "feature/production-application-service: Application Service 레이어 추가" -- `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/application/ArtifactApplicationService.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/application/ProductionResultApplicationService.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/dto/response/production/ArtifactDetailResponseDto.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/dto/response/production/ArtifactSummaryDto.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/dto/response/production/ProductionResponseDto.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/dto/response/production/ProductionResponseDtoMapper.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/dto/response/production/FileArtifactDto.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/dto/response/production/ImageArtifactDto.java

# 6. Production Entity Improvement
Write-Host "[6/12] feature/production-entity-improvement 저장 중..." -ForegroundColor Cyan
git stash push -u -m "feature/production-entity-improvement: Entity 구조 개선" -- `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/entity/job/JobEntity.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/entity/production/ProductionArtifactEntity.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/entity/production/ProductionArtifactDetailEntity.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/repository/production/ProductionArtifactRepository.java

# 7. Production Job Processing
Write-Host "[7/12] feature/production-job-processing 저장 중..." -ForegroundColor Cyan
git stash push -u -m "feature/production-job-processing: Job 처리 로직 개선" -- `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/job/JobEntityCreationService.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/job/JobStateService.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/job/process/JobProcessor.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/job/process/execution/ai/builder/AIRequestBuilder.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/job/process/execution/ai/retry/AIErrorClassifier.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/job/process/execution/ai/retry/AIRetryExecutor.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/job/queue/JobAsyncScheduler.java

# 8. Production Storage Config
Write-Host "[8/12] feature/production-storage-config 저장 중..." -ForegroundColor Cyan
git stash push -u -m "feature/production-storage-config: Storage 전략 개선" -- `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/config/S3Config.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/config/condition/ `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/storage/LocalStorageStrategy.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/storage/S3StorageStrategy.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/storage/StorageStrategyFactory.java

# 9. Production Controller Service
Write-Host "[9/12] feature/production-controller-service 저장 중..." -ForegroundColor Cyan
git stash push -u -m "feature/production-controller-service: Controller 및 서비스 개선" -- `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/controller/production/ProductionResultController.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/production/ProductionArtifactService.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/production/ArtifactAccessServiceImpl.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/artifact/ImageArtifactHandler.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/image/JavaImageProcessor.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/image/ThumbnailService.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/parser/AIResponseParser.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/dto/request/production/ProductionRequest.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/model/ai/AIContentRequest.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/model/contract/result/ArtifactType.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/module/exception/ModuleErrorCode.java

# 10. Prompt Guideline Improvement
Write-Host "[10/12] feature/prompt-guideline-improvement 저장 중..." -ForegroundColor Cyan
git stash push -u -m "feature/prompt-guideline-improvement: Guideline 구조 개선" -- `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/guideline/AnalyticalGuidelines.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/guideline/CreativeGuidelines.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/guideline/DomainResolution.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/guideline/EducationalGuidelines.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/guideline/GeneralGuidelines.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/guideline/GuidelinePolicy.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/guideline/GuidelineRule.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/guideline/I18nText.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/guideline/PracticalGuidelines.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/guideline/RuleLevel.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/guideline/RuleType.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/guideline/TechnicalGuidelines.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/service/guideline/EnglishGuidelineRenderer.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/service/guideline/GuidelineRenderer.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/service/guideline/JapaneseGuidelineRenderer.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/service/guideline/KoreanGuidelineRenderer.java

# 11. Prompt Controller DTO
Write-Host "[11/12] feature/prompt-controller-dto 저장 중..." -ForegroundColor Cyan
git stash push -u -m "feature/prompt-controller-dto: Prompt Controller 및 DTO 개선" -- `
    sharedPrompts/src/main/java/org/example/sharedprompts/controller/prompt/PromptMetadataController.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/dto/prompt/response/SimpleDomainResponseDto.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/dto/prompt/response/SimpleStyleResponseDto.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/dto/prompt/response/SimpleToneResponseDto.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/enums/StyleType.java `
    sharedPrompts/src/main/java/org/example/sharedprompts/domain/prompt/enums/TaskDomain.java `
    sharedPrompts/src/test/java/org/example/sharedprompts/domain/prompt/enums/TaskDomainTest.java

# 12. Global Config Improvement
Write-Host "[12/12] feature/global-config-improvement 저장 중..." -ForegroundColor Cyan
git stash push -u -m "feature/global-config-improvement: 전역 설정 개선" -- `
    sharedPrompts/src/main/java/org/example/sharedprompts/global/config/async/security/SecurityContextTaskDecorator.java `
    sharedPrompts/src/main/resources/application.yml `
    sharedPrompts/env.example `
    sharedPrompts/postman/Postman_Collection.json

Write-Host "`n모든 브랜치별 stash가 완료되었습니다!`n" -ForegroundColor Green
Write-Host "stash 목록 확인: git stash list" -ForegroundColor Yellow
Write-Host "특정 stash 적용: git stash apply stash@{N}" -ForegroundColor Yellow

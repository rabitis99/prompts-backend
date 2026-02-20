# Artifact API 개선 계획

## 📋 개요

현재 `ProductionResultController`의 Artifact 조회 엔드포인트와 `StorageController`의 Presigned URL 생성 엔드포인트 간에 기능 중복과 일관성 문제가 있습니다. 또한 내부적으로 Presigned URL 생성 로직이 여러 곳에 분산되어 있고, S3 경로 파싱 로직이 중복되어 있습니다. 이 문서는 실제 코드베이스를 분석하여 배포를 고려한 개선 계획을 제시합니다.

## 🔍 현재 문제점 분석

### 1. API 엔드포인트 중복 및 일관성 부족

#### 현재 구조
```
GET  /productions/{productionId}/artifacts                    # 목록 조회 (ProductionResultController)
GET  /productions/{productionId}/artifacts/{artifactId}       # 상세 조회 (presigned URL 포함, ProductionResultController)
POST /storage/upload-url                                      # 업로드용 presigned URL (StorageController)
POST /storage/download-url                                    # 다운로드용 presigned URL (StorageController)
POST /storage/preview-url                                     # 미리보기용 presigned URL (StorageController)
```

#### 문제점
1. **기능 중복**: `GET /productions/{productionId}/artifacts/{artifactId}`가 이미 presigned URL을 포함하여 반환하는데, 별도로 `/storage/download-url`, `/storage/preview-url` 엔드포인트가 존재
2. **HTTP 메서드 불일치**: Presigned URL 생성이 POST로 되어 있으나, 실제로는 조회 작업 (GET이 적합)
3. **RESTful 원칙 위반**: 리소스 중심 설계가 아닌 동작 중심 설계
4. **클라이언트 혼란**: 같은 목적을 달성하는 여러 방법이 존재
5. **엔드포인트 위치 불일치**: Artifact 관련 엔드포인트가 `ProductionResultController`와 `StorageController`에 분산

### 2. Presigned URL 생성 로직 중복 및 불일치

#### 중복된 구현
- **ArtifactApplicationService.generatePresignedUrl()**: 
  - `PresignedStrategyResolver`를 사용하여 Content-Type 기반 전략 선택
  - `parseS3Path()`로 bucket과 key를 추출
  - `PresignedStrategy.generatePresignedUrl()` 호출
  - 이미지: inline, 문서: attachment (Content-Disposition)
  
- **StorageCommandService.generateDownloadPresignedUrl()**: 
  - `S3PresignedUrlService.generateDownloadUrl()` 직접 호출
  - Content-Type을 고려하지 않음
  - 항상 기본 다운로드 URL 생성 (Content-Disposition 없음)
  
- **StorageCommandService.generatePreviewPresignedUrl()**: 
  - `S3PresignedUrlService.generatePreviewUrl()` 직접 호출
  - 항상 inline으로 생성 (이미지가 아닌 파일도 inline)

#### 문제점
1. **두 가지 다른 방식**: 
   - `ArtifactApplicationService`는 `PresignedStrategy` 패턴 사용 (Content-Type 기반 전략)
   - `StorageCommandService`는 직접 `S3PresignedUrlService` 호출 (Content-Type 무시)
2. **일관성 부족**: 같은 작업을 다른 방식으로 수행
3. **Content-Type 처리 불일치**: `StorageCommandService`는 Content-Type을 고려하지 않아 문서 파일도 inline으로 생성될 수 있음
4. **유지보수 어려움**: 로직 변경 시 여러 곳 수정 필요
5. **PresignedStrategyResolver 불일치**: 
   - `PresignedStrategyResolver.resolve()`는 예외를 던지지만
   - `ArtifactApplicationService`에서는 null 체크를 하고 있음 (실제로는 예외가 발생함)

### 3. 소유권 검증 중복

#### 중복된 검증
- **ArtifactApplicationService.getArtifact()**: 
  - Production 소유권 검증 (`production.getUserId().equals(userId)`)
  - Production과 Artifact의 관계 검증 (stream filter)
  
- **StorageCommandService.resolveS3KeyWithOwnerCheck()**: 
  - Artifact 소유권 검증 (동일한 로직: `production.getUserId().equals(userId)`)

#### 문제점
1. **중복 검증**: 같은 데이터에 대해 두 번 검증
2. **성능 저하**: 불필요한 DB 조회 및 검증 로직 실행
3. **일관성 부족**: 검증 로직이 분산되어 있음
4. **에러 메시지 불일치**: 같은 상황에서 다른 에러 코드 사용 가능

### 4. S3 경로 파싱 로직 중복

#### 중복된 메서드
1. **ArtifactApplicationService.parseS3Path()**: 
   - `s3://bucket/key` 형식에서 bucket과 key를 모두 추출
   - `S3Location` record 반환
   - s3:// 접두사가 없으면 defaultBucket 사용

2. **StorageCommandService.extractS3Key()**: 
   - `s3://bucket/key` 형식에서 key만 추출
   - bucket 정보 무시
   - s3:// 접두사가 없으면 그대로 반환

3. **ArtifactAccessServiceImpl.extractS3Key()**: 
   - `s3://bucket/key` 형식에서 key만 추출
   - 예외 발생 (다른 구현과 다름)
   - s3:// 접두사가 없으면 그대로 반환

4. **ArtifactMetadataHelper.extractFileName()**: 
   - 파일명만 추출 (이미 존재하는 유틸리티)
   - `/`와 `\` 모두 처리

5. **StorageFacade.extractFileName()**: 
   - 파일명만 추출 (중복)
   - `/`만 처리

6. **DocumentPresignedStrategy.extractFileName()**: 
   - 파일명만 추출 (중복)
   - `/`만 처리

7. **ThumbnailService.extractFileName()**: 
   - 파일명만 추출 (중복)

#### 문제점
1. **코드 중복**: 같은 로직이 여러 곳에 분산
2. **일관성 부족**: 각 구현이 약간씩 다름
3. **버그 위험**: 한 곳 수정 시 다른 곳 반영 안 됨
4. **테스트 어려움**: 각 구현을 개별적으로 테스트해야 함

### 5. Deprecated 메서드

- `StorageCommandService.generateDownloadPresignedUrl(String s3Key)` - 보안 취약점 (소유권 검증 없음)
- `StorageCommandService.generatePreviewPresignedUrl(String s3Key)` - 보안 취약점 (소유권 검증 없음)

### 6. PresignedStrategyResolver 예외 처리 불일치

- `PresignedStrategyResolver.resolve()`는 `IllegalArgumentException`을 던지지만
- `ArtifactApplicationService.generatePresignedUrl()`에서는 null 체크를 하고 있음
- 실제로는 예외가 발생하므로 null 체크는 의미 없음

## 🎯 개선 목표

1. **API 일관성**: RESTful 원칙에 맞는 리소스 중심 설계
2. **코드 중복 제거**: 공통 로직 통합
3. **성능 최적화**: 불필요한 중복 검증 제거
4. **하위 호환성**: 기존 클라이언트 영향 최소화
5. **점진적 마이그레이션**: 단계적 배포 가능
6. **Content-Type 일관성**: 모든 Presigned URL 생성에서 Content-Type 기반 전략 사용

## 📐 개선 방안

### Phase 1: 내부 리팩토링 (하위 호환성 유지)

#### 1.1 공통 유틸리티 클래스 생성

**파일**: `domain/production/util/S3PathUtils.java`

```java
package org.example.sharedprompts.module.domain.production.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * S3 경로 파싱 유틸리티
 * s3:// 형식의 경로를 파싱하는 공통 로직을 제공합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class S3PathUtils {
    
    /**
     * S3 경로 정보를 담는 record
     */
    public record S3Location(String bucket, String key) {
        public S3Location {
            if (bucket == null || bucket.isBlank()) {
                throw new IllegalArgumentException("Bucket cannot be null or blank");
            }
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Key cannot be null or blank");
            }
        }
    }
    
    /**
     * s3:// 형식의 경로에서 S3 key를 추출합니다.
     * @param s3Path s3://bucket/key 형식 또는 key 형식
     * @return S3 key (bucket 제외), null이거나 비어있으면 null 반환
     */
    public static String extractS3Key(String s3Path) {
        if (s3Path == null || s3Path.isBlank()) {
            return null;
        }
        if (s3Path.startsWith("s3://")) {
            String withoutPrefix = s3Path.substring(5);
            int slashIndex = withoutPrefix.indexOf('/');
            if (slashIndex > 0 && slashIndex < withoutPrefix.length() - 1) {
                String key = withoutPrefix.substring(slashIndex + 1);
                return key.isBlank() ? null : key;
            }
            // s3://bucket 또는 s3://bucket/ 형식은 유효하지 않음
            return null;
        }
        return s3Path;
    }
    
    /**
     * s3:// 형식의 경로에서 bucket 이름을 추출합니다.
     * @param s3Path s3://bucket/key 형식
     * @return bucket 이름, 추출 불가 시 null
     */
    public static String extractBucket(String s3Path) {
        if (s3Path != null && s3Path.startsWith("s3://")) {
            String withoutPrefix = s3Path.substring(5);
            int slashIndex = withoutPrefix.indexOf('/');
            if (slashIndex > 0) {
                return withoutPrefix.substring(0, slashIndex);
            }
        }
        return null;
    }
    
    /**
     * S3 경로 문자열을 파싱하여 bucket과 key를 추출합니다.
     * s3:// 접두사가 없으면 defaultBucket을 사용합니다.
     * 
     * @param s3Path S3 경로 (s3://bucket/key 형식 또는 key만 있는 형식)
     * @param defaultBucket s3:// 접두사가 없을 때 사용할 기본 bucket
     * @return S3Location (bucket, key), 파싱 실패 시 null
     */
    public static S3Location parseS3Path(String s3Path, String defaultBucket) {
        if (s3Path == null || s3Path.isBlank()) {
            return null;
        }

        if (s3Path.startsWith("s3://")) {
            String withoutPrefix = s3Path.substring(5);
            int slashIndex = withoutPrefix.indexOf('/');
            if (slashIndex > 0 && slashIndex < withoutPrefix.length() - 1) {
                String bucket = withoutPrefix.substring(0, slashIndex);
                String key = withoutPrefix.substring(slashIndex + 1);
                if (key.isBlank()) {
                    return null;
                }
                return new S3Location(bucket, key);
            }
            // s3://bucket 또는 s3://bucket/ 형식은 유효하지 않음
            return null;
        }

        // s3:// 접두사가 없는 경우 defaultBucket 사용
        if (defaultBucket == null || defaultBucket.isBlank()) {
            return null;
        }
        return new S3Location(defaultBucket, s3Path);
    }
    
    /**
     * S3 경로에서 파일명을 추출합니다.
     * ArtifactMetadataHelper.extractFileName()을 사용합니다.
     * 
     * @param s3Path S3 경로
     * @return 파일명, 추출 불가 시 null
     */
    public static String extractFileName(String s3Path) {
        return ArtifactMetadataHelper.extractFileName(s3Path);
    }
}
```

#### 1.2 PresignedStrategyResolver 개선

**문제**: `resolve()`가 예외를 던지지만 사용하는 곳에서 null 체크를 하고 있음

**해결**: Optional 반환 또는 null-safe 메서드 추가

```java
package org.example.sharedprompts.module.domain.production.service.production.presign;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Content-Type 기반 PresignedStrategy 해결자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PresignedStrategyResolver {

    private final List<PresignedStrategy> strategies;

    /**
     * Content-Type에 맞는 PresignedStrategy 반환
     * 
     * @param contentType 파일의 Content-Type
     * @return 적합한 PresignedStrategy
     * @throws IllegalArgumentException 지원되지 않는 Content-Type이거나 null일 때 발생
     */
    public PresignedStrategy resolve(String contentType) {
        if (contentType == null) {
            log.warn("ContentType is null");
            throw new IllegalArgumentException("Content type cannot be null");
        }
        return strategies.stream()
                .filter(strategy -> strategy.supports(contentType))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("No PresignedStrategy found for contentType: {}", contentType);
                    return new IllegalArgumentException(
                        "Unsupported content type for presigned URL generation: " + contentType);
                });
    }
    
    /**
     * Content-Type에 맞는 PresignedStrategy를 Optional로 반환합니다.
     * 전략을 찾을 수 없거나 contentType이 null인 경우 Optional.empty()를 반환합니다.
     * 
     * @param contentType 파일의 Content-Type
     * @return 적합한 PresignedStrategy를 담은 Optional
     */
    public Optional<PresignedStrategy> resolveOptional(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return Optional.empty();
        }
        return strategies.stream()
                .filter(strategy -> strategy.supports(contentType))
                .findFirst();
    }
}
```

#### 1.3 StorageCommandService 개선

**목표**: `PresignedStrategy`를 사용하여 Content-Type 기반 URL 생성

```java
package org.example.sharedprompts.module.domain.production.application.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.infra.storage.S3KeyGenerator;
import org.example.sharedprompts.module.domain.production.infra.storage.S3PresignedUrlService;
import org.example.sharedprompts.module.domain.production.model.tenant.TenantContext;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactDetailRepository;
import org.example.sharedprompts.module.domain.production.service.production.presign.PresignedStrategy;
import org.example.sharedprompts.module.domain.production.service.production.presign.PresignedStrategyResolver;
import org.example.sharedprompts.module.domain.production.util.S3PathUtils;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Storage Command Service
 * Presigned URL 생성 등 명령 작업을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageCommandService {

    private final S3PresignedUrlService presignedUrlService;
    private final S3KeyGenerator keyGenerator;
    private final ProductionArtifactDetailRepository artifactDetailRepository;
    private final PresignedStrategyResolver presignedStrategyResolver; // 추가

    @Value("${artifact.url.default-ttl:300}")
    private int defaultTtlSeconds;
    
    @Value("${production.storage.s3.bucket:}")
    private String defaultBucket;

    /**
     * 업로드용 Presigned URL 생성
     * TTL은 S3PresignedUrlService의 기본값을 사용합니다.
     * 비동기 컨텍스트 등에서 tenantId가 null일 수 있습니다.
     */
    public String generateUploadPresignedUrl(Long userId, String jobId, String fileName, String contentType) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("TenantContext.getCurrentTenantId() returned null - userId: {}, jobId: {}, fileName: {}. " +
                    "Presigned URL will be generated without tenant prefix. Consider using tenant-aware method in async contexts.",
                    userId, jobId, fileName);
        }
        String s3Key = keyGenerator.generateKey(tenantId, userId, jobId, fileName);
        return presignedUrlService.generateUploadUrl(s3Key, contentType, null);
    }

    /**
     * 다운로드용 Presigned URL 생성 (보안 검증 포함)
     * PresignedStrategy를 사용하여 Content-Type에 맞는 URL 생성
     * 
     * @param artifactId 아티팩트 ID
     * @param userId 사용자 ID
     * @return Presigned URL
     */
    @Transactional(readOnly = true)
    public String generateDownloadPresignedUrl(Long artifactId, Long userId) {
        ArtifactContext context = resolveArtifactWithOwnerCheck(artifactId, userId);
        
        // PresignedStrategy를 사용하여 Content-Type 기반 URL 생성
        PresignedStrategy strategy = presignedStrategyResolver.resolveOptional(context.getContentType())
                .orElse(null);
        
        if (strategy != null) {
            // 전략이 있으면 전략 사용
            return strategy.generatePresignedUrl(
                context.getBucket(),
                context.getS3Key(),
                context.getContentType(),
                Duration.ofSeconds(defaultTtlSeconds)
            );
        }
        
        // 전략이 없으면 기본 다운로드 URL 생성
        return presignedUrlService.generateDownloadUrl(
            context.getS3Key(), 
            Duration.ofSeconds(defaultTtlSeconds)
        );
    }

    /**
     * 미리보기용 Presigned URL 생성 (보안 검증 포함)
     * 이미지는 inline, 문서는 다운로드용으로 생성
     * 
     * @param artifactId 아티팩트 ID
     * @param userId 사용자 ID
     * @return Presigned URL
     */
    @Transactional(readOnly = true)
    public String generatePreviewPresignedUrl(Long artifactId, Long userId) {
        ArtifactContext context = resolveArtifactWithOwnerCheck(artifactId, userId);
        
        // 이미지는 inline, 문서는 다운로드용으로 생성
        PresignedStrategy strategy = presignedStrategyResolver.resolveOptional(context.getContentType())
                .orElse(null);
        
        if (strategy != null) {
            // 전략이 있으면 전략 사용 (이미지는 inline, 문서는 attachment)
            return strategy.generatePresignedUrl(
                context.getBucket(),
                context.getS3Key(),
                context.getContentType(),
                Duration.ofSeconds(defaultTtlSeconds)
            );
        }
        
        // 전략이 없으면 기본 미리보기 URL 생성 (inline)
        return presignedUrlService.generatePreviewUrl(
            context.getS3Key(), 
            Duration.ofSeconds(defaultTtlSeconds)
        );
    }

    /**
     * 아티팩트 정보와 소유권을 검증하여 컨텍스트 반환
     */
    private ArtifactContext resolveArtifactWithOwnerCheck(Long artifactId, Long userId) {
        ProductionArtifactDetailEntity artifact = artifactDetailRepository.findById(artifactId)
                .orElseThrow(() -> new BaseException(ModuleErrorCode.PRODUCTION_NOT_FOUND));

        ProductionArtifactEntity production = artifact.getArtifact();
        if (production == null) {
            throw new BaseException(ModuleErrorCode.PRODUCTION_NOT_FOUND);
        }

        if (!production.getUserId().equals(userId)) {
            throw new BaseException(ModuleErrorCode.PRODUCTION_FORBIDDEN);
        }

        String s3Key = artifact.getS3Key();
        if (s3Key == null || s3Key.isBlank()) {
            throw new BaseException(ModuleErrorCode.STORAGE_ERROR, null, 
                "Artifact s3Key is not available");
        }

        // S3PathUtils를 사용하여 bucket과 key 추출
        S3PathUtils.S3Location location = S3PathUtils.parseS3Path(s3Key, defaultBucket);
        if (location == null) {
            throw new BaseException(ModuleErrorCode.STORAGE_ERROR, null, 
                "Invalid S3 path: " + s3Key);
        }
        
        String contentType = artifact.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return new ArtifactContext(location.bucket(), location.key(), contentType, artifact);
    }

    /**
     * 다운로드용 Presigned URL 생성 (레거시 - 보안 취약점 있음)
     * @deprecated 보안을 위해 generateDownloadPresignedUrl(Long artifactId, Long userId) 사용을 권장합니다.
     */
    @Deprecated
    public String generateDownloadPresignedUrl(String s3Key) {
        // TTL은 S3PresignedUrlService의 기본값을 사용합니다.
        return presignedUrlService.generateDownloadUrl(s3Key, null);
    }

    /**
     * 미리보기용 Presigned URL 생성 (레거시 - 보안 취약점 있음)
     * @deprecated 보안을 위해 generatePreviewPresignedUrl(Long artifactId, Long userId) 사용을 권장합니다.
     */
    @Deprecated
    public String generatePreviewPresignedUrl(String s3Key) {
        // TTL은 S3PresignedUrlService의 기본값을 사용합니다.
        return presignedUrlService.generatePreviewUrl(s3Key, null);
    }
    
    /**
     * 아티팩트 컨텍스트
     */
    @lombok.Value
    private static class ArtifactContext {
        String bucket;
        String s3Key;
        String contentType;
        ProductionArtifactDetailEntity artifact;
    }
}
```

#### 1.4 ArtifactApplicationService 개선

**목표**: `StorageCommandService`를 사용하여 중복 제거

```java
package org.example.sharedprompts.module.domain.production.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactRepository;
import org.example.sharedprompts.module.domain.production.application.storage.StorageCommandService; // 추가
import org.example.sharedprompts.module.domain.production.service.production.ArtifactAccessService;
import org.example.sharedprompts.module.dto.response.production.ArtifactDetailResponseDto;
import org.example.sharedprompts.module.dto.response.production.ArtifactDetailResponseDtoMapper;
import org.example.sharedprompts.module.dto.response.production.ArtifactSummaryDto;
import org.example.sharedprompts.module.dto.response.production.ArtifactSummaryDtoMapper;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactApplicationService {

    private final ProductionArtifactRepository productionArtifactRepository;
    private final StorageCommandService storageCommandService; // 추가: 중복 제거
    private final ArtifactAccessService artifactAccessService;

    @Transactional(readOnly = true)
    public List<ArtifactSummaryDto> getArtifacts(Long productionId, Long userId) {
        log.info("Artifacts requested - productionId: {}, userId: {}", productionId, userId);
        
        ProductionArtifactEntity production = productionArtifactRepository
                .findByIdWithArtifacts(productionId)
                .orElseThrow(() -> new BaseException(ModuleErrorCode.PRODUCTION_NOT_FOUND));
        
        if (!production.getUserId().equals(userId)) {
            throw new BaseException(ModuleErrorCode.PRODUCTION_FORBIDDEN);
        }
        
        return ArtifactSummaryDtoMapper.toDtoList(production.getArtifacts());
    }

    @Transactional(readOnly = true)
    public ArtifactDetailResponseDto getArtifact(Long productionId, Long artifactId, Long userId) {
        log.info("Artifact detail requested - productionId: {}, artifactId: {}, userId: {}", 
                productionId, artifactId, userId);
        
        ProductionArtifactEntity production = productionArtifactRepository
                .findByIdWithArtifacts(productionId)
                .orElseThrow(() -> new BaseException(ModuleErrorCode.PRODUCTION_NOT_FOUND));
        
        if (!production.getUserId().equals(userId)) {
            throw new BaseException(ModuleErrorCode.PRODUCTION_FORBIDDEN);
        }
        
        ProductionArtifactDetailEntity artifact = production.getArtifacts().stream()
                .filter(detail -> detail.getId().equals(artifactId))
                .findFirst()
                .orElseThrow(() -> new BaseException(ModuleErrorCode.PRODUCTION_NOT_FOUND));
        
        // StorageCommandService를 통해 presigned URL 생성 (중복 제거)
        // 이미지면 미리보기용, 아니면 다운로드용
        String presignedUrl = null;
        if (artifact.getS3Key() != null && !artifact.getS3Key().isBlank()) {
            try {
                String contentType = artifact.getContentType();
                if (contentType != null && contentType.toLowerCase().startsWith("image/")) {
                    presignedUrl = storageCommandService.generatePreviewPresignedUrl(
                        artifactId, userId);
                } else {
                    presignedUrl = storageCommandService.generateDownloadPresignedUrl(
                        artifactId, userId);
                }
            } catch (Exception e) {
                log.warn("Failed to generate presigned URL for artifact - artifactId: {}", 
                    artifactId, e);
                // presigned URL 생성 실패해도 상세 정보는 반환
            }
        }
        
        String cdnUrl = artifactAccessService.generateCdnUrl(artifact.getS3Key());
        
        return ArtifactDetailResponseDtoMapper.toDto(
                production,
                artifact,
                presignedUrl,
                cdnUrl,
                null // thumbnailUrls는 별도 처리 필요
        );
    }
}
```

**주의**: `StorageCommandService.resolveArtifactWithOwnerCheck()`에서 이미 소유권 검증을 하므로, `ArtifactApplicationService.getArtifact()`에서의 검증은 중복입니다. 하지만 두 가지 접근 방식이 있으므로:

- **Option A**: `ArtifactApplicationService`에서만 검증하고, `StorageCommandService`는 검증 없이 사용
- **Option B**: `StorageCommandService`에서만 검증하고, `ArtifactApplicationService`는 검증 없이 사용

**권장**: Option B (StorageCommandService에서만 검증) - 보안 로직을 한 곳에 집중

하지만 현재는 `getArtifact()`가 Production 레벨에서 검증하고 있으므로, 일관성을 위해 Option A를 유지하되 `StorageCommandService`의 검증을 제거하는 것도 고려할 수 있습니다. 이 경우 `StorageCommandService`는 검증 없이 사용하고, 호출하는 쪽에서 검증을 담당합니다.

**최종 권장**: Option B - `StorageCommandService`에서만 검증하고, `ArtifactApplicationService`는 검증 없이 사용. 이렇게 하면 보안 로직이 한 곳에 집중되고, 다른 곳에서 `StorageCommandService`를 사용할 때도 자동으로 검증됩니다.

#### 1.5 기타 클래스에서 S3PathUtils 사용

- `ArtifactAccessServiceImpl.extractS3Key()` → `S3PathUtils.extractS3Key()` 사용
- `StorageFacade.extractFileName()` → `S3PathUtils.extractFileName()` 사용 (또는 `ArtifactMetadataHelper.extractFileName()` 직접 사용)
- `DocumentPresignedStrategy.extractFileName()` → `S3PathUtils.extractFileName()` 사용
- `ThumbnailService.extractFileName()` → `S3PathUtils.extractFileName()` 사용

### Phase 2: API 개선 (하위 호환성 유지)

#### 2.1 새로운 RESTful 엔드포인트 추가

**파일**: `controller/production/ArtifactController.java` (신규 생성)

```java
package org.example.sharedprompts.module.controller.production;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.module.domain.production.application.ArtifactApplicationService;
import org.example.sharedprompts.module.domain.production.application.storage.StorageCommandService;
import org.example.sharedprompts.module.dto.response.production.ArtifactDetailResponseDto;
import org.example.sharedprompts.module.dto.response.production.ArtifactSummaryDto;
import org.example.sharedprompts.module.presentation.StorageController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Artifact Controller
 * Artifact 조회 및 Presigned URL 생성을 담당합니다.
 */
@RestController
@RequestMapping("/productions/{productionId}/artifacts")
@RequiredArgsConstructor
@Slf4j
public class ArtifactController {
    
    private final ArtifactApplicationService artifactApplicationService;
    private final StorageCommandService storageCommandService;
    
    /**
     * Artifact 목록 조회
     * GET /productions/{productionId}/artifacts
     */
    @GetMapping
    public ResponseEntity<CustomResponse<List<ArtifactSummaryDto>>> getArtifacts(
        @PathVariable Long productionId,
        @CurrentUser AuthUser authUser
    ) {
        List<ArtifactSummaryDto> artifacts = artifactApplicationService.getArtifacts(
            productionId, authUser.getId());
        return CustomResponseHelper.ok(artifacts);
    }
    
    /**
     * Artifact 상세 조회 (presigned URL 포함)
     * GET /productions/{productionId}/artifacts/{artifactId}
     */
    @GetMapping("/{artifactId}")
    public ResponseEntity<CustomResponse<ArtifactDetailResponseDto>> getArtifact(
        @PathVariable Long productionId,
        @PathVariable Long artifactId,
        @CurrentUser AuthUser authUser
    ) {
        ArtifactDetailResponseDto artifact = artifactApplicationService.getArtifact(
            productionId, artifactId, authUser.getId());
        return CustomResponseHelper.ok(artifact);
    }
    
    /**
     * Artifact 다운로드용 Presigned URL 생성
     * GET /productions/{productionId}/artifacts/{artifactId}/download-url
     * 
     * @deprecated Use GET /artifacts/{artifactId}/download-url instead
     */
    @GetMapping("/{artifactId}/download-url")
    @Deprecated
    public ResponseEntity<CustomResponse<StorageController.PresignedUrlResponse>> getDownloadUrl(
        @PathVariable Long productionId,
        @PathVariable Long artifactId,
        @CurrentUser AuthUser authUser
    ) {
        log.warn("Deprecated endpoint used - GET /productions/{productionId}/artifacts/{artifactId}/download-url. " +
            "Use GET /artifacts/{artifactId}/download-url instead");
        
        String presignedUrl = storageCommandService.generateDownloadPresignedUrl(
            artifactId, authUser.getId());
        return CustomResponseHelper.ok(new StorageController.PresignedUrlResponse(presignedUrl));
    }
    
    /**
     * Artifact 미리보기용 Presigned URL 생성
     * GET /productions/{productionId}/artifacts/{artifactId}/preview-url
     * 
     * @deprecated Use GET /artifacts/{artifactId}/preview-url instead
     */
    @GetMapping("/{artifactId}/preview-url")
    @Deprecated
    public ResponseEntity<CustomResponse<StorageController.PresignedUrlResponse>> getPreviewUrl(
        @PathVariable Long productionId,
        @PathVariable Long artifactId,
        @CurrentUser AuthUser authUser
    ) {
        log.warn("Deprecated endpoint used - GET /productions/{productionId}/artifacts/{artifactId}/preview-url. " +
            "Use GET /artifacts/{artifactId}/preview-url instead");
        
        String presignedUrl = storageCommandService.generatePreviewPresignedUrl(
            artifactId, authUser.getId());
        return CustomResponseHelper.ok(new StorageController.PresignedUrlResponse(presignedUrl));
    }
}
```

#### 2.2 새로운 통합 Artifact 엔드포인트 (권장)

**파일**: `controller/production/ArtifactResourceController.java` (신규 생성)

```java
package org.example.sharedprompts.module.controller.production;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.module.domain.production.application.storage.StorageCommandService;
import org.example.sharedprompts.module.presentation.StorageController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Artifact Resource Controller
 * Artifact 리소스 중심의 Presigned URL 생성을 담당합니다.
 * 
 * 이 컨트롤러는 RESTful 원칙에 따라 Artifact를 리소스로 취급합니다.
 */
@RestController
@RequestMapping("/artifacts")
@RequiredArgsConstructor
@Slf4j
public class ArtifactResourceController {
    
    private final StorageCommandService storageCommandService;
    
    /**
     * Artifact 다운로드용 Presigned URL 생성
     * GET /artifacts/{artifactId}/download-url
     */
    @GetMapping("/{artifactId}/download-url")
    public ResponseEntity<CustomResponse<StorageController.PresignedUrlResponse>> getDownloadUrl(
        @PathVariable Long artifactId,
        @CurrentUser AuthUser authUser
    ) {
        log.info("Download presigned URL requested - userId: {}, artifactId: {}",
            authUser.getId(), artifactId);
        
        String presignedUrl = storageCommandService.generateDownloadPresignedUrl(
            artifactId, authUser.getId());
        return CustomResponseHelper.ok(new StorageController.PresignedUrlResponse(presignedUrl));
    }
    
    /**
     * Artifact 미리보기용 Presigned URL 생성
     * GET /artifacts/{artifactId}/preview-url
     */
    @GetMapping("/{artifactId}/preview-url")
    public ResponseEntity<CustomResponse<StorageController.PresignedUrlResponse>> getPreviewUrl(
        @PathVariable Long artifactId,
        @CurrentUser AuthUser authUser
    ) {
        log.info("Preview presigned URL requested - userId: {}, artifactId: {}",
            authUser.getId(), artifactId);
        
        String presignedUrl = storageCommandService.generatePreviewPresignedUrl(
            artifactId, authUser.getId());
        return CustomResponseHelper.ok(new StorageController.PresignedUrlResponse(presignedUrl));
    }
}
```

#### 2.3 StorageController 개선

```java
package org.example.sharedprompts.module.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.module.domain.production.application.storage.StorageCommandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Storage Controller
 * Presigned URL 생성을 담당합니다.
 * 실제 파일 전송은 서버를 거치지 않고 클라이언트에서 직접 S3로 업로드/다운로드합니다.
 */
@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
@Slf4j
public class StorageController {

    private final StorageCommandService storageCommandService;

    /**
     * 업로드용 Presigned URL 생성
     * POST /storage/upload-url
     */
    @PostMapping("/upload-url")
    public ResponseEntity<CustomResponse<PresignedUrlResponse>> generateUploadUrl(
            @Valid @RequestBody UploadUrlRequest request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Upload presigned URL requested - userId: {}, fileName: {}, contentType: {}",
                authUser.getId(), request.fileName(), request.contentType());

        String presignedUrl = storageCommandService.generateUploadPresignedUrl(
                authUser.getId(),
                request.jobId(),
                request.fileName(),
                request.contentType()
        );

        PresignedUrlResponse response = new PresignedUrlResponse(presignedUrl);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 다운로드용 Presigned URL 생성
     * POST /storage/download-url
     * 
     * @deprecated Use GET /artifacts/{artifactId}/download-url instead
     */
    @PostMapping("/download-url")
    @Deprecated
    public ResponseEntity<CustomResponse<PresignedUrlResponse>> generateDownloadUrl(
            @Valid @RequestBody DownloadUrlRequest request,
            @CurrentUser AuthUser authUser
    ) {
        log.warn("Deprecated endpoint used - POST /storage/download-url. " +
            "Use GET /artifacts/{artifactId}/download-url instead");
        
        String presignedUrl = storageCommandService.generateDownloadPresignedUrl(
                request.artifactId(), authUser.getId());
        PresignedUrlResponse response = new PresignedUrlResponse(presignedUrl);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 미리보기용 Presigned URL 생성
     * POST /storage/preview-url
     * 
     * @deprecated Use GET /artifacts/{artifactId}/preview-url instead
     */
    @PostMapping("/preview-url")
    @Deprecated
    public ResponseEntity<CustomResponse<PresignedUrlResponse>> generatePreviewUrl(
            @Valid @RequestBody PreviewUrlRequest request,
            @CurrentUser AuthUser authUser
    ) {
        log.warn("Deprecated endpoint used - POST /storage/preview-url. " +
            "Use GET /artifacts/{artifactId}/preview-url instead");
        
        String presignedUrl = storageCommandService.generatePreviewPresignedUrl(
                request.artifactId(), authUser.getId());
        PresignedUrlResponse response = new PresignedUrlResponse(presignedUrl);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 업로드용 Presigned URL 생성 요청 DTO
     */
    public record UploadUrlRequest(
            @JsonProperty("jobId")
            @NotBlank(message = "jobId is required")
            String jobId,

            @JsonProperty("fileName")
            @NotBlank(message = "fileName is required")
            String fileName,

            @JsonProperty("contentType")
            @NotBlank(message = "contentType is required")
            String contentType
    ) {
    }

    /**
     * 다운로드용 Presigned URL 생성 요청 DTO
     */
    public record DownloadUrlRequest(
            @JsonProperty("artifactId")
            @NotNull(message = "artifactId is required")
            Long artifactId
    ) {
    }

    /**
     * 미리보기용 Presigned URL 생성 요청 DTO
     */
    public record PreviewUrlRequest(
            @JsonProperty("artifactId")
            @NotNull(message = "artifactId is required")
            Long artifactId
    ) {
    }

    @Getter
    public static class PresignedUrlResponse {
        private final String presignedUrl;

        public PresignedUrlResponse(String presignedUrl) {
            this.presignedUrl = presignedUrl;
        }
    }
}
```

#### 2.4 ProductionResultController 정리

```java
package org.example.sharedprompts.module.controller.production;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.module.domain.production.application.ArtifactApplicationService;
import org.example.sharedprompts.module.domain.production.application.ProductionResultApplicationService;
import org.example.sharedprompts.module.dto.response.production.ArtifactDetailResponseDto;
import org.example.sharedprompts.module.dto.response.production.ArtifactSummaryDto;
import org.example.sharedprompts.module.dto.response.production.JobResponseDto;
import org.example.sharedprompts.module.dto.response.production.ProductionResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProductionResultController {

    private final ProductionResultApplicationService applicationService;
    private final ArtifactApplicationService artifactApplicationService;

    @GetMapping("/production/{productionId}")
    public ResponseEntity<CustomResponse<ProductionResponseDto>> getProductionResult(
            @PathVariable Long productionId,
            @CurrentUser AuthUser authUser
    ) {
        ProductionResponseDto response = applicationService.getProductionResult(
                productionId, 
                authUser.getId()
        );
        
        return CustomResponseHelper.ok(response);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<CustomResponse<JobResponseDto>> getJobStatus(
            @PathVariable String jobId,
            @CurrentUser AuthUser authUser
    ) {
        JobResponseDto response = applicationService.getJobStatus(jobId, authUser.getId());
        
        return CustomResponseHelper.ok(response);
    }

    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<CustomResponse<String>> retryJob(
            @PathVariable String jobId,
            @CurrentUser AuthUser authUser
    ) {
        applicationService.retryJob(jobId, authUser.getId());
        
        return CustomResponseHelper.ok("Job retry started successfully");
    }

    /**
     * Artifact 목록 조회
     * GET /productions/{productionId}/artifacts
     * 
     * @deprecated Use GET /productions/{productionId}/artifacts (ArtifactController) instead
     */
    @GetMapping("/productions/{productionId}/artifacts")
    @Deprecated
    public ResponseEntity<CustomResponse<List<ArtifactSummaryDto>>> getArtifacts(
            @PathVariable Long productionId,
            @CurrentUser AuthUser authUser
    ) {
        log.warn("Deprecated endpoint used - GET /productions/{productionId}/artifacts " +
            "in ProductionResultController. This endpoint is now handled by ArtifactController.");
        
        List<ArtifactSummaryDto> artifacts = artifactApplicationService.getArtifacts(
                productionId,
                authUser.getId()
        );
        
        return CustomResponseHelper.ok(artifacts);
    }

    /**
     * Artifact 상세 조회
     * GET /productions/{productionId}/artifacts/{artifactId}
     * 
     * @deprecated Use GET /productions/{productionId}/artifacts/{artifactId} (ArtifactController) instead
     */
    @GetMapping("/productions/{productionId}/artifacts/{artifactId}")
    @Deprecated
    public ResponseEntity<CustomResponse<ArtifactDetailResponseDto>> getArtifact(
            @PathVariable Long productionId,
            @PathVariable Long artifactId,
            @CurrentUser AuthUser authUser
    ) {
        log.warn("Deprecated endpoint used - GET /productions/{productionId}/artifacts/{artifactId} " +
            "in ProductionResultController. This endpoint is now handled by ArtifactController.");
        
        ArtifactDetailResponseDto artifact = artifactApplicationService.getArtifact(
                productionId,
                artifactId,
                authUser.getId()
        );
        
        return CustomResponseHelper.ok(artifact);
    }
}
```

## 🚀 배포 전략

### Step 1: Phase 1 배포 (내부 리팩토링)
- **기간**: 1-2주
- **영향**: 내부 코드만 변경, API 변경 없음
- **검증**: 
  - 기존 테스트 통과 확인
  - 통합 테스트 실행
  - 성능 테스트 (중복 검증 제거 효과 확인)

### Step 2: Phase 2 배포 (새 엔드포인트 추가)
- **기간**: 2주
- **영향**: 새 엔드포인트 추가, 기존 엔드포인트는 유지
- **검증**: 
  - 새 엔드포인트 테스트
  - 기존 엔드포인트 정상 동작 확인
  - 클라이언트 마이그레이션 가이드 제공
  - API 문서 업데이트

### Step 3: 클라이언트 마이그레이션
- **기간**: 4-6주 (클라이언트 팀과 협의)
- **작업**:
  - 클라이언트 코드 업데이트
  - 새 엔드포인트 사용으로 전환
  - 모니터링 및 피드백 수집
  - 점진적 전환 (A/B 테스트 가능)

### Step 4: Phase 3 배포 (Deprecated 처리)
- **기간**: 1주
- **작업**:
  - 기존 엔드포인트에 `@Deprecated` 추가
  - 로그 경고 추가
  - 문서 업데이트
  - 모니터링 대시보드 설정

### Step 5: 완전 제거 (선택적)
- **기간**: 3-6개월 후
- **조건**: 
  - 모든 클라이언트가 새 엔드포인트 사용
  - 모니터링에서 기존 엔드포인트 사용량 0% 확인
  - 클라이언트 팀 승인
- **작업**: Deprecated 엔드포인트 제거

## 📊 개선 효과

### 코드 품질
- ✅ 코드 중복 제거: `extractS3Key()`, `extractFileName()`, `parseS3Path()` 등 공통 로직 통합
- ✅ 일관성 향상: Presigned URL 생성 로직 통일 (PresignedStrategy 사용)
- ✅ 유지보수성 향상: 변경 시 한 곳만 수정
- ✅ 테스트 용이성: 공통 로직을 한 곳에서 테스트

### 성능
- ✅ 중복 검증 제거: 소유권 검증 로직 통합
- ✅ 불필요한 DB 조회 감소: 검증 로직 최적화
- ✅ 캐싱 효율성: 공통 로직 사용으로 캐시 히트율 향상 가능

### API 설계
- ✅ RESTful 원칙 준수: 리소스 중심 설계
- ✅ HTTP 메서드 적절성: GET for 조회, POST for 생성
- ✅ 일관성 향상: 통일된 엔드포인트 구조
- ✅ 명확성 향상: 엔드포인트 목적이 명확함

### 보안
- ✅ 보안 로직 집중: 소유권 검증 한 곳에서 관리
- ✅ Deprecated 메서드 제거: 보안 취약점 제거
- ✅ Content-Type 기반 처리: 모든 Presigned URL 생성에서 일관된 처리

## ⚠️ 주의사항

### 하위 호환성
1. **기존 엔드포인트 유지**: 모든 기존 엔드포인트는 Deprecated 처리 후에도 동작
2. **점진적 마이그레이션**: 클라이언트가 천천히 전환할 수 있도록 충분한 시간 제공
3. **모니터링**: 기존 엔드포인트 사용량 모니터링하여 제거 시점 결정
4. **롤백 계획**: 문제 발생 시 즉시 롤백 가능하도록 준비

### 테스트
1. **기존 테스트 통과**: 모든 기존 테스트가 통과해야 함
2. **새 테스트 작성**: 새 엔드포인트에 대한 테스트 작성
3. **통합 테스트**: 전체 플로우 테스트
4. **성능 테스트**: 중복 제거 전후 성능 비교
5. **보안 테스트**: 소유권 검증 로직 테스트

### 문서화
1. **API 문서 업데이트**: 새 엔드포인트 문서화
2. **마이그레이션 가이드**: 클라이언트 마이그레이션 가이드 제공
3. **Deprecation 공지**: Deprecated 엔드포인트 공지 및 제거 일정 안내
4. **코드 주석**: 개선된 코드에 명확한 주석 추가

### 성능 고려사항
1. **캐싱**: Presigned URL 생성 결과 캐싱 고려
2. **DB 쿼리 최적화**: 소유권 검증 시 필요한 최소한의 데이터만 조회
3. **비동기 처리**: 필요시 비동기 처리 고려

## 📝 체크리스트

### Phase 1 (내부 리팩토링)
- [ ] `S3PathUtils` 유틸리티 클래스 생성
- [ ] `PresignedStrategyResolver`에 `resolveOptional()` 메서드 추가
- [ ] `StorageCommandService`에 `PresignedStrategy` 통합
- [ ] `ArtifactApplicationService`에서 `StorageCommandService` 사용
- [ ] 기타 클래스에서 `S3PathUtils` 사용
- [ ] 중복 코드 제거
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 통과 확인
- [ ] 성능 테스트 실행

### Phase 2 (새 엔드포인트 추가)
- [ ] `ArtifactController` 생성
- [ ] `ArtifactResourceController` 생성
- [ ] `StorageController` Deprecated 처리
- [ ] `ProductionResultController` Deprecated 처리
- [ ] API 문서 업데이트
- [ ] 테스트 작성
- [ ] 배포 및 모니터링
- [ ] 클라이언트 마이그레이션 가이드 작성

### Phase 3 (클라이언트 마이그레이션)
- [ ] 마이그레이션 가이드 작성
- [ ] 클라이언트 팀과 협의
- [ ] 모니터링 대시보드 설정
- [ ] 피드백 수집 및 개선
- [ ] 점진적 전환 지원

### Phase 4 (완전 제거)
- [ ] 사용량 모니터링 (3-6개월)
- [ ] 최종 확인
- [ ] Deprecated 엔드포인트 제거
- [ ] 문서 정리

## 🔗 참고

- [FILE_STRUCTURE.md](./FILE_STRUCTURE.md) - 전체 파일 구조 및 기존 개선 제안
- RESTful API 설계 원칙
- Spring Boot Best Practices
- AWS S3 Presigned URL Best Practices

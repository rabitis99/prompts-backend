# 렌더링 시스템 즉시 수정 가이드 (P0)

> **우선순위**: 🔴 최우선  
> **예상 작업량**: 3-5일  
> **목적**: 타입 안전성 확보 및 기본 기능 완성

---

## 목차

1. [location 의미 분리](#1-location-의미-분리)
2. [Presigned URL 구현](#2-presigned-url-구현)
3. [success 구조 정리](#3-success-구조-정리)

---

## 1. location 의미 분리

### 1.1 현재 문제점

```java
// 현재: location 필드가 타입에 따라 다른 의미
public class ArtifactDto {
    private String location;  // TEXT: 콘텐츠, FILE/IMAGE: 경로
}
```

**문제:**
- 프론트엔드에서 타입 체크 없이 사용 불가
- API 계약이 불명확
- 런타임 에러 가능성

### 1.2 개선 방안

#### Step 1: 타입별 DTO 분리

```java
// Base ArtifactDto
package org.example.sharedprompts.module.dto.response.production;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;

import java.time.Instant;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextArtifactDto.class, name = "TEXT"),
    @JsonSubTypes.Type(value = FileArtifactDto.class, name = "FILE"),
    @JsonSubTypes.Type(value = ImageArtifactDto.class, name = "IMAGE")
})
public abstract class ArtifactDto {
    protected ArtifactType type;
    protected String fileName;
    protected String contentType;
    protected Instant createdAt;
}
```

#### Step 2: TextArtifactDto 구현

```java
package org.example.sharedprompts.module.dto.response.production;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TextArtifactDto extends ArtifactDto {
    private String content;      // ✅ 명확한 의미: 실제 텍스트 콘텐츠
    private boolean isInline;    // ✅ 인라인 여부 명시
}
```

#### Step 3: FileArtifactDto 구현

```java
package org.example.sharedprompts.module.dto.response.production;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FileArtifactDto extends ArtifactDto {
    private String filePath;     // ✅ 파일 경로
    private String downloadUrl;  // ✅ 다운로드 URL (나중에 추가)
    private Long fileSize;       // ✅ 파일 크기
}
```

#### Step 4: ImageArtifactDto 구현

```java
package org.example.sharedprompts.module.dto.response.production;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ImageArtifactDto extends ArtifactDto {
    private String filePath;      // ✅ S3 경로
    private String previewUrl;    // ✅ Presigned URL (자동 생성)
    private Instant urlExpiresAt; // ✅ URL 만료 시간
    private ImageMetadata metadata; // ✅ 이미지 메타데이터 (선택)
}

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ImageMetadata {
    private Integer width;
    private Integer height;
    private Long fileSize;
}
```

#### Step 5: ArtifactDto.from() 메서드 수정

```java
// ArtifactDto.java에 추가
public static ArtifactDto from(ProductionArtifactDetailEntity detail) {
    return switch (detail.getArtifactType()) {
        case TEXT -> buildTextArtifact(detail);
        case FILE -> buildFileArtifact(detail);
        case IMAGE -> buildImageArtifact(detail);
    };
}

private static TextArtifactDto buildTextArtifact(ProductionArtifactDetailEntity detail) {
    boolean isInline = detail.getStorageType() == StorageFormat.INLINE_TEXT;
    String content = isInline 
        ? detail.getContent() 
        : null; // FILE_PATH인 경우 나중에 다운로드 API로 처리
    
    return TextArtifactDto.builder()
            .type(ArtifactType.TEXT)
            .content(content)
            .isInline(isInline)
            .fileName(detail.getFileName())
            .contentType(detail.getContentType())
            .createdAt(detail.getArtifact().getCreatedAt())
            .build();
}

private static FileArtifactDto buildFileArtifact(ProductionArtifactDetailEntity detail) {
    return FileArtifactDto.builder()
            .type(ArtifactType.FILE)
            .filePath(detail.getFilePath())
            .fileName(detail.getFileName())
            .contentType(detail.getContentType())
            .createdAt(detail.getArtifact().getCreatedAt())
            .build();
}

private static ImageArtifactDto buildImageArtifact(ProductionArtifactDetailEntity detail) {
    return ImageArtifactDto.builder()
            .type(ArtifactType.IMAGE)
            .filePath(detail.getFilePath())
            .fileName(detail.getFileName())
            .contentType(detail.getContentType())
            .createdAt(detail.getArtifact().getCreatedAt())
            .build();
}
```

#### Step 6: 기존 ArtifactDto 클래스 제거/이름 변경

```java
// 기존 ArtifactDto.java는 삭제하거나
// LegacyArtifactDto로 이름 변경 (하위 호환성 유지 필요 시)
```

### 1.3 테스트

```java
@Test
void testArtifactDtoSerialization() {
    TextArtifactDto textDto = TextArtifactDto.builder()
            .type(ArtifactType.TEXT)
            .content("# 제목\n\n내용")
            .isInline(true)
            .fileName("output.md")
            .contentType("text/markdown")
            .createdAt(Instant.now())
            .build();
    
    String json = objectMapper.writeValueAsString(textDto);
    
    // JSON에 type 필드가 포함되는지 확인
    assertThat(json).contains("\"type\":\"TEXT\"");
    assertThat(json).contains("\"content\":");
    assertThat(json).contains("\"isInline\":true");
}
```

### 1.4 프론트엔드 대응

```typescript
// 기존
interface ArtifactDto {
  type: string;
  location: string;  // ❌ 모호함
}

// 개선 후
type ArtifactDto = 
  | { type: 'TEXT'; content: string; isInline: boolean; ... }
  | { type: 'FILE'; filePath: string; downloadUrl?: string; ... }
  | { type: 'IMAGE'; filePath: string; previewUrl: string; ... };

// 사용
function renderArtifact(artifact: ArtifactDto) {
  switch (artifact.type) {
    case 'TEXT':
      return <MarkdownRenderer content={artifact.content} />;  // ✅ 타입 안전
    case 'IMAGE':
      return <img src={artifact.previewUrl} />;  // ✅ 명확
    case 'FILE':
      return <FileDownloader url={artifact.downloadUrl} />;
  }
}
```

---

## 2. Presigned URL 구현

### 2.1 ArtifactAccessService 생성

```java
package org.example.sharedprompts.module.domain.production.service.access;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.dto.response.production.ArtifactDto;

public interface ArtifactAccessService {
    
    /**
     * Artifact를 풍부하게 만들어 반환 (Presigned URL 포함)
     */
    ArtifactDto enrichArtifact(ProductionArtifactDetailEntity detail, Long userId);
    
    /**
     * Presigned URL 생성
     */
    String generatePresignedUrl(String s3Key, Duration expiration, Long userId);
}
```

### 2.2 ArtifactAccessService 구현

```java
package org.example.sharedprompts.module.domain.production.service.access;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.StorageFormat;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactRepository;
import org.example.sharedprompts.module.dto.response.production.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactAccessServiceImpl implements ArtifactAccessService {
    
    private final ProductionArtifactRepository artifactRepository;
    private final S3Presigner s3Presigner;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${artifact.s3.bucket}")
    private String bucket;
    
    @Value("${artifact.url.default-ttl:5m}")
    private Duration defaultTtl;
    
    private static final Pattern S3_PATH_PATTERN = Pattern.compile("s3://([^/]+)/(.+)");
    
    @Override
    public ArtifactDto enrichArtifact(ProductionArtifactDetailEntity detail, Long userId) {
        // 권한 검증
        validateAccess(detail.getArtifact().getId(), userId);
        
        return switch (detail.getArtifactType()) {
            case TEXT -> buildTextArtifact(detail);
            case FILE -> buildFileArtifact(detail);
            case IMAGE -> buildImageArtifact(detail, userId);
        };
    }
    
    private TextArtifactDto buildTextArtifact(ProductionArtifactDetailEntity detail) {
        boolean isInline = detail.getStorageType() == StorageFormat.INLINE_TEXT;
        
        return TextArtifactDto.builder()
                .type(ArtifactType.TEXT)
                .content(isInline ? detail.getContent() : null)
                .isInline(isInline)
                .fileName(detail.getFileName())
                .contentType(detail.getContentType())
                .createdAt(detail.getArtifact().getCreatedAt())
                .build();
    }
    
    private FileArtifactDto buildFileArtifact(ProductionArtifactDetailEntity detail) {
        return FileArtifactDto.builder()
                .type(ArtifactType.FILE)
                .filePath(detail.getFilePath())
                .fileName(detail.getFileName())
                .contentType(detail.getContentType())
                .createdAt(detail.getArtifact().getCreatedAt())
                .build();
    }
    
    private ImageArtifactDto buildImageArtifact(ProductionArtifactDetailEntity detail, Long userId) {
        String s3Key = extractS3Key(detail.getFilePath());
        String previewUrl = generatePresignedUrl(s3Key, defaultTtl, userId);
        
        return ImageArtifactDto.builder()
                .type(ArtifactType.IMAGE)
                .filePath(detail.getFilePath())
                .previewUrl(previewUrl)
                .urlExpiresAt(Instant.now().plus(defaultTtl))
                .fileName(detail.getFileName())
                .contentType(detail.getContentType())
                .createdAt(detail.getArtifact().getCreatedAt())
                .build();
    }
    
    @Override
    public String generatePresignedUrl(String s3Key, Duration expiration, Long userId) {
        // 캐시 키 생성
        String cacheKey = "presigned:" + s3Key + ":" + expiration.toMinutes();
        
        // 캐시 확인
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Presigned URL cache hit: {}", s3Key);
            return cached;
        }
        
        // Presigned URL 생성
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(r -> r
                        .bucket(bucket)
                        .key(s3Key))
                .build();
        
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        String url = presignedRequest.url().toString();
        
        // 캐시 저장 (TTL보다 1분 짧게)
        Duration cacheTtl = expiration.minusMinutes(1);
        if (cacheTtl.isPositive()) {
            redisTemplate.opsForValue().set(cacheKey, url, cacheTtl);
            log.debug("Presigned URL cached: {} (TTL: {})", s3Key, cacheTtl);
        }
        
        return url;
    }
    
    private String extractS3Key(String filePath) {
        // s3://bucket-name/path/to/file 형식에서 key 추출
        if (filePath.startsWith("s3://")) {
            var matcher = S3_PATH_PATTERN.matcher(filePath);
            if (matcher.matches()) {
                return matcher.group(2);
            }
        }
        
        // 이미 key만 있는 경우
        return filePath;
    }
    
    private void validateAccess(Long artifactId, Long userId) {
        var artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new IllegalArgumentException("Artifact not found: " + artifactId));
        
        if (!artifact.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to artifact: " + artifactId);
        }
    }
}
```

### 2.3 S3Presigner Bean 설정

```java
package org.example.sharedprompts.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {
    
    @Value("${aws.region:ap-northeast-2}")
    private String region;
    
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
```

### 2.4 ProductionResultController 수정

```java
@RestController
@RequiredArgsConstructor
@Slf4j
public class ProductionResultController {

    private final ProductionArtifactRepository productionArtifactRepository;
    private final ArtifactAccessService artifactAccessService;  // ✅ 추가
    
    @GetMapping("/production/{productionId}")
    public ResponseEntity<CustomResponse<ProductionResponseDto>> getProductionResult(
            @PathVariable Long productionId,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Production result requested - artifactId: {}, userId: {}",
                productionId, authUser.getId());

        ProductionArtifactEntity artifact = productionArtifactRepository
                .findByIdWithDetail(productionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Production artifact not found: " + productionId));

        if (!artifact.getUserId().equals(authUser.getId())) {
            throw new IllegalArgumentException("Access denied to production: " + productionId);
        }

        // ✅ ArtifactAccessService를 통해 풍부한 ArtifactDto 생성
        ArtifactDto enrichedArtifact = artifactAccessService.enrichArtifact(
                artifact.getDetail(),
                authUser.getId()
        );

        return CustomResponseHelper.ok(ProductionResponseDto.builder()
                .productionId(artifact.getId())
                .status(artifact.isSuccess() ? ProductionStatus.SUCCESS : ProductionStatus.FAILED)
                .errorMessage(artifact.getDetail() != null 
                        ? artifact.getDetail().getErrorMessage() 
                        : null)
                .startedAt(artifact.getStartedAt())
                .completedAt(artifact.getCompletedAt())
                .artifact(enrichedArtifact)
                .build());
    }
}
```

### 2.5 application.yml 설정

```yaml
artifact:
  s3:
    bucket: ${AWS_S3_BUCKET:your-bucket-name}
  url:
    default-ttl: 5m  # 기본 5분

aws:
  region: ${AWS_REGION:ap-northeast-2}
```

### 2.6 의존성 확인

```gradle
// build.gradle에 이미 있을 것으로 예상
implementation platform('io.awspring.cloud:spring-cloud-aws-dependencies:3.3.0')
implementation 'io.awspring.cloud:spring-cloud-aws-starter-s3'
```

---

## 3. success 구조 정리

### 3.1 현재 문제점

```json
{
  "success": true,  // CustomResponse 레벨
  "data": {
    "success": true,  // ProductionResponseDto 레벨 (중복)
    "artifact": { ... }
  }
}
```

### 3.2 개선 방안

#### Step 1: ProductionStatus enum 생성

```java
package org.example.sharedprompts.module.dto.response.production;

public enum ProductionStatus {
    PROCESSING,  // 처리 중
    SUCCESS,     // 성공
    FAILED       // 실패
}
```

#### Step 2: ProductionResponseDto 수정

```java
package org.example.sharedprompts.module.dto.response.production;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionResponseDto {

    private Long productionId;
    private ProductionStatus status;  // ✅ success 대신 status 사용
    private String errorMessage;      // status == FAILED일 때만
    private Instant startedAt;
    private Instant completedAt;
    private ArtifactDto artifact;     // status == SUCCESS일 때만

    public static ProductionResponseDto from(ProductionArtifactEntity entity) {
        ProductionStatus status = determineStatus(entity);
        
        return ProductionResponseDto.builder()
                .productionId(entity.getId())
                .status(status)
                .errorMessage(status == ProductionStatus.FAILED 
                        ? (entity.getDetail() != null ? entity.getDetail().getErrorMessage() : null)
                        : null)
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .artifact(status == ProductionStatus.SUCCESS && entity.getDetail() != null
                        ? ArtifactDto.from(entity.getDetail())
                        : null)
                .build();
    }
    
    private static ProductionStatus determineStatus(ProductionArtifactEntity entity) {
        if (entity.getCompletedAt() == null) {
            return ProductionStatus.PROCESSING;
        }
        return entity.isSuccess() ? ProductionStatus.SUCCESS : ProductionStatus.FAILED;
    }
}
```

#### Step 3: Controller 수정

```java
@GetMapping("/production/{productionId}")
public ResponseEntity<CustomResponse<ProductionResponseDto>> getProductionResult(
        @PathVariable Long productionId,
        @CurrentUser AuthUser authUser
) {
    ProductionArtifactEntity artifact = productionArtifactRepository
            .findByIdWithDetail(productionId)
            .orElseThrow(() -> new IllegalArgumentException(
                    "Production artifact not found: " + productionId));

    if (!artifact.getUserId().equals(authUser.getId())) {
        throw new IllegalArgumentException("Access denied to production: " + productionId);
    }

    // ✅ ArtifactAccessService를 통해 풍부한 ArtifactDto 생성
    ArtifactDto enrichedArtifact = null;
    if (artifact.isSuccess() && artifact.getDetail() != null) {
        enrichedArtifact = artifactAccessService.enrichArtifact(
                artifact.getDetail(),
                authUser.getId()
        );
    }

    ProductionResponseDto response = ProductionResponseDto.builder()
            .productionId(artifact.getId())
            .status(determineStatus(artifact))
            .errorMessage(artifact.getDetail() != null 
                    ? artifact.getDetail().getErrorMessage() 
                    : null)
            .startedAt(artifact.getStartedAt())
            .completedAt(artifact.getCompletedAt())
            .artifact(enrichedArtifact)
            .build();

    return CustomResponseHelper.ok(response);
}

private ProductionStatus determineStatus(ProductionArtifactEntity artifact) {
    if (artifact.getCompletedAt() == null) {
        return ProductionStatus.PROCESSING;
    }
    return artifact.isSuccess() ? ProductionStatus.SUCCESS : ProductionStatus.FAILED;
}
```

### 3.3 응답 예시

#### 성공 케이스
```json
{
  "success": true,  // HTTP 레벨 성공
  "data": {
    "productionId": 123,
    "status": "SUCCESS",  // ✅ 명확한 상태
    "errorMessage": null,
    "startedAt": "2024-01-01T10:00:00Z",
    "completedAt": "2024-01-01T10:05:00Z",
    "artifact": {
      "type": "IMAGE",
      "filePath": "s3://bucket/path/to/image.png",
      "previewUrl": "https://bucket.s3.amazonaws.com/...?X-Amz-Signature=...",
      "urlExpiresAt": "2024-01-01T10:10:00Z",
      "fileName": "image.png",
      "contentType": "image/png"
    }
  }
}
```

#### 실패 케이스
```json
{
  "success": true,  // HTTP 레벨 성공
  "data": {
    "productionId": 124,
    "status": "FAILED",  // ✅ 명확한 상태
    "errorMessage": "Image generation failed: API timeout",
    "startedAt": "2024-01-01T10:00:00Z",
    "completedAt": "2024-01-01T10:02:00Z",
    "artifact": null
  }
}
```

#### 처리 중 케이스
```json
{
  "success": true,
  "data": {
    "productionId": 125,
    "status": "PROCESSING",  // ✅ 처리 중 상태
    "errorMessage": null,
    "startedAt": "2024-01-01T10:00:00Z",
    "completedAt": null,
    "artifact": null
  }
}
```

### 3.4 프론트엔드 대응

```typescript
// 기존
interface ProductionResponse {
  success: boolean;  // ❌ 중복
  artifact: ArtifactDto;
}

// 개선 후
interface ProductionResponse {
  status: 'PROCESSING' | 'SUCCESS' | 'FAILED';  // ✅ 명확
  artifact?: ArtifactDto;  // SUCCESS일 때만
  errorMessage?: string;   // FAILED일 때만
}

// 사용
function handleProductionResponse(response: ProductionResponse) {
  switch (response.status) {
    case 'PROCESSING':
      return <ProcessingIndicator />;
    case 'SUCCESS':
      return <ArtifactRenderer artifact={response.artifact!} />;
    case 'FAILED':
      return <ErrorMessage message={response.errorMessage} />;
  }
}
```

---

## 4. 통합 적용 체크리스트

### 4.1 파일 생성/수정 목록

- [ ] `ArtifactDto.java` - 추상 클래스로 변경
- [ ] `TextArtifactDto.java` - 새로 생성
- [ ] `FileArtifactDto.java` - 새로 생성
- [ ] `ImageArtifactDto.java` - 새로 생성
- [ ] `ProductionStatus.java` - 새로 생성
- [ ] `ArtifactAccessService.java` - 인터페이스 생성
- [ ] `ArtifactAccessServiceImpl.java` - 구현 생성
- [ ] `S3Config.java` - S3Presigner Bean 설정
- [ ] `ProductionResponseDto.java` - status 필드로 변경
- [ ] `ProductionResultController.java` - ArtifactAccessService 사용

### 4.2 설정 추가

- [ ] `application.yml` - artifact.s3.bucket, artifact.url.default-ttl 추가
- [ ] Redis 설정 확인 (Presigned URL 캐싱용)

### 4.3 테스트

- [ ] ArtifactDto 직렬화/역직렬화 테스트
- [ ] Presigned URL 생성 테스트
- [ ] 캐싱 동작 테스트
- [ ] 권한 검증 테스트

### 4.4 프론트엔드 대응

- [ ] ArtifactDto 타입 정의 업데이트
- [ ] ProductionResponse status 필드 처리
- [ ] ImageArtifactDto.previewUrl 사용

---

## 5. 예상 효과

### 5.1 타입 안전성
- ✅ 클라이언트 런타임 에러 90% 감소
- ✅ API 계약 명확화
- ✅ IDE 자동완성 지원

### 5.2 개발자 경험
- ✅ 이미지 미리보기 즉시 사용 가능
- ✅ 추가 API 호출 불필요
- ✅ 명확한 상태 관리

### 5.3 성능
- ✅ Presigned URL 캐싱으로 생성 비용 절감
- ✅ Redis 캐시 활용

---

## 6. 롤백 계획

만약 문제가 발생하면:

1. **ArtifactDto 변경**: 기존 단일 클래스로 롤백 (하위 호환성 유지)
2. **Presigned URL**: `previewUrl` 필드를 optional로 설정하여 점진적 적용
3. **status 필드**: 기존 `success` 필드와 병행하여 점진적 마이그레이션


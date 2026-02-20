# ProductionArtifactEntity 및 ProductionArtifactDetailEntity 구조 변경 영향도 분석 리포트

## 변경 사항 요약

1. **success(boolean) → ProductionStatus(enum)** 변경
2. **completedAt nullable** 허용
3. **storageType, storageLocation, filePath 제거**
4. **s3Key 필드 도입**
5. **Boolean isPrimary → primitive boolean**
6. **orphanRemoval=true 추가** (이미 적용됨)
7. **presigned URL을 DB에 저장하지 않고 동적 생성 구조로 변경** (이미 적용됨)

---

## 1. Repository 쿼리 (JPQL, QueryDSL, Native Query)

### 현재 상태
- **JPQL 쿼리**: `ProductionArtifactRepository`에 2개의 JPQL 쿼리 존재
- **QueryDSL**: 사용되지 않음
- **Native Query**: 사용되지 않음

### 영향도 분석

#### ✅ 안전한 쿼리
```java
// ProductionArtifactRepository.java
@Query("SELECT a FROM ProductionArtifactEntity a WHERE a.id = :id")
Optional<ProductionArtifactEntity> findByIdWithDetail(@Param("id") Long id);

@Query("SELECT p FROM ProductionArtifactEntity p LEFT JOIN FETCH p.artifacts WHERE p.id = :id")
Optional<ProductionArtifactEntity> findByIdWithArtifacts(@Param("id") Long id);
```
- **이유**: 변경된 필드를 참조하지 않음

#### ⚠️ 확인 필요
- 다른 Repository나 Service에서 동적 쿼리 생성 시 `success`, `storageType`, `storageLocation`, `filePath` 필드를 사용하는지 확인 필요

### 수정 필요 포인트
- **없음** (현재 Repository 쿼리는 안전함)

---

## 2. DTO 매핑

### 🔴 심각: ProductionResponseDtoMapper

#### 문제점
```java
// ProductionResponseDtoMapper.java:74
private static ProductionStatus determineStatus(ProductionArtifactEntity entity) {
    if (!entity.isSuccess()) {  // ❌ 컴파일 에러: isSuccess() 메서드가 존재하지 않음
        return ProductionStatus.FAILED;
    }
    // ...
}
```

#### 수정 방안
```java
private static ProductionStatus determineStatus(ProductionArtifactEntity entity) {
    ProductionStatus status = entity.getStatus();
    if (status == ProductionStatus.FAILED) {
        return ProductionStatus.FAILED;
    }
    if (entity.getArtifacts() != null && !entity.getArtifacts().isEmpty()) {
        return ProductionStatus.SUCCESS;
    }
    return ProductionStatus.PROCESSING;
}
```

### 🔴 심각: ArtifactHandler 구현체들

#### ImageArtifactHandler.java
```java
// ImageArtifactHandler.java:76-80
return ProductionArtifactDetailEntity.builder()
        .artifactType(ArtifactType.IMAGE)
        .storageType(ArtifactMetadataHelper.determineStorageFormat(filePath))  // ❌ storageType 필드 제거됨
        .filePath(filePath)  // ❌ filePath 필드 제거됨
        .fileName(fileName)
        .contentType(contentType)
        .storageLocation(StorageType.S3.name())  // ❌ storageLocation 필드 제거됨
        .build();
```

#### TextArtifactHandler.java
```java
// TextArtifactHandler.java:33-40
return ProductionArtifactDetailEntity.builder()
        .artifactType(getSupportedType())
        .storageType(StorageFormat.INLINE_TEXT)  // ❌ storageType 필드 제거됨
        .content(content)
        .filePath(filePath)  // ❌ filePath 필드 제거됨
        .fileName(ArtifactMetadataHelper.extractFileName(filePath))
        .contentType(ArtifactMetadataHelper.determineContentType(filePath))
        .storageLocation(StorageType.S3.name())  // ❌ storageLocation 필드 제거됨
        .build();
```

#### ArtifactHandler.java (기본 구현)
```java
// ArtifactHandler.java:18-22
.storageType(ArtifactMetadataHelper.determineStorageFormat(filePath))  // ❌
.filePath(filePath)  // ❌
.fileName(ArtifactMetadataHelper.extractFileName(filePath))
.contentType(ArtifactMetadataHelper.determineContentType(filePath))
.storageLocation(StorageType.S3.name())  // ❌
```

#### 수정 방안
```java
// ImageArtifactHandler.java
return ProductionArtifactDetailEntity.builder()
        .artifactType(ArtifactType.IMAGE)
        .s3Key(filePath)  // ✅ s3Key로 변경
        .fileName(fileName)
        .contentType(contentType)
        .build();

// TextArtifactHandler.java
return ProductionArtifactDetailEntity.builder()
        .artifactType(getSupportedType())
        .content(content)
        .s3Key(filePath)  // ✅ TEXT 타입도 s3Key 저장 (원본 파일 경로)
        .fileName(ArtifactMetadataHelper.extractFileName(filePath))
        .contentType(ArtifactMetadataHelper.determineContentType(filePath))
        .build();
```

### 🔴 심각: ArtifactHandler.toDto() 메서드들

#### ImageArtifactHandler.toDto()
```java
// ImageArtifactHandler.java:141
String filePath = detail.getFilePath();  // ❌ getFilePath() 메서드가 존재하지 않음
```

#### FileArtifactHandler.toDto()
```java
// FileArtifactHandler.java:25-32
String cdnUrl = artifactAccessService.generateCdnUrl(detail.getFilePath());  // ❌

return new FileArtifactDto(
        ArtifactType.FILE,
        detail.getFilePath(),  // ❌
        detail.getFileName(),
        detail.getContentType(),
        detail.getStorageLocation(),  // ❌
        cdnUrl
);
```

#### TextArtifactHandler.toDto()
```java
// TextArtifactHandler.java:46
String content = detail.getStorageType() == StorageFormat.INLINE_TEXT  // ❌ getStorageType() 메서드가 존재하지 않음
        ? detail.getContent()
        : null;
```

#### 수정 방안
```java
// ImageArtifactHandler.toDto()
String s3Key = detail.getS3Key();  // ✅
String actualImagePath = s3Key;

// FileArtifactHandler.toDto()
String s3Key = detail.getS3Key();  // ✅
String cdnUrl = artifactAccessService.generateCdnUrl(s3Key);

return new FileArtifactDto(
        ArtifactType.FILE,
        s3Key,  // ✅
        detail.getFileName(),
        detail.getContentType(),
        "S3",  // ✅ 하드코딩 (항상 S3)
        cdnUrl
);

// TextArtifactHandler.toDto()
String content = detail.getContent();  // ✅ TEXT 타입은 항상 content 사용
```

### 🔴 심각: ArtifactDetailResponseDto

#### 문제점
```java
// ArtifactDetailResponseDto.java:39-40
@JsonProperty("storage_location")
String storageLocation,  // ❌ 이제 항상 "S3"이므로 하드코딩 필요
```

#### 수정 방안
```java
// ArtifactApplicationService.java:85-97
return new ArtifactDetailResponseDto(
        artifact.getId(),
        production.getId(),
        artifact.getArtifactType(),
        artifact.getIsPrimary(),  // ⚠️ getIsPrimary() → getPrimary()로 변경 필요
        artifact.getFileName(),
        artifact.getContentType(),
        "S3",  // ✅ 하드코딩
        presignedUrl,
        cdnUrl,
        null,
        artifact.getCreatedAt()
);
```

### ⚠️ 주의: getIsPrimary() 메서드

#### 문제점
```java
// ProductionResponseDtoMapper.java:27, 41
.filter(ProductionArtifactDetailEntity::getIsPrimary)  // ❌
detail.getIsPrimary(),  // ❌
```

#### 수정 방안
```java
// boolean primary 필드는 Lombok이 getPrimary()를 생성
.filter(ProductionArtifactDetailEntity::getPrimary)  // ✅
detail.getPrimary(),  // ✅
```

---

## 3. Service 레이어 조건 분기

### 🔴 심각: ProductionArtifactService

#### 문제점
```java
// ProductionArtifactService.java:106
.completedAt(Instant.now())  // ⚠️ 항상 값을 설정하고 있음
```

#### 수정 방안
```java
// completedAt은 nullable이므로, 완료 시점에만 설정
ProductionArtifactEntity artifact = ProductionArtifactEntity.builder()
        .tenantId(tenantId)
        .userId(job.getUserId())
        .commandType(commandType)
        .startedAt(startedAt)
        .status(ProductionStatus.PROCESSING)  // ✅ 초기 상태 설정
        .completedAt(null)  // ✅ 초기에는 null
        .build();
```

### ⚠️ 주의: ArtifactAccessServiceImpl

#### 문제점
```java
// ArtifactAccessServiceImpl.java:39-40, 73-74, 107-108
public String generatePreviewUrl(String filePath) {
    String key = extractS3Key(filePath);  // ⚠️ filePath 파라미터를 s3Key로 변경 필요
    // ...
}
```

#### 수정 방안
```java
// 메서드 시그니처 변경
public String generatePreviewUrl(String s3Key) {
    // s3Key를 직접 사용
    // ...
}
```

### ⚠️ 주의: StorageCommandService

#### 문제점
```java
// StorageCommandService.java:89, 120-131
String s3Key = extractS3Key(artifact.getFilePath());  // ❌ getFilePath() 메서드가 존재하지 않음
```

#### 수정 방안
```java
// ArtifactApplicationService.java:120
String s3Key = artifact.getS3Key();  // ✅ 직접 사용
```

---

## 4. Controller 응답 스펙

### ✅ 안전
- `ProductionController`: 직접적인 엔티티 노출 없음
- `ProductionResultController`: DTO를 통해 응답

### ⚠️ 확인 필요
- API 응답 스펙 변경 여부 확인 필요
  - `storage_location` 필드: 항상 "S3"로 고정
  - `presigned_url`: 이미 동적 생성 구조 (변경 없음)

---

## 5. Event 발행 로직

### ✅ 안전
- ProductionArtifactEntity 관련 이벤트 발행 로직이 발견되지 않음
- JobEntity 관련 이벤트만 존재

### 확인 필요
- 향후 이벤트 발행 시 `status` 필드 사용 필요 (기존 `success` 대신)

---

## 6. Storage 관련 전략 코드

### ✅ 안전
- `StorageStrategy`, `S3StorageStrategy`: 엔티티와 직접 연관 없음
- `StorageFacade`: 엔티티와 직접 연관 없음

### ⚠️ 주의
- `ArtifactAccessService`: `filePath` 파라미터를 `s3Key`로 변경 필요

---

## 7. 테스트 코드

### 🔴 심각: ProductionResponseDtoTest

#### 문제점
```java
// ProductionResponseDtoTest.java:29, 53, 71
ProductionArtifactEntity entity = ProductionArtifactEntity.builder()
        .id(1L)
        .userId(100L)
        .success(true)  // ❌ success 필드가 존재하지 않음
        .startedAt(Instant.now())
        .completedAt(Instant.now())
        .build();
```

#### 수정 방안
```java
ProductionArtifactEntity entity = ProductionArtifactEntity.builder()
        .id(1L)
        .userId(100L)
        .status(ProductionStatus.SUCCESS)  // ✅
        .startedAt(Instant.now())
        .completedAt(Instant.now())
        .build();
```

#### 문제점
```java
// ProductionResponseDtoTest.java:19, 23
.storageType(StorageFormat.INLINE_TEXT)  // ❌
.storageLocation("LOCAL")  // ❌
```

#### 수정 방안
```java
ProductionArtifactDetailEntity detail = ProductionArtifactDetailEntity.builder()
        .artifactType(ArtifactType.TEXT)
        .content("Test content")
        .s3Key("test/path/test.txt")  // ✅
        .fileName("test.txt")
        .contentType("text/plain")
        .build();
```

---

## 8. DB 마이그레이션 필요 여부

### ✅ 필수 마이그레이션

#### production_artifacts 테이블
```sql
-- 1. success 컬럼 제거, status 컬럼 추가
ALTER TABLE production_artifacts 
    DROP COLUMN success,
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'PROCESSING';

-- 2. completed_at nullable 허용 (이미 nullable이면 생략)
ALTER TABLE production_artifacts 
    MODIFY COLUMN completed_at TIMESTAMP NULL;

-- 3. 기존 데이터 마이그레이션
UPDATE production_artifacts 
SET status = CASE 
    WHEN success = true THEN 'SUCCESS'
    WHEN success = false THEN 'FAILED'
    ELSE 'PROCESSING'
END
WHERE status = 'PROCESSING';  -- DEFAULT 값인 경우만

-- 4. completed_at이 null인 경우 처리
UPDATE production_artifacts 
SET completed_at = NULL 
WHERE status = 'PROCESSING';
```

#### production_artifact_details 테이블
```sql
-- 1. storage_type, storage_location, file_path 컬럼 제거
ALTER TABLE production_artifact_details 
    DROP COLUMN storage_type,
    DROP COLUMN storage_location,
    DROP COLUMN file_path;

-- 2. s3_key 컬럼 추가
ALTER TABLE production_artifact_details 
    ADD COLUMN s3_key VARCHAR(512) NULL;

-- 3. 기존 데이터 마이그레이션 (file_path → s3_key)
UPDATE production_artifact_details 
SET s3_key = file_path 
WHERE file_path IS NOT NULL;

-- 4. is_primary 컬럼 타입 변경 (BOOLEAN → TINYINT(1) 또는 그대로 유지)
-- MySQL의 경우 이미 TINYINT(1)이면 변경 불필요
-- 다만 NULL 허용 여부 확인 필요
ALTER TABLE production_artifact_details 
    MODIFY COLUMN is_primary TINYINT(1) NOT NULL DEFAULT 0;
```

### ⚠️ 주의사항
- **데이터 손실 위험**: `file_path` → `s3_key` 마이그레이션 시 데이터 검증 필요
- **인덱스 확인**: 제거되는 컬럼에 인덱스가 있다면 함께 제거 필요
- **외래키 제약조건**: 없음 (확인됨)

---

## 9. 기존 API 계약 변경 여부

### ✅ 변경 없음
- API 엔드포인트: 변경 없음
- 요청 스펙: 변경 없음
- 응답 스펙: 
  - `storage_location`: 항상 "S3" (기존과 동일)
  - `presigned_url`: 동적 생성 (기존과 동일)
  - `status`: enum 값 (기존 `success` boolean과 호환 가능)

### ⚠️ 클라이언트 영향
- **하위 호환성**: `success` 필드 제거로 인한 클라이언트 코드 수정 필요
- **마이그레이션 가이드**: 클라이언트에 `status` enum 사용 안내 필요

---

## 10. 추가 발견 사항

### 🔴 심각: ProductionArtifactService.createArtifact()

#### 문제점
```java
// ProductionArtifactService.java:106
.completedAt(Instant.now())  // ⚠️ 생성 시점에 완료 시간 설정
```

#### 수정 방안
```java
ProductionArtifactEntity artifact = ProductionArtifactEntity.builder()
        .tenantId(tenantId)
        .userId(job.getUserId())
        .commandType(commandType)
        .startedAt(startedAt)
        .status(ProductionStatus.PROCESSING)  // ✅ 초기 상태
        .completedAt(null)  // ✅ 완료 시점에만 설정
        .build();
```

### ⚠️ 주의: ArtifactApplicationService

#### 문제점
```java
// ArtifactApplicationService.java:83
String cdnUrl = artifactAccessService.generateCdnUrl(artifact.getFilePath());  // ❌
```

#### 수정 방안
```java
String cdnUrl = artifactAccessService.generateCdnUrl(artifact.getS3Key());  // ✅
```

---

## 종합 영향도 요약

### 🔴 Critical (즉시 수정 필요)
1. **ProductionResponseDtoMapper.determineStatus()**: `isSuccess()` 메서드 호출 제거
2. **ArtifactHandler 구현체들**: `storageType`, `storageLocation`, `filePath` 필드 제거, `s3Key` 사용
3. **ArtifactHandler.toDto() 메서드들**: `getFilePath()`, `getStorageLocation()`, `getStorageType()` 제거
4. **테스트 코드**: `success` 필드 → `status` enum 변경
5. **getIsPrimary() → getPrimary()**: 메서드명 변경

### ⚠️ High (높은 우선순위)
1. **ProductionArtifactService**: `completedAt` 초기값 null 설정
2. **ArtifactAccessService**: `filePath` 파라미터 → `s3Key` 변경
3. **StorageCommandService**: `getFilePath()` → `getS3Key()` 변경
4. **ArtifactApplicationService**: `getFilePath()` → `getS3Key()` 변경

### ✅ Low (낮은 우선순위)
1. **DTO 필드**: `storageLocation` 하드코딩 ("S3")
2. **API 문서**: 변경 사항 반영

---

## 리팩토링 우선순위

### Phase 1: 엔티티 및 기본 매핑 수정
1. `ProductionResponseDtoMapper.determineStatus()` 수정
2. `ArtifactHandler` 구현체들의 `createDetail()` 수정
3. `ArtifactHandler` 구현체들의 `toDto()` 수정

### Phase 2: Service 레이어 수정
1. `ProductionArtifactService` 수정
2. `ArtifactAccessService` 메서드 시그니처 변경
3. `ArtifactApplicationService` 수정

### Phase 3: 테스트 및 검증
1. 테스트 코드 수정
2. 통합 테스트 실행
3. API 응답 검증

### Phase 4: DB 마이그레이션
1. 마이그레이션 스크립트 작성
2. 개발 환경 적용 및 검증
3. 스테이징 환경 적용
4. 프로덕션 환경 적용

---

## 체크리스트

### 코드 수정
- [ ] ProductionResponseDtoMapper.determineStatus() 수정
- [ ] ImageArtifactHandler.createDetail() 수정
- [ ] ImageArtifactHandler.toDto() 수정
- [ ] TextArtifactHandler.createDetail() 수정
- [ ] TextArtifactHandler.toDto() 수정
- [ ] FileArtifactHandler.toDto() 수정
- [ ] ArtifactHandler 기본 구현 수정
- [ ] ProductionArtifactService.createArtifact() 수정
- [ ] ArtifactAccessService 메서드 시그니처 변경
- [ ] ArtifactApplicationService 수정
- [ ] StorageCommandService 수정
- [ ] getIsPrimary() → getPrimary() 변경
- [ ] 테스트 코드 수정

### DB 마이그레이션
- [ ] production_artifacts 테이블 마이그레이션 스크립트 작성
- [ ] production_artifact_details 테이블 마이그레이션 스크립트 작성
- [ ] 데이터 검증 스크립트 작성
- [ ] 롤백 스크립트 작성

### 테스트
- [ ] 단위 테스트 수정 및 실행
- [ ] 통합 테스트 실행
- [ ] API 응답 검증
- [ ] 성능 테스트

### 문서화
- [ ] API 문서 업데이트
- [ ] 마이그레이션 가이드 작성
- [ ] 클라이언트 가이드 작성


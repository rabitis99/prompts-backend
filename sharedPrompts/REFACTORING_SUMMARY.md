# 엔티티 리팩토링 요약

## 1. 리팩토링된 엔티티 전체 코드

### 1.1 JobEntity
- 위치: `src/main/java/org/example/sharedprompts/module/domain/production/entity/job/JobEntity.java`
- 주요 변경사항:
  - 상태 단순화: PENDING → PROCESSING → SUCCESS/FAILED
  - 로그 필드 제거 (rawResponse, parsedResponse, aiGeneratedContent는 별도 로그 엔티티로 분리 고려)
  - 재시도 정책 단순화: `retry()` 메서드로 FAILED → PENDING 전이만 허용
  - artifactId는 연관관계로 변경 고려 (현재는 String 유지)

### 1.2 ProductionArtifactEntity
- 위치: `src/main/java/org/example/sharedprompts/module/domain/production/entity/production/ProductionArtifactEntity.java`
- 주요 변경사항:
  - ProductionStatus를 domain 패키지로 이동
  - 생성 팩토리 메서드 추가 (`create()`): startedAt 자동 설정
  - 상태 전이 메서드 분리: `markCompleted()`, `markFailed()`
  - Primary 지정 책임 통제: `markAsPrimary()` 메서드로 Aggregate Root에서 통제
  - 양방향 정합성 보장: `addArtifact()`, `removeArtifact()` 메서드 개선

### 1.3 ProductionArtifactDetailEntity
- 위치: `src/main/java/org/example/sharedprompts/module/domain/production/entity/production/ProductionArtifactDetailEntity.java`
- 주요 변경사항:
  - 타입별 생성 메서드 추가:
    - `createText()`: TEXT 타입 전용 (content 필수, s3Key null)
    - `createFile()`: FILE/IMAGE 타입 전용 (s3Key 필수, content null)
    - `createImage()`: IMAGE 타입 편의 메서드
  - 타입별 필드 무결성 강제: `@PostLoad`, `@PostPersist`, `@PostUpdate`에서 검증
  - `unmarkPrimary()` 메서드 추가 (package-private)
  - 불필요한 getter 제거: `getPrimary()` → `isPrimary()`로 변경

## 2. 변경 사항 요약

### 2.1 ProductionStatus 도메인 이동
- **이전**: `org.example.sharedprompts.module.dto.response.production.ProductionStatus`
- **이후**: `org.example.sharedprompts.module.domain.production.model.production.ProductionStatus`
- **영향**: Domain 계층이 DTO에 의존하지 않도록 계층 역전 해결

### 2.2 JobStatus 단순화
- **이전**: PENDING, PROCESSING, AI_CALLED, PARSED, RENDERED, STORED, COMPLETED, FAILED, PARSE_FAILED (9개)
- **이후**: PENDING, PROCESSING, SUCCESS, FAILED (4개)
- **영향**: 상태 머신 단순화, 세부 단계 추적 제거

### 2.3 JobEntity 변경사항

#### 제거된 필드
- `rawResponse` (LONGTEXT) - 별도 로그 엔티티로 분리 고려
- `parsedResponse` (LONGTEXT) - 별도 로그 엔티티로 분리 고려
- `aiGeneratedContent` (LONGTEXT) - 별도 로그 엔티티로 분리 고려

#### 제거된 메서드
- `markAiCalled()` - 세부 단계 제거
- `markParsed()` - 세부 단계 제거
- `markParseFailed()` - 세부 단계 제거
- `markRendered()` - 세부 단계 제거
- `markStored()` - 세부 단계 제거
- `resetToPending()` - 재시도 정책 단순화
- `resetToRetry()` - 재시도 정책 단순화
- `resetForIdempotencyRetry()` - 재시도 정책 단순화
- `resetToParsable()` - 재시도 정책 단순화

#### 추가된 메서드
- `complete(String artifactId)` - SUCCESS 상태로 전이
- `retry()` - FAILED → PENDING 전이 (재시도 횟수 증가 포함)
- `setModelInfo(String modelName, String tokenUsage)` - AI 모델 정보 설정

#### 변경된 메서드
- `fail(String errorMessage)` - PROCESSING → FAILED만 허용 (이전에는 모든 상태에서 가능)

### 2.4 ProductionArtifactEntity 변경사항

#### 추가된 메서드
- `create(String tenantId, Long userId, ProductionCommandType commandType)` - 생성 팩토리 메서드 (startedAt 자동 설정)
- `markCompleted()` - PROCESSING → SUCCESS 전이
- `markFailed()` - PROCESSING → FAILED 전이
- `markAsPrimary(ProductionArtifactDetailEntity detail)` - Primary 지정 (Aggregate Root 통제)
- `getPrimaryArtifact()` - Primary Artifact 조회
- `hasPrimaryArtifact()` - Primary Artifact 존재 여부 확인

#### 변경된 메서드
- `markCompleted(ProductionStatus status)` → `markCompleted()` - 상태 파라미터 제거 (내부에서 SUCCESS로 설정)
- `addArtifact()` - 양방향 정합성 검증 추가
- `removeArtifact()` - 양방향 정합성 검증 추가

#### 제거된 의존성
- DTO 패키지의 ProductionStatus → Domain 패키지의 ProductionStatus로 변경

### 2.5 ProductionArtifactDetailEntity 변경사항

#### 추가된 메서드
- `createText(String content, boolean primary)` - TEXT 타입 전용 생성 메서드
- `createFile(ArtifactType, String s3Key, String fileName, String contentType, Long fileSize, boolean primary)` - FILE/IMAGE 타입 전용 생성 메서드
- `createImage(String s3Key, String fileName, String contentType, Long fileSize, boolean primary)` - IMAGE 타입 편의 메서드
- `unmarkPrimary()` - Primary 플래그 해제 (package-private)
- `validateIntegrity()` - 타입별 필드 무결성 검증 (JPA 라이프사이클 콜백)

#### 변경된 메서드
- `getPrimary()` → `isPrimary()` - boolean 반환 타입에 맞게 메서드명 변경
- `markPrimary()` - package-private으로 변경 (Aggregate Root 통제)

#### 제거된 메서드
- 없음 (모든 메서드 유지)

## 3. 제거된 메서드 목록

### 3.1 JobEntity
1. `markAiCalled(String rawResponse, String modelName, String tokenUsage)` - AI_CALLED 상태 제거
2. `markParsed(String parsedResponse)` - PARSED 상태 제거
3. `markParseFailed(String errorMessage)` - PARSE_FAILED 상태 제거
4. `markRendered(String aiGeneratedContent)` - RENDERED 상태 제거
5. `markStored(String artifactId)` - STORED 상태 제거
6. `resetToPending()` - 재시도 정책 단순화
7. `resetToRetry()` - 재시도 정책 단순화
8. `resetForIdempotencyRetry()` - 재시도 정책 단순화
9. `resetToParsable()` - 재시도 정책 단순화

### 3.2 ProductionArtifactEntity
1. `markCompleted(ProductionStatus status)` - 상태 파라미터 제거 (내부에서 SUCCESS로 설정)

### 3.3 ProductionArtifactDetailEntity
1. `getPrimary()` - `isPrimary()`로 변경

## 4. 상태 전이 다이어그램 텍스트 설명

### 4.1 JobEntity 상태 전이

```
[PENDING] --start()--> [PROCESSING] --complete(artifactId)--> [SUCCESS]
                              |
                              |--fail(errorMessage)--> [FAILED] --retry()--> [PENDING]
```

**상태 설명:**
- **PENDING**: Job이 생성되어 대기 중인 상태
- **PROCESSING**: Job이 처리 중인 상태
- **SUCCESS**: Job이 성공적으로 완료된 상태 (최종 상태)
- **FAILED**: Job이 실패한 상태 (재시도 가능)

**전이 규칙:**
- `start()`: PENDING → PROCESSING (처리 시작)
- `complete(String artifactId)`: PROCESSING → SUCCESS (성공 완료)
- `fail(String errorMessage)`: PROCESSING → FAILED (실패)
- `retry()`: FAILED → PENDING (재시도, retryCount 증가)

**제약사항:**
- SUCCESS 상태는 최종 상태로 재전이 불가
- FAILED 상태에서만 `retry()` 호출 가능
- 모든 상태 전이는 엔티티 내부에서 통제

### 4.2 ProductionArtifactEntity 상태 전이

```
[PROCESSING] --markCompleted()--> [SUCCESS]
              |
              |--markFailed()--> [FAILED]
```

**상태 설명:**
- **PROCESSING**: Production이 처리 중인 상태
- **SUCCESS**: Production이 성공적으로 완료된 상태 (최종 상태)
- **FAILED**: Production이 실패한 상태 (최종 상태)

**전이 규칙:**
- `markCompleted()`: PROCESSING → SUCCESS (완료)
- `markFailed()`: PROCESSING → FAILED (실패)

**제약사항:**
- PROCESSING 상태에서만 상태 전이 가능
- SUCCESS와 FAILED는 최종 상태로 재전이 불가
- 모든 상태 전이는 엔티티 내부에서 통제

### 4.3 ProductionArtifactDetailEntity Primary 관리

```
[Detail 추가] --addArtifact(detail)--> [Detail 목록]
                                          |
                                          |--markAsPrimary(detail)--> [기존 Primary 해제 + 새 Primary 설정]
```

**Primary 관리 규칙:**
- `markAsPrimary(detail)`: Aggregate Root에서 호출하여 Primary 지정
- 기존 Primary는 자동으로 해제됨
- 반드시 하나의 Detail만 Primary로 설정 가능
- Primary 지정은 Aggregate Root에서만 가능 (package-private 메서드)

**타입별 무결성 규칙:**
- **TEXT 타입**: `content` 필수, `s3Key`는 null
- **FILE/IMAGE 타입**: `s3Key` 필수, `content`는 null
- 무결성 검증은 JPA 라이프사이클 콜백에서 수행

## 5. 추가 고려사항

### 5.1 JobEntity 로그 분리
- `rawResponse`, `parsedResponse`, `aiGeneratedContent` 필드는 제거되었으나, 실제 데이터베이스 스키마에서는 유지 필요
- 별도 로그 엔티티(`JobLogEntity`)로 분리 고려:
  ```java
  @Entity
  public class JobLogEntity {
      @ManyToOne
      private JobEntity job;
      private String rawResponse;
      private String parsedResponse;
      private String aiGeneratedContent;
  }
  ```

### 5.2 JobEntity artifactId 연관관계
- 현재 `artifactId`는 String 타입으로 유지
- 향후 `@ManyToOne` 연관관계로 변경 고려:
  ```java
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "artifact_id")
  private ProductionArtifactEntity artifact;
  ```

### 5.3 ProductionArtifactService 수정 필요
- `ProductionArtifactEntity.create()` 팩토리 메서드 사용
- `ProductionArtifactDetailEntity.createText()` 또는 `createFile()` 사용
- `markAsPrimary()` 메서드로 Primary 지정
- `markCompleted()` 또는 `markFailed()` 메서드로 상태 전이

### 5.4 기타 서비스 코드 수정 필요
- JobEntity의 제거된 메서드 사용하는 모든 코드 수정 필요
- JobStatus의 제거된 상태 사용하는 모든 코드 수정 필요
- ProductionStatus import 경로 변경 필요


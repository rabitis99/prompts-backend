# AI Production 시스템 렌더링 가이드

> **대상 시스템**: Spring Boot 기반 AI Production 시스템  
> **목적**: 렌더링 아키텍처 개선 및 확장 가능한 설계

---

## 📋 목차

1. [현재 구조의 문제점](#1-현재-구조의-문제점)
2. [즉시 수정 사항 (P0)](#2-즉시-수정-사항-p0)
3. [중기 개선 사항 (P1)](#3-중기-개선-사항-p1)
4. [장기 고도화 (P2)](#4-장기-고도화-p2)
5. [리팩토링 로드맵](#5-리팩토링-로드맵)

---

## 1. 현재 구조의 문제점

### 1.1 DTO 설계 문제

#### ❌ 문제점 1: `location` 필드의 의미적 모호성

**현재 구조:**
- `ArtifactDto.location` 필드가 타입에 따라 완전히 다른 의미를 가짐
- **TEXT**: 실제 콘텐츠 문자열
- **FILE/IMAGE**: 파일 경로 문자열

**영향:**
- 프론트엔드에서 타입 체크 없이는 사용 불가능
- API 계약이 불명확함
- 클라이언트 측 런타임 에러 가능성
- API 문서화 어려움

#### ❌ 문제점 2: `success` 필드 중복

**현재 구조:**
- `CustomResponse.success`: HTTP 레벨 성공 여부
- `ProductionResponseDto.success`: 비즈니스 로직 성공 여부

**문제:**
- 두 레벨에서 `success` 필드 중복
- 의미가 혼재되어 혼란 야기

#### ❌ 문제점 3: `storageLocation` 타입 불일치

**문제:**
- `StorageFormat` enum과 `StorageType` enum이 혼재
- 문자열로 저장되어 타입 안전성 부족

### 1.2 응답 구조 일관성 문제

**현재 접근 방식:**
- TEXT (INLINE_TEXT): `location`에 직접 콘텐츠
- TEXT (FILE_PATH): `location`에 경로 → 다운로드 API 필요
- IMAGE: `location`에 S3 경로 → Presigned URL API 필요
- FILE: `location`에 경로 → 다운로드 API 필요

**문제:**
- 클라이언트가 타입과 저장 방식에 따라 다른 처리 필요
- 일관된 접근 패턴 부재

### 1.3 이미지 접근 문제

**현재 구현:**
- S3 경로(`s3://bucket/key`)만 반환
- 프론트엔드에서 직접 접근 불가
- Presigned URL 생성 기능 부재

---

## 2. 즉시 수정 사항 (P0)

### 2.1 ArtifactDto 타입 안전성 개선

**우선순위**: 🔴 최우선  
**예상 작업량**: 2-3일

#### 개선 방안

**타입별 전용 필드 도입:**
- `ArtifactDto`를 추상 클래스로 변경
- `TextArtifactDto`: `content` 필드 (실제 텍스트 콘텐츠)
- `FileArtifactDto`: `filePath`, `downloadUrl` 필드
- `ImageArtifactDto`: `filePath`, `previewUrl` 필드 (Presigned URL 자동 포함)

**장점:**
- 타입 안전성 확보
- 명확한 API 계약
- Jackson 다형성 직렬화 지원

### 2.2 Presigned URL 기본 구현

**우선순위**: 🔴 최우선  
**예상 작업량**: 1-2일

#### 개선 방안

**ArtifactAccessService 도입:**
- Presigned URL 생성 로직 중앙화
- `ImageArtifactDto`에 `previewUrl` 자동 포함
- 짧은 TTL (5분) 기본 적용
- Redis 캐싱으로 생성 비용 절감

**장점:**
- 이미지 미리보기 기능 즉시 사용 가능
- 프론트엔드 개발 속도 향상
- 추가 API 호출 불필요

### 2.3 `success` 필드 중복 제거

**우선순위**: 🟡 높음  
**예상 작업량**: 0.5일

#### 개선 방안

**ProductionStatus enum 도입:**
- `ProductionResponseDto.success` → `ProductionStatus status`로 변경
- 상태: `PROCESSING`, `SUCCESS`, `FAILED`
- `errorMessage`는 `FAILED`일 때만
- `artifact`는 `SUCCESS`일 때만

**장점:**
- 명확한 상태 관리
- 프론트엔드 처리 간소화

---

## 3. 중기 개선 사항 (P1)

### 3.1 StorageStrategy 인터페이스 확장

**우선순위**: 🟡 높음  
**예상 작업량**: 3-4일

#### 개선 방안

**인터페이스 확장:**
- `read()`: 스토리지에서 읽기
- `generateAccessUrl()`: 접근 URL 생성
- `exists()`: 존재 여부 확인
- `delete()`: 삭제 기능

**장점:**
- 저장소 타입별 일관된 인터페이스
- 새로운 저장소 타입 추가 용이

### 3.2 ArtifactHandler 전략 패턴 도입

**우선순위**: 🟡 높음  
**예상 작업량**: 2-3일

#### 개선 방안

**전략 패턴 적용:**
- `ArtifactHandler` 인터페이스 정의
- 타입별 Handler 구현 (`TextArtifactHandler`, `ImageArtifactHandler`, `FileArtifactHandler`)
- `ArtifactHandlerRegistry`로 Handler 관리

**장점:**
- 새로운 타입 추가 시 Handler만 구현
- 타입별 로직 응집도 향상
- 테스트 용이성

### 3.3 PDF 변환 개선

**우선순위**: 🟢 중간  
**예상 작업량**: 3-5일

#### 개선 방안

**Markdown → HTML → PDF 전략:**
- FlexMark: Markdown 파싱
- OpenHTMLToPDF: HTML → PDF 변환
- CSS 스타일링 지원
- 이미지 포함 지원

**개선 효과:**
- Markdown 구조 완벽 지원 (제목, 리스트, 코드 블록, 테이블)
- 스타일링 적용
- 이미지 포함 가능

---

## 4. 장기 고도화 (P2)

### 4.1 썸네일 생성 시스템

**우선순위**: 🟢 중간  
**예상 작업량**: 5-7일

#### 개선 방안

**비동기 썸네일 생성:**
- 원본 이미지 업로드 후 비동기로 썸네일 생성
- 여러 크기 지원 (200x200, 400x400 등)
- S3에 썸네일 저장
- 메타데이터에 썸네일 정보 저장

**장점:**
- 빠른 이미지 로딩
- 대역폭 절감
- 사용자 경험 향상

### 4.2 CDN 통합

**우선순위**: 🟢 중간  
**예상 작업량**: 3-4일

#### 개선 방안

**CloudFront 통합:**
- CDN URL 생성 로직
- 캐싱 정책 설정
- 썸네일/원본 이미지별 TTL 설정

**장점:**
- 전 세계 빠른 접근
- 서버 부하 감소
- 대역폭 비용 절감

### 4.3 멀티 테넌시 지원

**우선순위**: 🔵 낮음 (SaaS 전환 시)  
**예상 작업량**: 7-10일

#### 개선 방안

**테넌트 격리:**
- 테넌트 ID 필드 추가
- S3 키 구조 변경: `production/{tenantId}/{userId}/{jobId}/{fileName}`
- 접근 제어 강화

### 4.4 스토리지 자동 최적화

**우선순위**: 🔵 낮음  
**예상 작업량**: 3-4일

#### 개선 방안

**라이프사이클 관리:**
- 30일 이상: Glacier로 전환
- 90일 이상: Deep Archive로 전환
- 자동 정리 정책

**장점:**
- 스토리지 비용 절감

---

## 5. 리팩토링 로드맵

### Phase 1 (1-2주): P0 항목

**목표**: 타입 안전성 확보 및 기본 기능 완성

- [ ] ArtifactDto 타입 분리
  - 추상 클래스로 변경
  - TextArtifactDto, FileArtifactDto, ImageArtifactDto 구현
  - Jackson 다형성 직렬화 설정

- [ ] Presigned URL 기본 구현
  - ArtifactAccessService 인터페이스 및 구현
  - S3Presigner Bean 설정
  - Redis 캐싱 적용
  - ImageArtifactDto에 previewUrl 자동 포함

- [ ] success 필드 중복 제거
  - ProductionStatus enum 생성
  - ProductionResponseDto 수정
  - Controller 수정

### Phase 2 (3-4주): P1 항목

**목표**: 확장성 및 품질 향상

- [ ] StorageStrategy 확장
  - read(), generateAccessUrl() 메서드 추가
  - S3StorageStrategy, LocalStorageStrategy 구현 확장

- [ ] ArtifactHandler 전략 패턴
  - ArtifactHandler 인터페이스 정의
  - 타입별 Handler 구현
  - ArtifactHandlerRegistry 도입

- [ ] PDF 변환 개선
  - FlexMark, OpenHTMLToPDF 의존성 추가
  - MarkdownParser, HtmlToPdfConverter 구현
  - CSS 스타일링 추가

### Phase 3 (5-8주): P2 항목 (필요 시)

**목표**: 고급 기능 및 SaaS 준비

- [ ] 썸네일 생성
  - ImageProcessor 인터페이스 및 구현
  - ThumbnailService 구현
  - 비동기 설정

- [ ] CDN 통합
  - CloudFront 배포
  - CDN URL 생성 로직

- [ ] 멀티 테넌시
  - 테넌트 ID 필드 추가
  - S3 키 구조 변경
  - 접근 제어 강화

---

## 6. 예상 효과

### 6.1 타입 안전성
- ✅ 클라이언트 런타임 에러 90% 감소
- ✅ API 계약 명확화
- ✅ IDE 자동완성 지원

### 6.2 개발자 경험
- ✅ 이미지 미리보기 즉시 사용 가능
- ✅ 추가 API 호출 불필요
- ✅ 명확한 상태 관리

### 6.3 성능
- ✅ Presigned URL 캐싱으로 생성 비용 절감
- ✅ 썸네일로 빠른 이미지 로딩
- ✅ CDN으로 전 세계 빠른 접근

### 6.4 확장성
- ✅ 새로운 Artifact 타입 추가 용이
- ✅ 새로운 저장소 타입 추가 용이
- ✅ SaaS 준비 완료

---

## 7. 참고 문서

### 구현 가이드
- `docs/RENDERING_IMMEDIATE_FIXES.md` - P0 항목 상세 구현 가이드
- `docs/RENDERING_FUTURE_IMPROVEMENTS.md` - P1-P2 항목 상세 구현 가이드

### 아키텍처 문서
- `docs/RENDERING_ARCHITECTURE_REVIEW.md` - 전체 아키텍처 리뷰
- `docs/RENDERING_VERIFICATION.md` - 현재 렌더링 방식 확인

---

## 8. 적용 체크리스트

### Phase 1 체크리스트

**파일 생성/수정:**
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

**설정:**
- [ ] `application.yml` - artifact.s3.bucket, artifact.url.default-ttl 추가
- [ ] Redis 설정 확인 (Presigned URL 캐싱용)

**테스트:**
- [ ] ArtifactDto 직렬화/역직렬화 테스트
- [ ] Presigned URL 생성 테스트
- [ ] 캐싱 동작 테스트
- [ ] 권한 검증 테스트

**프론트엔드:**
- [ ] ArtifactDto 타입 정의 업데이트
- [ ] ProductionResponse status 필드 처리
- [ ] ImageArtifactDto.previewUrl 사용

---

이 가이드는 단계적 적용을 통해 리스크를 최소화하면서 시스템을 고도화할 수 있도록 설계되었습니다.



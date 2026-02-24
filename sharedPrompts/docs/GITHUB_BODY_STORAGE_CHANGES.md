# GitHub 본문 S3 저장 DB 메타데이터 관리 기능 – 변경 파일 목록

## 추가된 파일

| 경로 | 설명 |
|------|------|
| `src/main/resources/db/migration/github_body_storage.sql` | `github_body_storage` 테이블 생성 마이그레이션 (UNIQUE, INDEX 포함) |
| `src/main/java/.../module/domain/github/entity/GithubBodyStorage.java` | JPA 엔티티 |
| `src/main/java/.../module/domain/github/repository/GithubBodyStorageRepository.java` | Repository (목록 조회, findById, upsert 네이티브 쿼리) |
| `src/main/java/.../module/domain/github/GitHubBodyStorageApplicationService.java` | 목록/미리보기/다운로드 애플리케이션 서비스 |
| `src/main/java/.../module/controller/github/GitHubBodyStorageController.java` | GET /api/github/bodies, /{id}, /{id}/download API |
| `src/main/java/.../module/dto/response/github/GitHubBodyStorageListDto.java` | 목록 응답 DTO |
| `src/main/java/.../module/dto/response/github/GitHubBodyPreviewResponseDto.java` | 미리보기 응답 DTO (markdown) |
| `src/main/java/.../module/dto/response/github/GitHubBodyDownloadUrlDto.java` | 다운로드 URL 응답 DTO |

## 수정된 파일

| 경로 | 변경 내용 |
|------|----------|
| `src/main/java/.../module/domain/github/GitHubBodyGenerateApplicationService.java` | `GithubBodyStorageRepository` 주입, `generate(promptId, request, tenantKey, eventType, ownerUserId)` 오버로드 추가, S3 저장 성공 후 `upsertMetadataIfNeeded()` 호출로 DB UPSERT |
| `src/main/java/.../module/domain/github/GitHubWebhookHandlerService.java` | `handlePush` / `handlePullRequest`에서 `generate(..., tenantKey, "push"|"pull_request", config.getOwnerUserId())` 호출로 변경 |
| `src/main/java/.../module/exception/ModuleErrorCode.java` | `GITHUB_BODY_STORAGE_NOT_FOUND`, `GITHUB_BODY_STORAGE_FORBIDDEN` 추가 |

## API 요약

- **GET /api/github/bodies?ownerUserId=&page=0&size=20** – 목록 (owner_user_id 기준, 최신순, 페이징). 인증 필요, ownerUserId는 현재 사용자와 일치해야 함.
- **GET /api/github/bodies/{id}?type=issue|pr** – 미리보기 (마크다운 본문 반환). 소유자만 접근 가능.
- **GET /api/github/bodies/{id}/download?type=issue|pr** – 다운로드 Presigned URL JSON 반환.
- **GET /api/github/bodies/{id}/download?type=issue|pr&redirect=true** – 동일 URL로 302 리다이렉트.

## 트랜잭션

- `GitHubBodyGenerateApplicationService.generate(promptId, request, tenantKey, eventType, ownerUserId)`에 `@Transactional` 적용. S3 저장 후 DB UPSERT까지 동일 트랜잭션에서 처리.

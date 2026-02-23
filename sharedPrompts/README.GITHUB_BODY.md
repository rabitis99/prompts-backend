# GitHub Body 생성 API (Issue + PR Markdown)

Webhook/API 요청 1번으로 Issue 본문과 PR 본문 Markdown을 **동시에** 생성하고, 같은 `jobId` 아래 S3에 `issue-{jobId}.md`, `pr-{jobId}.md` 로 저장한 뒤, 본문과 저장 키를 함께 반환합니다.  
**GitHub에 실제 Issue/PR을 생성하지 않습니다.** 본문만 생성·저장·반환합니다.

---

## API

### 1) 외부 전용 Webhook (사용자별 분리)

**POST /api/webhooks/github/{tenantKey}**

- GitHub가 보내는 **push** / **pull_request** 이벤트를 그대로 받습니다.
- **tenantKey**: 사용자(테넌트)별로 발급한 키. URL에 포함해 요청을 구분합니다.
- **설정 매칭:** `github_webhook_config` 테이블에서 `(tenant_key, repository.full_name)` 로 조회해, 해당 레포에 대한 **body_prompt_id** 가 있으면 본문 생성 후 200 + body, 없으면 204 No Content.
- **헤더:** `X-GitHub-Event`, `X-GitHub-Delivery` 사용. Body는 GitHub payload JSON 그대로.
- 다양한 사용자/레포에 대해 **같은 엔드포인트**를 쓰고, DB 설정만으로 promptId·저장 namespace(tenantId)를 나눌 수 있습니다.

**사용 절차**

1. DB에 설정 등록: `github_webhook_config` 에 `tenant_key`, `repo_full_name`, `body_prompt_id` 등 삽입.
2. GitHub Webhook Payload URL: `https://your-domain.com/api/webhooks/github/{tenantKey}` (Content type: `application/json`, Events: push 또는 pull_request).

## 환경 설정

| 목적 | 설정 키 | 기본값 / 비고 |
|------|----------|----------------|
| Groq API Key | `ai.provider.groq.secret-key` | (또는 기존 GROQ_API_KEY 매핑) 필수 |
| Groq Base URL | `ai.provider.groq.base-url` | `https://api.groq.com/openai/v1` |
| Groq Model | `ai.provider.groq.default-model-id` | **기본:** `llama-3.3-70b-versatile` / **옵션:** `llama-3.1-8b-instant` (비용·속도 절감) |
| Groq temperature | (클라이언트 확장 시) | 스펙 권장 0.2~0.3. 현재 TextAiClient에서 temperature 미지원으로 Groq 기본값 사용 |
| S3 | `production.storage.type=S3` | `production.storage.s3.prefix` 기본 `production` |

---

## jobId 결정 규칙 (resolveJobId)

- **우선순위:** `jobId` → `deliveryId` → `sha`
- **세 값 모두 없으면:** `400 Bad Request`  
  메시지: `"jobId, deliveryId, sha 중 하나 이상 필요"`
- 같은 `jobId` 로 재요청 시 **멱등:** 이미 S3에 `issue-{jobId}.md`, `pr-{jobId}.md` 가 **둘 다 존재**하면 Groq 호출·재생성·재저장 모두 스킵하고, S3에서 두 파일을 읽어 `issueBody`/`prBody` 를 채운 뒤 기존 S3 키와 함께 동일 응답 형태로 반환합니다.
- **부분 저장 상태:** PR 저장 실패 후 보상 삭제(issue 파일 삭제)가 실패하면 `issue-{jobId}.md`만 S3에 남을 수 있습니다. 이 경우 같은 `jobId`로 재요청하면 "둘 다 존재" 조건을 만족하지 못해 **재생성 경로**로 진입하며, 기존 issue 파일이 새로 생성된 본문으로 덮어써질 수 있습니다.

---

## 사용자별 Webhook 설정 (github_webhook_config)

| 컬럼 | 설명 |
|------|------|
| tenant_key | Webhook URL에 쓰는 사용자별 키 (예: `acme-1234`) |
| repo_full_name | GitHub repository full name (예: `owner/repo`) |
| body_prompt_id | 본문 템플릿으로 쓸 prompt ID |
| enabled | 1=활성, 0=비활성 |
| owner_user_id | (선택) 설정 소유 사용자 ID |

- **유니크:** `(tenant_key, repo_full_name)`. 같은 tenant에 레포별로 다른 promptId 지정 가능.
- 마이그레이션: `src/main/resources/db/migration/github_webhook_config.sql` 참고.

---

## S3 저장 키 규칙 (강제)

- **형식:** `{prefix}/{tenantId}/0/{jobId}/{fileName}`
  - `prefix`: `production.storage.s3.prefix` (기본 `production`)
  - `tenantId`: **우선순위** request.tenantId → TenantContext → `"github"`. Webhook 경로에서는 **tenantKey** 를 tenantId로 사용해 사용자별로 분리.
  - `0`: 웹훅용 고정 userId
  - `fileName`: `issue-{jobId}.md`, `pr-{jobId}.md`
- **예:**
  - `production/github/0/abc123/issue-abc123.md`
  - `production/acme-1234/0/abc123/issue-abc123.md` (tenantKey=acme-1234 인 경우)
- **contentType:** `text/markdown`

---

## 입력 크기 제한 (서비스 안정성)

- **commits:** 최대 50줄. 초과 시 앞 50줄만 사용하고 `"... and N more"` 로 축약해 프롬프트에 반영.
- **files:** 최대 200줄. 초과 시 앞 200줄만 사용하고 `"... and N more"` 로 축약.

---

## 요청 / 응답 예시

### 요청 (JSON)

```json
{
  "jobId": "optional-job-id",
  "deliveryId": "webhook-delivery-id",
  "sha": "abc123def",
  "repoFullName": "owner/repo",
  "branch": "feature/xyz",
  "baseBranch": "main",
  "title": "feat: add GitHub body API",
  "author": "dev",
  "date": "2025-02-23",
  "commits": "feat: add endpoint\n\n- POST /github/bodies/generate",
  "files": "src/.../GitHubBodyController.java\nsrc/.../GitHubBodyGeneratorService.java",
  "tenantId": "github"
}
```

- `jobId`, `deliveryId`, `sha` 중 **하나 이상 필수**.
- `baseBranch` 없으면 `"main"` 사용.
- `commits` / `files` 는 줄 단위 문자열 (50줄/200줄 초과 시 자동 축약).

### 응답 (200 OK)

```json
{
  "success": true,
  "data": {
    "jobId": "optional-job-id",
    "issueBody": "## TL;DR\n- ...\n\n[AUTO_JOB_ID:optional-job-id]\n[AUTO_PUSH_DELIVERY:...]\n[AUTO_PUSH_SHA:...]",
    "prBody": "# Summary\n- ...\n\n[AUTO_JOB_ID:optional-job-id]\n...",
    "storedIssueFileKey": "production/github/0/optional-job-id/issue-optional-job-id.md",
    "storedPrFileKey": "production/github/0/optional-job-id/pr-optional-job-id.md"
  }
}
```

---

## DELIVERY에서 사용하는 방법

- 응답의 `storedIssueFileKey` / `storedPrFileKey` 로 S3에서 본문을 읽어와서, GitHub API로 Issue 또는 PR 생성 시 `body` 필드에 그대로 넣으면 됩니다.
- 즉, DELIVERY는 **storedIssueFileKey**·**storedPrFileKey** 를 읽어 각각 Issue 본문 / PR 본문으로 사용합니다.
- **저장소 미사용 시:** S3/StorageFacade가 비활성이면 `storedIssueFileKey`, `storedPrFileKey`가 `null`로 반환될 수 있습니다. 호출 측에서는 null인 경우 본문만 사용하고 저장 키를 참조하지 않도록 처리하는 것이 좋습니다.

---

## 원자성 / 보상 (권장 구현)

- 저장 순서: issue → pr.
- pr 저장 실패 시, 이미 저장된 issue 파일을 **delete(보상 삭제)** 하여 jobId 하위에 한 파일만 남지 않게 합니다.
- delete 실패 시 경고 로그 후, 최종적으로 실패 응답(500/502) 처리합니다.

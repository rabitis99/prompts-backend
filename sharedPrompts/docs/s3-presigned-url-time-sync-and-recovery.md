# S3 Presigned URL: 시간 동기화 점검 가이드 및 재발 방지

## 1. 문제 원인 분석

### 1.1 `AccessDenied` / `Request has expired` 발생 원리 (AWS 공식)

- **SigV4 Presigned URL**은 다음 두 시간 요소로 유효성이 결정됩니다.
  - **X-Amz-Date**: 서명 시점의 요청 시간 (UTC, 형식: `yyyyMMddTHHmmssZ`)
  - **X-Amz-Expires**: 위 시점부터의 유효 기간(초)

- S3는 **요청을 수신한 시점의 자신의 서버 시간**과 다음을 비교합니다.
  - `X-Amz-Date ≤ 현재(S3) 시간`
  - `X-Amz-Date + X-Amz-Expires ≥ 현재(S3) 시간`

- **“생성 직후에도 Request has expired”**가 나오는 전형적인 원인:
  - **Presigned URL을 생성한 서버의 시계가 S3(UTC)보다 느리다(뒤처져 있다).**
  - 서버가 사용한 “현재 시각”(X-Amz-Date)이 이미 S3 기준으로는 “과거”이고, 그 과거 시각 + Expires도 S3 “현재”보다 이전이면, S3는 요청을 “이미 만료된 요청”으로 판단해 `Request has expired`를 반환합니다.

즉, **추측이 아니라 AWS 동작**: 서명 시 사용한 시간이 **UTC 기준으로 S3와 일치하지 않거나 서버 시계가 느리면** 생성 직후에도 만료로 처리될 수 있습니다.

### 1.2 커스텀 서명 vs SDK 사용 여부

- 이 프로젝트는 **AWS SDK v3 `S3Presigner`**만 사용합니다. 커스텀 SigV4 구현은 없습니다.
- `S3Presigner`는 **JVM이 인식하는 현재 시각**(시스템 시계)을 사용해 X-Amz-Date와 서명을 생성합니다.
- 따라서 **시스템 시계가 잘못되거나 타임존/드리프트 문제**가 있으면 동일 증상이 발생합니다.

---

## 2. 시간 검증 체크리스트

Presigned URL 생성 시 아래 로그가 출력되도록 되어 있습니다. 403 발생 시 다음을 비교·기록하세요.

| 항목 | 설명 | 확인 방법 |
|------|------|-----------|
| **Instant.now()** | JVM이 사용한 현재 시각(UTC) | 로그 `[Presign time diagnostic]` |
| **ZonedDateTime.now(UTC)** | UTC 기준 현재 시각 | 동일 로그 |
| **systemDefaultZone** | JVM 기본 타임존 | 동일 로그 (드리프트 원인 분석용) |
| **X-Amz-Date (in URL)** | Presigned URL에 들어간 서명 시각 | 동일 로그 또는 URL 쿼리 파라미터 |
| **S3 ServerTime** | S3가 403 응답 본문에 넣는 서버 시간 | 403 XML 응답의 `<ServerTime>` |

**분석 방법**

- 로그의 `X-Amz-Date`와 403 응답의 `<ServerTime>`을 UTC로 비교합니다.
- `X-Amz-Date`가 `ServerTime`보다 **몇 초/분 이상 과거**이면, Presigned URL을 생성한 서버의 시계가 S3보다 느린 것입니다. → **해당 서버의 시계 동기화(NTP 등) 필요.**

---

## 3. 시간 동기화 점검 가이드

### 3.1 Linux (호스트)

**현재 시간(UTC) 확인**

```bash
date -u
```

**타임존 및 NTP 동기화 상태 확인**

```bash
timedatectl
```

- `System clock synchronized: yes` 인지 확인.
- `NTP service: active` 또는 `Time zone: UTC` 등으로 의도한 설정인지 확인.

**NTP/chrony 권장 설정 (예: chrony)**

```bash
# chrony 설치 (예: RHEL/CentOS/Amazon Linux)
sudo yum install -y chrony
# 또는 Debian/Ubuntu
sudo apt-get install -y chrony

# 설정 예시: /etc/chrony.conf
# pool ntp.aws.amazon.com iburst   # AWS NTP (리전별 엔드포인트 있음)
# 또는
# pool 2.pool.ntp.org iburst
# driftfile /var/lib/chrony/drift
# makestep 0.1 3

sudo systemctl enable chronyd
sudo systemctl start chronyd
sudo chronyc tracking
sudo chronyc sources
```

- AWS 환경에서는 `ntp.aws.amazon.com` 등 AWS NTP 사용을 권장합니다.

### 3.2 Docker 컨테이너 내부 시간 확인

컨테이너는 호스트 커널의 시계를 공유하지만, 컨테이너만 재시작된 경우 등에 시간이 어긋날 수 있습니다.

**컨테이너 안에서 확인**

```bash
# 컨테이너 셸 접속 후
date -u
```

**호스트와 비교**

```bash
# 호스트
date -u

# 컨테이너 (이미지에 따라 다름)
docker run --rm <image> date -u
```

- 호스트와 1분 이상 차이나면 NTP 동기화를 호스트와 컨테이너 모두에서 점검하세요.
- Docker Compose/Kubernetes에서는 호스트의 NTP 동기화가 우선입니다.

### 3.3 요약

- **Presigned URL을 생성하는 서버/컨테이너**의 시계가 **UTC 기준으로 정확**해야 합니다.
- `date -u`와 `timedatectl`로 주기적으로 확인하고, NTP(chrony 등)로 동기화를 유지하세요.

---

## 4. 안정성 개선 (적용/권장 사항)

### 4.1 Presigned URL 만료 시간

- **기본 TTL을 15분(900초)으로 확장**해 두었습니다. (보안 요구사항에 따라 최대 60분까지 조정 가능.)
- 설정:
  - `production.storage.s3.presigned-url.ttl-seconds` (기본값: 900)
  - `artifact.url.default-ttl` (기본값: 900)
- 클록이 소폭 느린 환경에서도 짧은 TTL로 인한 “생성 직후 만료” 가능성을 줄이기 위함입니다.

### 4.2 프론트엔드: 403 시 URL 재발급

- Presigned URL로 GET 요청 시 **403(Forbidden)** 이 오면:
  - 응답 본문에 `Request has expired` 또는 `AccessDenied`가 포함된 경우, **같은 아티팩트에 대해 Presigned URL 재발급 API를 한 번 호출**한 뒤, 새 URL로 다시 요청하는 로직을 권장합니다.
- 예시 흐름:
  1. 아티팩트 접근용 Presigned URL 발급 API 호출 → URL 수신
  2. 해당 URL로 GET (이미지/다운로드)
  3. **403 수신 시** → (선택) 본문에 “expired”/“AccessDenied” 포함 여부 확인 → 재발급 API 호출 → 새 URL로 1회 재시도
- 재시도는 1회로 제한하는 것을 권장합니다 (무한 루프 방지).

### 4.3 구조 개선 제안: Presigned URL 직접 반환 대신 302 리다이렉트

- **현재**: 클라이언트가 “Presigned URL 발급 API”를 호출해 URL을 받고, 해당 URL로 직접 S3에 요청합니다.
- **개선 제안**: “아티팩트 접근”을 하나의 진입점으로 묶고, 서버가 Presigned URL을 생성한 뒤 **302 Redirect**로 그 URL로 보내는 방식입니다.

**예시**

- **엔드포인트**: `GET /artifact/{id}/access?type=download` 또는 `GET /artifact/{id}/access?type=preview`
- **동작**:
  1. 서버가 인증/소유권 검증 후 Presigned URL 생성 (요청 시점의 “현재 시각” 사용).
  2. `302 Found` + `Location: <presigned-url>` 반환.
  3. 클라이언트(브라우저/앱)는 해당 URL로 리다이렉트되어 S3에서 직접 다운로드/미리보기.

**장점**

- Presigned URL을 클라이언트에 노출하지 않고, “접근 시점”에 맞춰 매번 새로 생성할 수 있음.
- 생성 직후 바로 리다이렉트되므로, 서버 시계가 정상이라면 “생성 직후 만료” 가능성이 거의 없음.
- 403이 나오면 “아티팩트 접근 URL”을 다시 호출하면 서버가 새 Presigned URL을 만들어 302로 주므로, 재발급 로직을 서버 진입점 한 곳으로 모을 수 있음.

**구현 시 유의**

- 다운로드 시 `Content-Disposition` 등은 Presigned URL 생성 시 `GetObjectRequest`에 이미 포함되므로, 302 응답 본문은 비워 두거나 짧은 HTML 메시지만 넣으면 됩니다.

---

## 5. 수정 코드 요약 (Java)

### 5.1 시간 진단 로그 (이미 반영됨)

- `S3PresignedUrlService`에서 Presigned URL 생성 직후:
  - `Instant.now()`, `ZonedDateTime.now(ZoneOffset.UTC)`, `ZoneId.systemDefault()` 로그 출력
  - 생성된 URL에서 `X-Amz-Date` 쿼리 파라미터 추출 후 로그 출력
- 로그 메시지: `[Presign time diagnostic] operation=... | Instant.now()=... | ZonedDateTime.now(UTC)=... | systemDefaultZone=... | X-Amz-Date(in URL)=...`

### 5.2 Presigned URL TTL

- `ProductionS3Properties.PresignedUrl.ttlSeconds` 기본값: **900** (15분)
- `application.yml`: `production.storage.s3.presigned-url.ttl-seconds` 기본값 **900**
- `artifact.url.default-ttl` 기본값 **900**

### 5.3 302 리다이렉트 예시 (제안)

```java
// 예시: 다운로드 접근 시 Presigned URL로 302 리다이렉트
@GetMapping("/artifact/{artifactId}/access")
public ResponseEntity<Void> accessArtifact(
        @PathVariable Long artifactId,
        @RequestParam(defaultValue = "download") String type,
        @AuthenticationPrincipal UserPrincipal principal) {
    String presignedUrl = storageCommandService.generateDownloadPresignedUrl(artifactId, principal.getUserId());
    return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(presignedUrl))
            .build();
}
```

- 실제 적용 시에는 소유권 검증, `type=preview` 분기 등은 기존 `StorageCommandService`/`PresignedUrlService`와 맞춰 구성하면 됩니다.

---

## 6. 참고 (AWS 문서)

- [Authenticating Requests: Using Query Parameters (SigV4)](https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-query-string-auth.html)  
  - X-Amz-Date, X-Amz-Expires 형식 및 유효성 검사 기준
- [Troubleshooting S3 presigned URL 403 / SignatureDoesNotMatch](https://repost.aws/knowledge-center/s3-presigned-url-signature-mismatch)  
  - 시계 동기화, 리전/버킷/키 일치 등 점검 항목

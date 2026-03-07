# Time-Consistency Verification Audit Report

**Date:** 2025-03-07  
**Scope:** Full repository scan after UTC refactor  
**Policy:** Canonical storage = UTC; DB = LocalDateTime in UTC; events/schedulers/external APIs = UTC; display conversion only in response mappers.

---

## 1. Remaining Critical Issues

**None.**

The only issue that could have caused production bugs (TossPay timestamp parsing dropping offset) was found and fixed in this audit (see below).

### 1.1 ~~TossPay timestamp parsing drops offset~~ — FIXED

**Location:**  
`TossPayResponseParser.parseRequiredDateTime()`  
`src/main/java/org/example/sharedprompts/domain/payment/infrastructure/external/provider/toss/util/TossPayResponseParser.java`

**Previous code:**  
`OffsetDateTime.parse(...).toLocalDateTime()` dropped the offset, so offset-aware Toss timestamps (e.g. KST) could be stored as if UTC.

**Fix applied:**  
Convert to UTC before producing `LocalDateTime`:  
`LocalDateTime.ofInstant(OffsetDateTime.parse(...).toInstant(), ZoneOffset.UTC)`.

---

**No other critical issues** were found in:

- Entities, schedulers, services, repositories, event builders, cleanup jobs, retry/expiration logic
- Payment providers (PayPal, KakaoPay) and persistence code
- DB boundary logic (“today” ranges use UTC)
- Repository time parameters (retry/expiration pass UTC)
- Redis serialization (UTC semantics)
- S3 presigning (no timezone used for signing)

---

## 2. Minor Risks

### 2.1 Unused repository API: `countByStatusAndCreatedAtAfter(status, dateTime)`

**Location:**  
`PaymentQueryRepositoryPort.countByStatusAndCreatedAtAfter(PaymentStatus, LocalDateTime)`  
Implemented by `PaymentRepositoryAdapter` → `PaymentJpaAdapter.countByDateAndStatus(dateTime, status)`  
Query: `DATE(p.createdAt) = DATE(:date)` with session `time_zone=UTC`.

**Issue:**  
No callers in the codebase. If future code uses this method, it must pass a `LocalDateTime` that represents the intended **UTC** day (e.g. `StatisticsDateUtils.todayStart()` or `LocalDate.now(ZoneOffset.UTC).atStartOfDay()`). Passing a server-local “today” would break consistency.

**Recommendation:**  
Document that `dateTime` must be in UTC (e.g. JavaDoc), or narrow the API to a `LocalDate` and build UTC start-of-day inside the adapter.

---

### 2.2 S3 diagnostic logging uses `ZoneId.systemDefault()`

**Location:**  
`S3PresignedUrlService.logPresignTimeDiagnostics()`  
`src/main/java/org/example/sharedprompts/module/domain/production/infra/storage/S3PresignedUrlService.java`

**Code:**  
`ZoneId systemZone = ZoneId.systemDefault();` used only in a `log.debug(...)` message.

**Issue:**  
No impact on signing or persistence. Presigning uses AWS SDK (`Instant.now()` and `signatureDuration`). This is diagnostic-only to help debug “Request has expired” when comparing server time to X-Amz-Date.

**Recommendation:**  
None required. Optionally add a one-line comment that `systemDefault` is for diagnostics only, not for signing or storage.

---

### 2.3 Tests: unqualified `LocalDateTime.now()` / `LocalDate.now()`

**Locations (examples):**

- `DefaultPaymentStatusCheckServiceTest`: `.approvedAt(LocalDateTime.now())`, `.canceledAt(LocalDateTime.now())`
- `DefaultPaymentHistoryQueryServiceTest`: multiple `.approvedAt(LocalDateTime.now()...)`
- `PaymentRepositoryAdapterTest`: `LocalDateTime.now().minusDays(1)`, `LocalDateTime.now()`, `LocalDateTime.now().minusHours(1)`
- `PaymentGatewayPortAdapterTest`: `.canceledAt(LocalDateTime.now())`, `.refundedAt(LocalDateTime.now())`
- `PaymentEventPublisherAdapterTest`: `.timestamp(LocalDateTime.now())` in event builders

**Issue:**  
Tests can behave differently depending on the JVM timezone (e.g. CI vs local). They do not affect production, but can cause flakiness or confusion.

**Recommendation:**  
(Flag only; do not change tests automatically.) Prefer `LocalDateTime.now(ZoneOffset.UTC)` in test builders that set timestamps used for comparisons or persistence, so tests are timezone-independent and aligned with production.

---

## 3. Safe Patterns Confirmed

| Area | Finding |
|------|--------|
| **Payment entity** | `markSuccess`, `markCanceled`, `scheduleNextRetry`, `applyWebhookResult` use `LocalDateTime.now(ZoneOffset.UTC)`. |
| **Payment domain events** | All event builders set `.timestamp(LocalDateTime.now(ZoneOffset.UTC))`. |
| **PayPal provider** | `ofInstant(..., ZoneOffset.UTC)` for approvedAt, canceledAt, refundedAt; fallbacks use `LocalDateTime.now(ZoneOffset.UTC)`. |
| **KakaoPay parser** | Epoch → `ofEpochSecond(..., 0, ZoneOffset.UTC)`; OffsetDateTime → `toInstant()` then `ofInstant(..., ZoneOffset.UTC)`; KST-only → `atZone(Asia/Seoul).toInstant()` then `ofInstant(..., ZoneOffset.UTC)`; fallback `LocalDateTime.now(ZoneOffset.UTC)`. |
| **TossPay parser** | `parseRequiredDateTime`: `OffsetDateTime.parse(...).toInstant()` then `LocalDateTime.ofInstant(..., ZoneOffset.UTC)` (fix applied in this audit). |
| **PaymentRetryScheduler** | `findRetryablePayments(..., LocalDateTime.now(ZoneOffset.UTC))`. |
| **PaymentExpirationScheduler** | `expirationTime = LocalDateTime.now(ZoneOffset.UTC).minus(Duration.ofMinutes(...))`. |
| **UserTierServiceImpl** | `LocalDate.now(ZoneOffset.UTC)`; `startOfDay` / `endOfDay` from that date; `countTodaySuccessfulPayments` uses repository `CURRENT_DATE` (session UTC). |
| **StatisticsDateUtils** | `now()` = `LocalDateTime.now(ZoneOffset.UTC)`; `todayStart()` = `LocalDate.now(ZoneOffset.UTC)` + `LocalTime.MIN`. |
| **PromptStatisticsServiceImpl** | Uses `StatisticsDateUtils.now()` and `todayStart()` for `countCreatedPromptsBetween` ranges. |
| **JpaAuditingConfig** | `utcDateTimeProvider` returns `LocalDateTime.now(ZoneOffset.UTC)` for `@CreatedDate` / `@LastModifiedDate`. |
| **PaymentRepository** | `countTodaySuccessfulPayments` uses `CURRENT_DATE` with Hibernate `time_zone=UTC`. |
| **Redis ObjectMapper** | `redisObjectMapper` uses same `LocalDateTimeUtcSerializer` / `LocalDateTimeUtcDeserializer` as main ObjectMapper (ISO-8601+Z, UTC). |
| **JacksonConfig** | Main and Redis ObjectMappers use UTC for `LocalDateTime`; no custom serializer introduces timezone ambiguity. |
| **PaymentConfirmResponseMapper** | `payment.getApprovedAt().atZone(ZONE_SEOUL).toOffsetDateTime()` for response only; persistence unchanged. |
| **AdminController** | Rate-limit statistics default range uses `LocalDateTime.now(ZoneOffset.UTC)`. |
| **Outbox / monitoring / cleanup** | `JobOutboxEntity`, `FailedPaymentEvent`, `CompensationTask`, `NotificationEventProcessor`, `NotificationGroupingService`, `NotificationCleanupScheduler`, `PointServiceImpl`, `ExchangeRateScheduler`, `PaymentMonitoringService`, `User` (softDelete), `Cashback` (markAsPaid), `TagCountUpdateEvent`, `AdminMaintenanceServiceImpl`, `LocalRebuildStatusManager` all use `LocalDateTime.now(ZoneOffset.UTC)` for persisted or compared timestamps. |
| **S3 presigning** | Signing uses AWS SDK only; no timezone manipulation before signing; expiration is `Duration`-based. |
| **Instant usage** | `Instant.now()` used in job domain, ShedLock, rate limit headers, PayPal fallbacks, etc., is appropriate (UTC). |

---

## 4. False Positives

| Item | Why it is safe |
|------|----------------|
| **PaymentConfirmResponseMapper** `atZone(ZONE_SEOUL)` | Display-only: converts stored UTC to Seoul for API response. Does not affect persistence or internal comparisons. |
| **S3PresignedUrlService** `ZoneId.systemDefault()` | Used only in a diagnostic log message. Not used for signing, expiration, or any persisted value. |
| **KakaoPayResponseParser** `ZoneId.of("Asia/Seoul")` | Intentional: parses KST-only local strings, then converts to UTC via `toInstant()` and `ofInstant(..., ZoneOffset.UTC)`. |
| **JobEntity / Job** `Instant.now()` | Domain uses `Instant`; no conversion to `LocalDateTime` without UTC. Correct. |
| **PayPalResponseParser** `Instant.now()` fallback | Fallback for optional timestamp parsing; `Instant` is UTC. PayPal provider then uses `ofInstant(..., ZoneOffset.UTC)` when mapping to domain. |

---

## 5. Overall System Time Safety Score

**Good** (production-safe after TossPay parser fix applied in this audit)

**Summary:**

- **Payments:** Retry, expiration, and persistence use UTC. PayPal and KakaoPay are aligned with UTC. **TossPay** is the only remaining risk if they send offset-aware (e.g. KST) timestamps; one small change in `TossPayResponseParser` would make it consistent and future-proof.
- **Schedulers / expiration / statistics:** All use UTC “now” and UTC day boundaries.
- **DB boundaries:** “Today” and range queries use UTC (CURRENT_DATE with session UTC, or UTC start/end from `StatisticsDateUtils` / `UserTierServiceImpl`).
- **External API timestamps:** PayPal, KakaoPay, and TossPay all normalize to UTC before producing `LocalDateTime` for persistence.
- **Caching / Redis:** LocalDateTime serialization is UTC (ISO-8601+Z).
- **S3:** No timezone used for signing; safe.

**Conclusion:**  
The system is **production-safe with respect to time consistency**. The only identified production risk (TossPay parser) was fixed in this audit. No further architectural change or DB migration is required; optional improvements are documented in §2 (minor risks) and §4 (false positives).

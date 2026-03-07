# Time Consistency Analysis Report — Spring Boot Backend

**Scope:** Static analysis of UTC handling, JPA Auditing, LocalDateTime vs Instant, S3 presigned URLs, PayPal timestamps, Jackson serialization, and related time usage.

**Conclusion:** The system’s **UTC policy is partially safe**. Auditing and Jackson are aligned with UTC, but **PayPal timestamp conversion**, **several `LocalDateTime.now()` usages**, **“today” semantics**, and **Redis ObjectMapper** introduce real risks. No large DB migration is required for the recommended minimal fixes.

---

## 1. Confirmed Safe Time Handling

### 1.1 JPA Auditing — UTC

- **`JpaAuditingConfig`** provides `LocalDateTime.now(ZoneOffset.UTC)` for `@CreatedDate` / `@LastModifiedDate`.
- **`application.yml`**: `hibernate.jdbc.time_zone: UTC` so JDBC session (and thus `CURRENT_DATE` / `CURDATE()` in MySQL) is UTC.
- **BaseEntity** uses `LocalDateTime createdAt/updatedAt`; with the above, they are stored and compared in UTC. **Acceptable to keep as LocalDateTime** for auditing.

### 1.2 Jackson — API and UTC semantics

- **`JacksonConfig`** (default `ObjectMapper`):
  - **LocalDateTime**: `LocalDateTimeUtcSerializer` / `LocalDateTimeUtcDeserializer` treat values as UTC and use ISO-8601 with `Z` (via `atOffset(ZoneOffset.UTC).toInstant()` and `LocalDateTime.ofInstant(instant, ZoneOffset.UTC)`). API contract is unambiguous.
  - **Instant**: standard ISO-8601 serialization.
- **`spring.jackson.time-zone: UTC`** in `application.yml` is consistent with the above.

### 1.3 Prompt domain — Instant at boundaries

- **`PromptDetailView`**, **`PromptSummaryView`**, **`PromptDetailResponse`**, **`PromptSummaryResponse`** use `Instant` for `createdAt` / `updatedAt`.
- **`PromptServiceImpl`** converts entity `LocalDateTime` to `Instant` with `toInstant(ZoneOffset.UTC)` when building views. Correct given DB is UTC.

### 1.4 Production / Job domain — Instant where it matters

- **`JobEntity`**: `startedAt`, `completedAt` are `Instant`. **`Job`** domain model and **`JobMapper`**, **`ArtifactDetailResponseDtoMapper`** use `Instant` or `toInstant(ZoneOffset.UTC)` for artifact timestamps.
- **`JobProcessorDelegate`**, **`JobLockManager`**, **`StorageLifecycleService`**, **`StaleThresholdPolicy`**, **`JobMessage`**, **`JobQueuePublisher`**, **`JobFailureLogFactory`** use `Instant.now()` for real-world moments. Appropriate.

### 1.5 S3 Presigned URL — signing time

- **`S3PresignedUrlService`** does **not** use `ZoneId.systemDefault()` for signing. It uses:
  - `Instant now = Instant.now()` for diagnostics only.
  - AWS SDK’s `presignGetObject` / `presignPutObject` use the SDK’s own clock (effectively system clock in epoch terms) for signature.
- **Presigning itself is safe** from a timezone perspective. Clock drift between app server and AWS can still cause “Request has expired”; the existing diagnostic log (`logPresignTimeDiagnostics`) and configurable TTL are the right mitigations.

### 1.6 PayPal response parsing (raw)

- **`PayPalResponseParser.parseApprovedAt` / `parseRefundedAt`** parse PayPal’s ISO-8601 strings with `OffsetDateTime.parse(..., ISO_DATE_TIME).toInstant()`. Correct for UTC/offset-aware timestamps. Fallback `Instant.now()` on parse failure is a separate (semantic) concern, not a timezone bug.

### 1.7 Rate limit and notifications (epoch / string)

- **`RateLimitHeaderUtil`**, **`RateLimitResponseWriter`** use `Instant.now().getEpochSecond()`.
- **`RedisNotificationEmbedBuilder`** uses `Instant.now().toString()` for a string field. No timezone ambiguity.

---

## 2. Potential Risk Areas

### 2.1 PayPal → Domain: `ZoneId.systemDefault()` (critical for payments)

**File:** `PayPalPaymentProvider.java`

**Snippets:**

```122:123:src/main/java/org/example/sharedprompts/domain/payment/infrastructure/external/provider/impl/PayPalPaymentProvider.java
                    .approvedAt(LocalDateTime.ofInstant(response.approvedAt(), ZoneId.systemDefault()))
```

Same pattern at lines 156, 207, 270 for `approvedAt`, `canceledAt`, `refundedAt`.

**Why it’s risky:**

- PayPal returns UTC (or offset-aware) timestamps; they are converted to `LocalDateTime` using **server default timezone**. If the server runs in KST, you store “KST wall time” in a column that is later interpreted as UTC by Hibernate (`time_zone=UTC`), so the stored instant is wrong (e.g. 9 hours off). Refunds, cancellations, and reporting by time would be incorrect.

**Recommendation:** Use `ZoneOffset.UTC` (or keep `Instant` until persistence) so that what you store matches the rest of the UTC-based stack.

---

### 2.2 Payment entity and events: `LocalDateTime.now()` without UTC

**Files:**

- **`Payment.java`**  
  - `markSuccess`: `this.approvedAt = LocalDateTime.now();`  
  - `markCanceled`: `this.canceledAt = LocalDateTime.now();`  
  - `scheduleNextRetry`: `this.nextRetryAt = LocalDateTime.now().plus(...);`  
  - `applyWebhookResult`: `this.approvedAt = LocalDateTime.now();` when status is SUCCESS and approvedAt is null.

- **Payment domain events** (`PaymentApprovedEvent`, `PaymentConfirmedEvent`, `PaymentCanceledEvent`, `PaymentExpiredEvent`, `PaymentFailedEvent`, `PaymentRefundedEvent`):  
  - All set `timestamp(LocalDateTime.now())` in builders.

**Why it’s risky:**

- DB and auditing assume UTC. If the server JVM is not in UTC, these fields and event timestamps are stored/compared in the wrong instant. Affects retry windows, expiration, and analytics.

**Recommendation:** Use `LocalDateTime.now(ZoneOffset.UTC)` everywhere that is persisted or compared with DB timestamps (or use `Instant` for event timestamps and nextRetryAt).

---

### 2.3 Schedulers and expiration: `LocalDateTime.now()` vs DB

**Files:**

- **`PaymentRetryScheduler`**: `findRetryablePayments(..., LocalDateTime.now())`. DB compares with `nextRetryAt` (UTC-stored). If server is not UTC, retry timing is wrong.
- **`PaymentExpirationScheduler`**: `LocalDateTime.now().minus(Duration.ofMinutes(...))` for `expirationTime`. Same mismatch if server timezone ≠ UTC.

**Recommendation:** Use `LocalDateTime.now(ZoneOffset.UTC)` for any “now” passed into queries or compared with persisted UTC timestamps.

---

### 2.4 “Today” semantics: `LocalDate.now()` / `LocalDateTime.now()` for ranges

**Files:**

- **`UserTierServiceImpl`**: `LocalDate today = LocalDate.now();` then `startOfDay = today.atStartOfDay()`, `endOfDay = today.plusDays(1).atStartOfDay()` passed to `moduleUsageRepository.countTodayByUserIdAndModuleType(userId, mt, startOfDay, endOfDay)`.  
  With `time_zone=UTC`, Hibernate sends these as literal UTC timestamps. So “today” becomes the **server’s calendar day** interpreted as UTC, not necessarily UTC’s calendar day. If the product means “UTC day,” this is wrong when the server is not UTC; if it means “user/server local day,” then it’s inconsistent with `PaymentRepository.countTodaySuccessfulPayments`, which uses `CURRENT_DATE` (UTC in session).

- **`PromptStatisticsServiceImpl`**: `LocalDateTime now = LocalDateTime.now();`, `todayStart = LocalDateTime.of(now.toLocalDate(), LocalTime.MIN)`, etc., for `countCreatedPromptsBetween(todayStart, now)`. Same idea: “today” is server-local, but DB stores UTC; so boundaries are wrong unless server is UTC.

- **`StatisticsDateUtils`**: `now()` and `todayStart()` use `LocalDateTime.now()` / `LocalDate.now()`. Any caller that uses these for DB range queries has the same risk.

**Why it’s risky:**

- Inconsistency between “today” in payment count (CURRENT_DATE in UTC) and “today” in module usage / prompt stats (server-local day sent as UTC). Also wrong UTC-day boundaries when server is not UTC.

**Recommendation:** Define “today” consistently (e.g. UTC day) and use `LocalDate.now(ZoneOffset.UTC)` (and derive start/end in UTC) for all “today” range queries, or pass explicit zone from a single place.

---

### 2.5 Notification and duplicate check window

**File:** `NotificationEventProcessor.java`

- `LocalDateTime since = LocalDateTime.now().minusMinutes(duplicateCheckWindowMinutes);` for `existsRecentNotification(..., since)`.

**Why it’s risky:**

- If `createdAt` in the notification table is UTC (via auditing), comparing with server-local “now” can shift the window by the server’s offset.

**Recommendation:** Use `LocalDateTime.now(ZoneOffset.UTC)` for `since`.

---

### 2.6 Outbox and ad-hoc entities: `LocalDateTime.now()` in persistence

**Files:**

- **`JobOutboxEntity`**: `markSent()` sets `setSentAt(LocalDateTime.now())`; `@PrePersist` sets `createdAt = LocalDateTime.now()` if null. Outbox is persisted with the same Hibernate `time_zone=UTC`, so these should be UTC for consistency.
- **`FailedPaymentEvent`**: `processedAt = LocalDateTime.now()` in `markProcessed()`.
- **`CompensationTask`** (monitoring): `createdAt = LocalDateTime.now()`.

**Why it’s risky:**

- Any of these running on a non-UTC server will store server-local “now” into columns that are read as UTC, causing drift in retry/sent/processed semantics.

**Recommendation:** Use `LocalDateTime.now(ZoneOffset.UTC)` for all persisted “now” values (or a shared clock bean in UTC).

---

### 2.7 PaymentConfirmResponseMapper: explicit KST for API

**File:** `PaymentConfirmResponseMapper.java`

- `ZONE_SEOUL = ZoneId.of("Asia/Seoul")`; `response.setApprovedAt(payment.getApprovedAt().atZone(ZONE_SEOUL).toOffsetDateTime())`.

**Assessment:**

- This is **intentional** display/API semantics: “show approved time in Seoul.” If `payment.getApprovedAt()` is stored in UTC (as intended), this conversion is correct. Risk appears only if `approvedAt` were ever stored in server default (e.g. due to PayPal bug above); then the value would already be wrong before this mapper.

---

### 2.8 KakaoPayResponseParser: fallback and KST

**File:** `KakaoPayResponseParser.java`

- Unix timestamp path: `LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.of("+09:00"))` — correct for KST.
- ISO without offset: `localDateTime.atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime()` — returns the same local date/time in a different type; the resulting `LocalDateTime` is then stored. With Hibernate `time_zone=UTC`, that value is written as-is, so you are effectively storing “KST wall time” as if it were UTC, which is wrong.
- Fallback: `return LocalDateTime.now();` on parse failure — server-local, inconsistent with UTC storage.

**Recommendation:** For Kakao (KST) timestamps, convert to UTC before producing a `LocalDateTime` for persistence, e.g. `Instant.ofEpochSecond(timestamp).atOffset(ZoneOffset.UTC).toLocalDateTime()` for Unix, and for ISO without offset parse as KST then to UTC, then to `LocalDateTime` (UTC). Fallback should be `LocalDateTime.now(ZoneOffset.UTC)` or an `Instant` if the domain allows.

---

### 2.9 Redis ObjectMapper: LocalDateTime without UTC

**File:** `JacksonConfig.java` — `redisObjectMapper()`

- Uses `LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))` and matching deserializer. No `Z` or timezone.

**Why it’s risky:**

- If the same `LocalDateTime` values are written by the default ObjectMapper (which treats them as UTC and serializes with Z) and read by the Redis ObjectMapper (no zone), or vice versa, round-trip semantics change. Caches that store payment/event timestamps could expose or reinforce timezone bugs.

**Recommendation:** Use the same UTC-based LocalDateTime serialization for Redis as for the API, or store only `Instant` in Redis for timestamps.

---

### 2.10 Other `LocalDateTime.now()` usages (lower impact but consistent)

- **`PointServiceImpl`**: `expiryDate = LocalDateTime.now().plusDays(30)` — should be UTC if compared or stored with UTC columns.
- **`ExchangeRateScheduler`**: `fetchedAt = LocalDateTime.now()` — same.
- **`AdminController`**: `LocalDateTime now = LocalDateTime.now()` — depends on usage (admin report boundaries).
- **`AdminMaintenanceServiceImpl`** / **`LocalRebuildStatusManager`**: `startTime = LocalDateTime.now()`, `LocalDateTime.now()` for duration and `updateFinishedAt` — operational metrics; should still use UTC for consistency.
- **`NotificationCleanupScheduler`**: `cutoffDate = LocalDateTime.now().minusDays(retentionDays)` — should be UTC if comparing to audited UTC dates.
- **`NotificationGroupingService`**: `since = LocalDateTime.now().minusMinutes(windowMinutes)` — same.
- **`TagCountUpdateEvent`**: `occurredAt = LocalDateTime.now()` — event timestamp; better as UTC.
- **`User`**: `deletedAt = LocalDateTime.now()` — soft delete; should be UTC.
- **`Cashback`**: `paidAt = LocalDateTime.now()` — should be UTC.

Using `LocalDateTime.now(ZoneOffset.UTC)` (or a shared UTC clock) in these places keeps the system consistent and avoids subtle bugs if the server is ever run in another region.

---

## 3. Critical Bugs (if any)

### 3.1 PayPal timestamps stored with wrong instant (high impact)

- **Where:** `PayPalPaymentProvider` uses `LocalDateTime.ofInstant(..., ZoneId.systemDefault())` for `approvedAt`, `canceledAt`, `refundedAt`.
- **Effect:** Stored values are interpreted by Hibernate as UTC; they are actually server-local. So every PayPal approval/cancel/refund time is shifted by the server’s offset (e.g. −9 hours in KST). Refunds, disputes, and reporting are wrong.
- **Fix:** Replace with `LocalDateTime.ofInstant(..., ZoneOffset.UTC)` (or pass `Instant` through and convert at persistence with UTC).

### 3.2 Payment entity and retry/expiration “now” (high impact)

- **Where:** `Payment` (approvedAt, canceledAt, nextRetryAt, webhook approvedAt), `PaymentRetryScheduler`, `PaymentExpirationScheduler`.
- **Effect:** If JVM is not UTC, retry and expiration windows are shifted; approval/cancel times are wrong in DB and in events.
- **Fix:** Use `LocalDateTime.now(ZoneOffset.UTC)` (or a UTC clock bean) for all “now” values that are persisted or used in queries.

### 3.3 “Today” inconsistency (medium impact)

- **Where:** `UserTierServiceImpl` (module usage “today”), `PromptStatisticsServiceImpl` (prompt “today”), `StatisticsDateUtils`. Payment “today” uses `CURRENT_DATE` (UTC).
- **Effect:** When server is not UTC, “today” for usage/stats is the server’s calendar day interpreted as UTC, so boundaries and counts can be off by one day or misaligned with payment “today.”
- **Fix:** Use UTC for “today” everywhere: e.g. `LocalDate.now(ZoneOffset.UTC)` and derive start/end in UTC for all day-boundary queries.

---

## 4. PayPal Timestamp Handling Risk

- **Current:** `LocalDateTime.ofInstant(response.approvedAt(), ZoneId.systemDefault())` (and same for canceledAt, refundedAt). PayPal returns UTC (or offset-aware) instants; conversion uses server default zone, then the value is stored and read as UTC.
- **Risk:** High. Stored timestamps are wrong whenever server timezone ≠ UTC; affects all PayPal-based payments/refunds/cancellations.
- **Fix:** Use `ZoneOffset.UTC` instead of `ZoneId.systemDefault()`. Optionally keep `Instant` in the result DTO and convert to `LocalDateTime` only at the persistence layer with UTC, to avoid future mistakes.

---

## 5. S3 Presigned URL Safety Check

- **Signing:** Presigning uses the AWS SDK; no `systemDefault()` in the signing path. Presigned URL generation is **safe** from timezone bugs.
- **Clock drift:** Server clock skew vs AWS can still cause “Request has expired.” The code already logs diagnostics and allows configurable TTL; NTP and monitoring are the right operational controls.
- **Recommendation:** No code change for timezone; keep TTL and diagnostics as-is.

---

## 6. Migration Recommendations

### 6.1 Keep as LocalDateTime (no schema change)

- **BaseEntity** `createdAt` / `updatedAt`: already fed by UTC in auditing; keep for consistency and to avoid large-scale migration.
- Other auditing-style fields that are consistently set with `LocalDateTime.now(ZoneOffset.UTC)` can stay LocalDateTime.

### 6.2 Prefer Instant where it represents a real-world moment

- **New** event timestamps (e.g. payment events): use `Instant` in domain and DTOs where possible; convert to/from `LocalDateTime` only at DB boundary with UTC.
- **New** “point-in-time” fields (e.g. nextRetryAt, approvedAt if you ever do a new service): consider `Instant` in domain and store as UTC (e.g. `TIMESTAMP` with time_zone or stored in UTC).
- **Existing** `approvedAt` / `canceledAt` / `refundedAt` / `nextRetryAt`: no need to change column type immediately; fixing the **source** of the value (UTC) is enough. Optional later step: add an API or internal model that exposes them as `Instant` and keep DB as-is.

### 6.3 LocalDateTime acceptable where semantics are “date + time in one zone”

- Auditing (created/updated) with UTC provider.
- Display-oriented “today” ranges **if** you explicitly use UTC (e.g. `LocalDate.now(ZoneOffset.UTC)` and UTC start/end). Then LocalDateTime is still “UTC wall time” and acceptable.

---

## 7. Minimal Fix Strategy (no large DB migrations)

1. **PayPal:** In `PayPalPaymentProvider`, replace every `ZoneId.systemDefault()` with `ZoneOffset.UTC` when converting PayPal `Instant` to `LocalDateTime`. No DB or API contract change.
2. **Payment entity:** In `Payment`, set `approvedAt`, `canceledAt`, `nextRetryAt`, and webhook fallback `approvedAt` with `LocalDateTime.now(ZoneOffset.UTC)`.
3. **Payment events:** In all payment event builders, set timestamp with `LocalDateTime.now(ZoneOffset.UTC)` (or switch to `Instant` in the event type and keep a single conversion at publish).
4. **Schedulers:** In `PaymentRetryScheduler` and `PaymentExpirationScheduler`, pass `LocalDateTime.now(ZoneOffset.UTC)` into repository calls.
5. **“Today” logic:** In `UserTierServiceImpl`, `PromptStatisticsServiceImpl`, and `StatisticsDateUtils`, use `LocalDate.now(ZoneOffset.UTC)` and build start/end of day in UTC (e.g. `today.atStartOfDay(ZoneOffset.UTC)` and `today.plusDays(1).atStartOfDay(ZoneOffset.UTC)` or equivalent). Ensure any repository that takes “today” bounds receives UTC-day boundaries.
6. **Outbox / monitoring / cleanup:** In `JobOutboxEntity`, `FailedPaymentEvent`, `CompensationTask`, `NotificationEventProcessor`, `NotificationCleanupScheduler`, `NotificationGroupingService`, use `LocalDateTime.now(ZoneOffset.UTC)` for any persisted or compared timestamp.
7. **KakaoPay:** In `KakaoPayResponseParser`, convert KST to UTC before producing `LocalDateTime` for persistence; use `LocalDateTime.now(ZoneOffset.UTC)` (or `Instant.now()`) as fallback.
8. **Redis:** In `JacksonConfig.redisObjectMapper()`, register the same UTC-based `LocalDateTimeUtcSerializer` / `LocalDateTimeUtcDeserializer` as the default ObjectMapper (or document that Redis stores only UTC-interpreted LocalDateTime).
9. **Misc:** Apply `LocalDateTime.now(ZoneOffset.UTC)` in `PointServiceImpl`, `ExchangeRateScheduler`, `User`, `Cashback`, `AdminMaintenanceServiceImpl`, `LocalRebuildStatusManager`, `TagCountUpdateEvent`, and `AdminController` where the value is stored or used in time-based logic.

Optional: introduce a small `Clock` or `TimeProvider` bean that returns `Instant.now()` or `LocalDateTime.now(ZoneOffset.UTC)` and use it everywhere instead of static `now()` calls, to make testing and future policy changes easier.

---

## 8. Long-Term Best Practice Architecture

| Layer           | Recommendation |
|----------------|----------------|
| **DB**         | Store all timestamps in UTC. Keep `hibernate.jdbc.time_zone=UTC`. Prefer `TIMESTAMP` (or equivalent) with a single, documented UTC interpretation. |
| **Domain**     | Use **Instant** for “moment in time” (payment approval, event time, retry time). Use **LocalDateTime** only for “date+time in a single zone” (e.g. auditing in UTC) or calendar-day logic explicitly in UTC. Avoid `LocalDateTime` for cross-boundary or external API timestamps. |
| **External APIs** | Parse external timestamps (PayPal, Kakao) to **Instant** (or OffsetDateTime then toInstant()). Convert to domain/DB only at the edge with a single rule (e.g. always UTC). Never use `ZoneId.systemDefault()` for external API timestamps. |
| **UI / API**   | Expose timestamps as ISO-8601 with offset (e.g. `OffsetDateTime`) or with `Z` (Instant). For “display timezone” (e.g. Seoul), convert at the presentation layer only (as in `PaymentConfirmResponseMapper`), from a single UTC source. |
| **S3 / signing** | Use system clock only for “now” in epoch sense; no timezone in signing. Rely on NTP and TTL for expiry. |
| **Caching (Redis)** | Use the same UTC interpretation for date/time types as the rest of the app (e.g. same Jackson UTC serialization for LocalDateTime, or store Instant). |

---

## Summary

- **UTC policy:** Partially safe. Auditing and Hibernate are UTC; Jackson API is UTC. Risks come from **PayPal conversion**, **unqualified `LocalDateTime.now()`**, **“today” semantics**, and **Redis ObjectMapper**.
- **Critical:** Fix PayPal `ZoneId.systemDefault()` and all payment/retry/expiration “now” to UTC; then align “today” and remaining “now” usages to UTC. No DB schema change required for this minimal set.
- **Payments, S3, events, expiration:** Safe after the above fixes; S3 is already safe from timezone issues; expiration and retry become correct once “now” is UTC.

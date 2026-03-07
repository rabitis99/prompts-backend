# Time Consistency Refactor Summary

This document summarizes the repository-wide refactor applied to enforce UTC for canonical storage and internal comparison, and to remove reliance on `ZoneId.systemDefault()` for persisted timestamps.

---

## 1. Files Changed

| File | Change |
|------|--------|
| `PayPalPaymentProvider.java` | `ofInstant(..., ZoneOffset.UTC)` for approvedAt/canceledAt/refundedAt; `LocalDateTime.now(ZoneOffset.UTC)` for cancel fallbacks |
| `Payment.java` | `LocalDateTime.now(ZoneOffset.UTC)` in markSuccess, markCanceled, scheduleNextRetry, applyWebhookResult |
| `PaymentApprovedEvent.java` | `.timestamp(LocalDateTime.now(ZoneOffset.UTC))` |
| `PaymentCanceledEvent.java` | same |
| `PaymentConfirmedEvent.java` | same |
| `PaymentExpiredEvent.java` | same |
| `PaymentFailedEvent.java` | same |
| `PaymentRefundedEvent.java` | same |
| `StatisticsDateUtils.java` | `now()` → `LocalDateTime.now(ZoneOffset.UTC)`; `todayStart()` → `LocalDate.now(ZoneOffset.UTC)` |
| `UserTierServiceImpl.java` | `LocalDate.now(ZoneOffset.UTC)` for “today” boundaries |
| `PromptStatisticsServiceImpl.java` | Use `StatisticsDateUtils.now()` and `StatisticsDateUtils.todayStart()` for UTC ranges |
| `PaymentRetryScheduler.java` | `LocalDateTime.now(ZoneOffset.UTC)` for retry cutoff |
| `PaymentExpirationScheduler.java` | `LocalDateTime.now(ZoneOffset.UTC)` for expiration time |
| `JobOutboxEntity.java` | `LocalDateTime.now(ZoneOffset.UTC)` in markSent() and @PrePersist |
| `NotificationEventProcessor.java` | `LocalDateTime.now(ZoneOffset.UTC)` for duplicate window |
| `NotificationGroupingService.java` | same for group window |
| `NotificationCleanupScheduler.java` | `LocalDateTime.now(ZoneOffset.UTC)` for cutoff date |
| `AdminController.java` | `LocalDateTime.now(ZoneOffset.UTC)` for rate-limit stats default range |
| `AdminMaintenanceServiceImpl.java` | `LocalDateTime.now(ZoneOffset.UTC)` for startTime, duration, updateFinishedAt |
| `LocalRebuildStatusManager.java` | `LocalDateTime.now(ZoneOffset.UTC)` for startedAt |
| `FailedPaymentEvent.java` | `LocalDateTime.now(ZoneOffset.UTC)` in markProcessed() |
| `CompensationTask.java` | Default `createdAt` = `LocalDateTime.now(ZoneOffset.UTC)` |
| `PointServiceImpl.java` | `LocalDateTime.now(ZoneOffset.UTC).plusDays(30)` for expiry window |
| `ExchangeRateScheduler.java` | `LocalDateTime.now(ZoneOffset.UTC)` for fetchedAt |
| `PaymentMonitoringService.java` | `LocalDateTime.now(ZoneOffset.UTC)` for duration calculation |
| `User.java` | `LocalDateTime.now(ZoneOffset.UTC)` in softDelete() |
| `Cashback.java` | `LocalDateTime.now(ZoneOffset.UTC)` in markAsPaid() |
| `TagCountUpdateEvent.java` | `occurredAt` default = `LocalDateTime.now(ZoneOffset.UTC)` |
| `JacksonConfig.java` | Redis `ObjectMapper`: use `LocalDateTimeUtcSerializer` / `LocalDateTimeUtcDeserializer` (ISO-8601+Z) instead of pattern `yyyy-MM-dd HH:mm:ss` |
| `KakaoPayResponseParser.java` | Epoch seconds → UTC; OffsetDateTime → UTC; KST local → UTC; fallback `LocalDateTime.now(ZoneOffset.UTC)` |

---

## 2. Exact Categories of Fixes

- **PayPal UTC conversion**  
  All `LocalDateTime.ofInstant(..., ZoneId.systemDefault())` for `approvedAt`, `canceledAt`, `refundedAt` replaced with `ZoneOffset.UTC`. All `LocalDateTime.now()` in the same provider replaced with `LocalDateTime.now(ZoneOffset.UTC)`.

- **UTC-safe `now()` replacements**  
  Every persisted or comparison-sensitive `LocalDateTime.now()` was replaced with `LocalDateTime.now(ZoneOffset.UTC)` in: Payment entity, payment domain events, schedulers (retry, expiration, notification cleanup, exchange rate), outbox, notification processor/grouping, admin controller/maintenance, FailedPaymentEvent, CompensationTask, PointServiceImpl, PaymentMonitoringService, User, Cashback, TagCountUpdateEvent, PayPal fallbacks.

- **UTC-safe `today()` / range replacements**  
  `StatisticsDateUtils.now()` and `todayStart()` now use UTC. `UserTierServiceImpl` uses `LocalDate.now(ZoneOffset.UTC)` for “today” boundaries. `PromptStatisticsServiceImpl` uses `StatisticsDateUtils.now()` and `todayStart()` for DB range queries.

- **Redis serialization alignment**  
  Redis `ObjectMapper` now uses the same UTC semantics as the main API `ObjectMapper`: `LocalDateTime` serialized/deserialized as ISO-8601 with Z (UTC). No more timezone-ambiguous `yyyy-MM-dd HH:mm:ss`.

- **Payment parser normalization (Kakao)**  
  Epoch seconds → `LocalDateTime.ofEpochSecond(..., ZoneOffset.UTC)`. Offset-aware ISO → `toInstant()` then `LocalDateTime.ofInstant(..., ZoneOffset.UTC)`. KST-only local → `atZone(Asia/Seoul).toInstant()` then `LocalDateTime.ofInstant(..., ZoneOffset.UTC)`. Parse failure fallback → `LocalDateTime.now(ZoneOffset.UTC)`.

- **Notification/scheduler UTC**  
  Duplicate-check window and grouping window use `LocalDateTime.now(ZoneOffset.UTC).minusMinutes(...)`. Cleanup cutoff uses `LocalDateTime.now(ZoneOffset.UTC).minusDays(retentionDays)`.

---

## 3. Safety Notes – Intentionally NOT Changed

- **BaseEntity / JpaAuditingConfig**  
  No change. Auditing fields and configuration were already UTC-safe; no migration or type change.

- **S3PresignedUrlService**  
  `ZoneId.systemDefault()` remains only in diagnostic logging (`logPresignTimeDiagnostics`). Signing and presign flow were not modified per requirements.

- **Schema / DB**  
  No new migrations. Column types remain `LocalDateTime`/`timestamp`; only the semantics of stored values are consistently UTC.

- **API contracts**  
  No breaking changes. Request/response DTOs and JSON shape unchanged; Jackson already serializes `LocalDateTime` as ISO-8601+Z.

- **Test code**  
  Test files under `src/test` were not modified. Optional follow-up: use `LocalDateTime.now(ZoneOffset.UTC)` in tests that build entities with “now” for consistency and timezone-independent tests.

---

## 4. Validation Checklist

- No remaining persisted/comparison-critical `LocalDateTime.now()` or `LocalDate.now()` without UTC in `src/main`.
- No remaining external payment timestamp conversion using `ZoneId.systemDefault()` (only S3 diagnostic log still uses it, by design).
- Redis `LocalDateTime` format matches main UTC semantics (ISO-8601+Z).
- S3 presign signing behavior unchanged.
- BaseEntity auditing unchanged.
- Existing UTC-safe code (e.g. JacksonConfig main bean, JPA time_zone) left as-is.

---

## 5. Follow-up Recommendations (Optional)

- **Tests**  
  Consider replacing `LocalDateTime.now()` with `LocalDateTime.now(ZoneOffset.UTC)` in test builders (e.g. `DefaultPaymentStatusCheckServiceTest`, `DefaultPaymentHistoryQueryServiceTest`, `PaymentRepositoryAdapterTest`, `PaymentGatewayPortAdapterTest`, `PaymentEventPublisherAdapterTest`) so tests are timezone-independent and consistent with production.

- **Utility**  
  If desired, introduce a small `UtcTimeProvider` or `UtcDateTimes` with `nowUtc()`, `todayUtc()` to centralize UTC “now” and reduce duplication; current refactor uses inline `ZoneOffset.UTC` for minimal change.

- **Monitoring**  
  Optional: add a health or diagnostic endpoint that returns server time vs UTC to help operators verify environment (e.g. NTP) when debugging time-sensitive issues.

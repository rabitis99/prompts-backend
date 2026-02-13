sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/storage/StorageLifecycleService.java (2)
102-118: Instant.now() 호출을 루프 외부로 이동 고려

processObject가 객체마다 Instant.now()를 호출합니다. 대량의 객체를 처리할 때 시간 기준이 미세하게 달라질 수 있고, 경계값에 있는 객체가 비일관적으로 처리될 수 있습니다. processLifecycleTransitions에서 시작 시점의 Instant를 한 번 캡처하여 전달하면 일관성이 보장됩니다.

♻️ 수정 제안
processLifecycleTransitions 내부:

+        Instant now = Instant.now();
         // ...
         for (S3Object s3Object : response.contents()) {
-            if (processObject(s3Object)) {
+            if (processObject(s3Object, now)) {
processObject 시그니처 변경:

-    private boolean processObject(S3Object s3Object) {
+    private boolean processObject(S3Object s3Object, Instant now) {
         try {
             Instant lastModified = s3Object.lastModified();
-            long ageInDays = ChronoUnit.DAYS.between(lastModified, Instant.now());
+            long ageInDays = ChronoUnit.DAYS.between(lastModified, now);
157-157: FQN 대신 import 사용 권장

java.util.HashMap을 FQN(fully qualified name)으로 사용하고 있습니다. 파일 상단에 import java.util.HashMap;을 추가하고 간결하게 참조하는 것이 관례적입니다.

sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/production/ProductionArtifactService.java (1)
100-112: capturedTenantId 변수가 불필요합니다.

tenantId는 Line 78에서 선언된 지역 변수로 이미 effectively final이므로, 별도의 capturedTenantId 없이 lambda에서 직접 사용 가능합니다.

♻️ 간소화 제안
-            // tenantId를 명시적으로 캡처하여 비동기 스레드에 전달
-            // ThreadLocal 기반 TenantContext는 비동기 스레드에 전파되지 않으므로 명시적 전달 필요
-            String capturedTenantId = tenantId;
             TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                 `@Override`
                 public void afterCommit() {
-                    thumbnailService.generateThumbnailsAsync(detailId, filePath, capturedTenantId, userId, jobId);
+                    thumbnailService.generateThumbnailsAsync(detailId, filePath, tenantId, userId, jobId);
                 }
             });
비동기 스레드로의 tenantId 전달 방식 자체는 이전 리뷰 피드백이 잘 반영되었습니다.

sharedPrompts/src/main/java/org/example/sharedprompts/module/domain/production/service/storage/LocalStorageStrategy.java (1)
69-77: tenantId만 경로 탐색 검증을 하고 jobId는 하지 않는 불일치가 있습니다.

tenantId에는 .., /, \\를 명시적으로 검증하지만, jobId도 String 타입으로 동일한 경로 탐색 위험이 있습니다. validateWithinBasePath가 최종 방어선 역할을 하므로 당장 보안 취약점은 아니지만, defense-in-depth 관점에서 jobId에도 동일한 검증을 적용하는 것이 좋습니다.

🛡️ 입력 검증 통합 제안
+    private void validatePathSegment(String value, String paramName) {
+        if (value != null && (value.contains("..") || value.contains("/") || value.contains("\\"))) {
+            throw new LocalStorageException("Invalid " + paramName + ": path traversal detected");
+        }
+    }
+
     private Path buildDirectory(String tenantId, Long userId, String jobId) {
+        validatePathSegment(jobId, "jobId");
         if (tenantId != null && !tenantId.isBlank()) {
-            if (tenantId.contains("..") || tenantId.contains("/") || tenantId.contains("\\")) {
-                throw new LocalStorageException("Invalid tenantId: path traversal detected");
-            }
+            validatePathSegment(tenantId, "tenantId");
             return Paths.get(basePath, tenantId, String.valueOf(userId), jobId);
         }
         return Paths.get(basePath, String.valueOf(userId), jobId);
     }
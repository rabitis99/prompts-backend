package org.example.sharedprompts.module.domain.github;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.storage.StorageFacade;
import org.example.sharedprompts.module.domain.production.infra.storage.S3KeyGenerator;
import org.example.sharedprompts.module.domain.production.model.tenant.TenantContext;
import org.example.sharedprompts.module.dto.request.github.GitHubBodyRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Saves generated GitHub Issue and PR bodies as .md files (S3) under the same jobId.
 * Key rule: {prefix}/{tenantId}/0/{jobId}/issue-{jobId}.md, pr-{jobId}.md.
 * Idempotency: when both files already exist, returns existing keys without overwrite.
 * On PR save failure after Issue save: deletes Issue (compensation) and rethrows.
 */
@Service
@Slf4j
public class GitHubBodyStorageService {

    private static final long WEBHOOK_USER_ID = 0L;
    private static final String DEFAULT_TENANT_ID = "github";

    private final StorageFacade storageFacade;
    private final S3KeyGenerator keyGenerator;

    public GitHubBodyStorageService(
            @Autowired(required = false) @Nullable StorageFacade storageFacade,
            @Autowired(required = false) @Nullable S3KeyGenerator keyGenerator) {
        this.storageFacade = storageFacade;
        this.keyGenerator = keyGenerator;
    }

    /**
     * If both issue-{jobId}.md and pr-{jobId}.md exist, returns their keys (for idempotent skip).
     */
    public Optional<StoredKeys> tryGetExistingKeys(GitHubBodyRequestDto request) {
        if (storageFacade == null || keyGenerator == null) {
            return Optional.empty();
        }
        String jobId = request.resolveJobId();
        String tenantId = resolveTenantId(request);
        String issueKey = keyGenerator.generateKey(tenantId, WEBHOOK_USER_ID, jobId, GitHubBodyFileNames.issueFileName(jobId));
        String prKey = keyGenerator.generateKey(tenantId, WEBHOOK_USER_ID, jobId, GitHubBodyFileNames.prFileName(jobId));
        if (storageFacade.exists(issueKey) && storageFacade.exists(prKey)) {
            log.debug("GitHub bodies already exist for jobId: {}, returning existing keys", jobId);
            return Optional.of(new StoredKeys(issueKey, prKey));
        }
        return Optional.empty();
    }

    /**
     * Saves both under the same jobId. Same key rule as above.
     * If PR save fails after Issue save, deletes the Issue object and rethrows.
     * Returns empty when storage or keyGenerator is not available (no null keys propagated).
     */
    public Optional<StoredKeys> saveBothAsMarkdown(String issueBody, String prBody, GitHubBodyRequestDto request) {
        if (storageFacade == null || keyGenerator == null) {
            log.warn("Storage or keyGenerator not available, skipping save for jobId: {}", request.resolveJobId());
            return Optional.empty();
        }
        String jobId = request.resolveJobId();
        String tenantId = resolveTenantId(request);
        String issueFileName = GitHubBodyFileNames.issueFileName(jobId);
        String prFileName = GitHubBodyFileNames.prFileName(jobId);

        String issueKey = null;
        try {
            issueKey = uploadOne(issueBody, tenantId, jobId, issueFileName);
            String prKey = uploadOne(prBody, tenantId, jobId, prFileName);
            if (issueKey != null && prKey != null) {
                log.info("GitHub bodies saved for jobId: {} - issue: {}, pr: {}", jobId, issueKey, prKey);
            }
            return Optional.of(new StoredKeys(issueKey, prKey));
        } catch (Exception e) {
            if (issueKey != null) {
                try {
                    storageFacade.delete(issueKey);
                    log.warn("Compensation: deleted issue file after PR save failure - key: {}", issueKey);
                } catch (Exception deleteEx) {
                    log.error("Compensation delete failed for key {}: {}", issueKey, deleteEx.getMessage());
                }
            }
            throw e;
        }
    }

    /** Download body by S3 key (UTF-8). For idempotent response when both keys already exist. */
    public String downloadBody(String s3Key) {
        if (storageFacade == null || s3Key == null) return "";
        byte[] bytes = storageFacade.download(s3Key);
        return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : "";
    }

    private String resolveTenantId(GitHubBodyRequestDto request) {
        if (request.tenantId() != null && !request.tenantId().isBlank()) return request.tenantId();
        String fromContext = TenantContext.getCurrentTenantId();
        return (fromContext != null && !fromContext.isBlank()) ? fromContext : DEFAULT_TENANT_ID;
    }

    @Nullable
    private String uploadOne(String body, String tenantId, String jobId, String fileName) {
        if (body == null || body.isBlank()) return null;
        return storageFacade.upload(body, tenantId, WEBHOOK_USER_ID, jobId, fileName);
    }

    public record StoredKeys(
            @Nullable String storedIssueFileKey,
            @Nullable String storedPrFileKey) {}
}

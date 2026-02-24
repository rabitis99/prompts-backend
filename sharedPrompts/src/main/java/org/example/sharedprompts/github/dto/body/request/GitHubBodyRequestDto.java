package org.example.sharedprompts.github.dto.body.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Input for generating GitHub Issue and PR body (webhook/API).
 * jobId / deliveryId / sha 중 하나는 반드시 있어야 하며, resolveJobId() 우선순위: jobId → deliveryId → sha.
 * 본문 템플릿은 path의 prompts/{promptId} 로 조회.
 */
public record GitHubBodyRequestDto(
        @JsonProperty("job_id")
        String jobId,
        @JsonProperty("delivery_id")
        String deliveryId,
        @JsonProperty("sha")
        String sha,
        @JsonProperty("repo_full_name")
        String repoFullName,
        @JsonProperty("branch")
        String branch,
        @JsonProperty("base_branch")
        String baseBranch,
        @JsonProperty("title")
        String title,
        @JsonProperty("author")
        String author,
        @JsonProperty("date")
        String date,
        @JsonProperty("commits")
        String commits,
        @JsonProperty("files")
        String files,
        @JsonProperty("tenant_id")
        String tenantId
) {

    /** baseBranch가 null/blank면 "main". */
    public String resolveBaseBranch() {
        return (baseBranch != null && !baseBranch.isBlank()) ? baseBranch : "main";
    }

    /**
     * Job identifier for grouping issue + PR outputs.
     * 우선순위: jobId → deliveryId → sha. 세 값 모두 없으면 BaseException.
     */
    public String resolveJobId() {
        if (jobId != null && !jobId.isBlank()) return jobId;
        if (deliveryId != null && !deliveryId.isBlank()) return deliveryId;
        if (sha != null && !sha.isBlank()) return sha;
        throw new BaseException(ModuleErrorCode.GITHUB_BODY_JOB_ID_REQUIRED, null,
                "jobId, deliveryId, sha 중 하나 이상 필요");
    }

    /** Template용 commits 문자열. */
    public String commitsText() {
        return commits != null ? commits : "";
    }

    /** Template용 files 문자열. */
    public String filesText() {
        return files != null ? files : "";
    }

    /** Template용 repo (repoFullName 또는 빈 문자열). */
    public String repoForTemplate() {
        return repoFullName != null ? repoFullName : "";
    }

    /** Template용 deliveryId. */
    public String deliveryIdForTemplate() {
        return deliveryId != null ? deliveryId : "";
    }

    /** Template용 sha. */
    public String shaForTemplate() {
        return sha != null ? sha : "";
    }

    /** JSON에서 List<String> commits → 단일 문자열로 받기 위한 헬퍼. (Object로 받아서 서비스에서 변환하거나, 커스텀 deserializer 사용 시 활용) */
    public static String joinIfList(Object commits) {
        if (commits == null) return "";
        if (commits instanceof String s) return s;
        if (commits instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining("\n"));
        }
        return commits.toString();
    }

    /** Used internally when generating Issue vs PR body. */
    public enum Kind {
        ISSUE,
        PR
    }
}

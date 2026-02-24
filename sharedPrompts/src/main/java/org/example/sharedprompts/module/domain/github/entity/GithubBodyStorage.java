package org.example.sharedprompts.module.domain.github.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * GitHub 본문 S3 저장 메타데이터 엔티티.
 * S3에 저장된 issue-{jobId}.md, pr-{jobId}.md에 대한 메타데이터를 보관하여
 * 목록 조회, 미리보기, 다운로드 API를 지원합니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "github_body_storage",
        indexes = {
                @Index(name = "uk_tenant_repo_job", columnList = "tenant_key, repo_full_name, job_id", unique = true),
                @Index(name = "idx_owner_created", columnList = "owner_user_id, created_at"),
                @Index(name = "idx_tenant_repo_created", columnList = "tenant_key, repo_full_name, created_at")
        }
)
public class GithubBodyStorage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_key", nullable = false, length = 64)
    private String tenantKey;

    @Column(name = "repo_full_name", nullable = false, length = 256)
    private String repoFullName;

    @Column(name = "job_id", nullable = false, length = 128)
    private String jobId;

    @Column(name = "delivery_id", length = 128)
    private String deliveryId;

    @Column(name = "event_type", length = 32)
    private String eventType;

    @Column(name = "stored_issue_file_key", nullable = false, length = 512)
    private String storedIssueFileKey;

    @Column(name = "stored_pr_file_key", nullable = false, length = 512)
    private String storedPrFileKey;

    @Column(name = "body_prompt_id", nullable = false)
    private Long bodyPromptId;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

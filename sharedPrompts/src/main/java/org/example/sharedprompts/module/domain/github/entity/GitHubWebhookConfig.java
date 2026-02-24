package org.example.sharedprompts.module.domain.github.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;

/**
 * 사용자(테넌트)별 GitHub Webhook 설정.
 * tenantKey + repoFullName 으로 조회하여 해당 레포의 Issue/PR 본문 생성 시 사용할 promptId·저장 namespace 결정.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "github_webhook_config",
        indexes = {
                @Index(name = "idx_github_webhook_config_tenant_repo", columnList = "tenant_key, repo_full_name", unique = true),
                @Index(name = "uk_owner_repo", columnList = "owner_user_id, repo_full_name", unique = true)
        }
)
public class GitHubWebhookConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 외부 Webhook URL 경로/헤더에 쓰는 사용자별 키 (예: /webhooks/github/{tenantKey}).
     */
    @Column(name = "tenant_key", nullable = false, length = 64)
    private String tenantKey;

    /**
     * GitHub repository full name (owner/repo). Webhook payload의 repository.full_name 과 매칭.
     */
    @Column(name = "repo_full_name", nullable = false, length = 256)
    private String repoFullName;

    /**
     * 이 설정을 소유한 사용자 ID (선택, 감사/권한용).
     */
    @Column(name = "owner_user_id")
    private Long ownerUserId;

    /**
     * Issue/PR 본문 템플릿으로 사용할 prompt ID (prompts 테이블).
     */
    @Column(name = "body_prompt_id", nullable = false)
    private Long bodyPromptId;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /**
     * GitHub Webhook Secret (해당 tenantKey URL에 대해 GitHub에 설정한 값).
     * X-Hub-Signature-256 검증 시 사용. 사용자(테넌트)별로 다를 수 있음.
     */
    @Column(name = "webhook_secret", length = 512)
    private String webhookSecret;

    /** 본문 템플릿용 prompt를 다른 것으로 바꿀 때 사용 (createOrGet 등에서 호출). */
    public void updateBodyPromptId(Long bodyPromptId) {
        this.bodyPromptId = bodyPromptId;
    }

    /** Webhook Secret 갱신 (GitHub에 설정한 값과 동일하게 유지). */
    public void updateWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }
}

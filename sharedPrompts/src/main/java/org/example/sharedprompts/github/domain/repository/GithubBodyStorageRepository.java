package org.example.sharedprompts.github.domain.repository;

import org.example.sharedprompts.github.domain.model.GithubBodyStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * GitHub 본문 S3 저장 메타데이터 Repository.
 * 목록 조회(owner_user_id 기준 페이징), 단건 조회, UPSERT를 제공합니다.
 */
public interface GithubBodyStorageRepository extends JpaRepository<GithubBodyStorage, Long> {

    /**
     * 소유자 기준 최신순 페이징 조회 (목록 API용).
     */
    Page<GithubBodyStorage> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId, Pageable pageable);

    /**
     * (tenant_key, repo_full_name, job_id) 기준 멱등 UPSERT.
     * MySQL INSERT ... ON DUPLICATE KEY UPDATE 사용.
     */
    @Modifying
    @Query(value = """
            INSERT INTO github_body_storage (tenant_key, repo_full_name, job_id, delivery_id, event_type, stored_issue_file_key, stored_pr_file_key, body_prompt_id, owner_user_id, created_at)
            VALUES (:tenantKey, :repoFullName, :jobId, :deliveryId, :eventType, :storedIssueFileKey, :storedPrFileKey, :bodyPromptId, :ownerUserId, CURRENT_TIMESTAMP(6)) AS new_row
            ON DUPLICATE KEY UPDATE delivery_id = new_row.delivery_id, event_type = new_row.event_type, stored_issue_file_key = new_row.stored_issue_file_key, stored_pr_file_key = new_row.stored_pr_file_key, body_prompt_id = new_row.body_prompt_id, owner_user_id = new_row.owner_user_id
            """, nativeQuery = true)
    void upsert(
            @Param("tenantKey") String tenantKey,
            @Param("repoFullName") String repoFullName,
            @Param("jobId") String jobId,
            @Param("deliveryId") String deliveryId,
            @Param("eventType") String eventType,
            @Param("storedIssueFileKey") String storedIssueFileKey,
            @Param("storedPrFileKey") String storedPrFileKey,
            @Param("bodyPromptId") Long bodyPromptId,
            @Param("ownerUserId") Long ownerUserId
    );
}

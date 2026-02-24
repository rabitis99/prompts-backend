package org.example.sharedprompts.module.github.port.out;

import org.example.sharedprompts.module.github.domain.model.GithubBodyStorage;
import org.example.sharedprompts.module.github.port.exception.PersistenceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * GitHub 본문 저장소 영속성 포트.
 *
 * 구현 책임:
 * - {@link org.example.sharedprompts.module.github.adapter.out.persistence.JpaBodyStorageAdapter}
 *
 * LSP 계약:
 * - 조회: Optional 또는 Page로 반환 (없으면 empty/zero)
 * - 저장: 예외 발생 (제약, 트랜잭션 오류 등)
 * - Upsert: (tenantKey, repoFullName, jobId) 키로 merge (idempotent)
 */
public interface BodyStoragePersistencePort {

  /**
   * ID로 본문 저장소 조회.
   *
   * @param storageId 저장소 레코드 ID
   * @return 엔티티 또는 empty
   * @throws PersistenceException DB 조회 실패
   */
  Optional<GithubBodyStorage> findById(Long storageId);

  /**
   * 사용자의 저장된 본문 목록 (최신순).
   *
   * @param ownerUserId 소유자 사용자 ID
   * @param pageable 페이징
   * @return 페이지 (empty 가능)
   * @throws PersistenceException DB 조회 실패
   */
  Page<GithubBodyStorage> findByOwner(Long ownerUserId, Pageable pageable);

  /**
   * 본문 저장소 메타데이터 저장 또는 업데이트 (idempotent upsert).
   *
   * 동작:
   * - (tenantKey, repoFullName, jobId) 미존재: 신규 insert
   * - (tenantKey, repoFullName, jobId) 존재: 지정된 필드만 update
   *   (deliveryId, eventType, storedIssueFileKey, storedPrFileKey, bodyPromptId, ownerUserId)
   * - createdAt은 update 되지 않음 (INSERT 시만 설정)
   *
   * @param storage 저장소 엔티티 (tenantKey, repoFullName, jobId, storedIssueFileKey, storedPrFileKey 필수)
   * @throws IllegalArgumentException storage null 또는 필수 필드 null
   * @throws PersistenceException DB upsert 실패
   */
  void upsert(GithubBodyStorage storage);
}

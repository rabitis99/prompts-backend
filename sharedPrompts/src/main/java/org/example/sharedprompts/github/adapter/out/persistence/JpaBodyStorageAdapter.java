package org.example.sharedprompts.github.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.github.domain.model.GithubBodyStorage;
import org.example.sharedprompts.github.domain.repository.GithubBodyStorageRepository;
import org.example.sharedprompts.github.port.exception.PersistenceException;
import org.example.sharedprompts.github.port.out.BodyStoragePersistencePort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JPA 기반 BodyStorage 영속성 포트 구현.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaBodyStorageAdapter implements BodyStoragePersistencePort {

  private final GithubBodyStorageRepository repository;

  @Override
  public Optional<GithubBodyStorage> findById(Long storageId) {
    if (storageId == null) {
      return Optional.empty();
    }
    try {
      return repository.findById(storageId);
    } catch (Exception e) {
      log.warn("Failed to find body storage - id: {}: {}", storageId, e.getMessage());
      throw new PersistenceException("DB query failed", e);
    }
  }

  @Override
  public Page<GithubBodyStorage> findByOwner(Long ownerUserId, Pageable pageable) {
    if (ownerUserId == null) {
      return Page.empty();
    }
    try {
      return repository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId, pageable);
    } catch (Exception e) {
      log.warn("Failed to find body storages - ownerUserId: {}: {}", ownerUserId, e.getMessage());
      throw new PersistenceException("DB query failed", e);
    }
  }

  @Override
  public void upsert(GithubBodyStorage storage) {
    if (storage == null) {
      throw new IllegalArgumentException("storage cannot be null");
    }
    if (storage.getTenantKey() == null || storage.getRepoFullName() == null || storage.getJobId() == null) {
      throw new IllegalArgumentException("storage missing required fields: tenantKey, repoFullName, jobId");
    }
    if (storage.getStoredIssueFileKey() == null || storage.getStoredPrFileKey() == null) {
      throw new IllegalArgumentException("storage missing required fields: storedIssueFileKey, storedPrFileKey");
    }

    try {
      repository.upsert(
          storage.getTenantKey(),
          storage.getRepoFullName(),
          storage.getJobId(),
          storage.getDeliveryId(),
          storage.getEventType(),
          storage.getStoredIssueFileKey(),
          storage.getStoredPrFileKey(),
          storage.getBodyPromptId(),
          storage.getOwnerUserId()
      );
      log.debug("Body storage upserted - tenantKey: {}, repo: {}, jobId: {}",
          storage.getTenantKey(), storage.getRepoFullName(), storage.getJobId());
    } catch (Exception e) {
      log.error("Failed to upsert body storage: {}", e.getMessage());
      throw new PersistenceException("DB upsert failed", e);
    }
  }
}

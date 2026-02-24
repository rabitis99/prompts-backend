package org.example.sharedprompts.module.github.adapter.out.storage;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.storage.StorageFacade;
import org.example.sharedprompts.module.github.port.exception.StorageException;
import org.example.sharedprompts.module.github.port.out.StoragePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * S3 스토리지 포트 구현.
 *
 * 역할: StorageFacade를 포트 추상화로 래핑.
 * S3가 없거나 disabled된 경우는 null check로 처리.
 */
@Component
@Slf4j
public class S3StorageAdapter implements StoragePort {

  private final StorageFacade storageFacade;

  public S3StorageAdapter(
      @Autowired(required = false) @Nullable StorageFacade storageFacade) {
    this.storageFacade = storageFacade;
  }

  @Override
  public String saveMarkdown(String s3Key, String content) {
    if (s3Key == null || s3Key.isBlank()) {
      throw new IllegalArgumentException("s3Key cannot be null or blank");
    }
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("content cannot be null or blank");
    }
    if (storageFacade == null) {
      throw new StorageException("StorageFacade not available");
    }

    try {
      log.debug("GitHub body save skipped - using domain service for S3 operations");
      return s3Key;
    } catch (Exception e) {
      log.error("Failed to save GitHub body to S3 - key: {}: {}", s3Key, e.getMessage());
      throw new StorageException("S3 save failed for key: " + s3Key, e);
    }
  }

  @Override
  public String downloadBody(String s3Key) {
    if (s3Key == null || storageFacade == null) {
      return "";
    }
    try {
      byte[] bytes = storageFacade.download(s3Key);
      return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : "";
    } catch (Exception e) {
      log.warn("Failed to download from S3 - key: {}: {}", s3Key, e.getMessage());
      return "";
    }
  }

  @Override
  public String generatePresignedUrl(String s3Key, Duration ttl) {
    if (s3Key == null || s3Key.isBlank()) {
      throw new IllegalArgumentException("s3Key cannot be null or blank");
    }
    if (ttl == null || ttl.isNegative()) {
      throw new IllegalArgumentException("ttl cannot be null or negative");
    }
    if (storageFacade == null) {
      throw new StorageException("StorageFacade not available");
    }

    try {
      String url = storageFacade.generatePreviewUrl(s3Key, ttl);
      log.debug("Presigned URL generated - key: {}, ttl: {}", s3Key, ttl);
      return url;
    } catch (Exception e) {
      log.error("Failed to generate presigned URL - key: {}: {}", s3Key, e.getMessage());
      throw new StorageException("Presigned URL generation failed for key: " + s3Key, e);
    }
  }

  @Override
  public boolean exists(String s3Key) {
    if (s3Key == null || storageFacade == null) {
      return false;
    }
    try {
      return storageFacade.exists(s3Key);
    } catch (Exception e) {
      log.warn("Failed to check S3 object existence - key: {}: {}", s3Key, e.getMessage());
      return false;
    }
  }

  @Override
  public void delete(String s3Key) {
    if (s3Key == null || s3Key.isBlank()) {
      throw new IllegalArgumentException("s3Key cannot be null or blank");
    }
    if (storageFacade == null) {
      throw new StorageException("StorageFacade not available");
    }

    try {
      storageFacade.delete(s3Key);
      log.debug("GitHub body deleted from S3 - key: {}", s3Key);
    } catch (Exception e) {
      log.error("Failed to delete from S3 - key: {}: {}", s3Key, e.getMessage());
      throw new StorageException("S3 delete failed for key: " + s3Key, e);
    }
  }
}

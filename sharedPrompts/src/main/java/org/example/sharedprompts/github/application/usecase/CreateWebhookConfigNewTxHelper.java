package org.example.sharedprompts.github.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.github.domain.model.GitHubWebhookConfig;
import org.example.sharedprompts.github.port.out.WebhookConfigPersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * GitHub Webhook 설정 조회용 신규 트랜잭션 헬퍼.
 *
 * DataIntegrityViolationException 발생 후 커밋된 행을 다시 조회할 때
 * REQUIRES_NEW 트랜잭션을 사용하기 위해 분리된 빈입니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateWebhookConfigNewTxHelper {

  private final WebhookConfigPersistencePort configPort;

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Optional<GitHubWebhookConfig> findExistingInNewTransaction(Long ownerUserId, String repoFullName) {
    return configPort.findByOwner(ownerUserId, repoFullName);
  }
}


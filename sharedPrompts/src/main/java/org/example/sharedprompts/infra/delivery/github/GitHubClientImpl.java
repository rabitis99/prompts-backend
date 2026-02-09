package org.example.sharedprompts.infra.delivery.github;

import org.example.sharedprompts.module.domain.delivery.api.client.GitHubClient;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryContext;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryResult;
import org.example.sharedprompts.module.domain.delivery.api.model.DefaultDeliveryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * GitHub API와 연동하는 Client 구현체.
 * Delivery 계층에서 사용되며, 외부 API 호출을 담당한다.
 */
@Component
public class GitHubClientImpl implements GitHubClient {
    
    private static final Logger log = LoggerFactory.getLogger(GitHubClientImpl.class);
    
    /**
     * 파일을 GitHub에 업로드한다.
     */
    public DeliveryResult upload(String filePath, String action, DeliveryContext context) {
        String repository = context.getAttribute("repository", String.class);
        String branch = context.getAttribute("branch", String.class);
        String commitMessage = context.getAttribute("commitMessage", String.class);
        
        // TODO: 실제 GitHub API 연동 구현 시 null 검증 필요
        // repository, branch, commitMessage 등 필수 속성에 대한 null 체크 및 검증 로직 추가
        
        if (action == null) {
            log.warn("GitHub action이 null입니다: userId={}", context.getUserId());
            return DefaultDeliveryResult.failure("action is required");
        }
        
        log.info("GitHub 업로드 시도: userId={}, repository={}, branch={}, action={}, filePath={}", 
                context.getUserId(), repository, branch, action, filePath);
        
        // TODO: 실제 GitHub API 연동 구현
        // - GitHub REST API 또는 GraphQL API 사용
        // - 인증 토큰 관리
        // - PR 생성, Commit, Issue 생성 등
        
        switch (action.toLowerCase()) {
            case "pr":
                log.info("GitHub PR 생성 (시뮬레이션): repository={}, branch={}", repository, branch);
                break;
            case "commit":
                log.info("GitHub Commit 생성 (시뮬레이션): repository={}, message={}", repository, commitMessage);
                break;
            case "issue":
                log.info("GitHub Issue 생성 (시뮬레이션): repository={}", repository);
                break;
            default:
                log.warn("알 수 없는 GitHub 액션: {}", action);
                return DefaultDeliveryResult.failure("Unknown GitHub action: " + action);
        }
        
        // 현재는 시뮬레이션으로 성공 반환
        log.info("GitHub 업로드 완료 (시뮬레이션): action={}", action);
        return DefaultDeliveryResult.success();
    }
}


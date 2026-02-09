package org.example.sharedprompts.infra.delivery.github;

import org.example.sharedprompts.domain.delivery.api.DeliveryContext;
import org.example.sharedprompts.domain.delivery.api.DeliveryResult;
import org.example.sharedprompts.domain.delivery.api.DefaultDeliveryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * GitHub API와 연동하는 Client.
 * Delivery 계층에서 사용되며, 외부 API 호출을 담당한다.
 * 
 * 현재는 기본 구현으로, 실제 GitHub API 연동은 추후 구현 예정.
 */
@Component
public class GitHubClient {
    
    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);
    
    /**
     * 파일을 GitHub에 업로드한다.
     * 
     * @param filePath 업로드할 파일 경로
     * @param action 수행할 액션 (pr, commit, issue 등)
     * @param context Delivery 컨텍스트
     * @return Delivery 결과
     */
    public DeliveryResult upload(String filePath, String action, DeliveryContext context) {
        String repository = context.getAttribute("repository", String.class);
        String branch = context.getAttribute("branch", String.class);
        String commitMessage = context.getAttribute("commitMessage", String.class);
        
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
        }
        
        // 현재는 시뮬레이션으로 성공 반환
        log.info("GitHub 업로드 완료 (시뮬레이션): action={}", action);
        return DefaultDeliveryResult.success();
    }
}


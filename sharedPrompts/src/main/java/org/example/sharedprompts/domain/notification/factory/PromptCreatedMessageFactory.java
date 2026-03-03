package org.example.sharedprompts.domain.notification.factory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.notification.message.PromptCreatedMessage;
import org.example.sharedprompts.domain.notification.service.target.NotificationTargetService;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.user.User;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 프롬프트 생성 SSE 알림 메시지를 생성하는 Factory
 * - 팔로워 조회와 메시지 생성을 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptCreatedMessageFactory {

    private final NotificationTargetService notificationTargetService;

    /**
     * 프롬프트 생성 SSE 알림 메시지 생성
     * - 팔로워 조회를 포함하여 메시지 생성
     *
     * @param prompt 생성된 프롬프트 엔티티
     * @param author 프롬프트 작성자
     * @return 프롬프트 생성 SSE 알림 메시지 (팔로워가 없으면 null)
     */
    public PromptCreatedMessage createMessage(Prompt prompt, User author) {
        List<Long> followerIds = getFollowerIdsIfNotEmpty(author.getId(), prompt.getId());
        if (followerIds == null) {
            return null;
        }

        return PromptCreatedMessage.builder()
                .promptId(prompt.getId())
                .authorId(author.getId())
                .authorNickname(author.getNickname())
                .promptSummary(prompt.getDescription())
                .followerIds(followerIds)
                .build();
    }

    /**
     * 프롬프트 정보를 기반으로 메시지 생성 (팔로워 조회 포함)
     *
     * @param promptId 프롬프트 ID
     * @param authorId 작성자 ID
     * @param authorNickname 작성자 닉네임
     * @param promptSummary 프롬프트 요약
     * @return 프롬프트 생성 SSE 알림 메시지 (팔로워가 없으면 null)
     */
    public PromptCreatedMessage createMessage(Long promptId, Long authorId, 
                                               String authorNickname, String promptSummary) {
        List<Long> followerIds = getFollowerIdsIfNotEmpty(authorId, promptId);
        if (followerIds == null) {
            return null;
        }

        return PromptCreatedMessage.builder()
                .promptId(promptId)
                .authorId(authorId)
                .authorNickname(authorNickname)
                .promptSummary(promptSummary)
                .followerIds(followerIds)
                .build();
    }

    /**
     * 팔로워 ID 목록 조회 및 빈 목록 체크
     * 
     * @param authorId 작성자 ID
     * @param promptId 프롬프트 ID (로깅용)
     * @return 팔로워 ID 목록 (비어있으면 null)
     */
    private List<Long> getFollowerIdsIfNotEmpty(Long authorId, Long promptId) {
        List<Long> followerIds = notificationTargetService.getPromptCreatedTargets(authorId);
        
        if (followerIds.isEmpty()) {
            log.debug("No followers to notify for prompt: promptId={}, authorId={}", 
                    promptId, authorId);
            return null;
        }
        
        return followerIds;
    }
}


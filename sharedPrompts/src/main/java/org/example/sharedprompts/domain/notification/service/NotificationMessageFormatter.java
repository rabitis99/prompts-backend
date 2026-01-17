package org.example.sharedprompts.domain.notification.service;

import org.example.sharedprompts.domain.user.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

/**
 * 알림 메시지 포맷팅 서비스
 * - 알림 타입별 메시지 템플릿 관리
 * - 템플릿을 Map으로 관리하여 중앙화
 */
@Component
public class NotificationMessageFormatter {

    /**
     * 메시지 템플릿 맵
     * - 알림 타입별 포맷팅 함수를 저장
     */
    private final Map<String, Function<User, String>> messageTemplates = Map.of(
            "comment", this::formatCommentMessage,
            "reply", this::formatReplyMessage,
            "prompt_like", this::formatPromptLikeMessage,
            "comment_like", this::formatCommentLikeMessage
    );

    /**
     * 댓글 알림 메시지 생성
     */
    public String formatCommentMessage(User commentAuthor) {
        return String.format("%s님이 당신의 프롬프트에 댓글을 남겼습니다.", commentAuthor.getNickname());
    }

    /**
     * 대댓글 알림 메시지 생성
     */
    public String formatReplyMessage(User commentAuthor) {
        return String.format("%s님이 당신의 댓글에 답글을 남겼습니다.", commentAuthor.getNickname());
    }

    /**
     * 프롬프트 좋아요 알림 메시지 생성
     */
    public String formatPromptLikeMessage(User likeUser) {
        return String.format("%s님이 당신의 프롬프트를 좋아합니다.", likeUser.getNickname());
    }

    /**
     * 댓글 좋아요 알림 메시지 생성
     */
    public String formatCommentLikeMessage(User likeUser) {
        return String.format("%s님이 당신의 댓글을 좋아합니다.", likeUser.getNickname());
    }

    /**
     * 템플릿 키로 메시지 포맷팅
     * - 템플릿 키 기반 포맷팅
     */
    public String format(String templateKey, User user) {
        Function<User, String> formatter = messageTemplates.get(templateKey);
        if (formatter != null) {
            return formatter.apply(user);
        }
        throw new IllegalArgumentException("Unknown template key: " + templateKey);
    }
}


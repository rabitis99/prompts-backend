package org.example.sharedprompts.domain.notification.service.formatter;

import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 알림 메시지 포맷팅 서비스
 * - 알림 타입별 메시지 템플릿 관리
 * - 템플릿을 Map으로 관리하여 중앙화
 * - 동적 추가 가능: 런타임에 새로운 템플릿 등록 지원
 */
@Component
public class NotificationMessageFormatter {

    /**
     * 메시지 템플릿 맵
     * - 알림 타입별 포맷팅 함수를 저장
     * - 동적 추가 가능한 mutable Map 사용
     */
    private final Map<String, Function<User, String>> messageTemplates = new ConcurrentHashMap<>();

    /**
     * 기본 템플릿 초기화
     * - 기본 알림 타입들의 템플릿을 등록
     */
    public NotificationMessageFormatter() {
        registerTemplate("comment", this::formatCommentMessage);
        registerTemplate("reply", this::formatReplyMessage);
        registerTemplate("prompt_like", this::formatPromptLikeMessage);
        registerTemplate("comment_like", this::formatCommentLikeMessage);
        registerTemplate("prompt_favorite", this::formatPromptFavoriteMessage);
        registerTemplate("follow", this::formatFollowMessage);
    }

    /**
     * 템플릿 등록
     * - 런타임에 새로운 포맷팅 템플릿을 동적으로 추가
     *
     * @param templateKey 템플릿 키
     * @param formatter 포맷팅 함수
     */
    public void registerTemplate(String templateKey, Function<User, String> formatter) {
        messageTemplates.put(templateKey, formatter);
    }

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
     * 프롬프트 즐겨찾기 알림 메시지 생성
     */
    public String formatPromptFavoriteMessage(User favoriteUser) {
        return String.format("%s님이 당신의 프롬프트를 즐겨찾기에 추가했습니다.", favoriteUser.getNickname());
    }

    /**
     * 팔로우 알림 메시지 생성
     */
    public String formatFollowMessage(User follower) {
        return String.format("%s님이 팔로우를 요청했습니다.", follower.getNickname());
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
        throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "templateKey", 
                "Unknown template key: " + templateKey);
    }
}


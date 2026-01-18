package org.example.sharedprompts.domain.notification.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.comment.event.CommentEvent;
import org.example.sharedprompts.domain.comment.repository.CommentRepository;
import org.example.sharedprompts.domain.favorite.event.FavoriteEvent;
import org.example.sharedprompts.domain.follow.event.FollowEvent;
import org.example.sharedprompts.domain.like.event.LikeEvent;
import org.example.sharedprompts.domain.notification.enums.NotificationType;
import org.example.sharedprompts.domain.notification.enums.RelatedEntityType;
import org.example.sharedprompts.domain.notification.message.NotificationMessage;
import org.example.sharedprompts.domain.notification.producer.NotificationProducer;
import org.example.sharedprompts.domain.notification.repository.NotificationRepository;
import org.example.sharedprompts.domain.notification.setting.repository.UserNotificationSettingRepository;
import org.example.sharedprompts.domain.notification.service.NotificationGroupingService;
import org.example.sharedprompts.domain.notification.service.NotificationMessageFormatter;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 알림 이벤트 처리 서비스
 * - 이벤트를 받아서 알림 대상을 결정하고 메시지를 생성하는 비즈니스 로직 처리
 * - RabbitMQ로 메시지 발행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventProcessor {

    private final UserRepository userRepository;
    private final PromptRepository promptRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final UserNotificationSettingRepository notificationSettingRepository;
    private final NotificationGroupingService groupingService;
    private final NotificationProducer notificationProducer;
    private final NotificationMessageFormatter messageFormatter;

    @Value("${notification.duplicate.check-window-minutes:5}")
    private int duplicateCheckWindowMinutes;

    /**
     * 댓글 생성 이벤트 처리
     * - 프롬프트 작성자에게 알림 (본인이 댓글 단 경우 제외)
     * - 대댓글인 경우 부모 댓글 작성자에게도 알림 (본인이 댓글 단 경우 제외)
     */
    public void processCommentCreated(CommentEvent.Created event) {
        Comment comment = getComment(event.commentId());
        User commentAuthor = comment.getUser();
        Prompt prompt = comment.getPrompt();
        User promptAuthor = prompt.getAuthor();

        List<NotificationMessage> notifications = new ArrayList<>();

        // 프롬프트 작성자에게 알림 (본인이 댓글 단 경우 제외)
        boolean isNotSelf = !commentAuthor.getId().equals(promptAuthor.getId());
        boolean isEnabled = isNotificationEnabled(promptAuthor, NotificationType.COMMENT);
        boolean isNotDuplicate = !isDuplicateNotification(promptAuthor.getId(), NotificationType.COMMENT, event.promptId());
        
        if (isNotSelf && isEnabled && isNotDuplicate) {
            String message = messageFormatter.formatCommentMessage(commentAuthor);
            notifications.add(createNotification(
                    promptAuthor.getId(),
                    NotificationType.COMMENT,
                    RelatedEntityType.PROMPT,
                    event.promptId(),
                    event.userId(),
                    message
            ));
        }

        // 대댓글인 경우 부모 댓글 작성자에게도 알림
        if (event.parentId() != null) {
            Comment parentComment = commentRepository.findById(event.parentId()).orElse(null);
            if (parentComment != null) {
                User parentCommentAuthor = parentComment.getUser();

                // 부모 댓글 작성자에게 알림 (본인이 댓글 단 경우 및 프롬프트 작성자와 동일한 경우 제외)
                boolean isSelfComment = commentAuthor.getId().equals(parentCommentAuthor.getId());
                boolean isPromptAuthor = parentCommentAuthor.getId().equals(promptAuthor.getId());
                boolean isNotSelfOrPromptAuthor = !isSelfComment && !isPromptAuthor;
                boolean isParentNotificationEnabled = isNotificationEnabled(parentCommentAuthor, NotificationType.COMMENT);
                boolean isParentNotDuplicate = !isDuplicateNotification(parentCommentAuthor.getId(), NotificationType.COMMENT, event.promptId());
                
                if (isNotSelfOrPromptAuthor && isParentNotificationEnabled && isParentNotDuplicate) {
                    String replyMessage = messageFormatter.formatReplyMessage(commentAuthor);
                    notifications.add(createNotification(
                            parentCommentAuthor.getId(),
                            NotificationType.COMMENT,
                            RelatedEntityType.PROMPT,
                            event.promptId(),
                            event.userId(),
                            replyMessage
                    ));
                }
            }
        }

        // 모든 알림 메시지 배치 발행
        notifications.forEach(notificationProducer::publishNotification);
    }

    /**
     * 프롬프트 좋아요 이벤트 처리
     * - 프롬프트 작성자에게 알림 (본인이 좋아요 누른 경우 제외)
     */
    public void processPromptLiked(LikeEvent.PromptLiked event) {
        Prompt prompt = getPrompt(event.promptId());
        User promptAuthor = prompt.getAuthor();
        User likeUser = getUser(event.userId());

        // 프롬프트 작성자에게 알림 (본인이 좋아요 누른 경우 제외)
        if (event.userId().equals(promptAuthor.getId())) {
            return; // 본인이 좋아요 누른 경우 알림 제외
        }
        
        if (!isNotificationEnabled(promptAuthor, NotificationType.LIKE)) {
            return; // 알림 설정이 꺼져 있는 경우
        }
        
        if (isDuplicateNotification(promptAuthor.getId(), NotificationType.LIKE, event.promptId())) {
            return; // 중복 알림인 경우
        }
        
        String message = messageFormatter.formatPromptLikeMessage(likeUser);
        NotificationMessage notification = createNotification(
                promptAuthor.getId(),
                NotificationType.LIKE,
                RelatedEntityType.PROMPT,
                event.promptId(),
                event.userId(),
                message
        );
        publishNotification(notification);
    }

    /**
     * 댓글 좋아요 이벤트 처리
     * - 댓글 작성자에게 알림 (본인이 좋아요 누른 경우 제외)
     */
    public void processCommentLiked(LikeEvent.CommentLiked event) {
        Comment comment = getComment(event.commentId());
        User commentAuthor = comment.getUser();
        User likeUser = getUser(event.userId());

        // 댓글 작성자에게 알림 (본인이 좋아요 누른 경우 제외)
        if (event.userId().equals(commentAuthor.getId())) {
            return; // 본인이 좋아요 누른 경우 알림 제외
        }
        
        if (!isNotificationEnabled(commentAuthor, NotificationType.LIKE)) {
            return; // 알림 설정이 꺼져 있는 경우
        }
        
        // relatedEntityId는 댓글 ID로 설정하여 댓글 좋아요임을 명확히 함
        Long commentId = comment.getId();
        
        if (isDuplicateNotification(commentAuthor.getId(), NotificationType.LIKE, commentId)) {
            return; // 중복 알림인 경우
        }
        
        String message = messageFormatter.formatCommentLikeMessage(likeUser);
        NotificationMessage notification = createNotification(
                commentAuthor.getId(),
                NotificationType.LIKE,
                RelatedEntityType.COMMENT,
                commentId,
                event.userId(),
                message
        );
        publishNotification(notification);
    }

    /**
     * 프롬프트 즐겨찾기 이벤트 처리
     * - 프롬프트 작성자에게 알림 (본인이 즐겨찾기 추가한 경우 제외)
     */
    public void processPromptFavorited(FavoriteEvent.PromptFavorited event) {
        Prompt prompt = getPrompt(event.promptId());
        User promptAuthor = prompt.getAuthor();
        User favoriteUser = getUser(event.userId());

        // 프롬프트 작성자에게 알림 (본인이 즐겨찾기 추가한 경우 제외)
        if (event.userId().equals(promptAuthor.getId())) {
            return; // 본인이 즐겨찾기 추가한 경우 알림 제외
        }
        
        if (!isNotificationEnabled(promptAuthor, NotificationType.FAVORITE)) {
            return; // 알림 설정이 꺼져 있는 경우
        }
        
        if (isDuplicateNotification(promptAuthor.getId(), NotificationType.FAVORITE, event.promptId())) {
            return; // 중복 알림인 경우
        }
        
        String message = messageFormatter.formatPromptFavoriteMessage(favoriteUser);
        NotificationMessage notification = createNotification(
                promptAuthor.getId(),
                NotificationType.FAVORITE,
                RelatedEntityType.PROMPT,
                event.promptId(),
                event.userId(),
                message
        );
        publishNotification(notification);
    }

    /**
     * 팔로우 요청 이벤트 처리
     * - 팔로우 대상자(followingId)에게 알림 (본인이 팔로우 요청한 경우 제외)
     */
    public void processFollowRequested(FollowEvent.Requested event) {
        User followingUser = getUser(event.followingId());
        User followerUser = getUser(event.followerId());

        // 팔로우 대상자에게 알림 (본인이 팔로우 요청한 경우 제외)
        if (event.followerId().equals(event.followingId())) {
            return; // 본인이 자신을 팔로우 요청한 경우 알림 제외
        }
        
        if (!isNotificationEnabled(followingUser, NotificationType.FOLLOW)) {
            return; // 알림 설정이 꺼져 있는 경우
        }
        
        // relatedEntityId는 팔로우 대상자(followingId)로 설정
        if (isDuplicateNotification(followingUser.getId(), NotificationType.FOLLOW, event.followingId())) {
            return; // 중복 알림인 경우
        }
        
        String message = messageFormatter.formatFollowMessage(followerUser);
        NotificationMessage notification = createNotification(
                followingUser.getId(),
                NotificationType.FOLLOW,
                RelatedEntityType.USER,
                event.followingId(),
                event.followerId(),
                message
        );
        publishNotification(notification);
    }

    // ======================
    //      내부 헬퍼 메서드
    // ======================

    /**
     * 댓글 조회
     */
    private Comment getComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));
    }

    /**
     * 프롬프트 조회
     */
    private Prompt getPrompt(Long promptId) {
        return promptRepository.findById(promptId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));
    }

    /**
     * 사용자 조회
     */
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 알림 메시지 생성
     */
    private NotificationMessage createNotification(Long userId, NotificationType type,
                                                    RelatedEntityType relatedEntityType,
                                                    Long relatedEntityId, Long actorId, String message) {
        String groupKey = groupingService.generateGroupKey(userId, type, relatedEntityId);
        return NotificationMessage.builder()
                .userId(userId)
                .type(type)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .actorId(actorId)
                .message(message)
                .groupKey(groupKey)
                .build();
    }

    /**
     * 알림 메시지 발행
     */
    private void publishNotification(NotificationMessage notification) {
        notificationProducer.publishNotification(notification);
    }

    /**
     * 중복 알림 체크
     * - 최근 N분 내 같은 타입, 같은 관련 엔티티에 대한 알림이 있는지 확인
     */
    private boolean isDuplicateNotification(Long userId, NotificationType type, Long relatedEntityId) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(duplicateCheckWindowMinutes);
        return notificationRepository.existsRecentNotification(userId, type, relatedEntityId, since);
    }

    /**
     * 알림 설정 확인
     * - 사용자의 알림 타입별 수신 여부 확인
     */
    private boolean isNotificationEnabled(User user, NotificationType notificationType) {
        return notificationSettingRepository.isEnabled(user, notificationType);
    }
}


package org.example.sharedprompts.domain.like.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.like.CommentLikeId;
import org.example.sharedprompts.domain.like.PromptLikeId;
import org.example.sharedprompts.domain.like.event.LikeEventPublisher;
import org.example.sharedprompts.domain.like.repository.CommentLikeRepository;
import org.example.sharedprompts.domain.like.repository.PromptLikeRepository;
import org.example.sharedprompts.dto.like.response.CommentLikeResponseDto;
import org.example.sharedprompts.dto.like.response.PromptLikeResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final PromptLikeRepository likeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PromptLikeDomainService promptLikeDomainService;
    private final CommentLikeDomainService commentLikeDomainService;
    private final LikeCountService likeCountService;
    private final LikeEventPublisher likeEventPublisher;

    @Override
    @Transactional
    public PromptLikeResponseDto likePrompt(Long userId, Long promptId) {
        promptLikeDomainService.like(userId, promptId);

        long likeCount = likeCountService.incrementAndGetPromptLikeCount(promptId);

        // 좋아요 알림 등 부수 효과를 위한 이벤트 발행
        likeEventPublisher.publishPromptLiked(userId, promptId);

        return PromptLikeResponseDto.of(true, likeCount);
    }

    @Override
    @Transactional
    public PromptLikeResponseDto unlikePrompt(Long userId, Long promptId) {
        promptLikeDomainService.unlike(userId, promptId);

        long likeCount = likeCountService.decrementAndGetPromptLikeCount(promptId);

        return PromptLikeResponseDto.of(false, likeCount);
    }

    @Override
    @Transactional
    public CommentLikeResponseDto likeComment(Long userId, Long commentId) {
        commentLikeDomainService.like(userId, commentId);

        long likeCount = likeCountService.incrementAndGetCommentLikeCount(commentId);

        // 댓글 좋아요 알림 등 부수 효과를 위한 이벤트 발행
        likeEventPublisher.publishCommentLiked(userId, commentId);

        return CommentLikeResponseDto.of(true, likeCount);
    }

    @Override
    @Transactional
    public CommentLikeResponseDto unlikeComment(Long userId, Long commentId) {
        commentLikeDomainService.unlike(userId, commentId);

        long likeCount = likeCountService.decrementAndGetCommentLikeCount(commentId);

        return CommentLikeResponseDto.of(false, likeCount);
    }

    @Override
    @Transactional(readOnly = true)
    public PromptLikeResponseDto checkPromptLike(Long userId, Long promptId) {
        PromptLikeId id = new PromptLikeId(promptId, userId);
        boolean isLiked = likeRepository.existsById(id);
        Long likeCount = likeCountService.getPromptLikeCounts(java.util.List.of(promptId))
                .getOrDefault(promptId, 0L);
        return PromptLikeResponseDto.of(isLiked, likeCount);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentLikeResponseDto checkCommentLike(Long userId, Long commentId) {
        CommentLikeId id = new CommentLikeId(commentId, userId);
        boolean isLiked = commentLikeRepository.existsById(id);
        Long likeCount = likeCountService.getCommentLikeCounts(java.util.List.of(commentId))
                .getOrDefault(commentId, 0L);
        return CommentLikeResponseDto.of(isLiked, likeCount);
    }

    // 중복 방지 및 FK 검증, 저장/삭제 로직은 PromptLikeDomainService / CommentLikeDomainService 에서 담당한다.
}

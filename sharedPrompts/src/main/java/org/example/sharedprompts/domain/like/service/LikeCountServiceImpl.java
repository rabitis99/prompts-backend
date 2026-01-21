package org.example.sharedprompts.domain.like.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.shared.service.BaseCountService;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LikeCountServiceImpl implements LikeCountService {

    private final BaseCountService baseCountService;

    private String commentLikeKey(Long commentId) {
        if (commentId == null) {
            throw new ApiException(ErrorCode.COMMENT_NOT_FOUND);
        }
        return "like:comment:" + commentId;
    }

    private String promptLikeKey(Long promptId) {
        if (promptId == null) {
            throw new ApiException(ErrorCode.PROMPT_NOT_FOUND);
        }
        return "like:prompt:" + promptId;
    }

    @Override
    public void incrementCommentLikeCount(Long commentId) {
        baseCountService.increment(commentLikeKey(commentId));
    }

    @Override
    public void incrementPromptLikeCount(Long promptId) {
        baseCountService.increment(promptLikeKey(promptId));
    }

    @Override
    public void decrementCommentLikeCount(Long commentId) {
        baseCountService.decrement(commentLikeKey(commentId));
    }

    @Override
    public void decrementPromptLikeCount(Long promptId) {
        baseCountService.decrement(promptLikeKey(promptId));
    }

    @Override
    public long incrementAndGetCommentLikeCount(Long commentId) {
        return baseCountService.incrementAndGet(commentLikeKey(commentId));
    }

    @Override
    public long incrementAndGetPromptLikeCount(Long promptId) {
        return baseCountService.incrementAndGet(promptLikeKey(promptId));
    }

    @Override
    public long decrementAndGetCommentLikeCount(Long commentId) {
        return baseCountService.decrementAndGet(commentLikeKey(commentId));
    }

    @Override
    public long decrementAndGetPromptLikeCount(Long promptId) {
        return baseCountService.decrementAndGet(promptLikeKey(promptId));
    }

    @Override
    public void setCommentLikeCount(Long commentId, long count) {
        baseCountService.set(commentLikeKey(commentId), count);
    }

    @Override
    public void setPromptLikeCount(Long promptId, long count) {
        baseCountService.set(promptLikeKey(promptId), count);
    }

    @Override
    public Map<Long, Long> getCommentLikeCounts(List<Long> commentIds) {
        return baseCountService.getCounts(commentIds, this::commentLikeKey);
    }

    @Override
    public Map<Long, Long> getPromptLikeCounts(List<Long> promptIds) {
        return baseCountService.getCounts(promptIds, this::promptLikeKey);
    }
}

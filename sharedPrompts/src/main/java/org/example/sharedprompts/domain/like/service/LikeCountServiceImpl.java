package org.example.sharedprompts.domain.like.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.shared.service.BaseCountService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LikeCountServiceImpl implements LikeCountService {

    private final BaseCountService baseCountService;

    private String commentLikeKey(Long commentId) {
        return "like:comment:" + commentId;
    }

    private String promptLikeKey(Long promptId) {
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
    public Map<Long, Long> getCommentLikeCounts(List<Long> commentIds) {
        return baseCountService.getCounts(commentIds, this::commentLikeKey);
    }

    @Override
    public Map<Long, Long> getPromptLikeCounts(List<Long> promptIds) {
        return baseCountService.getCounts(promptIds, this::promptLikeKey);
    }
}

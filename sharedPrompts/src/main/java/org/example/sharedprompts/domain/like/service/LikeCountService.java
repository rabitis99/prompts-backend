package org.example.sharedprompts.domain.like.service;

import java.util.List;
import java.util.Map;

public interface LikeCountService {

    void incrementCommentLikeCount(Long commentId);
    void incrementPromptLikeCount(Long promptId);

    void decrementCommentLikeCount(Long commentId);
    void decrementPromptLikeCount(Long promptId);

    long incrementAndGetCommentLikeCount(Long commentId);
    long incrementAndGetPromptLikeCount(Long promptId);

    long decrementAndGetCommentLikeCount(Long commentId);
    long decrementAndGetPromptLikeCount(Long promptId);

    // 관리자/복구용: DB 기준으로 Redis 카운트를 재설정할 때 사용
    void setCommentLikeCount(Long commentId, long count);
    void setPromptLikeCount(Long promptId, long count);

    Map<Long, Long> getCommentLikeCounts(List<Long> commentIds);
    Map<Long, Long> getPromptLikeCounts(List<Long> promptIds);


}

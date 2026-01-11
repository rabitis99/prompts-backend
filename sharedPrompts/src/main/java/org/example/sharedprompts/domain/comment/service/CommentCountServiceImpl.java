package org.example.sharedprompts.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.shared.service.BaseCountService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentCountServiceImpl implements CommentCountService {

    private final BaseCountService baseCountService;

    // Redis Key 헬퍼
    private String commentKey(Long promptId) {
        return "comment:prompt:" + promptId;
    }

    private String replyKey(Long parentId) {
        return "comment:parent:" + parentId;
    }

    // ------------------ 증가/감소 ------------------

    @Override
    public void incrementCommentCount(Long promptId) {
        baseCountService.increment(commentKey(promptId));
    }

    @Override
    public void incrementReplyCount(Long parentId) {
        baseCountService.increment(replyKey(parentId));
    }

    @Override
    public void decrementCommentCount(Long promptId) {
        baseCountService.decrement(commentKey(promptId));
    }

    @Override
    public void decrementReplyCount(Long parentId) {
        baseCountService.decrement(replyKey(parentId));
    }

    // ------------------ 조회 ------------------

    @Override
    public Map<Long, Long> getReplyCounts(List<Long> parentIds) {
        return baseCountService.getCounts(parentIds, this::replyKey);
    }

    @Override
    public Map<Long, Long> getCommentCounts(List<Long> promptIds) {
        return baseCountService.getCounts(promptIds, this::commentKey);
    }
}

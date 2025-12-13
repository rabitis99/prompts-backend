package org.example.sharedprompts.domain.comment.service;

import java.util.List;
import java.util.Map;

public interface CommentCountService {

    void incrementCommentCount(Long promptId);
    void incrementReplyCount(Long parentId);

    void decrementCommentCount(Long promptId);
    void decrementReplyCount(Long parentId);

    Map<Long, Long> getReplyCounts(List<Long> parentIds);
    Map<Long, Long> getCommentCounts(List<Long> promptIds);
}

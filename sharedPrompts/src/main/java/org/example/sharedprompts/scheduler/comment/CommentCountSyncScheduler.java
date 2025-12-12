package org.example.sharedprompts.scheduler.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.comment.repository.CommentRepository;
import org.example.sharedprompts.domain.comment.service.CommentCountService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentCountSyncScheduler {

    private final CommentRepository commentRepository;
    private final CommentCountService commentCountService;

    private static final int BATCH_SIZE = 1000;

    @Scheduled(fixedRate = 10 * 60 * 1000) // 10분마다 실행
    @Transactional
    public void syncCommentCounts() {
        // 1. 모든 댓글 조회
        List<Long> allCommentIds = commentRepository.findAllIds();

        int total = allCommentIds.size();
        int processed = 0;


        while (processed < total) {
            // 2. 배치 단위로 ID 자르기
            int end = Math.min(processed + BATCH_SIZE, total);
            List<Long> batchIds = allCommentIds.subList(processed, end);

            // 3. Redis에서 batch MGET으로 count 조회
            Map<Long, Long> counts = commentCountService.getReplyCounts(batchIds);

            // 4. DB에 bulk update
            counts.forEach(commentRepository::updateCount);

            processed = end;
        }

    }
}

package org.example.sharedprompts.domain.admin.maintenance.rebuild.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.like.service.LikeCountService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * 배치 처리 유틸리티
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RebuildBatchProcessor {
    
    private final LikeCountService likeCountService;
    
    /**
     * 프롬프트 배치 처리
     */
    public void processPromptBatch(List<Map.Entry<Long, Long>> batch, AtomicLong totalProcessed) {
        processBatch(batch, totalProcessed, 
                (id, count) -> likeCountService.setPromptLikeCount(id, count),
                "promptId");
    }
    
    /**
     * 댓글 배치 처리
     */
    public void processCommentBatch(List<Map.Entry<Long, Long>> batch, AtomicLong totalProcessed) {
        processBatch(batch, totalProcessed,
                (id, count) -> likeCountService.setCommentLikeCount(id, count),
                "commentId");
    }
    
    /**
     * 공통 배치 처리 로직
     */
    private void processBatch(List<Map.Entry<Long, Long>> batch, 
                             AtomicLong totalProcessed,
                             BiConsumer<Long, Long> processor,
                             String idLabel) {
        for (Map.Entry<Long, Long> entry : batch) {
            try {
                processor.accept(entry.getKey(), entry.getValue());
                totalProcessed.incrementAndGet();
            } catch (Exception e) {
                log.error("배치 처리 중 오류 - {}: {}", idLabel, entry.getKey(), e);
                throw new RuntimeException(String.format("배치 처리 실패 - %s: %d", idLabel, entry.getKey()), e);
            }
        }
    }
}


package org.example.sharedprompts.domain.admin.maintenance.rebuild.global;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.admin.maintenance.config.AdminMaintenanceProperties;
import org.example.sharedprompts.domain.admin.maintenance.rebuild.util.RebuildBatchProcessor;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.comment.repository.CommentRepository;
import org.example.sharedprompts.domain.like.service.LikeCountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 댓글 Like count 재빌드 실행기
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentRebuildExecutor {
    
    private final CommentRepository commentRepository;
    private final LikeCountService likeCountService;
    private final RebuildBatchProcessor batchProcessor;
    private final AdminMaintenanceProperties properties;
    
    /**
     * 댓글 Like count 재빌드 실행
     */
    public long execute() {
        AtomicLong totalProcessed = new AtomicLong(0);
        int pageSize = properties.getRebuild().getPageSize();
        boolean batchEnabled = properties.getRebuild().getBatch().isEnabled();
        int batchSize = properties.getRebuild().getBatch().getSize();
        
        log.info("댓글 Like count 재빌드 시작 (배치 처리: {})", batchEnabled);
        
        if (batchEnabled) {
            executeWithBatch(pageSize, batchSize, totalProcessed);
        } else {
            executeWithoutBatch(pageSize, totalProcessed);
        }
        
        long result = totalProcessed.get();
        log.info("댓글 Like count 재빌드 완료 - 총 처리 건수: {}", result);
        return result;
    }
    
    /**
     * 배치 처리 없이 재빌드
     */
    private void executeWithoutBatch(int pageSize, AtomicLong totalProcessed) {
        int page = 0;
        Page<Comment> commentPage;
        
        do {
            commentPage = commentRepository.findAll(PageRequest.of(page, pageSize));
            int totalPages = commentPage.getTotalPages();
            
            log.info("댓글 Like count 재빌드 진행 중 - 페이지: {}/{}", page + 1, totalPages);
            
            int processedCount = 0;
            for (Comment comment : commentPage) {
                try {
                    long likeCount = comment.getLikeCount();
                    if (likeCount > 0) {
                        likeCountService.setCommentLikeCount(comment.getId(), likeCount);
                        processedCount++;
                        totalProcessed.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("댓글 Like count 재빌드 실패 - commentId: {}, 페이지: {}/{}", 
                            comment.getId(), page + 1, totalPages, e);
                    throw new RuntimeException(
                            String.format("댓글 Like count 재빌드 실패 - commentId: %d, 페이지: %d/%d", 
                                    comment.getId(), page + 1, totalPages), e);
                }
            }
            
            log.debug("댓글 Like count 재빌드 완료 - 페이지: {}/{}, 처리 건수: {}", 
                    page + 1, totalPages, processedCount);
            
            page++;
        } while (commentPage.hasNext());
    }
    
    /**
     * 배치 처리로 재빌드
     */
    private void executeWithBatch(int pageSize, int batchSize, AtomicLong totalProcessed) {
        int page = 0;
        Page<Comment> commentPage;
        List<Map.Entry<Long, Long>> batch = new ArrayList<>();
        
        do {
            commentPage = commentRepository.findAll(PageRequest.of(page, pageSize));
            int totalPages = commentPage.getTotalPages();
            
            log.info("댓글 Like count 재빌드 진행 중 - 페이지: {}/{}", page + 1, totalPages);
            
            for (Comment comment : commentPage) {
                long likeCount = comment.getLikeCount();
                if (likeCount > 0) {
                    batch.add(Map.entry(comment.getId(), likeCount));
                    
                    if (batch.size() >= batchSize) {
                        batchProcessor.processCommentBatch(batch, totalProcessed);
                        batch.clear();
                    }
                }
            }
            
            page++;
        } while (commentPage.hasNext());
        
        // 남은 배치 처리
        if (!batch.isEmpty()) {
            batchProcessor.processCommentBatch(batch, totalProcessed);
        }
    }
}


package org.example.sharedprompts.domain.admin.service;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.admin.enums.MaintenanceJobStatus;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.comment.repository.CommentRepository;
import org.example.sharedprompts.domain.like.service.LikeCountService;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.dto.admin.response.RebuildLikeCountsStatusResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMaintenanceServiceImpl implements AdminMaintenanceService {

    private final PromptRepository promptRepository;
    private final CommentRepository commentRepository;
    private final LikeCountService likeCountService;

    // ===== 좋아요 카운트 재빌드 작업 상태 관리 =====
    private volatile MaintenanceJobStatus rebuildStatus = MaintenanceJobStatus.IDLE;
    private volatile LocalDateTime rebuildStartedAt;
    private volatile LocalDateTime rebuildFinishedAt;
    private volatile String rebuildErrorMessage;

    @Override
    @Transactional(readOnly = true)
    public void rebuildLikeCountsFromDb() {
        // 프롬프트 like_count 기준으로 Redis 재설정 (페이징 처리로 메모리 사용량 제한)
        int page = 0;
        int size = 1_000;
        Page<Prompt> promptPage;
        do {
            promptPage = promptRepository.findAll(PageRequest.of(page, size));
            promptPage.forEach(prompt -> {
                long likeCount = prompt.getLikeCount();
                if (likeCount > 0) {
                    likeCountService.setPromptLikeCount(prompt.getId(), likeCount);
                }
            });
            page++;
        } while (promptPage.hasNext());

        // 댓글 like_count 기준으로 Redis 재설정 (페이징 처리로 메모리 사용량 제한)
        page = 0;
        Page<Comment> commentPage;
        do {
            commentPage = commentRepository.findAll(PageRequest.of(page, size));
            commentPage.forEach(comment -> {
                long likeCount = comment.getLikeCount();
                if (likeCount > 0) {
                    likeCountService.setCommentLikeCount(comment.getId(), likeCount);
                }
            });
            page++;
        } while (commentPage.hasNext());
    }

    @Override
    @Async
    public void rebuildLikeCountsFromDbAsync() {
        // 이미 실행 중이면 중복 실행 방지 (스레드 안전하게 check-then-act 보장)
        synchronized (this) {
            if (rebuildStatus == MaintenanceJobStatus.RUNNING) {
                log.warn("좋아요 카운트 재빌드 작업이 이미 실행 중입니다.");
                return;
            }

            rebuildStatus = MaintenanceJobStatus.RUNNING;
            rebuildStartedAt = LocalDateTime.now();
            rebuildFinishedAt = null;
            rebuildErrorMessage = null;
        }

        try {
            rebuildLikeCountsFromDb();
            rebuildStatus = MaintenanceJobStatus.COMPLETED;
        } catch (Exception e) {
            log.error("좋아요 카운트 재빌드 작업 중 오류 발생", e);
            rebuildStatus = MaintenanceJobStatus.FAILED;
            rebuildErrorMessage = e.getMessage();
        } finally {
            rebuildFinishedAt = LocalDateTime.now();
        }
    }

    @Override
    public RebuildLikeCountsStatusResponseDto getRebuildLikeCountsStatus() {
        return RebuildLikeCountsStatusResponseDto.builder()
                .status(rebuildStatus)
                .startedAt(rebuildStartedAt)
                .finishedAt(rebuildFinishedAt)
                .errorMessage(rebuildErrorMessage)
                .build();
    }
}




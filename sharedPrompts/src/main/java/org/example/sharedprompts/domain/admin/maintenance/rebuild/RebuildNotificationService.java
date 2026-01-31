package org.example.sharedprompts.domain.admin.maintenance.rebuild;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 재빌드 작업 알림 서비스
 * 작업 시작/완료/실패 시 알림을 발송합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RebuildNotificationService {
    
    public void notifyStart() {
        log.info("🔔 [알림] 좋아요 카운트 재빌드 작업이 시작되었습니다.");
    }
    
    public void notifyCompleted(Duration duration, long processedPrompts, long processedComments) {
        log.info("✅ [알림] 좋아요 카운트 재빌드 작업이 완료되었습니다. " +
                "소요 시간: {}초, 처리된 프롬프트: {}, 처리된 댓글: {}", 
                duration.getSeconds(), processedPrompts, processedComments);
    }
    
    public void notifyFailed(String errorMessage, Duration duration) {
        log.error("❌ [알림] 좋아요 카운트 재빌드 작업이 실패했습니다. " +
                "오류: {}, 소요 시간: {}초", errorMessage, duration.getSeconds());
    }
    
    public void notifyShedLockFailure() {
        log.warn("⚠️ [알림] ShedLock 획득 실패 - 다른 인스턴스에서 작업이 실행 중입니다.");
    }
}


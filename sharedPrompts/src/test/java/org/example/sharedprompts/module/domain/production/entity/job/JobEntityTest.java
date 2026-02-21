package org.example.sharedprompts.module.domain.production.entity.job;

import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JobEntity 상태 전이 테스트")
class JobEntityTest {

    private JobEntity pendingJob() {
        return JobEntity.builder()
                .jobId("job-uuid-1234")
                .idempotencyKey("idem-key-1")
                .promptId(1L)
                .userId(10L)
                .tenantId("tenant-1")
                .commandType("TEXT")
                .commandJson("{}")
                .build();
    }

    // ===== start() =====

    @Test
    @DisplayName("PENDING 상태에서 start() 호출 시 PROCESSING으로 전이된다")
    void start_from_pending_transitions_to_processing() {
        JobEntity job = pendingJob();

        job.start();

        assertThat(job.getStatus()).isEqualTo(JobStatus.PROCESSING);
        assertThat(job.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("PROCESSING 상태에서 start() 호출 시 예외가 발생한다")
    void start_from_processing_throws_exception() {
        JobEntity job = pendingJob();
        job.start();

        assertThatThrownBy(job::start)
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("PENDING");
    }

    // ===== complete() =====

    @Test
    @DisplayName("PROCESSING 상태에서 complete() 호출 시 SUCCEEDED로 전이된다")
    void complete_from_processing_transitions_to_succeeded() {
        JobEntity job = pendingJob();
        job.start();

        job.complete("123");

        assertThat(job.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(job.getArtifactId()).isEqualTo("123");
        assertThat(job.getProductionId()).isEqualTo(123L);
        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("PENDING 상태에서 complete() 호출 시 예외가 발생한다")
    void complete_from_pending_throws_exception() {
        JobEntity job = pendingJob();

        assertThatThrownBy(() -> job.complete("1"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("PROCESSING");
    }

    // ===== fail() =====

    @Test
    @DisplayName("PROCESSING 상태에서 fail() 호출 시 FAILED로 전이된다")
    void fail_from_processing_transitions_to_failed() {
        JobEntity job = pendingJob();
        job.start();

        job.fail("AI 서비스 오류");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("AI 서비스 오류");
        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.isFailed()).isTrue();
    }

    @Test
    @DisplayName("PENDING 상태에서 fail() 호출 시 예외가 발생한다")
    void fail_from_pending_throws_exception() {
        JobEntity job = pendingJob();

        assertThatThrownBy(() -> job.fail("error"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("PROCESSING");
    }

    // ===== retry() =====

    @Test
    @DisplayName("FAILED 상태에서 retry() 호출 시 PENDING으로 전이되고 retryCount가 증가한다")
    void retry_from_failed_transitions_to_pending() {
        JobEntity job = pendingJob();
        job.start();
        job.fail("error");

        job.retry();

        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getRetryCount()).isEqualTo(1);
        assertThat(job.getErrorMessage()).isNull();
        assertThat(job.getArtifactId()).isNull();
        assertThat(job.getStartedAt()).isNull();
        assertThat(job.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("FAILED 상태에서 retry()를 두 번 호출하면 retryCount가 2가 된다")
    void retry_twice_increments_retry_count_to_2() {
        JobEntity job = pendingJob();
        job.start();
        job.fail("1차 실패");
        job.retry();

        // 두 번째 실패 후 재시도
        job.start();
        job.fail("2차 실패");
        job.retry();

        assertThat(job.getRetryCount()).isEqualTo(2);
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    @DisplayName("PENDING 상태에서 retry() 호출 시 예외가 발생한다")
    void retry_from_pending_throws_exception() {
        JobEntity job = pendingJob();

        assertThatThrownBy(job::retry)
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("FAILED");
    }

    // ===== markAsRetrying() =====

    @Test
    @DisplayName("FAILED 상태에서 markAsRetrying() 호출 시 RETRYING으로 전이되고 retryCount가 증가한다")
    void markAsRetrying_from_failed_transitions_to_retrying() {
        JobEntity job = pendingJob();
        job.start();
        job.fail("error");

        job.markAsRetrying();

        assertThat(job.getStatus()).isEqualTo(JobStatus.RETRYING);
        assertThat(job.getRetryCount()).isEqualTo(1);
        assertThat(job.isRetrying()).isTrue();
    }

    @Test
    @DisplayName("PENDING 상태에서 markAsRetrying() 호출 시 예외가 발생한다")
    void markAsRetrying_from_pending_throws_exception() {
        JobEntity job = pendingJob();

        assertThatThrownBy(job::markAsRetrying)
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("FAILED");
    }

    // ===== startFromRetrying() =====

    @Test
    @DisplayName("RETRYING 상태에서 startFromRetrying() 호출 시 PROCESSING으로 전이된다")
    void startFromRetrying_from_retrying_transitions_to_processing() {
        JobEntity job = pendingJob();
        job.start();
        job.fail("error");
        job.markAsRetrying();

        job.startFromRetrying();

        assertThat(job.getStatus()).isEqualTo(JobStatus.PROCESSING);
        assertThat(job.getErrorMessage()).isNull();
        assertThat(job.getArtifactId()).isNull();
        assertThat(job.getStartedAt()).isNotNull();
        // completedAt은 fail() 시 설정되며 startFromRetrying()은 초기화하지 않음
        assertThat(job.isProcessing()).isTrue();
    }

    @Test
    @DisplayName("PENDING 상태에서 startFromRetrying() 호출 시 예외가 발생한다")
    void startFromRetrying_from_pending_throws_exception() {
        JobEntity job = pendingJob();

        assertThatThrownBy(job::startFromRetrying)
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("RETRYING");
    }

    // ===== markAsUnknown() =====

    @Test
    @DisplayName("PROCESSING 상태에서 markAsUnknown() 호출 시 UNKNOWN으로 전이된다")
    void markAsUnknown_from_processing_transitions_to_unknown() {
        JobEntity job = pendingJob();
        job.start();

        job.markAsUnknown("S3 timeout");

        assertThat(job.getStatus()).isEqualTo(JobStatus.UNKNOWN);
        assertThat(job.getErrorMessage()).isEqualTo("S3 timeout");
        assertThat(job.isUnknown()).isTrue();
    }

    @Test
    @DisplayName("PENDING 상태에서 markAsUnknown() 호출 시 예외가 발생한다")
    void markAsUnknown_from_pending_throws_exception() {
        JobEntity job = pendingJob();

        assertThatThrownBy(() -> job.markAsUnknown("timeout"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("PROCESSING");
    }

    // ===== recoverAsSucceeded() =====

    @Test
    @DisplayName("UNKNOWN 상태에서 recoverAsSucceeded() 호출 시 SUCCEEDED로 전이된다")
    void recoverAsSucceeded_from_unknown_transitions_to_succeeded() {
        JobEntity job = pendingJob();
        job.start();
        job.markAsUnknown("timeout");

        job.recoverAsSucceeded("artifact-recovery");

        assertThat(job.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(job.getArtifactId()).isEqualTo("artifact-recovery");
        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("PENDING 상태에서 recoverAsSucceeded() 호출 시 예외가 발생한다")
    void recoverAsSucceeded_from_pending_throws_exception() {
        JobEntity job = pendingJob();

        assertThatThrownBy(() -> job.recoverAsSucceeded("artifact"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("UNKNOWN");
    }

    // ===== recoverAsFailed() =====

    @Test
    @DisplayName("UNKNOWN 상태에서 recoverAsFailed() 호출 시 FAILED로 전이된다")
    void recoverAsFailed_from_unknown_transitions_to_failed() {
        JobEntity job = pendingJob();
        job.start();
        job.markAsUnknown("timeout");

        job.recoverAsFailed("최대 재조회 초과");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("최대 재조회 초과");
        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.getArtifactId()).isNull();
    }

    @Test
    @DisplayName("PROCESSING 상태에서 recoverAsFailed() 호출 시 예외가 발생한다")
    void recoverAsFailed_from_processing_throws_exception() {
        JobEntity job = pendingJob();
        job.start();

        assertThatThrownBy(() -> job.recoverAsFailed("error"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("UNKNOWN");
    }

    // ===== isFinalState() =====

    @Test
    @DisplayName("SUCCEEDED 상태는 최종 상태(isFinalState=true)이다")
    void isFinalState_returns_true_for_succeeded() {
        JobEntity job = pendingJob();
        job.start();
        job.complete("artifact-1");
        assertThat(job.isFinalState()).isTrue();
    }

    @Test
    @DisplayName("FAILED 상태는 최종 상태(isFinalState=true)이다")
    void isFinalState_returns_true_for_failed() {
        JobEntity job = pendingJob();
        job.start();
        job.fail("error");
        assertThat(job.isFinalState()).isTrue();
    }

    @Test
    @DisplayName("UNKNOWN 상태는 최종 상태(isFinalState=true)이다")
    void isFinalState_returns_true_for_unknown() {
        JobEntity job = pendingJob();
        job.start();
        job.markAsUnknown("timeout");
        assertThat(job.isFinalState()).isTrue();
    }

    @Test
    @DisplayName("PENDING 상태는 최종 상태가 아니다")
    void isFinalState_returns_false_for_pending() {
        assertThat(pendingJob().isFinalState()).isFalse();
    }

    @Test
    @DisplayName("PROCESSING 상태는 최종 상태가 아니다")
    void isFinalState_returns_false_for_processing() {
        JobEntity job = pendingJob();
        job.start();
        assertThat(job.isFinalState()).isFalse();
    }

    @Test
    @DisplayName("RETRYING 상태는 최종 상태가 아니다")
    void isFinalState_returns_false_for_retrying() {
        JobEntity job = pendingJob();
        job.start();
        job.fail("error");
        job.markAsRetrying();
        assertThat(job.isFinalState()).isFalse();
    }

    // ===== resetForIdempotencyRetry() =====

    @Test
    @DisplayName("resetForIdempotencyRetry()는 FAILED → PENDING 전이(retry 위임)를 수행한다")
    void resetForIdempotencyRetry_delegates_to_retry() {
        JobEntity job = pendingJob();
        job.start();
        job.fail("idempotency key 중복");

        job.resetForIdempotencyRetry();

        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getRetryCount()).isEqualTo(1);
        assertThat(job.getErrorMessage()).isNull();
        assertThat(job.getArtifactId()).isNull();
        assertThat(job.getStartedAt()).isNull();
        assertThat(job.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("PROCESSING 상태에서 resetForIdempotencyRetry() 호출 시 예외가 발생한다")
    void resetForIdempotencyRetry_from_processing_throws_exception() {
        JobEntity job = pendingJob();
        job.start();

        assertThatThrownBy(job::resetForIdempotencyRetry)
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("FAILED");
    }

    // ===== 전체 정상 시나리오 =====

    @Test
    @DisplayName("정상 흐름: PENDING → PROCESSING → SUCCEEDED")
    void happy_path_pending_to_succeeded() {
        JobEntity job = pendingJob();

        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        job.start();
        assertThat(job.getStatus()).isEqualTo(JobStatus.PROCESSING);
        job.complete("final-artifact");
        assertThat(job.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(job.getArtifactId()).isEqualTo("final-artifact");
    }

    @Test
    @DisplayName("재시도 흐름: PENDING → PROCESSING → FAILED → RETRYING → PROCESSING → SUCCEEDED")
    void retry_flow_via_message_level_retry() {
        JobEntity job = pendingJob();

        job.start();                   // PENDING → PROCESSING
        job.fail("1차 실패");           // PROCESSING → FAILED
        job.markAsRetrying();          // FAILED → RETRYING (retryCount: 1)
        job.startFromRetrying();       // RETRYING → PROCESSING
        job.complete("artifact");      // PROCESSING → SUCCEEDED

        assertThat(job.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(job.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("UNKNOWN 복구 흐름: PROCESSING → UNKNOWN → SUCCEEDED")
    void unknown_recovery_flow() {
        JobEntity job = pendingJob();

        job.start();
        job.markAsUnknown("AI timeout");
        job.recoverAsSucceeded("recovered-artifact");

        assertThat(job.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(job.getArtifactId()).isEqualTo("recovered-artifact");
        assertThat(job.getErrorMessage()).isNull();
    }
}

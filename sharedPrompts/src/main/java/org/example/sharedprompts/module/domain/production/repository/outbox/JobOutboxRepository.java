package org.example.sharedprompts.module.domain.production.repository.outbox;

import org.example.sharedprompts.module.domain.production.entity.outbox.JobOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobOutboxRepository extends JpaRepository<JobOutboxEntity, Long> {

    /**
     * Multi-instance safe: FOR UPDATE SKIP LOCKED so other instances do not pick the same row.
     * Returns at most one row per call; call in a loop for batch processing.
     */
    @Query(value = "SELECT * FROM production_job_outbox WHERE status IN (:statuses) ORDER BY created_at ASC LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<JobOutboxEntity> findTop1ByStatusInOrderByCreatedAtAscForUpdate(@Param("statuses") List<String> statuses);
}

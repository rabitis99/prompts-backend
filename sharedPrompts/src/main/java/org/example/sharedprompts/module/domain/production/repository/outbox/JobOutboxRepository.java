package org.example.sharedprompts.module.domain.production.repository.outbox;

import org.example.sharedprompts.module.domain.production.entity.outbox.JobOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobOutboxRepository extends JpaRepository<JobOutboxEntity, Long> {

    List<JobOutboxEntity> findTop100ByStatusOrderByCreatedAtAsc(String status);
}

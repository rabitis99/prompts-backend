package org.example.sharedprompts.module.domain.production.repository.job;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobQueryRepository extends JpaRepository<JobEntity, Long> {

    @Query("SELECT j FROM JobEntity j WHERE j.jobId = :jobId")
    Optional<JobEntity> findByJobIdWithRelations(@Param("jobId") String jobId);

    @Query("SELECT j FROM JobEntity j WHERE j.userId = :userId ORDER BY j.createdAt DESC")
    List<JobEntity> findByUserIdWithRelations(@Param("userId") Long userId);
}


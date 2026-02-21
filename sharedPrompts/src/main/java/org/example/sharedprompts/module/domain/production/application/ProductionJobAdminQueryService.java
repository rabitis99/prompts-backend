package org.example.sharedprompts.module.domain.production.application;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자용 Production Job 조회 (실패/UNKNOWN Job 모니터링)
 */
@Service
@RequiredArgsConstructor
public class ProductionJobAdminQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public Page<JobEntity> findFailedJobs(int page, int size) {
        return jobRepository.findByStatusOrderByCreatedAtDesc(
                JobStatus.FAILED,
                createPageable(page, size)
        );
    }

    @Transactional(readOnly = true)
    public Page<JobEntity> findUnknownJobs(int page, int size) {
        return jobRepository.findByStatusOrderByCreatedAtDesc(
                JobStatus.UNKNOWN,
                createPageable(page, size)
        );
    }

    private static Pageable createPageable(int page, int size) {
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        int safePage = Math.max(0, page);
        return PageRequest.of(safePage, safeSize);
    }
}

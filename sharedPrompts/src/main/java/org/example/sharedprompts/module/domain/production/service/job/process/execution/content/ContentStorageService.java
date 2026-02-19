package org.example.sharedprompts.module.domain.production.service.job.process.execution.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.storage.StorageFacade;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.StorageException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentStorageService {

    private final StorageFacade storageFacade;

    public String store(byte[] data, String contentType, JobEntity job, String fileName) {
        log.info("Storing content - jobId: {}, fileName: {}, contentType: {}",
                job.getJobId(), fileName, contentType);

        try {
            return storageFacade.upload(data, contentType, job.getUserId(), job.getJobId(), fileName);
        } catch (Exception e) {
            log.error("Failed to store content - jobId: {}, fileName: {}", job.getJobId(), fileName, e);
            throw new StorageException("Failed to store content: " + e.getMessage(), e);
        }
    }
}


package org.example.sharedprompts.module.domain.production.service.job.process.recovery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessJobRecoveryService {

    private final AiCalledRecoveryService aiCalledRecoveryService;
    private final ParsedRecoveryService parsedRecoveryService;
    private final RenderedRecoveryService renderedRecoveryService;
    private final StoredRecoveryService storedRecoveryService;

    public void recoverFromAiCalled(String jobId) {
        aiCalledRecoveryService.recover(jobId);
    }

    public void recoverFromParsed(String jobId) {
        parsedRecoveryService.recover(jobId);
    }

    public void recoverFromRendered(String jobId) {
        renderedRecoveryService.recover(jobId);
    }

    public void recoverFromStored(String jobId) {
        storedRecoveryService.recover(jobId);
    }
}

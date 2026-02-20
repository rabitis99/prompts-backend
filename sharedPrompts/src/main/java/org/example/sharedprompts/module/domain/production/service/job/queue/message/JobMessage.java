package org.example.sharedprompts.module.domain.production.service.job.queue.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Builder
@ToString
public class JobMessage {

    @JsonProperty("jobId")
    private final String jobId;

    @JsonProperty("createdAt")
    private final Instant createdAt;

    @JsonProperty("retryCount")
    private final Integer retryCount;

    @JsonProperty("maxRetryCount")
    private final Integer maxRetryCount;

    @JsonCreator
    public JobMessage(
            @JsonProperty("jobId") String jobId,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("retryCount") Integer retryCount,
            @JsonProperty("maxRetryCount") Integer maxRetryCount
    ) {
        this.jobId = jobId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.retryCount = retryCount != null ? retryCount : 0;
        this.maxRetryCount = maxRetryCount != null ? maxRetryCount : 3;
    }

    public JobMessage withIncrementedRetry() {
        return JobMessage.builder()
                .jobId(this.jobId)
                .createdAt(this.createdAt)
                .retryCount(this.retryCount + 1)
                .maxRetryCount(this.maxRetryCount)
                .build();
    }

    public boolean isMaxRetryExceeded() {
        return retryCount >= maxRetryCount;
    }
}


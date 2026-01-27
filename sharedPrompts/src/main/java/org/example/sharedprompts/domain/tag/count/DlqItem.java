package org.example.sharedprompts.domain.tag.count;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DlqItem {
    private Set<String> tagsToDecrease;
    private Set<String> tagsToIncrease;
    private String error;
    private LocalDateTime occurredAt;
    private int retryCount = 0;
    private Long scheduledTime;

    public DlqItem(Set<String> tagsToDecrease, Set<String> tagsToIncrease,
                   String error, LocalDateTime occurredAt, int retryCount, Long scheduledTime) {
        this.tagsToDecrease = tagsToDecrease;
        this.tagsToIncrease = tagsToIncrease;
        this.error = error;
        this.occurredAt = occurredAt;
        this.retryCount = retryCount;
        this.scheduledTime = scheduledTime;

    }
}

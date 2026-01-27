package org.example.sharedprompts.domain.tag.count;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
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
    private Long scheduledTimeMillis;

    public DlqItem(Set<String> tagsToDecrease, Set<String> tagsToIncrease,
                   String error, LocalDateTime occurredAt, int retryCount, Long scheduledTimeMillis) {
        this.tagsToDecrease = tagsToDecrease == null ? null
                : Set.copyOf(tagsToDecrease);
        this.tagsToIncrease = tagsToIncrease == null ? null
                : Set.copyOf(tagsToIncrease);
        this.error = error;
        this.occurredAt = occurredAt;
        this.retryCount = retryCount;
        this.scheduledTimeMillis = scheduledTimeMillis;

    }
}

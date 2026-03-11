package org.example.sharedprompts.domain.prompt.application.port.out.usage;

import java.util.List;
import java.util.Map;

/** 프롬프트 사용(조회) 수 아웃바운드 포트 */
public interface PromptUsageCountPort {

    void incrementUsageCount(Long promptId);

    Map<Long, Long> getAndResetUsageCounts(List<Long> promptIds);

    void restoreUsageCounts(Map<Long, Long> usageCounts);
}

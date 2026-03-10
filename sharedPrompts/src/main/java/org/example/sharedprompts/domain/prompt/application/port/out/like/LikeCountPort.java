package org.example.sharedprompts.domain.prompt.application.port.out.like;

import java.util.List;
import java.util.Map;

/** 프롬프트 좋아요 수 조회 아웃바운드 포트 */
public interface LikeCountPort {

    Map<Long, Long> getPromptLikeCounts(List<Long> promptIds);
}

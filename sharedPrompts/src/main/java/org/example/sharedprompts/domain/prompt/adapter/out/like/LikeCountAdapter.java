package org.example.sharedprompts.domain.prompt.adapter.out.like;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.like.service.LikeCountService;
import org.example.sharedprompts.domain.prompt.application.port.out.like.LikeCountPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// Dependency-direction adapter (no semantic translation)
/** Adapter: bridges to like-domain service. Forwards calls without translation (types align). */
@Component
@RequiredArgsConstructor
public class LikeCountAdapter implements LikeCountPort {

    private final LikeCountService likeCountService;

    @Override
    public Map<Long, Long> getPromptLikeCounts(List<Long> promptIds) {
        return likeCountService.getPromptLikeCounts(promptIds);
    }
}

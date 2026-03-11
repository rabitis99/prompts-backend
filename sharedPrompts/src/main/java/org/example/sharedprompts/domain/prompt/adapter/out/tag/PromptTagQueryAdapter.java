package org.example.sharedprompts.domain.prompt.adapter.out.tag;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.port.out.tag.PromptTagQueryPort;
import org.example.sharedprompts.domain.tag.service.PromptTagService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// Dependency-direction adapter (no semantic translation)
/** Adapter: delegates tag reads to tag-domain service. */
@Component
@RequiredArgsConstructor
public class PromptTagQueryAdapter implements PromptTagQueryPort {

    private final PromptTagService promptTagService;

    @Override
    public List<String> getTagNames(Long promptId) {
        return promptTagService.getTagNamesByPromptIds(Collections.singletonList(promptId))
                .getOrDefault(promptId, List.of());
    }

    @Override
    public Map<Long, List<String>> getTagNamesByPromptIds(List<Long> promptIds) {
        return promptTagService.getTagNamesByPromptIds(promptIds);
    }
}

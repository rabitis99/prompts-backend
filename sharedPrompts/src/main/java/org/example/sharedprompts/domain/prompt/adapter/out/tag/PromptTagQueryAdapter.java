package org.example.sharedprompts.domain.prompt.adapter.out.tag;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.port.out.tag.PromptTagQueryPort;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.tag.service.PromptTagService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Dependency-direction adapter (no semantic translation)
/** Adapter: delegates tag reads to tag-domain service. */
@Component
@RequiredArgsConstructor
public class PromptTagQueryAdapter implements PromptTagQueryPort {

    private final PromptTagService promptTagService;

    @Override
    public List<String> getTagNames(Prompt prompt) {
        return promptTagService.getTags(prompt).stream()
                .map(tag -> tag.getName())
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<String>> getTagNamesByPromptIds(List<Long> promptIds) {
        return promptTagService.getTagNamesByPromptIds(promptIds);
    }
}

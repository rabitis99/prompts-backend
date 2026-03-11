package org.example.sharedprompts.domain.prompt.adapter.out.tag;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.port.out.tag.PromptTagCommandPort;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.tag.service.PromptTagService;
import org.springframework.stereotype.Component;

import java.util.List;

// Dependency-direction adapter (no semantic translation)
/** Adapter: bridges to tag-domain service. Forwards updateTags; port uses prompt-domain Prompt entity. */
@Component
@RequiredArgsConstructor
public class PromptTagCommandAdapter implements PromptTagCommandPort {

    private final PromptTagService promptTagService;

    @Override
    public void updateTags(Prompt prompt, List<String> tagNames) {
        promptTagService.updateTags(prompt, tagNames);
    }
}

package org.example.sharedprompts.domain.tag.service;

import org.example.sharedprompts.domain.tag.Tag;
import org.example.sharedprompts.domain.prompt.entity.Prompt;

import java.util.List;
import java.util.Map;

public interface PromptTagService {

    List<Tag> addTags(Prompt prompt, List<String> tagNames);

    void updateTags(Prompt prompt, List<String> tagNames);

    List<Tag> getTags(Prompt prompt);

    /** Batch: returns tag names per prompt id. Used to avoid N+1 in list views. */
    Map<Long, List<String>> getTagNamesByPromptIds(List<Long> promptIds);
}

package org.example.sharedprompts.domain.Tag.service;

import org.example.sharedprompts.domain.Tag.Tag;
import org.example.sharedprompts.domain.prompt.Prompt;

import java.util.List;

public interface PromptTagService {

    List<Tag> addTags(Prompt prompt, List<String> tagNames);

    void updateTags(Prompt prompt, List<String> tagNames);

    List<Tag> getTags(Prompt prompt);
}

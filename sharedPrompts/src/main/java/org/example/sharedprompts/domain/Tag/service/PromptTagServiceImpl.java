package org.example.sharedprompts.domain.Tag.service;

import lombok.RequiredArgsConstructor;

import org.example.sharedprompts.domain.Tag.PromptTag;
import org.example.sharedprompts.domain.Tag.Tag;
import org.example.sharedprompts.domain.Tag.repository.PromptTagRepository;
import org.example.sharedprompts.domain.Tag.repository.TagRepository;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PromptTagServiceImpl implements PromptTagService {

    private final PromptTagRepository promptTagRepository;
    private final TagRepository tagRepository;

    @Override
    public List<Tag> addTags(Prompt prompt, List<String> tagNames) {
        List<Tag> tags = tagNames.stream()
                .map(name -> tagRepository.save(new Tag(name)))
                .collect(Collectors.toList());

        for (Tag tag : tags) {
            if (!promptTagRepository.existsByPromptAndTag(prompt, tag)) {
                promptTagRepository.save(new PromptTag(prompt, tag));
            }
        }
        return tags;
    }

    @Override
    public void updateTags(Prompt prompt, List<String> tagNames) {
        promptTagRepository.deletePromptTagByPrompt(prompt);
        addTags(prompt, tagNames);
    }

    @Override
    public List<Tag> getTags(Prompt prompt) {
        return promptTagRepository.findPromptTagByPrompt(prompt)
                .stream()
                .map(PromptTag::getTag)
                .collect(Collectors.toList());
    }
}

package org.example.sharedprompts.domain.tag.service;

import lombok.RequiredArgsConstructor;

import org.example.sharedprompts.domain.tag.PromptTag;
import org.example.sharedprompts.domain.tag.Tag;
import org.example.sharedprompts.domain.tag.repository.PromptTagRepository;
import org.example.sharedprompts.domain.tag.repository.TagRepository;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.TagNormalizer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PromptTagServiceImpl implements PromptTagService {

    private final PromptTagRepository promptTagRepository;
    private final TagRepository tagRepository;

    @Override
    public List<Tag> addTags(Prompt prompt, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return List.of();

        List<String> processedNames = TagNormalizer.normalizeTags(tagNames);

        List<Tag> tags = new ArrayList<>();

        for (String name : processedNames) {
            Tag tag = getOrCreateTag(name);
            increaseTagCount(name);
            attachPromptTag(prompt, tag);
            tags.add(tag);
        }

        return tags;
    }

    private Tag getOrCreateTag(String name) {
        return tagRepository.findByName(name).orElseGet(() -> {
            try {
                return tagRepository.save(new Tag(name));
            } catch (DataIntegrityViolationException e) {
                return tagRepository.findByName(name)
                        .orElseThrow(() -> new ApiException(ErrorCode.TAG_ALREADY_EXISTS));
            }
        });
    }

    private void increaseTagCount(String name) {
        tagRepository.incrementCount(name);
    }

    private void attachPromptTag(Prompt prompt, Tag tag) {
        if (!promptTagRepository.existsByPromptAndTag(prompt, tag)) {
            promptTagRepository.save(new PromptTag(prompt, tag));
        }
    }

    @Override
    public void updateTags(Prompt prompt, List<String> tagNames) {
        promptTagRepository.deletePromptTagByPrompt(prompt);
        addTags(prompt, tagNames);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getTags(Prompt prompt) {
        return promptTagRepository.findPromptTagByPrompt(prompt)
                .stream()
                .map(PromptTag::getTag)
                .toList();
    }
}


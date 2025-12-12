package org.example.sharedprompts.domain.tag.service;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PromptTagServiceImpl implements PromptTagService {

    private final PromptTagRepository promptTagRepository;
    private final TagRepository tagRepository;

    @Override
    public List<Tag> addTags(Prompt prompt, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return List.of();

        List<String> processedNames = TagNormalizer.normalizeTags(tagNames);
        List<Tag> tags = new ArrayList<>(processedNames.size());

        for (String name : processedNames) {
            Tag tag = getOrCreateTag(name);

            boolean attached = attachPromptTag(prompt, tag);

            if (attached) {
                increaseTagCount(tag.getName());
            }

            tags.add(tag);
        }

        return tags;
    }

    private Tag getOrCreateTag(String name) {
        return tagRepository.findByName(name).orElseGet(() -> {
            try {
                return tagRepository.saveAndFlush(new Tag(name));
            } catch (DataIntegrityViolationException e) {
                return tagRepository.findByName(name)
                        .orElseThrow(() -> new ApiException(ErrorCode.TAG_CREATION_FAILED));
            }
        });
    }

    private boolean attachPromptTag(Prompt prompt, Tag tag) {
        try {
            promptTagRepository.saveAndFlush(new PromptTag(prompt, tag));
            return true;
        } catch (DataIntegrityViolationException e) {
            // 유니크 제약조건 위반인 경우만 무시
            if (e.getCause() != null && e.getCause().getMessage().contains("prompt_tags_unique")) {
                return false;
            }
            // 다른 무결성 위반은 로깅하고 재던지기
            log.error("Unexpected integrity violation when attaching prompt-tag", e);
            throw e;
        }
    }

    private void increaseTagCount(String name) {
        tagRepository.incrementCount(name);
    }

    private void decreaseTagCount(String name) {
        tagRepository.decrementCount(name);
    }

    @Override
    public void updateTags(Prompt prompt, List<String> tagNames) {

        // 1 기존 태그들 count 감소
        List<PromptTag> existing = promptTagRepository.findPromptTagByPrompt(prompt);
        existing.forEach(pt -> decreaseTagCount(pt.getTag().getName()));

        // 2 기존 PromptTag 제거
        promptTagRepository.deletePromptTagByPrompt(prompt);

        // 3 새 태그 적용
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

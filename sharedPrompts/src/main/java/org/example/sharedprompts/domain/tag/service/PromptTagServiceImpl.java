package org.example.sharedprompts.domain.tag.service;

import lombok.RequiredArgsConstructor;

import org.example.sharedprompts.domain.tag.PromptTag;
import org.example.sharedprompts.domain.tag.Tag;
import org.example.sharedprompts.domain.tag.repository.PromptTagRepository;
import org.example.sharedprompts.domain.tag.repository.TagRepository;
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
        if (tagNames == null || tagNames.isEmpty()) return List.of();
        // 1. 공백 제거 + 영어 대문자 + 중복 제거
        List<String> processedNames = tagNames.stream()
                .map(String::trim) // 공백 제거
                .filter(name -> !name.isEmpty()) // 빈 문자열 제거
                .map(name -> name.matches("^[a-zA-Z]+$") ? name.toUpperCase() : name)
                .distinct()
                .toList();

        List<Tag> tags = processedNames.stream()
                .map(name -> {
                    // 기존 태그가 있으면 재사용, 없으면 새로 생성
                    Tag tag = tagRepository.findByName(name)
                            .orElseGet(() -> tagRepository.save(new Tag(name)));
                    // count 증가
                    tag.setCount(tag.getCount() == null ? 1L : tag.getCount() + 1);
                    tagRepository.save(tag); // count 업데이트
                    return tag;
                })
                .toList();
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
    @Transactional(readOnly = true)
    public List<Tag> getTags(Prompt prompt) {
        return promptTagRepository.findPromptTagByPrompt(prompt)
                .stream()
                .map(PromptTag::getTag)
                .collect(Collectors.toList());
    }
}

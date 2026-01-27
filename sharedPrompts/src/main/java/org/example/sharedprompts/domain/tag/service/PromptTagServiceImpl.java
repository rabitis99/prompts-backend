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
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.NestedExceptionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PromptTagServiceImpl implements PromptTagService {

    private final PromptTagRepository promptTagRepository;
    private final TagRepository tagRepository;
    private final TagCountFacade tagCountFacade;

    @Override
    public List<Tag> addTags(Prompt prompt, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return List.of();

        List<String> processedNames = TagNormalizer.normalizeTags(tagNames);
        List<Tag> tags = new ArrayList<>(processedNames.size());

        for (String name : processedNames) {
            Tag tag = getOrCreateTag(name);

            boolean attached = attachPromptTag(prompt, tag);

            if (attached) {
                // 즉시 업데이트 (트랜잭션 내부이므로 안전)
                // 단일 태그는 개별 처리, 배치는 updateTags에서 처리
                tagCountFacade.incrementTagCount(tag.getName());
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

                Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);

                // 1. 제약 조건명이 명확히 일치할 경우
                if (cause instanceof ConstraintViolationException cve) {
                    String constraint = cve.getConstraintName();
                    if ("uk_tag_name".equalsIgnoreCase(constraint)) {
                        return tagRepository.findByName(name)
                                .orElseThrow(() -> new ApiException(ErrorCode.TAG_CREATION_FAILED));
                    }
                }

                // 2. 제약 조건명 미확인 (DB / 환경 차이 대응용 fallback)
                log.warn("Integrity violation without matching constraint name. fallback findByName. tag={}", name);
                return tagRepository.findByName(name)
                        .orElseThrow(() -> {
                            log.error("Tag creation failed after integrity violation. tag={}", name, e);
                            return new ApiException(ErrorCode.TAG_CREATION_FAILED);
                        });
            }
        });
    }

    private boolean attachPromptTag(Prompt prompt, Tag tag) {
        try {
            promptTagRepository.saveAndFlush(new PromptTag(prompt, tag));
            return true;
        } catch (DataIntegrityViolationException e) {
            // 유니크 제약조건(uk_prompt_tag) 위반인 경우만 무시
            Throwable mostSpecific = NestedExceptionUtils.getMostSpecificCause(e);
            if (mostSpecific instanceof ConstraintViolationException cve) {
                String constraintName = cve.getConstraintName();
                if ("uk_prompt_tag".equalsIgnoreCase(constraintName)) {
                    return false;
                }
            }
            // 다른 무결성 위반은 로깅하고 재던지기
            log.error("Unexpected integrity violation when attaching prompt-tag", e);
            throw e;
        }
    }

    @Override
    public void updateTags(Prompt prompt, List<String> tagNames) {
        // 1. 기존 태그와 새 태그 비교
        List<PromptTag> existing = promptTagRepository.findPromptTagByPrompt(prompt);
        Set<String> existingTagNames = existing.stream()
                .map(pt -> pt.getTag().getName())
                .collect(Collectors.toSet());

        List<String> processedNames = TagNormalizer.normalizeTags(tagNames);
        Set<String> newTagNames = new HashSet<>(processedNames);

        // 2. 제거할 태그와 추가할 태그 계산
        Set<String> toRemove = new HashSet<>(existingTagNames);
        toRemove.removeAll(newTagNames);

        Set<String> toAdd = new HashSet<>(newTagNames);
        toAdd.removeAll(existingTagNames);

        // 3. DB 변경 먼저 수행 (트랜잭션 내부)
        existing.stream()
                .filter(pt -> toRemove.contains(pt.getTag().getName()))
                .forEach(promptTagRepository::delete);

        List<Tag> tagsToAdd = toAdd.stream()
                .map(this::getOrCreateTag)
                .toList();

        tagsToAdd.forEach(tag -> attachPromptTag(prompt, tag));

        // 4. 트랜잭션 커밋 후 이벤트 발행
        // DB 변경이 성공적으로 커밋된 후에만 이벤트 발행
        // 파사드를 통해 트랜잭션 처리 로직 캡슐화
        Set<String> tagsToIncrease = tagsToAdd.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());
        
        tagCountFacade.publishTagCountUpdate(toRemove, tagsToIncrease);
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

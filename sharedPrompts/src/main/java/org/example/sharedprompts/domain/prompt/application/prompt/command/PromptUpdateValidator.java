package org.example.sharedprompts.domain.prompt.application.prompt.command;

import org.example.sharedprompts.domain.prompt.application.exception.InvalidPromptUpdateException;
import org.example.sharedprompts.global.util.TagNormalizer;
import org.springframework.stereotype.Component;

import java.util.List;

/** 프롬프트 수정 요청 검증·정규화 */
@Component
public class PromptUpdateValidator {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int DESCRIPTION_MAX_LENGTH = 5000;
    private static final int CONTENT_MAX_LENGTH = 100_000;
    private static final int TAG_MIN_LENGTH = 1;
    private static final int TAG_MAX_LENGTH = 50;
    private static final int MAX_TAG_COUNT = 20;

    public UpdatePromptPayload validateAndNormalize(UpdatePromptPayload payload) {
        if (payload == null) {
            throw new InvalidPromptUpdateException("수정 요청이 없습니다.");
        }

        String title = normalizeString(payload.title());
        String description = normalizeString(payload.description());
        String content = normalizeString(payload.content());
        List<String> tags = payload.tags() != null ? TagNormalizer.normalizeTags(payload.tags()) : null;
        Boolean isPublic = payload.isPublic();

        validateTitle(title);
        validateDescription(description);
        validateContent(content);
        validateTags(tags);

        return new UpdatePromptPayload(
                title,
                description,
                isPublic,
                tags,
                content
        );
    }

    private static String normalizeString(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateTitle(String title) {
        if (title == null) return;
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new InvalidPromptUpdateException(
                    "제목은 최대 " + TITLE_MAX_LENGTH + "자까지 입력해주세요.");
        }
    }

    private void validateDescription(String description) {
        if (description == null) return;
        if (description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new InvalidPromptUpdateException(
                    "설명은 최대 " + DESCRIPTION_MAX_LENGTH + "자까지 입력해주세요.");
        }
    }

    private void validateContent(String content) {
        if (content == null) return;
        if (content.length() > CONTENT_MAX_LENGTH) {
            throw new InvalidPromptUpdateException(
                    "본문은 최대 " + CONTENT_MAX_LENGTH + "자까지 입력해주세요.");
        }
    }

    private void validateTags(List<String> tags) {
        if (tags == null) return;
        if (tags.size() > MAX_TAG_COUNT) {
            throw new InvalidPromptUpdateException(
                    "태그는 최대 " + MAX_TAG_COUNT + "개까지 입력해주세요.");
        }
        for (String tag : tags) {
            if (tag.length() < TAG_MIN_LENGTH || tag.length() > TAG_MAX_LENGTH) {
                throw new InvalidPromptUpdateException(
                        "태그는 " + TAG_MIN_LENGTH + "~" + TAG_MAX_LENGTH + "자로 입력해주세요.");
            }
        }
    }
}

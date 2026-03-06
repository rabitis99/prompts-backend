package org.example.sharedprompts.domain.prompt.application.service.orchestration;

import org.example.sharedprompts.domain.prompt.application.exception.InvalidPromptUpdateException;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.global.util.TagNormalizer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 프롬프트 수정 요청에 대한 검증·정규화를 수행합니다.
 * <p>
 * 생성 시점과 동일한 제약(길이, 태그 규칙 등)을 적용하여
 * 수정 API로 스키마/정책/콘텐츠 룰 우회를 방지합니다.
 */
@Component
public class PromptUpdateValidator {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int DESCRIPTION_MAX_LENGTH = 5000;
    private static final int TAG_MIN_LENGTH = 1;
    private static final int TAG_MAX_LENGTH = 50;
    private static final int MAX_TAG_COUNT = 20;

    /**
     * 페이로드를 검증하고 정규화한 새 페이로드를 반환합니다.
     * 제목/설명 trim, 태그 정규화 후 길이·값 제약을 검사합니다.
     *
     * @param payload 수정 요청 페이로드 (불변 유지를 위해 수정하지 않음)
     * @return 검증·정규화된 새 페이로드
     * @throws InvalidPromptUpdateException 제약 위반 시
     */
    public UpdatePromptPayload validateAndNormalize(UpdatePromptPayload payload) {
        if (payload == null) {
            throw new InvalidPromptUpdateException("수정 요청이 없습니다.");
        }

        String title = normalizeString(payload.title());
        String description = normalizeString(payload.description());
        List<String> tags = payload.tags() != null ? TagNormalizer.normalizeTags(payload.tags()) : null;
        Boolean isPublic = payload.isPublic();
        PromptCategory promptCategory = payload.promptCategory();

        validateTitle(title);
        validateDescription(description);
        validateTags(tags);

        return new UpdatePromptPayload(
                title,
                description,
                isPublic,
                promptCategory,
                tags
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

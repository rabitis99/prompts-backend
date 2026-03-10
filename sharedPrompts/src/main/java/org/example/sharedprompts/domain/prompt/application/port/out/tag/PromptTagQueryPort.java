package org.example.sharedprompts.domain.prompt.application.port.out.tag;

import org.example.sharedprompts.domain.prompt.entity.Prompt;

import java.util.List;
import java.util.Map;

/** 프롬프트 태그 조회 아웃바운드 포트 */
public interface PromptTagQueryPort {

    List<String> getTagNames(Prompt prompt);

    Map<Long, List<String>> getTagNamesByPromptIds(List<Long> promptIds);
}

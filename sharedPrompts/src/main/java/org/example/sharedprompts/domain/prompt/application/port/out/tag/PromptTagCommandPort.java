package org.example.sharedprompts.domain.prompt.application.port.out.tag;

import org.example.sharedprompts.domain.prompt.entity.Prompt;

import java.util.List;

/** 프롬프트 태그 갱신 아웃바운드 포트 */
public interface PromptTagCommandPort {

    void updateTags(Prompt prompt, List<String> tagNames);
}

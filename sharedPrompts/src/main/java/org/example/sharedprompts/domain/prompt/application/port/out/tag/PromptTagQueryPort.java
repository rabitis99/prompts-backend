package org.example.sharedprompts.domain.prompt.application.port.out.tag;

import java.util.List;
import java.util.Map;

/** 프롬프트 태그 조회 아웃바운드 포트. 단건/배치 모두 promptId 기준으로 조회한다. */
public interface PromptTagQueryPort {

    List<String> getTagNames(Long promptId);

    Map<Long, List<String>> getTagNamesByPromptIds(List<Long> promptIds);
}

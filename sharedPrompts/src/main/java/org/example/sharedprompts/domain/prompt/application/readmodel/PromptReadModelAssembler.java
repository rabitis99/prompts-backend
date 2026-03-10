package org.example.sharedprompts.domain.prompt.application.readmodel;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.port.out.like.LikeCountPort;
import org.example.sharedprompts.domain.prompt.application.port.out.tag.PromptTagQueryPort;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 목록 조회용 태그·좋아요 수 일괄 조립 (N+1 방지) */
@Component
@RequiredArgsConstructor
public class PromptReadModelAssembler {

    private final PromptTagQueryPort promptTagQueryPort;
    private final LikeCountPort likeCountPort;

    public AssembledReadModel assemble(List<Prompt> prompts) {
        List<Long> promptIds = prompts.stream().map(Prompt::getId).toList();
        Map<Long, List<String>> tagNamesByPromptId = promptTagQueryPort.getTagNamesByPromptIds(promptIds);
        Map<Long, Long> likeCountByPromptId = likeCountPort.getPromptLikeCounts(promptIds);
        return new AssembledReadModel(tagNamesByPromptId, likeCountByPromptId);
    }

    public record AssembledReadModel(
            Map<Long, List<String>> tagNamesByPromptId,
            Map<Long, Long> likeCountByPromptId
    ) {}
}

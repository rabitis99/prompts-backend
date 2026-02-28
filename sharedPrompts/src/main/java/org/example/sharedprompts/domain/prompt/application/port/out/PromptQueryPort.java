package org.example.sharedprompts.domain.prompt.application.port.out;

import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * 프롬프트 조회용 아웃바운드 포트.
 * <p>헥사고날: 서비스는 이 포트에만 의존하며, 영속성 구현(Repository)은 어댑터에 둔다.</p>
 */
public interface PromptQueryPort {

    Optional<Prompt> findById(Long promptId);

    Page<Prompt> searchPrompts(PromptSearchCondition condition, Long viewerId);

    Page<Prompt> searchMyPrompts(Long userId, PromptSearchCondition condition);

    Page<Prompt> searchUserPrompts(Long userId, PromptSearchCondition condition, Long viewerId);
}

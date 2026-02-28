package org.example.sharedprompts.domain.prompt.application.port.out;

import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * 프롬프트 조회용 아웃바운드 포트.
 * <p>헥사고날: 서비스는 이 포트에만 의존하며, 영속성 구현(Repository)은 어댑터에 둔다.</p>
 * <p><b>구현체:</b> {@link org.example.sharedprompts.domain.prompt.adapter.out.PromptPersistenceAdapter}
 * (내부에서 {@link org.example.sharedprompts.domain.prompt.repository.PromptRepository} 사용)</p>
 *
 * <p><b>Spring Data Page 의존성:</b> 반환 타입으로 {@link org.springframework.data.domain.Page}를 사용한다.
 * 순수 헥사고날 관점에서는 도메인·포트가 프레임워크에 무의존인 것이 이상적이나,
 * 페이지네이션 전용 추상화를 도입하면 복잡도가 커진다. 현재는 실용적 트레이드오프로 허용하며,
 * 필요 시 도메인 전용 페이지네이션 타입으로 전환할 수 있다.
 */
public interface PromptQueryPort {

    Optional<Prompt> findById(Long promptId);

    Page<Prompt> searchPrompts(PromptSearchCondition condition, Long viewerId);

    Page<Prompt> searchMyPrompts(Long userId, PromptSearchCondition condition);

    Page<Prompt> searchUserPrompts(Long userId, PromptSearchCondition condition, Long viewerId);
}

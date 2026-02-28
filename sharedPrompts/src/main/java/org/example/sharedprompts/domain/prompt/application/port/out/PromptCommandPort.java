package org.example.sharedprompts.domain.prompt.application.port.out;

import org.example.sharedprompts.domain.prompt.Prompt;

/**
 * 프롬프트 저장·삭제용 아웃바운드 포트.
 * <p>헥사고날: 서비스는 이 포트에만 의존하며, 영속성 구현(Repository)은 어댑터에 둔다.</p>
 * <p><b>구현체:</b> {@link org.example.sharedprompts.domain.prompt.adapter.out.PromptPersistenceAdapter}
 * (내부에서 {@link org.example.sharedprompts.domain.prompt.repository.PromptRepository} 사용)</p>
 */
public interface PromptCommandPort {

    Prompt save(Prompt prompt);

    void delete(Prompt prompt);
}

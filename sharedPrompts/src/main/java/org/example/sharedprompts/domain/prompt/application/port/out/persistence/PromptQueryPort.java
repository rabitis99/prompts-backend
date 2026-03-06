package org.example.sharedprompts.domain.prompt.application.port.out.persistence;

import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * 프롬프트 조회용 아웃바운드 포트.
 *
 * <p>단건 조회와 검색(페이지)을 담당한다.</p>
 */
public interface PromptQueryPort {

    /**
     * ID로 프롬프트를 조회한다. 없으면 빈 Optional.
     */
    Optional<Prompt> findById(Long promptId);

    /**
     * 검색 쿼리에 따라 프롬프트 페이지를 조회한다.
     */
    Page<Prompt> search(PromptSearchQuery query);
}

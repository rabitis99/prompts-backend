package org.example.sharedprompts.domain.prompt.application.port.out.persistence;

import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.springframework.data.domain.Page;

import java.util.Optional;

/** 프롬프트 조회 아웃바운드 포트. 단건·검색(페이지) */
public interface PromptQueryPort {

    Optional<Prompt> findById(Long promptId);

    Page<Prompt> search(PromptSearchQuery query);
}

package org.example.sharedprompts.domain.favorite.repository;

import org.example.sharedprompts.domain.prompt.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomFavoriteRepository {
    Page<Prompt> findPromptsByUserId(Long userId, Pageable pageable);
}


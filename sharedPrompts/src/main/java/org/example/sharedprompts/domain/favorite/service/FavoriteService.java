package org.example.sharedprompts.domain.favorite.service;

import org.example.sharedprompts.dto.favorite.response.FavoriteResponseDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.dto.common.PageResponse;
import org.springframework.data.domain.Pageable;

public interface FavoriteService {
    void addFavorite(Long userId, Long promptId);
    void removeFavorite(Long userId, Long promptId);
    FavoriteResponseDto checkFavorite(Long userId, Long promptId);
    PageResponse<PromptResponseDto> getFavoritePrompts(Long userId, Pageable pageable);
}


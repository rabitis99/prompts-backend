package org.example.sharedprompts.controller.favorite;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.favorite.service.FavoriteService;
import org.example.sharedprompts.dto.favorite.response.FavoriteResponseDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.global.response.CustomResponse;
import org.example.sharedprompts.global.response.CustomResponseHelper;
import org.example.sharedprompts.global.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/prompts/{promptId}/favorites")
    public ResponseEntity<CustomResponse<Void>> addFavorite(
            @PathVariable Long promptId,
            @CurrentUser AuthUser authUser
    ) {
        favoriteService.addFavorite(authUser.getId(), promptId);
        return CustomResponseHelper.ok(null);
    }

    @DeleteMapping("/prompts/{promptId}/favorites")
    public ResponseEntity<CustomResponse<Void>> removeFavorite(
            @PathVariable Long promptId,
            @CurrentUser AuthUser authUser
    ) {
        favoriteService.removeFavorite(authUser.getId(), promptId);
        return CustomResponseHelper.ok(null);
    }

    @GetMapping("/prompts/{promptId}/favorites")
    public ResponseEntity<CustomResponse<FavoriteResponseDto>> checkFavorite(
            @PathVariable Long promptId,
            @CurrentUser AuthUser authUser
    ) {
        FavoriteResponseDto response = favoriteService.checkFavorite(authUser.getId(), promptId);
        return CustomResponseHelper.ok(response);
    }

    @GetMapping("/favorites")
    public ResponseEntity<CustomResponse<PageResponse<PromptResponseDto>>> getFavoritePrompts(
            @CurrentUser AuthUser authUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<PromptResponseDto> response = favoriteService.getFavoritePrompts(authUser.getId(), pageable);
        return CustomResponseHelper.ok(response);
    }
}


package org.example.sharedprompts.domain.favorite.event;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FavoriteEvent {
    // 프롬프트 즐겨찾기 추가
    public record PromptFavorited(Long userId, Long promptId) {}

    // 프롬프트 즐겨찾기 취소
    public record PromptUnfavorited(Long promptId) {}
}


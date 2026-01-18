package org.example.sharedprompts.dto.favorite.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponseDto {

    @JsonProperty("is_favorite")
    private Boolean isFavorite;

    public static FavoriteResponseDto from(boolean isFavorite) {
        return FavoriteResponseDto.builder()
                .isFavorite(isFavorite)
                .build();
    }
}


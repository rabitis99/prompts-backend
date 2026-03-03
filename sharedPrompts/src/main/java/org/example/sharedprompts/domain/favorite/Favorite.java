package org.example.sharedprompts.domain.favorite;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.entity.BaseEntity;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "favorites",
        indexes = {
                // 추가된 인덱스 목록 (우선순위: 권장)
                // 사용자별 즐겨찾기 목록 조회 시 정렬 최적화
                @Index(name = "idx_favorites_user_created_at", columnList = "user_id, created_at"),
                // 프롬프트별 즐겨찾기 조회 시 정렬 최적화
                @Index(name = "idx_favorites_prompt_created_at", columnList = "prompt_id, created_at")
        }
)
public class Favorite extends BaseEntity {

    @EmbeddedId
    private FavoriteId id;

    @MapsId("promptId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id")
    private Prompt prompt;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}


package org.example.sharedprompts.domain.like;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.entity.BaseEntity;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "prompt_likes")
public class PromptLike extends BaseEntity {

    @EmbeddedId
    private PromptLikeId id;

    @MapsId("promptId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id")
    private Prompt prompt;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
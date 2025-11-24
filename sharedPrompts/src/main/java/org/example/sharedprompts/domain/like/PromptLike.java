package org.example.sharedprompts.domain.like;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.user.User;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "prompt_likes")
public class PromptLike {

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
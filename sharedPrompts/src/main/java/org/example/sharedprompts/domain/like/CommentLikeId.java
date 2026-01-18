package org.example.sharedprompts.domain.like;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CommentLikeId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long commentId;
    private Long userId;
}

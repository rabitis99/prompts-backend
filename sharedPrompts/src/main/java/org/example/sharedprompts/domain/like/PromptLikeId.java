package org.example.sharedprompts.domain.like;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PromptLikeId implements Serializable{

    private Long promptId;
    private Long userId;
}

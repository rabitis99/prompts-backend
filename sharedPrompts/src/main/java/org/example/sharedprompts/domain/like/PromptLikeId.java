package org.example.sharedprompts.domain.like;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PromptLikeId implements Serializable{

    private static final long serialVersionUID = 1L;

    private Long promptId;
    private Long userId;
}

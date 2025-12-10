package org.example.sharedprompts.domain.tag;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.Prompt;

@Entity
@Getter
public class PromptTag {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Prompt prompt;

    @ManyToOne(fetch = FetchType.LAZY)
    private Tag tag;

    public PromptTag(Prompt prompt, Tag tag) {
        this.prompt = prompt;
        this.tag = tag;
    }

    public PromptTag() {

    }
}
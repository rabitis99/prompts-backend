package org.example.sharedprompts.domain.Tag;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.Prompt;

@Entity
@Getter
public class PromptTag {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Prompt prompt;

    @ManyToOne
    private Tag tag;

    public PromptTag(Prompt prompt, Tag tag) {
        this.prompt = prompt;
        this.tag = tag;
    }

    public PromptTag() {

    }
}
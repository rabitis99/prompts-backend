package org.example.sharedprompts.domain.prompt;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class PromptTag {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Prompt prompt;

    @ManyToOne
    private Tag tag;
}